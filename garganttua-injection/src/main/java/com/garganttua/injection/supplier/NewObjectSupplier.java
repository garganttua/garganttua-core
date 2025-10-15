package com.garganttua.injection.supplier;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IConstructorBinder;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;

public class NewObjectSupplier<ObjectClass> implements IContextualObjectSupplier<ObjectClass, IDiContext> {

    private Class<ObjectClass> objectClass;
    private IConstructorBinder<ObjectClass> constructorBinder;

    public NewObjectSupplier(Class<ObjectClass> objectClass, IConstructorBinder<ObjectClass> constructorBinder) {
        this.constructorBinder = constructorBinder;
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
    }

    @Override
    public Optional<ObjectClass> getObject(IDiContext context)
            throws DiException {
        Objects.requireNonNull(context, "Context cannot be null");
        return this.constructorBinder.execute(context);
    }

    @Override
    public Optional<ObjectClass> getObject() throws DiException {
        return this.constructorBinder.execute();
    }

    @Override
    public Class<ObjectClass> getObjectClass() {
        return this.objectClass;
    }

    @Override
    public Class<IDiContext> getContextClass() {
        return IDiContext.class;
    }

}
