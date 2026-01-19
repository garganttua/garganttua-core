package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.MethodInvoker;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.methods.MultipleMethodReturn;
import com.garganttua.core.reflection.methods.ResolvedMethod;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodBinder<Returned>
        extends ExecutableBinder<Returned>
        implements IMethodBinder<Returned> {

    private final ResolvedMethod method;
    private final ISupplier<?> objectSupplier;
    private final boolean collection;

    public MethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers,
            boolean collection) {
        super(parameterSuppliers);
        log.atTrace().log("Creating MethodBinder: method={}, collection={}", method,
                collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.collection = collection;
        log.atInfo().log("MethodBinder created for method {} with {} parameters", method, parameterSuppliers.size());
    }

    public MethodBinder(ISupplier<?> objectSupplier,
            ResolvedMethod method,
            List<ISupplier<?>> parameterSuppliers) {
        this(objectSupplier, method, parameterSuppliers, false);
    }

    public static <T, ReturnedType> Optional<IMethodReturn<ReturnedType>> execute(
            Object owner,
            Class<T> ownerType,
            ResolvedMethod method,
            boolean collectionTarget,
            Object[] args) throws ReflectionException {

        log.atTrace().log("Executing static method execute: owner={}, ownerType={}, method={}, collectionTarget={}",
                owner, ownerType, method, collectionTarget);

        if (!method.isStatic())
            Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(collectionTarget, "Collection target cannot be null");

        if (collectionTarget && owner instanceof Collection<?> col) {
            log.atDebug().log("Executing method {} on collection with {} elements", method, col.size());
            List<IMethodReturn<ReturnedType>> results = new ArrayList<>();
            for (Object element : col) {
                results.add((IMethodReturn<ReturnedType>) new MethodInvoker<>(method).invoke(element, args));
            }
            log.atInfo().log("Executed method {} on collection successfully", method);
            return Optional.of(MultipleMethodReturn.ofMethodReturns(results, method.returnType()));
        }

        log.atDebug().log("Invoking method {} on owner of type {}", method, ownerType);

        IMethodReturn<ReturnedType> methodReturn = (IMethodReturn<ReturnedType>) new MethodInvoker<>(method)
                .invoke(owner, args);

        log.atDebug().log("Method {} executed successfully", method);
        return Optional.ofNullable(methodReturn);

    }

    @Override
    public Optional<IMethodReturn<Returned>> execute() throws ReflectionException {
        log.atTrace().log("Executing MethodBinder for method {}", method);
        Object[] args = this.buildArguments();
        try {
            Optional<IMethodReturn<Returned>> result = execute(
                    objectSupplier.supply().orElse(null),
                    objectSupplier.getSuppliedClass(),
                    this.method,
                    collection,
                    args);
            log.atDebug().log("MethodBinder execution completed for method {}", method);
            return result;
        } catch (SupplyException e) {
            log.atError().log("Supply error executing method {}", method, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getExecutableReference() {
        log.atTrace().log("Getting executable reference for method {}", method);
        return Methods.prettyColored(this.method.method());
    }

    @Override
    public Optional<IMethodReturn<Returned>> supply() throws SupplyException {
        try {
            return this.execute();
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        return this.objectSupplier.getSuppliedType();
    }

}
