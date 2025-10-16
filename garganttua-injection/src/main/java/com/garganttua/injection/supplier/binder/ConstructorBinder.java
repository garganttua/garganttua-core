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

public class ConstructorBinder<Constructed> extends ExecutableBinder<IDiContext> implements IConstructorBinder<Constructed> {

    private Class<Constructed> objectClass;
    private List<IObjectSupplier<?>> parameterSuppliers;
    private Constructor<Constructed> constructor;

    public ConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor, List<IObjectSupplier<?>> parameterSuppliers) {
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.parameterSuppliers = Objects.requireNonNull(parameterSuppliers, "Parameters suppliers cannot be null");
    }

    @Override
    public Optional<Constructed> execute() throws DiException {
        return this.execute(null);
    }

    @Override
    public Optional<Constructed> execute(IDiContext context)
            throws DiException {
                /* Objects.requireNonNull(context, "Context cannot be null"); */
        try {
            Object[] args = this.buildArguments(this.parameterSuppliers, context);
            return Optional.ofNullable(this.constructor.newInstance(args));

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new DiException("Error creating new instance of type " + this.objectClass.getSimpleName(), e);
        }
    }

    @Override
    public Class<Constructed> getConstructedClass() {
        return this.objectClass;
    }

}
