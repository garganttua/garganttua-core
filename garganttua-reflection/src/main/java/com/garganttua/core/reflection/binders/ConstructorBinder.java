package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.reflection.utils.ConstructorAccessManager;
import com.garganttua.core.supply.IObjectSupplier;

public class ConstructorBinder<Constructed>
        extends ExecutableBinder<Constructed>
        implements IConstructorBinder<Constructed> {

    private Class<Constructed> objectClass;
    private Constructor<Constructed> constructor;

    public ConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor, List<IObjectSupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");

    }

    @Override
    public Optional<Constructed> execute() throws ReflectionException {
        try(ConstructorAccessManager accessor = new ConstructorAccessManager(this.constructor) ) {
            Object[] args = this.buildArguments();
            return Optional.ofNullable(this.constructor.newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException("Error creating new instance of type " + objectClass.getSimpleName(), e);
        }
    }

    @Override
    public Class<Constructed> getConstructedType() {
        return this.objectClass;
    }

    @Override
    public String getExecutableReference() {
        return Constructors.prettyColored(constructor);
    }

    @Override
    public Constructor<?> constructor() {
        return this.constructor;
    }

}
