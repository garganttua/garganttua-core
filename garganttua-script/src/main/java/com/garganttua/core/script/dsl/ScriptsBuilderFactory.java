package com.garganttua.core.script.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory letting {@code Bootstrap.autoDetect(true).load()} discover the
 * {@link ScriptsBuilder}.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ScriptsBuilderFactory implements IBootstrapBuilderFactory {

    /**
     * Creates a fresh {@link ScriptsBuilder} for the bootstrap auto-detection chain.
     *
     * @return a new {@code ScriptsBuilder} instance
     * @throws DslException if the builder cannot be created
     */
    @Override
    public IBuilder<?> create() throws DslException {
        return ScriptsBuilder.builder();
    }
}
