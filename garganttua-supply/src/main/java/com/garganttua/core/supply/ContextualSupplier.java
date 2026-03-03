package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualSupplier<Supplied, Context> implements IContextualSupplier<Supplied, Context> {

    private IContextualSupply<Supplied, Context> supply;
    private IClass<Supplied> suppliedClass;
    private IClass<Context> contextClass;

    public ContextualSupplier(IContextualSupply<Supplied, Context> supply,
            IClass<Supplied> suppliedClass, IClass<Context> contextClass) {
        log.atTrace().log("Entering ContextualSupplier constructor with suppliedClass: {}, contextClass: {}", suppliedClass, contextClass);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.contextClass = Objects.requireNonNull(contextClass, "Context class cannot be null");
        log.atTrace().log("Exiting ContextualSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Context> getOwnerContextType() {
        return this.contextClass;
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    @Override
    public Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException {
        log.atTrace().log("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.atDebug().log("Supplying object of type {} with context type {}", this.suppliedClass.getSimpleName(), this.contextClass.getSimpleName());

        if (!this.contextClass.isInstance(ownerContext)) {
            log.atError().log("Context type mismatch: expected {}, but got {}", this.contextClass.getSimpleName(), ownerContext.getClass().getSimpleName());
            throw new SupplyException("Context type mismatch : waiting " + this.contextClass.getSimpleName() + " but "
                    + ownerContext.getClass().getSimpleName() + " provided");
        }

        Optional<Supplied> result = this.supply.supply(ownerContext, otherContexts);
        log.atDebug().log("Supply completed for type {}, result present: {}", this.suppliedClass.getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

}
