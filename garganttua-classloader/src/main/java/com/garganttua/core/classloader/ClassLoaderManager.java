package com.garganttua.core.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.garganttua.core.observability.Logger;

/**
 * Default {@link IClassLoaderManager} implementation. Adds JARs to the thread
 * context classloader (chaining {@link URLClassLoader} instances), reads the
 * {@code Garganttua-Packages} manifest attribute, and fires registered hooks
 * with the discovered package list.
 *
 * <p>Thread safety: {@code loadJar} is safe under concurrent callers
 * (CopyOnWriteArrayList hook list, contextClassLoader swap is per-thread).
 *
 * @since 2.0.0-ALPHA02
 */
public class ClassLoaderManager implements IClassLoaderManager {

    private static final Logger log = Logger.getLogger(ClassLoaderManager.class);

    private final CopyOnWriteArrayList<IClassLoaderRebuildHook> hooks = new CopyOnWriteArrayList<>();

    /**
     * Loads the given JAR onto the current thread's context classloader, then
     * reads its {@code Garganttua-Packages} manifest attribute and fires every
     * registered {@link IClassLoaderRebuildHook} with the discovered package
     * list. Hooks are only fired when at least one package is declared.
     *
     * @param jar the path to the JAR file to load
     * @throws ClassLoaderException if the JAR is missing, its URL cannot be
     *         resolved, or a rebuild hook fails
     */
    @Override
    public void loadJar(Path jar) throws ClassLoaderException {
        Objects.requireNonNull(jar, "jar path cannot be null");
        if (!Files.exists(jar)) {
            throw new ClassLoaderException("JAR file not found: " + jar);
        }
        URL jarUrl;
        try {
            jarUrl = jar.toUri().toURL();
        } catch (Exception e) {
            throw new ClassLoaderException("Failed to convert JAR path to URL: " + jar, e);
        }

        URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl},
                Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(cl);
        log.debug("JAR loaded onto thread context classloader: {}", jar);

        List<String> packages = JarManifestReader.getPackages(jarUrl);
        if (packages.isEmpty()) {
            log.debug("JAR {} has no Garganttua-Packages manifest attribute; hooks not fired", jar);
            return;
        }

        log.debug("JAR {} declared {} package(s); firing {} hook(s)", jar, packages.size(), hooks.size());
        for (IClassLoaderRebuildHook h : hooks) {
            try {
                h.onJarLoaded(packages);
            } catch (Exception e) {
                throw new ClassLoaderException(
                        "Rebuild hook " + h.getClass().getName() + " failed after loading " + jar, e);
            }
        }
    }

    /**
     * Registers a hook to be invoked whenever a loaded JAR declares packages.
     *
     * @param hook the rebuild hook to register
     */
    @Override
    public void addRebuildHook(IClassLoaderRebuildHook hook) {
        Objects.requireNonNull(hook, "hook cannot be null");
        this.hooks.add(hook);
    }

    /**
     * Returns an immutable snapshot of the currently registered rebuild hooks.
     *
     * @return an immutable copy of the registered hooks
     */
    @Override
    public List<IClassLoaderRebuildHook> getRebuildHooks() {
        return List.copyOf(this.hooks);
    }
}
