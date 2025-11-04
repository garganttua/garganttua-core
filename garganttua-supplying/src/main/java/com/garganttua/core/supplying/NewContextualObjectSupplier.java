package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

public class NewContextualObjectSupplier<SuppliedType>
        implements IContextualObjectSupplier<SuppliedType, Void> {

    private Class<SuppliedType> suppliedType;
    private IContextualConstructorBinder<SuppliedType> constructorBinder;

    public NewContextualObjectSupplier(Class<SuppliedType> suppliedType,
            IContextualConstructorBinder<SuppliedType> constructorBinder) {
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
    }

    @Override
    public Optional<SuppliedType> supply(Void ownerContext, Object... contexts)
            throws SupplyException {
        Objects.requireNonNull(ownerContext, "Owner cannot be null");
        try {
            return this.constructorBinder.execute(ownerContext, contexts);
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public Class<Void> getOwnerContextType() {
        return Void.class;
    }


}
