package com.garganttua.core.runtime;

import java.util.Map;
import java.util.UUID;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating runtime child contexts.
 *
 * <p>
 * This factory is automatically detected and registered during the auto-detection phase
 * thanks to the {@link ChildContext} annotation.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
@ChildContext
public class RuntimeContextFactory implements IInjectionChildContextFactory<IRuntimeContext<?, ?>> {

    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeContext<?, ?> createChildContext(IInjectionContext parent, Object... args) throws DiException {
        log.atTrace().log("[RuntimeContextFactory.createChildContext] Entering createChildContext with parent={} and args={}", parent, args);

        Object input = args[0];
        Class<?> outputType = (Class<?>) args[1];
        Map<String, ISupplier<?>> presetVariables = (Map<String, ISupplier<?>>) args[2];
        UUID uuid = (UUID) args[3];

        log.atDebug().log("[RuntimeContextFactory.createChildContext] Creating RuntimeContext with input={}, outputType={}, presetVariables={}", input, outputType, presetVariables);
        IRuntimeContext<?, ?> context = new RuntimeContext<>(parent, input, outputType, presetVariables, uuid);
        log.atDebug().log("[RuntimeContextFactory.createChildContext] RuntimeContext created with uuid={}", context.uuid());

        return context;
    }

}
