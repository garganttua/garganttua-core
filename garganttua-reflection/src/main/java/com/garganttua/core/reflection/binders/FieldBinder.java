package com.garganttua.core.reflection.binders;

import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public class FieldBinder<OnwerType, FieldType> implements IFieldBinder<OnwerType, FieldType> {

    private ObjectAddress address;
    private IObjectSupplier<?> valueSupplier;
    private Class<OnwerType> ownerType;
    @SuppressWarnings("unused")
    private Class<FieldType> fieldType;

    public FieldBinder(Class<FieldType> fieldType, Class<OnwerType> ownerType, ObjectAddress address,
            IObjectSupplier<?> valueSupplier) {
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerType = Objects.requireNonNull(ownerType, "Owner type cannot be null");
        this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null");
    }

    @Override
    public void setValue(IObjectSupplier<OnwerType> ownerSupplier) throws ReflectionException {
        Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        try {
            if (ownerSupplier.supply().isEmpty()) {
                throw new ReflectionException("Owner supplier did not supply any object");
            }
            if (this.valueSupplier.supply().isEmpty()) {
                throw new ReflectionException("Value supplier did not supply any object");
            }

            if (!this.ownerType.isAssignableFrom(ownerSupplier.getSuppliedType())) {
                throw new ReflectionException("Type mismatch, triing to inject value in owner of type "
                        + this.ownerType.getSimpleName() + " but owner type "
                        + ownerSupplier.getSuppliedType().getSimpleName() + " was supplied");
            }

            ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).setValue(this.address,
                    this.valueSupplier.supply().get());

        } catch (SupplyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public FieldType getValue(IObjectSupplier<OnwerType> ownerSupplier) throws ReflectionException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getValue'");
    }
}
