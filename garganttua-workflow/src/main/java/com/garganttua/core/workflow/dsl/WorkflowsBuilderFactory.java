package com.garganttua.core.workflow.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;

/**
 * SPI factory letting {@code Bootstrap.autoDetect(true).load()} discover the
 * plural {@link WorkflowsBuilder} without explicit wiring. Replaces the older
 * {@code WorkflowBuilderFactory} — the singular {@link WorkflowBuilder} is no
 * longer Bootstrap-discoverable on its own.
 *
 * <p>Listed in
 * {@code META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class WorkflowsBuilderFactory implements IBootstrapBuilderFactory {

    @Override
    public IBuilder<?> create() throws DslException {
        return WorkflowsBuilder.builder();
    }
}
