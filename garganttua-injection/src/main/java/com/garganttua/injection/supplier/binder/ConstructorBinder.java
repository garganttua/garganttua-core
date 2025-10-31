package com.garganttua.injection.supplier.binder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;

public class ConstructorBinder<Constructed>
        extends ExecutableBinder<Constructed, IDiContext>
        implements IConstructorBinder<Constructed> {

    private final Class<Constructed> objectClass;
    private final Constructor<Constructed> constructor;

    public ConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor,
            List<IObjectSupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
    }

    @Override
    public Optional<Constructed> execute(IDiContext context) throws DiException {
        try {
            Object[] args = this.buildArguments(context);
            return Optional.ofNullable(this.constructor.newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DiException("Error creating new instance of type " + objectClass.getSimpleName(), e);
        }
    }

    @Override
    public Class<Constructed> getConstructedClass() {
        return this.objectClass;
    }
}