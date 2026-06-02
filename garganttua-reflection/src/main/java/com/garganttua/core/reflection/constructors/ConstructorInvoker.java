package com.garganttua.core.reflection.constructors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

public class ConstructorInvoker<T> implements ISupplier<IMethodReturn<T>> {
    private static final Logger log = Logger.getLogger(ConstructorInvoker.class);

    private final ResolvedConstructor<T> constructor;
    private final IClass<T> constructedType;
    private final boolean force;

    public ConstructorInvoker(ResolvedConstructor<T> constructor) {
        this(constructor, false);
    }

    public ConstructorInvoker(ResolvedConstructor<T> constructor, boolean force) {
        Objects.requireNonNull(constructor, "Resolved constructor cannot be null");
        log.trace("Creating ConstructorInvoker for {}, force={}", constructor.constructedType().getName(), force);
        this.constructor = constructor;
        this.constructedType = constructor.constructedType();
        this.force = force;
        log.debug("ConstructorInvoker initialized for {}, force={}", constructedType.getName(), force);
    }

    public IMethodReturn<T> newInstance(Object... args) throws ReflectionException {
        log.trace("newInstance entry: constructedType={}, args count={}",
                constructedType.getName(), args != null ? args.length : 0);

        try (var mgr = new ConstructorAccessManager(constructor, this.force)) {
            T instance = constructor.newInstance(args);
            log.debug("Successfully created instance of {}", constructedType.getName());
            return SingleMethodReturn.of(instance, constructedType);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.debug("Constructor for {} threw exception: {}",
                    constructedType.getName(), cause.getClass().getName());
            return SingleMethodReturn.ofException(cause, constructedType);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Error creating instance of {}", constructedType.getName(), e);
            throw new ReflectionException(
                    "Error creating new instance of type " + constructedType.getSimpleName(), e);
        }
    }

    // --- ISupplier<IMethodReturn<T>> ---

    @Override
    public Optional<IMethodReturn<T>> supply() throws SupplyException {
        try {
            IMethodReturn<T> result = newInstance();
            if (result.hasException()) {
                throw new SupplyException("Constructor invocation failed", result.getException());
            }
            return Optional.of(result);
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        return getSuppliedClass().getType();
    }

    @Override
    public IClass<IMethodReturn<T>> getSuppliedClass() {
        return (IClass<IMethodReturn<T>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
    }
}
