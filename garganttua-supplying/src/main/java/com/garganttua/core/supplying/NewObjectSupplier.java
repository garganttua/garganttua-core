package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;

public class NewObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType> {

    private Class<SuppliedType> suppliedType;
    private IConstructorBinder<SuppliedType> constructorBinder;

    public NewObjectSupplier(Class<SuppliedType> suppliedType,
            IConstructorBinder<SuppliedType> constructorBinder) {
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        try {
            return this.constructorBinder.execute();
        } catch (ReflectionException e) {
            return Optional.empty();
        }
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.suppliedType;
    }

}