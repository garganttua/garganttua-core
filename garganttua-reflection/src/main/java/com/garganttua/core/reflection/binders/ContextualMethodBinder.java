package com.garganttua.core.reflection.binders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.Methods;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.atTrace().log("Creating ContextualMethodBinder: method={}, returnedClass={}, collection={}", method, returnedClass, collection);
        this.objectSupplier = Objects.requireNonNull(objectSupplier, "Object supplier cannot be null");
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.returnedClass = Objects.requireNonNull(returnedClass, "Returned class cannot be null");
        this.collection = collection;
        log.atDebug().log("ContextualMethodBinder created for method {} with {} parameters", method, parameterSuppliers.size());
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
        log.atTrace().log("Executing contextual method binder for method {}", method);
        Object[] args = this.buildArguments(ownerContext, contexts);

        try {

            Object owner = Supplier.contextualSupply(this.objectSupplier, ownerContext);
            log.atDebug().log("Executing method {} on owner of type {}", method, objectSupplier.getSuppliedType());

            Optional<ReturnedType> result = MethodBinder.execute(
                    owner,
                    objectSupplier.getSuppliedType(),
                    method,
                    returnedClass,
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
        return Methods.prettyColored((Method) ObjectQueryFactory.objectQuery(this.objectSupplier.getSuppliedType()).find(this.method).getLast());
    }

}
