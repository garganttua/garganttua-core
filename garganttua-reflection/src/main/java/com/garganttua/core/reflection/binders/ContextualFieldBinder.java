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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        implements IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType> {

    private ObjectAddress address;
    private IObjectSupplier<FieldType> valueSupplier;
    private IObjectSupplier<OnwerType> ownerSupplier;

    public ContextualFieldBinder(IObjectSupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            IObjectSupplier<FieldType> valueSupplier) {
        log.atTrace().log("Creating ContextualFieldBinder for fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(address, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        log.atDebug().log("ContextualFieldBinder created for field {}", fieldAddress);
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
        log.atTrace().log("setValue entry for field {}", address);
        try {
            OnwerType owner = Supplier.contextualSupply(this.ownerSupplier, ownerContext);
            FieldType value = Supplier.contextualSupply(this.valueSupplier, valueContext);

            if (owner == null) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            log.atDebug().log("Setting field {} value", address);
            ObjectQueryFactory.objectQuery(owner).setValue(this.address,
                    value);
            log.atInfo().log("Successfully set field {} value", address);

        } catch (SupplyException e) {
            log.atError().log("Supply error setting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public FieldType getValue(OwnerContextType ownerContext) throws ReflectionException {
        log.atTrace().log("getValue entry for field {}", address);
        try {

            if (ownerSupplier.supply().isEmpty()) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            log.atDebug().log("Getting field {} value", address);
            FieldType value = (FieldType) ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).getValue(this.address);
            log.atDebug().log("Successfully retrieved field {} value", address);
            return value;
        } catch (SupplyException e) {
            log.atError().log("Supply error getting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getFieldReference() {
        return Fields.prettyColored(
                (Field) ObjectQueryFactory.objectQuery(ownerSupplier.getSuppliedType()).find(address).getLast());
    }

}
