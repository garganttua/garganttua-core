package com.garganttua.core.aot.nativve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.garganttua.core.aot.commons.AOTRegistry;
import com.garganttua.core.aot.reflection.AOTClass;
import com.garganttua.core.aot.reflection.AOTParameterizedType;
import com.garganttua.core.aot.reflection.AOTReflectionProvider;
import com.garganttua.core.reflection.IClass;

/**
 * GraalVM native-image {@link Feature} that mirrors every AOT descriptor
 * shipped by the framework / consumer into the closed-world reflection
 * configuration at analysis time. Removes the need for hand-written or
 * mojo-generated {@code reflect-config.json} on top of the consumer-side AOT
 * pipeline — the same source of truth ({@link AOTRegistry}) drives both the
 * runtime IClass lookup and the native-image reflection registration.
 *
 * <p>Auto-activated through
 * {@code META-INF/native-image/com.garganttua.core/garganttua-aot-native-feature/native-image.properties}
 * so consumers only need to add {@code garganttua-starter-native} to the
 * classpath plus the GraalVM build plugin in their pom.
 *
 * @since 2.0.0-ALPHA02
 */
public class GarganttuaAotFeature implements Feature {

    /**
     * Returns the human-readable label shown by native-image when this feature
     * is active.
     *
     * @return a short description of what the feature registers
     */
    @Override
    public String getDescription() {
        return "Registers garganttua-core AOT descriptors with RuntimeReflection";
    }

    /**
     * Mirrors every AOT descriptor and every {@code @Reflected}-indexed class
     * into the native-image reflection configuration before closed-world
     * analysis begins.
     *
     * <p>Triggers {@link AOTReflectionProvider}'s static initialiser to populate
     * the {@link AOTRegistry}, marks the descriptor classes as
     * initialize-at-build-time, registers each resolved class plus its members
     * with {@link RuntimeReflection}, then performs a second pass over the
     * compile-time {@code @Reflected} index. Unresolvable class names are
     * skipped rather than failing the build.
     *
     * @param access the native-image build-time access used to resolve classes
     *               and the application class loader
     * @throws IllegalStateException if {@link AOTReflectionProvider} is absent
     *               from the native-image build classpath
     */
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // Trigger AOTReflectionProvider's <clinit>, which runs
        // CoreInfrastructureSeed.bootstrap(). The seed both pre-registers
        // the framework infrastructure interfaces AND walks the classpath
        // (via ServiceLoader on IAOTSelfRegistering) to fire each
        // AOTClass_*'s static initialiser, populating the registry.
        try {
            Class.forName(AOTReflectionProvider.class.getName(), true,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "AOTReflectionProvider must be on the native-image build classpath", e);
        }

        // The Class.forName above already ran AOTReflectionProvider.<clinit>,
        // which transitively initialised every AOTClass_*/AOTMethod_*/AOTField_*/
        // AOTConstructor_* descriptor (their INSTANCE fields). Those classes are
        // therefore initialised AT BUILD TIME — so they must be declared as such,
        // or native-image's default "initialize app classes at runtime" policy
        // fails with "Classes that should be initialized at run time got
        // initialized during image building". We target ONLY the AOT descriptor
        // classes (a blanket --initialize-at-build-time=com.garganttua breaks
        // ExpressionUtils / IOperationRequest, whose <clinit> must stay runtime).
        RuntimeClassInitialization.initializeAtBuildTime(AOTReflectionProvider.class);
        // AOTField_* descriptors now hold an AOTParameterizedType instance in
        // their genericType field (AOTFieldSourceGenerator emits
        // AOTParameterizedType.of(...) for parameterized fields). Those
        // instances reach the image heap when the AOTField_* INSTANCE is
        // initialised at build time, so AOTParameterizedType itself must be
        // initialize-at-build-time too — otherwise native-image (notably the
        // stricter heap check in GraalVM 25) rejects the reachable object.
        RuntimeClassInitialization.initializeAtBuildTime(AOTParameterizedType.class);

        int classCount = 0;
        int memberCount = 0;
        int descriptorInitCount = 0;
        for (String fqn : AOTRegistry.getInstance().registeredClasses()) {
            try {
                Class<?> clazz = access.findClassByName(fqn);
                if (clazz == null) {
                    // Could not be resolved by name lookup — skip rather than
                    // fail the whole build, the user's reflect-config.json
                    // can still cover the gap if needed.
                    continue;
                }
                RuntimeReflection.register(clazz);
                classCount++;
                memberCount += registerMembers(clazz);
                descriptorInitCount += initializeDescriptorAtBuildTime(
                        AOTRegistry.getInstance().get(fqn).orElse(null));
            } catch (RuntimeException e) {
                System.err.println("[GarganttuaAotFeature] Failed to register " + fqn + ": " + e.getMessage());
            }
        }
        // Second pass: register every @Reflected class for native reflection
        // from the compile-time @Reflected index. This is the layering-clean,
        // universal mechanism — the index (META-INF/garganttua/index/...Reflected)
        // is emitted by IndexedAnnotationProcessor in EVERY module (a resource
        // file, no aot-reflection dependency, no -Dgarganttua.direct.binders),
        // so it covers framework classes that ship no generated AOTClass_*
        // descriptor (e.g. ExpressionContext, whose man() built-in is resolved
        // reflectively and would otherwise throw NoSuchMethodException in the
        // closed world). Registering the class + members makes the live-
        // reflection fallback in AOTClass work natively.
        int reflectedCount = registerReflectedClassesFromIndex(access);

        System.out.println("[GarganttuaAotFeature] Registered " + classCount
                + " AOT descriptor classes (" + memberCount + " members) with RuntimeReflection; "
                + descriptorInitCount + " descriptor classes marked initialize-at-build-time; "
                + reflectedCount + " @Reflected classes registered from the annotation index.");
    }

    private static final String REFLECTED_INDEX_RESOURCE =
            "META-INF/garganttua/index/com.garganttua.core.reflection.annotations.Reflected";

    /**
     * Reads every {@code @Reflected} index file on the application classpath
     * and registers each listed class (plus its members) for native
     * reflection. Index lines are {@code C:<fqn>} (class) or
     * {@code M:<class>#<method>(<params>)} (member) — for the latter we
     * register the declaring class. Unresolvable names are skipped.
     */
    private static int registerReflectedClassesFromIndex(BeforeAnalysisAccess access) {
        Set<String> classNames = new LinkedHashSet<>();
        try {
            Enumeration<URL> urls = access.getApplicationClassLoader().getResources(REFLECTED_INDEX_RESOURCE);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String fqn = classFqnFromIndexLine(line.trim());
                        if (fqn != null) {
                            classNames.add(fqn);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[GarganttuaAotFeature] Failed reading @Reflected index: " + e.getMessage());
        }

        int registered = 0;
        for (String fqn : classNames) {
            try {
                Class<?> clazz = access.findClassByName(fqn);
                if (clazz == null) {
                    continue;
                }
                RuntimeReflection.register(clazz);
                registerMembers(clazz);
                registered++;
            } catch (RuntimeException | LinkageError e) {
                System.err.println("[GarganttuaAotFeature] @Reflected index: failed to register "
                        + fqn + ": " + e.getMessage());
            }
        }
        return registered;
    }

    /** Extracts the class FQN from a {@code C:}/{@code M:} index line, or null. */
    private static String classFqnFromIndexLine(String line) {
        if (line.isEmpty()) {
            return null;
        }
        if (line.startsWith("C:")) {
            String fqn = line.substring(2).trim();
            return fqn.isEmpty() ? null : fqn;
        }
        if (line.startsWith("M:")) {
            String body = line.substring(2).trim();
            int hash = body.indexOf('#');
            String fqn = (hash >= 0 ? body.substring(0, hash) : body).trim();
            return fqn.isEmpty() ? null : fqn;
        }
        return null;
    }

    /**
     * Eagerly register every declared constructor / method / field for
     * reflective access. Conservative on purpose: native-image is happiest
     * with explicit registration even when a constructor is never invoked
     * reflectively. The cost is a slight image-size increase, never a
     * correctness issue.
     */
    /**
     * Declare a descriptor's own class and its pre-generated member descriptor
     * classes ({@code AOTMethod_*}/{@code AOTField_*}/{@code AOTConstructor_*})
     * as initialize-at-build-time, matching the fact that they were already
     * initialised when {@code AOTReflectionProvider.<clinit>} ran. Returns the
     * number of classes marked. {@code null} descriptor (registry miss) is a
     * no-op.
     */
    private static int initializeDescriptorAtBuildTime(IClass<?> descriptor) {
        if (descriptor == null) {
            return 0;
        }
        // Use AOTClass's RAW backing arrays, not getDeclared*(): those fall back
        // to live-reflection synthesis for shallow descriptors and for any class
        // with a single constructor, returning synthesized instances whose class
        // is the base AOTConstructor rather than the generated AOTConstructor_X_0
        // that actually sits in the image heap.
        if (descriptor instanceof AOTClass<?> aot) {
            java.util.List<Class<?>> classes = aot.descriptorClassesForBuildTimeInit();
            for (Class<?> c : classes) {
                RuntimeClassInitialization.initializeAtBuildTime(c);
            }
            return classes.size();
        }
        RuntimeClassInitialization.initializeAtBuildTime(descriptor.getClass());
        return 1;
    }

    private static int registerMembers(Class<?> clazz) {
        int count = 0;
        try {
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            RuntimeReflection.register(ctors);
            count += ctors.length;
        } catch (LinkageError | RuntimeException ignored) {
            // primitive types, sealed-hidden classes, etc.
        }
        try {
            Method[] methods = clazz.getDeclaredMethods();
            RuntimeReflection.register(methods);
            count += methods.length;
        } catch (LinkageError | RuntimeException ignored) {
            // ignored
        }
        try {
            Field[] fields = clazz.getDeclaredFields();
            RuntimeReflection.register(fields);
            count += fields.length;
        } catch (LinkageError | RuntimeException ignored) {
            // ignored
        }
        return count;
    }
}
