package com.garganttua.core.reflection.binders;

import java.lang.reflect.Field;
import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

public class ContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        implements IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType> {

    private ObjectAddress address;
    private IObjectSupplier<FieldType> valueSupplier;
    private IObjectSupplier<OnwerType> ownerSupplier;

    public ContextualFieldBinder(IObjectSupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            IObjectSupplier<FieldType> valueSupplier) {
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
    }

    @Override
    public Class<OwnerContextType> getOwnerContextType() {
        if (this.ownerSupplier instanceof IContextualObjectSupplier<?, ?> contextual) {
            return (Class<OwnerContextType>) contextual.getOwnerContextType();
        }
        return (Class<OwnerContextType>) Void.class;
    }

    @Override
    public Class<FieldContextType> getValueContextType() {
        if (this.valueSupplier instanceof IContextualObjectSupplier<?, ?> contextual) {
            return (Class<FieldContextType>) contextual.getOwnerContextType();
        }
        return (Class<FieldContextType>) Void.class;
    }

    @Override
    public void setValue(OwnerContextType ownerContext, FieldContextType valueContext) throws ReflectionException {
        try {
            OnwerType owner = Supplier.contextualSupply(this.ownerSupplier, ownerContext);
            FieldType value = Supplier.contextualSupply(this.valueSupplier, valueContext);

            if (owner == null) {
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            ObjectQueryFactory.objectQuery(owner).setValue(this.address,
                    value);

        } catch (SupplyException e) {
            throw new ReflectionException(e);
        }
    }

    @Override
    public FieldType getValue(OwnerContextType ownerContext) throws ReflectionException {
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
