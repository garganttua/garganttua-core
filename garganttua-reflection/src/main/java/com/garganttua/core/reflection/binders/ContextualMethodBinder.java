package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.MethodResolver;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualMethodBinder<ReturnedType, OwnerContextType>
        extends ContextualExecutableBinder<ReturnedType, OwnerContextType>
        implements IContextualMethodBinder<ReturnedType, OwnerContextType> {

    private final ISupplier<?> objectSupplier;
    private final ResolvedMethod method;
    private final boolean collection;

    public ContextualMethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers,
            boolean collection) {
        super(parameterSuppliers);
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        log.atTrace().log("Creating ContextualMethodBinder: method={}, returnedClass={}, collection={}", method,
                method.returnType(), collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.collection = collection;
        log.atDebug().log("ContextualMethodBinder created for method {} with {} parameters", method,
                parameterSuppliers.size());
    }

    public ContextualMethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers) {
        this(objectSupplier, method, parameterSuppliers, false);
    }

    /**
     * Backward-compatible constructor using ObjectAddress.
     * Resolves the method from the owner type and address.
     */
    public ContextualMethodBinder(ISupplier<?> objectSupplier,
            ObjectAddress methodAddress,
            List<ISupplier<?>> parameterSuppliers,
            Class<?> returnType) {
        this(objectSupplier,
             MethodResolver.methodByAddress(objectSupplier.getSuppliedClass(), methodAddress, returnType,
                 parameterSuppliers.stream().map(s -> s.getSuppliedClass()).toArray(Class[]::new)),
             parameterSuppliers, false);
    }

    @Override
    public Class<OwnerContextType> getOwnerContextType() {
        if (this.objectSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (Class<OwnerContextType>) contextual.getOwnerContextType();
        }
        return (Class<OwnerContextType>) Void.class;
    }

    @Override
    public Optional<IMethodReturn<ReturnedType>> execute(OwnerContextType ownerContext, Object... contexts)
            throws ReflectionException {
        log.atTrace().log("Executing contextual method binder for method {}", method);

        Object[] mergedContexts = new Object[contexts.length + 1];
        mergedContexts[0] = ownerContext;
        System.arraycopy(contexts, 0, mergedContexts, 1, contexts.length);

        Object[] args = this.buildArguments(mergedContexts);

        try {

            Object owner = Supplier.contextualSupply(this.objectSupplier, ownerContext);
            log.atDebug().log("Executing method {} on owner of type {}", method, objectSupplier.getSuppliedClass());

            Optional<IMethodReturn<ReturnedType>> result = MethodBinder.execute(
                    owner,
                    objectSupplier.getSuppliedClass(),
                    method,
                    collection,
                    args);
            log.atInfo().log("Successfully executed contextual method {}", method);
            return result;
        } catch (SupplyException e) {
            log.atError().log("Supply error executing contextual method {}", method, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getExecutableReference() {
        return Methods.prettyColored(this.method.method());
    }

    @Override
    public Type getSuppliedType() {
        return this.method.returnType();
    }

    @Override
    public Optional<IMethodReturn<ReturnedType>> supply(OwnerContextType ownerContext, Object... otherContexts)
            throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }

}
