package com.garganttua.core.classloader;

import java.util.List;

/**
 * Callback invoked by {@link IClassLoaderManager} every time a JAR is loaded
 * into the classpath. Implementations receive the list of packages declared in
 * the JAR's manifest and decide what to do — typically register them in a
 * builder and trigger a rebuild.
 *
 * @since 2.0.0-ALPHA02
 */
@FunctionalInterface
public interface IClassLoaderRebuildHook {

    /**
     * Invoked after a JAR has been loaded into the classpath.
     *
     * @param packages packages discovered in the JAR's manifest (may be empty)
     * @throws Exception any error; the manager wraps it in
     *                   {@link ClassLoaderException}
     */
    void onJarLoaded(List<String> packages) throws Exception;
}
