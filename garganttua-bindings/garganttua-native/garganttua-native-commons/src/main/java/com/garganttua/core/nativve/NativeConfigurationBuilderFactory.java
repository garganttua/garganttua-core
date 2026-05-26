package com.garganttua.core.nativve;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory that lets {@code Bootstrap.autoDetect(true)} discover and
 * register a {@link NativeConfigurationBuilder} without explicit wiring.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class NativeConfigurationBuilderFactory implements IBootstrapBuilderFactory {

    @Override
    public IBuilder<?> create() throws DslException {
        return NativeConfigurationBuilder.builder();
    }
}
