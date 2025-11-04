package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.IConstructorBinder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.injection.supplier.binder.ExecutableBinder;

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
    public Optional<Constructed> execute() throws DiException {
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