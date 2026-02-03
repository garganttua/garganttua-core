package com.garganttua.core.reflection.binders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.reflection.utils.ConstructorAccessManager;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConstructorBinder<Constructed>
        extends ExecutableBinder<Constructed>
        implements IConstructorBinder<Constructed> {

    private Class<Constructed> objectClass;
    private Constructor<Constructed> constructor;

    public ConstructorBinder(Class<Constructed> objectClass,
            Constructor<Constructed> constructor, List<ISupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        log.atTrace().log("Creating ConstructorBinder for class={}, constructor params={}", objectClass.getName(), constructor.getParameterCount());
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
        log.atDebug().log("ConstructorBinder created for class {} with {} parameters", objectClass.getName(), parameterSuppliers.size());

    }

    @Override
    public Optional<IMethodReturn<Constructed>> execute() throws ReflectionException {
        log.atTrace().log("Executing constructor for class {}", objectClass.getName());
        try(ConstructorAccessManager accessor = new ConstructorAccessManager(this.constructor) ) {
            Object[] args = this.buildArguments();
            log.atDebug().log("Invoking constructor for class {} with {} arguments", objectClass.getName(), args.length);
            Constructed instance = this.constructor.newInstance(args);
            log.atDebug().log("Successfully created instance of class {}", objectClass.getName());
            return Optional.ofNullable(SingleMethodReturn.of(instance));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.atError().log("Error creating new instance of class {}", objectClass.getName(), e);
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

    @Override
    public Type getSuppliedType() {
        return this.objectClass;
    }

    @Override
    public Optional<IMethodReturn<Constructed>> supply() throws SupplyException {
        try {
            return this.execute();
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

}
