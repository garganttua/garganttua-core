package com.garganttua.core.reflection.binders;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodBinder<Returned>
        extends ExecutableBinder<Returned>
        implements IMethodBinder<Returned> {

    private final Class<Returned> returnedClass;
    private final IObjectSupplier<?> objectSupplier;
    private final ObjectAddress method;
    private final boolean collection;

    public MethodBinder(IObjectSupplier<?> objectSupplier,
            ObjectAddress method,
            List<IObjectSupplier<?>> parameterSuppliers,
            Class<Returned> returnedClass,
            boolean collection) {
        super(parameterSuppliers);
        log.atTrace().log("Creating MethodBinder: method={}, returnedClass={}, collection={}", method, returnedClass, collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.returnedClass = Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        this.collection = collection;
        log.atInfo().log("MethodBinder created for method {} with {} parameters", method, parameterSuppliers.size());
    }

    public MethodBinder(IObjectSupplier<?> objectSupplier,
            ObjectAddress method,
            List<IObjectSupplier<?>> parameterSuppliers,
            Class<Returned> returnedClass) {
        this(objectSupplier, method, parameterSuppliers, returnedClass, false);
    }

    public static <ReturnedType> Optional<ReturnedType> execute(
            Object owner,
            Class<?> ownerType,
            ObjectAddress method,
            Class<?> returnedClass,
            boolean collectionTarget,
            Object[] args) throws ReflectionException {

        log.atTrace().log("Executing static method: owner={}, ownerType={}, method={}, collectionTarget={}", owner, ownerType, method, collectionTarget);
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        Objects.requireNonNull(collectionTarget, "Collection target cannot be null");

        if (collectionTarget && owner instanceof Collection<?> col) {
            log.atDebug().log("Executing method {} on collection with {} elements", method, col.size());
            for (Object element : col) {
                ObjectQueryFactory.objectQuery(ownerType).invoke(element, method, args);
            }
            log.atInfo().log("Executed method {} on collection successfully", method);
            return Optional.empty();
        }

        log.atDebug().log("Invoking method {} on owner of type {}", method, ownerType);
        Object result = ObjectQueryFactory.objectQuery(ownerType)
                .invoke(owner, method, args);

        if (result != null && !returnedClass.isInstance(result)) {
            log.atError().log("Method {} returned type {} but expected {}", method, result.getClass().getName(), returnedClass.getName());
            throw new ReflectionException("Method " + method + " returned type "
                    + result.getClass().getName() + " but expected " + returnedClass.getName());
        }

        log.atDebug().log("Method {} executed successfully, result={}", method, result);
        return (Optional<ReturnedType>) Optional.ofNullable(result);

    }

    @Override
    public Optional<Returned> execute() throws ReflectionException {
        log.atTrace().log("Executing MethodBinder for method {}", method);
        Object[] args = this.buildArguments();
        try {
            Optional<Returned> result = execute(
                    objectSupplier.supply().get(),
                    objectSupplier.getSuppliedType(),
                    method,
                    returnedClass,
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
        return Methods.prettyColored((Method) ObjectQueryFactory.objectQuery(this.objectSupplier.getSuppliedType()).find(this.method).getLast());
    }

}
