package com.garganttua.core.runtime;

import java.util.Map;
import java.util.UUID;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeContextFactory implements IDiChildContextFactory<IRuntimeContext<?, ?>> {

    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeContext<?, ?> createChildContext(IDiContext parent, Object... args) throws DiException {
        log.atTrace().log("[RuntimeContextFactory.createChildContext] Entering createChildContext with parent={} and args={}", parent, args);

        Object input = args[0];
        Class<?> outputType = (Class<?>) args[1];
        Map<String, ISupplier<?>> presetVariables = (Map<String, ISupplier<?>>) args[2];
        UUID uuid = (UUID) args[3];

        log.atDebug().log("[RuntimeContextFactory.createChildContext] Creating RuntimeContext with input={}, outputType={}, presetVariables={}", input, outputType, presetVariables);
        IRuntimeContext<?, ?> context = new RuntimeContext<>(parent, input, outputType, presetVariables, uuid);
        log.atInfo().log("[RuntimeContextFactory.createChildContext] RuntimeContext created with uuid={}", context.uuid());

        return context;
    }

}
