package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualSupplier<Supplied, Context> implements IContextualSupplier<Supplied, Context> {

    private IContextualSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public ContextualSupplier(IContextualSupply<Supplied, Context> supply,
            Class<Supplied> suppliedType, Class<Context> contextType) {
        log.atTrace().log("Entering ContextualSupplier constructor with suppliedType: {}, contextType: {}", suppliedType, contextType);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        log.atTrace().log("Exiting ContextualSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public Class<Context> getOwnerContextType() {
        return this.contextType;
    }

    @Override
    public Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException {
        log.atTrace().log("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.atDebug().log("Supplying object of type {} with context type {}", this.suppliedType.getSimpleName(), this.contextType.getSimpleName());

        if (!this.contextType.isAssignableFrom(ownerContext.getClass())) {
            log.atError().log("Context type mismatch: expected {}, but got {}", this.contextType.getSimpleName(), ownerContext.getClass().getSimpleName());
            throw new SupplyException("Context type mismatch : waiting " + this.contextType.getSimpleName() + " but "
                    + ownerContext.getClass().getSimpleName() + " provided");
        }

        Optional<Supplied> result = this.supply.supply(ownerContext, otherContexts);
        log.atDebug().log("Supply completed for type {}, result present: {}", this.suppliedType.getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

}
