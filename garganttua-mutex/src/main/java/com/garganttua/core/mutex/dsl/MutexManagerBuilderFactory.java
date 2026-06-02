package com.garganttua.core.mutex.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory that lets {@code Bootstrap.autoDetect(true)} discover and
 * register a {@link MutexManagerBuilder} without explicit wiring.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class MutexManagerBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * Creates a fresh {@link MutexManagerBuilder} for the bootstrap to configure
     * and build during auto-detection.
     *
     * @return a new {@link MutexManagerBuilder} instance
     * @throws DslException if the builder cannot be created
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return MutexManagerBuilder.builder();
    }
}
