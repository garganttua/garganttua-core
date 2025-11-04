package com.garganttua.core.reflection.binders;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.GGObjectAddress;
import com.garganttua.core.reflection.GGReflectionException;
import com.garganttua.core.reflection.query.GGObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;

public class MethodBinder<Returned>
        extends ExecutableBinder<Returned, IDiContext>
        implements IMethodBinder<Returned> {

    private final Class<Returned> returnedClass;
    private final IObjectSupplier<?> objectSupplier;
    private final GGObjectAddress method;
    private final boolean collection;

    public MethodBinder(IObjectSupplier<?> objectSupplier,
            GGObjectAddress method,
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
            GGObjectAddress method,
            List<IObjectSupplier<?>> parameterSuppliers,
            Class<Returned> returnedClass) {
        this(objectSupplier, method, parameterSuppliers, returnedClass, false);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Optional<Returned> execute(IDiContext context) throws DiException {
        Optional<?> target = Optional.ofNullable(Supplier.getObject(this.objectSupplier, context));
        if (target.isEmpty()) {
            throw new DiException("Target object supplier returned empty for method " + method);
        }

        try {
            Object[] args = this.buildArguments(context);

            if (collection && Collection.class.isAssignableFrom(target.get().getClass())) {
                ((Collection<?>) target.get()).forEach(t -> {
                    try {
                        GGObjectQueryFactory.objectQuery(t.getClass()).invoke(t, this.method, args);
                    } catch (GGReflectionException e) {
                        e.printStackTrace();
                    }
                });
                return Optional.empty();
            } else {
                Object result = GGObjectQueryFactory.objectQuery(this.objectSupplier.getObjectClass())
                        .invoke(target.get(), this.method, args);

                if (result != null && !returnedClass.isInstance(result)) {
                    throw new DiException("Method " + method + " returned an object of type "
                            + result.getClass().getName() + " but expected " + returnedClass.getName());
                }

                return (Optional<Returned>) Optional.ofNullable(result);
            }

        } catch (GGReflectionException e) {
            throw new DiException("Error invoking method " + method, e);
        }
    }
}
