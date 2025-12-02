package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualConstructorBinder<Constructed>
        extends ContextualExecutableBinder<Constructed, Void>
        implements IContextualConstructorBinder<Constructed> {

    private final Class<Constructed> objectClass;
    private final Constructor<Constructed> constructor;

    public ContextualConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor,
            List<IObjectSupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        log.atTrace().log("Creating ContextualConstructorBinder for class={}, constructor params={}", objectClass.getName(), constructor.getParameterCount());
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
        log.atDebug().log("ContextualConstructorBinder created for class {} with {} parameters", objectClass.getName(), parameterSuppliers.size());
    }

    @Override
    public Class<Constructed> getConstructedType() {
        return this.objectClass;
    }

    @Override
    public Optional<Constructed> execute(Void ownerContext, Object... contexts) throws ReflectionException {
        log.atTrace().log("Executing contextual constructor for class {}", objectClass.getName());
        try {
            Object[] args = this.buildArguments(contexts);
            log.atDebug().log("Invoking constructor for class {} with {} arguments", objectClass.getName(), args.length);
            Constructed instance = this.constructor.newInstance(args);
            log.atInfo().log("Successfully created instance of class {}", objectClass.getName());
            return Optional.ofNullable(instance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.atError().log("Error creating new instance of class {}", objectClass.getName(), e);
            throw new ReflectionException("Error creating new instance of type " + objectClass.getSimpleName(), e);
        }
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