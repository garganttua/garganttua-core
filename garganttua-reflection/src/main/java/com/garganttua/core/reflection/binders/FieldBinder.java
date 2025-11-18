package com.garganttua.core.reflection.binders;

import java.lang.reflect.Field;
import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public class FieldBinder<OnwerType, FieldType> implements IFieldBinder<OnwerType, FieldType> {

    private ObjectAddress address;
    private IObjectSupplier<?> valueSupplier;
    private IObjectSupplier<OnwerType> ownerSupplier;

    public FieldBinder(IObjectSupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            IObjectSupplier<FieldType> valueSupplier) {
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
    }

    @Override
    public void setValue() throws ReflectionException {
        try {
            if (this.ownerSupplier.supply().isEmpty()) {
                throw new ReflectionException("Owner supplier did not supply any object");
            }
            ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).setValue(this.address,
                    this.valueSupplier.supply().get());

        } catch (SupplyException e) {
            throw new ReflectionException(e);
        }
    }

    @Override
    public FieldType getValue() throws ReflectionException {
        try {
            if (ownerSupplier.supply().isEmpty()) {
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            return (FieldType) ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).getValue(this.address);
        } catch (SupplyException e) {
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getFieldReference() {
        return Fields.prettyColored(
                (Field) ObjectQueryFactory.objectQuery(ownerSupplier.getSuppliedType()).find(address).getLast());
    }
}
