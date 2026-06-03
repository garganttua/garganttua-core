package com.garganttua.core.classloader.dsl;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.classloader.ClassLoaderManager;
import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder that produces a vanilla {@link IClassLoaderManager} with no hooks
 * registered. Bootstrap-discoverable via {@link ClassLoaderManagerBuilderFactory};
 * the surrounding application (typically Bootstrap itself, post-build) is
 * responsible for registering a rebuild hook on the built manager.
 *
 * <p>Builds cleanly with zero configuration — necessary so a Bootstrap that
 * auto-detects the manager but isn't using JAR hot-loading doesn't block.
 *
 * @since 2.0.0-ALPHA02
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("classloader")
public class ClassLoaderManagerBuilder implements IClassLoaderManagerBuilder {

    private static final Logger log = Logger.getLogger(ClassLoaderManagerBuilder.class);

    private ClassLoaderManagerBuilder() {
        log.trace("ClassLoaderManagerBuilder created");
    }

    /**
     * Creates a new, unconfigured builder instance.
     *
     * @return a fresh {@link IClassLoaderManagerBuilder}
     */
    public static IClassLoaderManagerBuilder builder() {
        return new ClassLoaderManagerBuilder();
    }

    /**
     * Builds a {@link ClassLoaderManager} with no hooks registered.
     *
     * @return the built {@link IClassLoaderManager}
     * @throws DslException if construction fails
     */
    @Override
    public IClassLoaderManager build() throws DslException {
        log.debug("Building ClassLoaderManager");
        return new ClassLoaderManager();
    }
}
