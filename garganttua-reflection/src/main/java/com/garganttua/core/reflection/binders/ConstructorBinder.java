package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.ConstructorInvoker;
import com.garganttua.core.reflection.constructors.Constructors;
import com.garganttua.core.reflection.constructors.ResolvedConstructor;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * {@link IConstructorBinder} implementation that instantiates {@code Constructed} objects by
 * invoking a resolved {@link IConstructor} with arguments produced from parameter suppliers.
 *
 * @param <Constructed> the type produced by the bound constructor
 */
public class ConstructorBinder<Constructed>
        extends ExecutableBinder<Constructed>
        implements IConstructorBinder<Constructed> {
    private static final Logger log = Logger.getLogger(ConstructorBinder.class);

    private IClass<Constructed> objectClass;
    private IConstructor<Constructed> constructor;

    /**
     * Creates a binder for the given constructor.
     *
     * @param objectClass        the class to instantiate
     * @param constructor        the constructor to invoke
     * @param parameterSuppliers suppliers producing the constructor arguments, in declaration order
     */
    public ConstructorBinder(IClass<Constructed> objectClass,
            IConstructor<Constructed> constructor, List<ISupplier<?>> parameterSuppliers) {
        super(parameterSuppliers);
        log.trace("Creating ConstructorBinder for class={}, constructor params={}", objectClass.getName(), constructor.getParameterCount());
        this.objectClass = Objects.requireNonNull(objectClass, "Object class cannot be null");
        this.constructor = Objects.requireNonNull(constructor, "Constructor cannot be null");
        log.debug("ConstructorBinder created for class {} with {} parameters", objectClass.getName(), parameterSuppliers.size());

    }

    /**
     * Builds the constructor arguments from the parameter suppliers and instantiates the object.
     *
     * @return the constructor result wrapped in an {@link IMethodReturn}
     * @throws ReflectionException if argument building or instantiation fails
     */
    @Override
    public Optional<IMethodReturn<Constructed>> execute() throws ReflectionException {
        log.trace("Executing constructor for class {}", objectClass.getName());
        Object[] args = this.buildArguments();
        log.debug("Invoking constructor for class {} with {} arguments", objectClass.getName(), args.length);
        ConstructorInvoker<Constructed> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(constructor));
        IMethodReturn<Constructed> result = invoker.newInstance(args);
        log.debug("Successfully created instance of class {}", objectClass.getName());
        return Optional.of(result);
    }

    /** {@return the class instantiated by this binder} */
    @Override
    public IClass<Constructed> getConstructedType() {
        return this.objectClass;
    }

    /** {@return a colored, human-readable rendering of the bound constructor} */
    @Override
    public String getExecutableReference() {
        return Constructors.prettyColored(constructor);
    }

    /** {@return the bound constructor} */
    @Override
    public IConstructor<?> constructor() {
        return this.constructor;
    }

    /** {@return the {@link Type} of the constructed object} */
    @Override
    public Type getSuppliedType() {
        return this.objectClass.getType();
    }

    /**
     * Supplies a freshly constructed instance by delegating to {@link #execute()}.
     *
     * @throws SupplyException if instantiation fails
     */
    @Override
    public Optional<IMethodReturn<Constructed>> supply() throws SupplyException {
        try {
            return this.execute();
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    /** {@return the supplied class, the constructed type viewed as an {@link IMethodReturn}} */
    @Override
    public IClass<IMethodReturn<Constructed>> getSuppliedClass() {
        return (IClass<IMethodReturn<Constructed>>) (IClass<?>) this.objectClass;
    }

}
