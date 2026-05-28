package com.garganttua.core.classloader;

import java.nio.file.Path;
import java.util.List;

/**
 * Bootstrap-discoverable manager responsible for loading JAR files into the
 * thread context classloader at runtime and notifying registered hooks so
 * downstream consumers (e.g. {@code Bootstrap.rebuild()}) can react to newly
 * available types.
 *
 * <p>Lives outside of {@code garganttua-bootstrap} on purpose: the bootstrap
 * module is the build orchestrator, not the runtime classpath manager. This
 * interface decouples the two responsibilities so a script's {@code include()}
 * (or any other JAR consumer) only needs to know about an
 * {@code IClassLoaderManager} — never about {@code IBootstrap}.
 *
 * <p>The default implementation in {@code garganttua-classloader} fires its
 * hooks with the list of packages declared in the loaded JAR's manifest
 * (Garganttua-Packages attribute), so that {@code Bootstrap} can add them via
 * {@code withPackage(...)} and then {@code rebuild()}.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IClassLoaderManager {

    /**
     * Add the JAR at {@code jar} to the thread context classloader and fire
     * every registered {@link IClassLoaderRebuildHook hook} with the packages
     * declared in the JAR's manifest. No-op (but logged) when the manifest has
     * no {@code Garganttua-Packages} attribute.
     *
     * @param jar absolute path to a JAR file on disk
     * @throws ClassLoaderException if the JAR cannot be opened, the manifest
     *                              cannot be read, or a hook fails
     */
    void loadJar(Path jar) throws ClassLoaderException;

    /**
     * Register a hook fired by every subsequent {@link #loadJar(Path)} call.
     * Hooks are invoked in registration order; failures propagate as a
     * {@link ClassLoaderException}.
     */
    void addRebuildHook(IClassLoaderRebuildHook hook);

    /**
     * Snapshot of hooks currently registered. Useful for diagnostics.
     */
    List<IClassLoaderRebuildHook> getRebuildHooks();
}
