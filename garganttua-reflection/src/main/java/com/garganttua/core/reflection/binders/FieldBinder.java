package com.garganttua.core.reflection.binders;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldBinder<OnwerType, FieldType> implements IFieldBinder<OnwerType, FieldType> {

    private ObjectAddress address;
    private ISupplier<?> valueSupplier;
    private ISupplier<OnwerType> ownerSupplier;

    public FieldBinder(ISupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            ISupplier<FieldType> valueSupplier) {
        log.atTrace().log("Creating FieldBinder: fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        log.atDebug().log("FieldBinder created for field {}", fieldAddress);
    }

    @Override
    public void setValue() throws ReflectionException {
        log.atTrace().log("Setting value for field {}", address);
        try {
            if (this.ownerSupplier.supply().isEmpty()) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }
            log.atDebug().log("Setting field {} value", address);
            ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).setValue(this.address,
                    this.valueSupplier.supply().get());
            log.atDebug().log("Field {} value set successfully", address);

        } catch (SupplyException e) {
            log.atError().log("Supply error setting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public FieldType getValue() throws ReflectionException {
        log.atTrace().log("Getting value for field {}", address);
        try {
            if (ownerSupplier.supply().isEmpty()) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            FieldType value = (FieldType) ObjectQueryFactory.objectQuery(ownerSupplier.supply().get()).getValue(this.address);
            log.atDebug().log("Field {} value retrieved: {}", address, value);
            return value;
        } catch (SupplyException e) {
            log.atError().log("Supply error getting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getFieldReference() {
        log.atTrace().log("Getting field reference for {}", address);
        return Fields.prettyColored(
                (Field) ObjectQueryFactory.objectQuery(ownerSupplier.getSuppliedClass()).find(address).getLast());
    }

    @Override
    public Type getSuppliedType() {
        return (Class<FieldType>) valueSupplier.getSuppliedClass();
    }

    @Override
    public java.util.Optional<FieldType> supply() throws SupplyException {
        try {
            return java.util.Optional.ofNullable(this.getValue());
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }
}
