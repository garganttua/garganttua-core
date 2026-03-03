package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IFieldValue;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.reflection.fields.SingleFieldValue;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldBinder<OnwerType, FieldType> implements IFieldBinder<OnwerType, FieldType> {

    private final ObjectAddress address;
    private final ISupplier<?> valueSupplier;
    private final ISupplier<OnwerType> ownerSupplier;
    private final IReflectionProvider reflectionProvider;
    private final ResolvedField resolvedField;

    public FieldBinder(ISupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            ISupplier<FieldType> valueSupplier, IReflectionProvider reflectionProvider) throws ReflectionException {
        log.atTrace().log("Creating FieldBinder: fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        this.reflectionProvider = Objects.requireNonNull(reflectionProvider, "Reflection provider cannot be null");
        this.resolvedField = FieldResolver.fieldByAddress(ownerSupplier.getSuppliedClass(), reflectionProvider, fieldAddress);
        log.atDebug().log("FieldBinder created for field {}", fieldAddress);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setValue() throws ReflectionException {
        log.atTrace().log("Setting value for field {}", address);
        try {
            if (this.ownerSupplier.supply().isEmpty()) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }
            log.atDebug().log("Setting field {} value", address);
            Object owner = ownerSupplier.supply().get();
            Object value = this.valueSupplier.supply().get();
            IFieldValue wrappedValue = SingleFieldValue.of(value, (IClass) valueSupplier.getSuppliedClass());
            new FieldAccessor(resolvedField).setValue(owner, wrappedValue);
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

            Object owner = ownerSupplier.supply().get();
            IFieldValue<?> fieldValue = new FieldAccessor<>(resolvedField).getValue(owner);
            FieldType value = (FieldType) fieldValue.first();
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
        return Fields.prettyColored((IField) resolvedField.fieldPath().getLast());
    }

    @Override
    public Type getSuppliedType() {
        return valueSupplier.getSuppliedClass().getType();
    }

    @Override
    public java.util.Optional<FieldType> supply() throws SupplyException {
        try {
            return java.util.Optional.ofNullable(this.getValue());
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    @Override
    public IClass<FieldType> getSuppliedClass() {
        return (IClass<FieldType>) valueSupplier.getSuppliedClass();
    }
}
