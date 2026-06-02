package com.garganttua.core.aot.reflection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTRegistry;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.aot.commons.IAOTSelfRegistering;

import jakarta.annotation.Priority;

/**
 * Discovers and force-loads AOT descriptors on startup, on behalf of
 * {@link CoreInfrastructureSeed}. Two concerns extracted out of the seed to
 * keep it focused on the static type registration list:
 * <ul>
 *   <li>{@link #runExtensionSeeds()} — runs higher-layer {@link IAOTInfrastructureSeed}s
 *       discovered via {@link java.util.ServiceLoader}, ordered by {@link Priority}.</li>
 *   <li>{@link #loadGeneratedDescriptors()} — triggers the static initialisers of every
 *       generated {@code AOTClass_*} (ServiceLoader on {@link IAOTSelfRegistering}, plus a
 *       legacy classpath walk under {@code META-INF/garganttua/aot/classes/}).</li>
 * </ul>
 */
final class GeneratedDescriptorLoader {

    private static final String AOT_CLASSES_DIR = "META-INF/garganttua/aot/classes/";

    private GeneratedDescriptorLoader() {
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : GeneratedDescriptorLoader.class.getClassLoader();
    }

    /**
     * Runs every {@link IAOTInfrastructureSeed} discovered via ServiceLoader,
     * sorted by {@link Priority} (higher first, default 0). Exceptions in one
     * seed don't prevent others from running — they're logged to stderr.
     */
    static void runExtensionSeeds() {
        ClassLoader cl = contextClassLoader();
        IAOTSeedContext ctx = new SeedContext(AOTRegistry.getInstance());
        try {
            List<IAOTInfrastructureSeed> seeds = new ArrayList<>();
            for (IAOTInfrastructureSeed seed : java.util.ServiceLoader.load(IAOTInfrastructureSeed.class, cl)) {
                seeds.add(seed);
            }
            seeds.sort(Comparator.comparingInt(GeneratedDescriptorLoader::readPriority).reversed());
            for (IAOTInfrastructureSeed seed : seeds) {
                try {
                    seed.seed(ctx);
                } catch (Throwable t) {
                    System.err.println("[GeneratedDescriptorLoader] Extension seed "
                            + seed.getClass().getName() + " failed: " + t);
                }
            }
        } catch (java.util.ServiceConfigurationError e) {
            System.err.println("[GeneratedDescriptorLoader] ServiceLoader for IAOTInfrastructureSeed failed: "
                    + e.getMessage());
        }
    }

    private static int readPriority(Object svc) {
        Priority p = svc.getClass().getAnnotation(Priority.class);
        return p == null ? 0 : p.value();
    }

    /**
     * Adapts the static seed helpers of {@link CoreInfrastructureSeed} to the
     * public {@link IAOTSeedContext} contract so extension seeds don't need to
     * know about {@link AOTClass}.
     */
    private record SeedContext(IAOTRegistry registry) implements IAOTSeedContext {
        @Override
        public void registerClass(Class<?> type) {
            CoreInfrastructureSeed.registerClass(type);
        }
        @Override
        public void registerInterface(Class<?> type) {
            CoreInfrastructureSeed.registerInterface(type);
        }
    }

    /**
     * Forces every AOT-generated descriptor's static initialiser to run, which
     * is what registers it into {@link AOTRegistry}.
     *
     * <p>Two mechanisms in order of preference, both run on every startup so
     * we tolerate jars built with the old packaging:
     * <ol>
     *   <li><strong>ServiceLoader</strong> on {@link IAOTSelfRegistering} —
     *       GraalVM-native-image-friendly out of the box (the processor writes
     *       {@code META-INF/services/...IAOTSelfRegistering} per JAR).</li>
     *   <li><strong>Legacy classpath walk</strong> under
     *       {@code META-INF/garganttua/aot/classes/} via {@code Class.forName}
     *       — NOT native-friendly but keeps old jars working on the JVM.</li>
     * </ol>
     */
    static void loadGeneratedDescriptors() {
        ClassLoader cl = contextClassLoader();
        // 1) ServiceLoader-based path (native-friendly, recommended).
        try {
            java.util.ServiceLoader<IAOTSelfRegistering> loader =
                    java.util.ServiceLoader.load(IAOTSelfRegistering.class, cl);
            // Iterator triggers class-load + <clinit> on each provider; the
            // returned instance itself is unused (each <clinit> already
            // registered into AOTRegistry).
            for (IAOTSelfRegistering ignored : loader) {
                // intentionally empty
            }
        } catch (java.util.ServiceConfigurationError e) {
            System.err.println("[GeneratedDescriptorLoader] ServiceLoader for IAOTSelfRegistering failed: "
                    + e.getMessage());
        }
        // 2) Legacy classpath walk — keeps pre-ServiceLoader-packaging jars
        //    working. No-op when the META-INF/garganttua/aot/classes/ dir is
        //    absent. NOT native-friendly; relies on dynamic Class.forName.
        try {
            Enumeration<URL> roots = cl.getResources(AOT_CLASSES_DIR);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                processRoot(root, cl);
            }
        } catch (IOException e) {
            System.err.println("[GeneratedDescriptorLoader] Failed to enumerate "
                    + AOT_CLASSES_DIR + ": " + e.getMessage());
        }
    }

    private static void processRoot(URL root, ClassLoader cl) {
        String protocol = root.getProtocol();
        try {
            if ("jar".equals(protocol)) {
                processJarRoot(root, cl);
            } else if ("file".equals(protocol)) {
                processFileRoot(root, cl);
            }
            // Other protocols (jrt:, custom) deliberately ignored — they're
            // rare in classpath-only application scenarios.
        } catch (Exception e) {
            System.err.println("[GeneratedDescriptorLoader] Failed to process root "
                    + root + ": " + e.getMessage());
        }
    }

    private static void processJarRoot(URL root, ClassLoader cl) throws IOException {
        URLConnection conn = root.openConnection();
        if (!(conn instanceof JarURLConnection jar)) {
            return;
        }
        try (JarFile jarFile = jar.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith(AOT_CLASSES_DIR)) {
                    continue;
                }
                // Skip the bare directory entry.
                if (name.length() <= AOT_CLASSES_DIR.length()) {
                    continue;
                }
                try (InputStream in = jarFile.getInputStream(e)) {
                    loadEntry(in, cl, name);
                }
            }
        }
    }

    private static void processFileRoot(URL root, ClassLoader cl) throws IOException {
        Path dir;
        try {
            dir = Path.of(root.toURI());
        } catch (Exception e) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.forEach(p -> {
                try (InputStream in = Files.newInputStream(p)) {
                    loadEntry(in, cl, p.toString());
                } catch (IOException ioe) {
                    System.err.println("[GeneratedDescriptorLoader] Failed to read "
                            + p + ": " + ioe.getMessage());
                }
            });
        }
    }

    private static void loadEntry(InputStream in, ClassLoader cl, String source) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String fqn = line.trim();
                if (fqn.isEmpty() || fqn.startsWith("#")) {
                    continue;
                }
                try {
                    // Class.forName with initialize=true is the whole point:
                    // it triggers the AOTClass_*'s static init which calls
                    // AOTRegistry.register().
                    Class.forName(fqn, true, cl);
                } catch (ClassNotFoundException | LinkageError ex) {
                    System.err.println("[GeneratedDescriptorLoader] Could not load AOT descriptor "
                            + fqn + " (from " + source + "): " + ex.getMessage());
                }
            }
        } catch (IOException ioe) {
            System.err.println("[GeneratedDescriptorLoader] Failed to read entry "
                    + source + ": " + ioe.getMessage());
        }
    }
}
