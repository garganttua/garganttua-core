package com.garganttua.core.runtime.aot;

import com.garganttua.core.aot.commons.IAOTInfrastructureSeed;
import com.garganttua.core.aot.commons.IAOTSeedContext;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.RuntimeContext;
import com.garganttua.core.runtime.RuntimeContextFactory;
import com.garganttua.core.runtime.RuntimeProcess;
import com.garganttua.core.runtime.RuntimeResult;
import com.garganttua.core.runtime.RuntimesRegistry;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepPipe;

/**
 * Pre-registers the runtime module's concrete-class surface into the AOT
 * registry on cold start. Discovered via
 * {@code META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed}.
 *
 * <p>Companion to {@code CoreInfrastructureSeed} which already covers the
 * {@code I*} interface side. This seed adds the concrete implementations
 * that framework wiring resolves at static-init time —
 * {@link RuntimeContextFactory} (referenced by
 * {@code @ChildContext(factory=RuntimeContextFactory.class)} on
 * {@code IRuntimeContext}), {@link Runtime} itself, the step
 * implementations, etc.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public class RuntimeInfrastructureSeed implements IAOTInfrastructureSeed {

    /**
     * Registers the runtime module's concrete classes into the AOT seed context.
     *
     * @param ctx the seed context that collects classes for native reflection registration
     */
    @Override
    public void seed(IAOTSeedContext ctx) {
        ctx.registerClass(Runtime.class);
        ctx.registerClass(RuntimeContext.class);
        ctx.registerClass(RuntimeContextFactory.class);
        ctx.registerClass(RuntimeProcess.class);
        ctx.registerClass(RuntimeResult.class);
        ctx.registerClass(RuntimesRegistry.class);
        ctx.registerClass(RuntimeStep.class);
        ctx.registerClass(RuntimeStepCatch.class);
        ctx.registerClass(RuntimeStepFallbackBinder.class);
        ctx.registerClass(RuntimeStepMethodBinder.class);
        ctx.registerClass(RuntimeStepOnException.class);
        ctx.registerClass(RuntimeStepPipe.class);
    }
}
