package com.garganttua.core.reflection.binders;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

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
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.returnedClass = Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        this.collection = collection;
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

        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(ownerType, "Owner type cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        Objects.requireNonNull(collectionTarget, "Collection target cannot be null");

        if (collectionTarget && owner instanceof Collection<?> col) {
            for (Object element : col) {
                ObjectQueryFactory.objectQuery(ownerType).invoke(element, method, args);
            }
            return Optional.empty();
        }

        Object result = ObjectQueryFactory.objectQuery(ownerType)
                .invoke(owner, method, args);

        if (result != null && !returnedClass.isInstance(result)) {
            throw new ReflectionException("Method " + method + " returned type "
                    + result.getClass().getName() + " but expected " + returnedClass.getName());
        }

        return (Optional<ReturnedType>) Optional.ofNullable(result);

    }

    @Override
    public Optional<Returned> execute() throws ReflectionException {
        Object[] args = this.buildArguments();
        try {
            return execute(
                    objectSupplier.supply().get(),
                    objectSupplier.getSuppliedType(),
                    method,
                    returnedClass,
                    collection,
                    args);
        } catch (SupplyException e) {
            throw new ReflectionException(e);
        }
    }

}
