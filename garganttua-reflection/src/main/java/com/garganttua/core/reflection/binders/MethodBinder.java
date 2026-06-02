package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.MethodInvoker;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.methods.MultipleMethodReturn;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * {@link IMethodBinder} implementation that invokes a resolved {@link ResolvedMethod} on an object
 * obtained from a supplier, with arguments produced from parameter suppliers. When configured as a
 * collection binder, the method is invoked on each element of a target {@link java.util.Collection}.
 *
 * @param <Returned> the method return type
 */
public class MethodBinder<Returned>
        extends ExecutableBinder<Returned>
        implements IMethodBinder<Returned> {
    private static final Logger log = Logger.getLogger(MethodBinder.class);

    private final ResolvedMethod method;
    private final ISupplier<?> objectSupplier;
    private final boolean collection;

    /**
     * Creates a method binder.
     *
     * @param objectSupplier     supplier of the object the method is invoked on
     * @param method             the resolved method to invoke
     * @param parameterSuppliers suppliers producing the method arguments, in declaration order
     * @param collection         when {@code true}, the method is invoked on each element of a target collection
     */
    public MethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers,
            boolean collection) {
        super(parameterSuppliers);
        log.trace("Creating MethodBinder: method={}, collection={}", method,
                collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.collection = collection;
        log.debug("MethodBinder created for method {} with {} parameters", method, parameterSuppliers.size());
    }

    /**
     * Creates a non-collection method binder.
     *
     * @param objectSupplier     supplier of the object the method is invoked on
     * @param method             the resolved method to invoke
     * @param parameterSuppliers suppliers producing the method arguments, in declaration order
     */
    public MethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers) {
        this(objectSupplier, method, parameterSuppliers, false);
    }

    /**
     * Invokes {@code method} on {@code owner} (or on each element when {@code collectionTarget} is set).
     *
     * @param <T>              the owner type
     * @param <ReturnedType>   the method return type
     * @param owner            the target object, may be {@code null} for static methods
     * @param ownerType        the declared type of the owner
     * @param method           the resolved method to invoke
     * @param collectionTarget when {@code true} and {@code owner} is a {@link java.util.Collection}, invokes per element
     * @param args             the resolved method arguments
     * @return the method result(s) wrapped in an {@link IMethodReturn}
     * @throws ReflectionException if invocation fails
     */
    public static <T, ReturnedType> Optional<IMethodReturn<ReturnedType>> execute(
            Object owner,
            IClass<T> ownerType,
            ResolvedMethod method,
            boolean collectionTarget,
            Object[] args) throws ReflectionException {

        log.trace("Executing static method execute: owner={}, ownerType={}, method={}, collectionTarget={}",
                owner, ownerType, method, collectionTarget);

        if (!Methods.isStatic(method))
            Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(collectionTarget, "Collection target cannot be null");

        if (collectionTarget && owner instanceof Collection<?> col) {
            log.debug("Executing method {} on collection with {} elements", method, col.size());
            List<IMethodReturn<ReturnedType>> results = new ArrayList<>();
            for (Object element : col) {
                results.add((IMethodReturn<ReturnedType>) new MethodInvoker<>(method).invoke(element, args));
            }
            log.debug("Executed method {} on collection successfully", method);
            return Optional
                    .of(MultipleMethodReturn.ofMethodReturns(results, method.getReturnType()));
        }

        log.debug("Invoking method {} on owner of type {}", method, ownerType);

        IMethodReturn<ReturnedType> methodReturn = (IMethodReturn<ReturnedType>) new MethodInvoker<>(method)
                .invoke(owner, args);

        log.debug("Method {} executed successfully", method);
        return Optional.ofNullable(methodReturn);

    }

    /**
     * Builds the arguments, supplies the target object and invokes the method.
     *
     * @return the method result wrapped in an {@link IMethodReturn}
     * @throws ReflectionException if argument building or invocation fails
     */
    @Override
    public Optional<IMethodReturn<Returned>> execute() throws ReflectionException {
        log.trace("Executing MethodBinder for method {}", method);
        Object[] args = this.buildArguments();
        try {
            Optional<IMethodReturn<Returned>> result = execute(
                    objectSupplier.supply().orElse(null),
                    objectSupplier.getSuppliedClass(),
                    this.method,
                    collection,
                    args);
            log.debug("MethodBinder execution completed for method {}", method);
            return result;
        } catch (SupplyException e) {
            log.error("Supply error executing method {}", method, e);
            throw new ReflectionException(e);
        }
    }

    /** {@return a colored, human-readable rendering of the bound method} */
    @Override
    public String getExecutableReference() {
        log.trace("Getting executable reference for method {}", method);
        return Methods.prettyColored(this.method);
    }

    /**
     * Supplies the method result by delegating to {@link #execute()}.
     *
     * @throws SupplyException if invocation fails
     */
    @Override
    public Optional<IMethodReturn<Returned>> supply() throws SupplyException {
        try {
            return this.execute();
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    /** {@return the {@link Type} supplied by the target object supplier} */
    @Override
    public Type getSuppliedType() {
        return this.objectSupplier.getSuppliedType();
    }

    /** {@return the supplied class, namely {@link IMethodReturn}} */
    @Override
    public IClass<IMethodReturn<Returned>> getSuppliedClass() {
        return (IClass<IMethodReturn<Returned>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
    }

}
