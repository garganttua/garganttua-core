package com.garganttua.core.classloader.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory letting {@code Bootstrap.autoDetect(true).load()} discover the
 * {@link ClassLoaderManagerBuilder}.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ClassLoaderManagerBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * Creates a new {@link ClassLoaderManagerBuilder} for Bootstrap auto-detection.
     *
     * @return a fresh class loader manager builder
     * @throws DslException if the builder cannot be created
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return ClassLoaderManagerBuilder.builder();
    }
}
