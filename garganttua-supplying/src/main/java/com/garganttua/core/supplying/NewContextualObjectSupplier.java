package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

public class NewContextualObjectSupplier<SuppliedType, OwnerContextType>
        implements IContextualObjectSupplier<SuppliedType, OwnerContextType> {

    private Class<SuppliedType> suppliedType;
    private IContextualConstructorBinder<SuppliedType, OwnerContextType> constructorBinder;
    private Class<OwnerContextType> ownerContextType;

    public NewContextualObjectSupplier(Class<SuppliedType> suppliedType,
            IContextualConstructorBinder<SuppliedType, OwnerContextType> constructorBinder,
            Class<OwnerContextType> ownerContextType) {
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.ownerContextType = Objects.requireNonNull(ownerContextType, "Owner context type cannot be null");
    }

    @Override
    public Optional<SuppliedType> supply(OwnerContextType ownerContext, Object... contexts)
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
    public Class<OwnerContextType> getOwnerContextClass() {
        return this.ownerContextType;
    }

}
