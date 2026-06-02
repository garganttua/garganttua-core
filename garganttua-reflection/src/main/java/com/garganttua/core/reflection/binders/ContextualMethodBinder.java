package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Context-aware {@link MethodBinder} variant. Resolves the target object and arguments from
 * evaluation contexts before invoking the bound {@link ResolvedMethod}.
 *
 * @param <ReturnedType>     the method return type
 * @param <OwnerContextType> the context type required to supply the target object
 */
public class ContextualMethodBinder<ReturnedType, OwnerContextType>
        extends ContextualExecutableBinder<ReturnedType, OwnerContextType>
        implements IContextualMethodBinder<ReturnedType, OwnerContextType> {
    private static final Logger log = Logger.getLogger(ContextualMethodBinder.class);

    private final ISupplier<?> objectSupplier;
    private final ResolvedMethod method;
    private final boolean collection;

    /**
     * Creates a contextual method binder.
     *
     * @param objectSupplier     supplier of the object the method is invoked on
     * @param method             the resolved method to invoke
     * @param parameterSuppliers suppliers producing the method arguments, in declaration order
     * @param collection         when {@code true}, the method is invoked on each element of a target collection
     */
    public ContextualMethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers,
            boolean collection) {
        super(parameterSuppliers);
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        log.trace("Creating ContextualMethodBinder: method={}, returnedClass={}, collection={}", method,
                method.getReturnType(), collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.collection = collection;
        log.debug("ContextualMethodBinder created for method {} with {} parameters", method,
                parameterSuppliers.size());
    }

    /**
     * Creates a non-collection contextual method binder.
     *
     * @param objectSupplier     supplier of the object the method is invoked on
     * @param method             the resolved method to invoke
     * @param parameterSuppliers suppliers producing the method arguments, in declaration order
     */
    public ContextualMethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers) {
        this(objectSupplier, method, parameterSuppliers, false);
    }

    /** {@return the context type required to supply the target object, or {@code null} when non-contextual} */
    @Override
    public IClass<OwnerContextType> getOwnerContextType() {
        if (this.objectSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (IClass<OwnerContextType>) contextual.getOwnerContextType();
        }
        return null;
    }

    /**
     * Resolves the target object and arguments from the contexts and invokes the method.
     *
     * @param ownerContext context used to supply the target object (prepended to {@code contexts} for parameters)
     * @param contexts     additional contexts forwarded to contextual parameter suppliers
     * @return the method result wrapped in an {@link IMethodReturn}
     * @throws ReflectionException if argument building or invocation fails
     */
    @Override
    public Optional<IMethodReturn<ReturnedType>> execute(OwnerContextType ownerContext, Object... contexts)
            throws ReflectionException {
        log.trace("Executing contextual method binder for method {}", method);

        Object[] mergedContexts = new Object[contexts.length + 1];
        mergedContexts[0] = ownerContext;
        System.arraycopy(contexts, 0, mergedContexts, 1, contexts.length);

        Object[] args = this.buildArguments(mergedContexts);

        try {

            Object owner = Supplier.contextualSupply(this.objectSupplier, ownerContext);
            log.debug("Executing method {} on owner of type {}", method, objectSupplier.getSuppliedClass());

            Optional<IMethodReturn<ReturnedType>> result = MethodBinder.execute(
                    owner,
                    objectSupplier.getSuppliedClass(),
                    method,
                    collection,
                    args);
            log.debug("Successfully executed contextual method {}", method);
            return result;
        } catch (SupplyException e) {
            log.error("Supply error executing contextual method {}", method, e);
            throw new ReflectionException(e);
        }
    }

    /** {@return a colored, human-readable rendering of the bound method} */
    @Override
    public String getExecutableReference() {
        return Methods.prettyColored(this.method);
    }

    /** {@return the {@link Type} of the method return value} */
    @Override
    public Type getSuppliedType() {
        return this.method.getReturnType().getType();
    }

    /**
     * Supplies the method result by delegating to {@link #execute(Object, Object...)}.
     *
     * @param ownerContext  context used to supply the target object
     * @param otherContexts additional contexts forwarded to parameter suppliers
     * @throws SupplyException if invocation fails
     */
    @Override
    public Optional<IMethodReturn<ReturnedType>> supply(OwnerContextType ownerContext, Object... otherContexts)
            throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }

    /** {@return the supplied class, namely {@link IMethodReturn}} */
    @Override
    public IClass<IMethodReturn<ReturnedType>> getSuppliedClass() {
        return (IClass<IMethodReturn<ReturnedType>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
    }

}
