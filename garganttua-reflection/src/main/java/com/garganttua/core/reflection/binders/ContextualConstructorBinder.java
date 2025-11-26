package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.supply.IObjectSupplier;

public class ContextualConstructorBinder<Constructed>
        extends ContextualExecutableBinder<Constructed, Void>
        implements IContextualConstructorBinder<Constructed> {

    private final Class<Constructed> objectClass;
    private final Constructor<Constructed> constructor;

    public ContextualConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor,
            List<IObjectSupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
    }

    @Override
    public Class<Constructed> getConstructedType() {
        return this.objectClass;
    }

    @Override
    public Optional<Constructed> execute(Void ownerContext, Object... contexts) throws ReflectionException {
        try {
            Object[] args = this.buildArguments(contexts);
            return Optional.ofNullable(this.constructor.newInstance(args));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException("Error creating new instance of type " + objectClass.getSimpleName(), e);
        }
    }

    @Override
    public String getExecutableReference() {
        return Constructors.prettyColored(constructor);
    }
}