package com.garganttua.core.observability.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory that lets {@code Bootstrap.autoDetect(true)} discover and
 * register an {@link ObservabilityBuilder} without explicit wiring.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservabilityBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * Create a fresh {@link ObservabilityBuilder} for Bootstrap to register.
     *
     * @return a new, empty observability builder
     * @throws DslException if the builder cannot be instantiated
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return ObservabilityBuilder.create();
    }
}
