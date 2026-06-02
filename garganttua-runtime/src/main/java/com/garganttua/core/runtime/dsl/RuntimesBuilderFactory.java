package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory that lets {@code Bootstrap.autoDetect(true)} discover and
 * register a {@link RuntimesBuilder} without explicit wiring.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class RuntimesBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * Creates a new {@link RuntimesBuilder} for bootstrap auto-detection.
     *
     * @return a fresh runtimes builder
     * @throws DslException if the builder cannot be created
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return RuntimesBuilder.builder();
    }
}
