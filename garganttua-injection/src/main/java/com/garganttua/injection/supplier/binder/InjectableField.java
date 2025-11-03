package com.garganttua.injection.supplier.binder;

import java.util.Objects;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.GGObjectAddress;
import com.garganttua.core.reflection.GGReflectionException;
import com.garganttua.core.reflection.binders.IInjectableField;
import com.garganttua.core.reflection.query.GGObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;

public class InjectableField<OnwerType, FieldType> implements IInjectableField<OnwerType> {

    private GGObjectAddress address;
    private IObjectSupplier<?> valueSupplier;
    private Class<OnwerType> ownerType;
    private Class<FieldType> fieldType;

    public InjectableField(Class<FieldType> fieldType, Class<OnwerType> ownerType, GGObjectAddress address,
            IObjectSupplier<?> valueSupplier) {
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerType = Objects.requireNonNull(ownerType, "Owner type cannot be null");
        this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null");
    }

    @Override
    public void inject(IObjectSupplier<OnwerType> ownerSupplier) throws DiException {
        Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        if( ownerSupplier.getObject().isEmpty() ){
            throw new DiException("Owner supplier did not supply any object");
        }

        if( this.valueSupplier.getObject().isEmpty() ){
            throw new DiException("Value supplier did not supply any object");
        }

        if( !this.ownerType.isAssignableFrom(ownerSupplier.getObjectClass()) ){
            throw new DiException("Type mismatch, triing to inject value in owner of type "+this.ownerType.getSimpleName()+" but owner type "+ownerSupplier.getObjectClass().getSimpleName()+" was supplied");
        }

        try {
            GGObjectQueryFactory.objectQuery(ownerSupplier.getObject().get()).setValue(this.address, this.valueSupplier.getObject().get());
        } catch (GGReflectionException | DiException e) {
            throw new DiException(e.getMessage(), e);
        }
    }
}
