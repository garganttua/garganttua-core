package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.ConstructorInvoker;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.reflection.constructors.ResolvedConstructor;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

public class ContextualConstructorBinder<Constructed>
        extends ContextualExecutableBinder<Constructed, Void>
        implements IContextualConstructorBinder<Constructed> {
    private static final IDiagnostic log = Diagnostics.of(ContextualConstructorBinder.class);

    private final IClass<Constructed> objectClass;
    private final IConstructor<Constructed> constructor;

    public ContextualConstructorBinder(IClass<Constructed> objectClass,
            IConstructor<Constructed> constructor,
            List<ISupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        log.trace("Creating ContextualConstructorBinder for class={}, constructor params={}",
                objectClass.getName(), constructor.getParameterCount());
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
        log.debug("ContextualConstructorBinder created for class {} with {} parameters", objectClass.getName(),
                parameterSuppliers.size());
    }

    @Override
    public IClass<Constructed> getConstructedType() {
        return this.objectClass;
    }

    @Override
    public Optional<IMethodReturn<Constructed>> execute(Void ownerContext, Object... contexts) throws ReflectionException {
        log.trace("Executing contextual constructor for class {}", objectClass.getName());
        Object[] args = this.buildArguments(contexts);
        log.debug("Invoking constructor for class {} with {} arguments", objectClass.getName(),
                args.length);
        ConstructorInvoker<Constructed> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(constructor));
        IMethodReturn<Constructed> result = invoker.newInstance(args);
        log.debug("Successfully created instance of class {}", objectClass.getName());
        return Optional.of(result);
    }

    @Override
    public String getExecutableReference() {
        return Constructors.prettyColored(constructor);
    }

    @Override
    public IConstructor<?> constructor() {
        return this.constructor;
    }

    @Override
    public Type getSuppliedType() {
        return this.objectClass.getType();
    }

    @Override
    public Optional<IMethodReturn<Constructed>> supply(Void ownerContext, Object... otherContexts) throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }

    @Override
    public IClass<IMethodReturn<Constructed>> getSuppliedClass() {
        return (IClass<IMethodReturn<Constructed>>) (IClass<?>) this.objectClass;
    }
}