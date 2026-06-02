package com.garganttua.core.runtime;

import java.util.Map;
import java.util.UUID;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.supply.ISupplier;

import com.garganttua.core.reflection.annotations.Reflected;
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
@ChildContext
@Reflected(queryAllDeclaredConstructors = true, queryAllDeclaredMethods = true)
public class RuntimeContextFactory implements IInjectionChildContextFactory<IRuntimeContext<?, ?>> {
    private static final Logger log = Logger.getLogger(RuntimeContextFactory.class);

    /**
     * Creates a runtime child context from the supplied positional arguments.
     *
     * @param parent the parent injection context the runtime context is derived from
     * @param args positional arguments in the order: input object, output {@code Class},
     *        preset variables map ({@code Map<String, ISupplier<?>>}) and execution {@link UUID}
     * @return a new {@link IRuntimeContext} bound to {@code parent}
     * @throws DiException if the child context cannot be created
     */
    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeContext<?, ?> createChildContext(IInjectionContext parent, Object... args) throws DiException {
        log.trace("[RuntimeContextFactory.createChildContext] Entering createChildContext with parent={} and args={}", parent, args);

        Object input = args[0];
        Class<?> outputType = (Class<?>) args[1];
        Map<String, ISupplier<?>> presetVariables = (Map<String, ISupplier<?>>) args[2];
        UUID uuid = (UUID) args[3];

        log.debug("[RuntimeContextFactory.createChildContext] Creating RuntimeContext with input={}, outputType={}, presetVariables={}", input, outputType, presetVariables);
        IRuntimeContext<?, ?> context = new RuntimeContext<>(parent, input, outputType, presetVariables, uuid);
        log.debug("[RuntimeContextFactory.createChildContext] RuntimeContext created with uuid={}", context.uuid());

        return context;
    }

}
