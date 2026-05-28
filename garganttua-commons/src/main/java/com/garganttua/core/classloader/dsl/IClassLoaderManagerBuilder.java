package com.garganttua.core.classloader.dsl;

import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.dsl.IBuilder;

/**
 * Builder interface for {@link IClassLoaderManager}. Bootstrap-discoverable —
 * see the implementation in {@code garganttua-classloader} and its SPI factory.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IClassLoaderManagerBuilder extends IBuilder<IClassLoaderManager> {
}
