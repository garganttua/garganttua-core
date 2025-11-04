package com.garganttua.core.reflection.binders;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public class ContextualMethodBinder<ReturnedType, OwnerContextType>
        extends ContextualExecutableBinder<ReturnedType, OwnerContextType>
        implements IContextualMethodBinder<ReturnedType, OwnerContextType> {

    private final Class<ReturnedType> returnedClass;
    private final IObjectSupplier<?> objectSupplier;
    private final ObjectAddress method;
    private final boolean collection;

    public ContextualMethodBinder(IObjectSupplier<?> objectSupplier,
            ObjectAddress method,
            List<IObjectSupplier<?>> parameterSuppliers,
            Class<ReturnedType> returnedClass,
            boolean collection) {
        super(parameterSuppliers);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.returnedClass = Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        this.collection = collection;
    }

    public ContextualMethodBinder(IObjectSupplier<?> objectSupplier,
            ObjectAddress method,
            List<IObjectSupplier<?>> parameterSuppliers,
            Class<ReturnedType> returnedClass) {
        this(objectSupplier, method, parameterSuppliers, returnedClass, false);
    }

    @Override
    public Class<OwnerContextType> getOwnerContextType() {
        if (this.objectSupplier instanceof IContextualObjectSupplier<?, ?> contextual) {
            return (Class<OwnerContextType>) contextual.getOwnerContextType();
        } 
        return (Class<OwnerContextType>) Void.class;
    }

    @Override
    public Optional<ReturnedType> execute(OwnerContextType ownerContext, Object... contexts)
            throws ReflectionException {

        Object[] args = this.buildArguments(ownerContext, contexts);
        Object owner;

        try {
            if( IContextualObjectSupplier.class.isAssignableFrom(objectSupplier.getClass()) ){
                owner = ((IContextualObjectSupplier<?,OwnerContextType>) objectSupplier).supply(ownerContext, contexts).get();
            } else {
                owner = objectSupplier.supply().get();
            }
            return MethodBinder.execute(
                    owner,
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
