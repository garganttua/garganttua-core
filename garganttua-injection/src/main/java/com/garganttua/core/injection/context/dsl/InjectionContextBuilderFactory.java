package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory that lets {@code Bootstrap.autoDetect(true)} discover and
 * register an {@link InjectionContextBuilder} without explicit wiring.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class InjectionContextBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * {@return a fresh {@link InjectionContextBuilder}}
     *
     * @throws DslException if the builder cannot be created
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return InjectionContextBuilder.builder();
    }
}
