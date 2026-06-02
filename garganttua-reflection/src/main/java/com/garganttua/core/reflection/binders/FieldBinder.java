package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
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

/**
 * {@link IFieldBinder} implementation that reads or writes a field reached through an
 * {@link ObjectAddress}, using suppliers for the owning object and the value to write.
 *
 * @param <OnwerType> the type owning the field
 * @param <FieldType> the field value type
 */
public class FieldBinder<OnwerType, FieldType> implements IFieldBinder<OnwerType, FieldType> {
    private static final Logger log = Logger.getLogger(FieldBinder.class);

    private final ObjectAddress address;
    private final ISupplier<?> valueSupplier;
    private final ISupplier<OnwerType> ownerSupplier;
    private final IReflectionProvider reflectionProvider;
    private final ResolvedField resolvedField;

    /**
     * Creates a field binder and resolves the field located at {@code fieldAddress}.
     *
     * @param ownerSupplier      supplier of the object owning the field
     * @param fieldAddress       dotted path locating the field within the owner
     * @param valueSupplier      supplier of the value to write
     * @param reflectionProvider provider used to resolve the field
     * @throws ReflectionException if the field cannot be resolved
     */
    public FieldBinder(ISupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            ISupplier<FieldType> valueSupplier, IReflectionProvider reflectionProvider) throws ReflectionException {
        log.trace("Creating FieldBinder: fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        this.reflectionProvider = Objects.requireNonNull(reflectionProvider, "Reflection provider cannot be null");
        this.resolvedField = FieldResolver.fieldByAddress(ownerSupplier.getSuppliedClass(), reflectionProvider, fieldAddress);
        log.debug("FieldBinder created for field {}", fieldAddress);
    }

    /**
     * Writes the supplied value into the field of the supplied owner.
     *
     * @throws ReflectionException if the owner cannot be supplied or the write fails
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    public void setValue() throws ReflectionException {
        log.trace("Setting value for field {}", address);
        try {
            if (this.ownerSupplier.supply().isEmpty()) {
                log.error("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }
            log.debug("Setting field {} value", address);
            Object owner = ownerSupplier.supply().get();
            Object value = this.valueSupplier.supply().get();
            IFieldValue wrappedValue = SingleFieldValue.of(value, (IClass) valueSupplier.getSuppliedClass());
            new FieldAccessor(resolvedField).setValue(owner, wrappedValue);
            log.debug("Field {} value set successfully", address);

        } catch (SupplyException e) {
            log.error("Supply error setting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    /**
     * Reads the field value from the supplied owner.
     *
     * @return the current field value
     * @throws ReflectionException if the owner cannot be supplied or the read fails
     */
    @Override
    public FieldType getValue() throws ReflectionException {
        log.trace("Getting value for field {}", address);
        try {
            if (ownerSupplier.supply().isEmpty()) {
                log.error("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            Object owner = ownerSupplier.supply().get();
            IFieldValue<?> fieldValue = new FieldAccessor<>(resolvedField).getValue(owner);
            FieldType value = (FieldType) fieldValue.first();
            log.debug("Field {} value retrieved: {}", address, value);
            return value;
        } catch (SupplyException e) {
            log.error("Supply error getting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    /** {@return a colored, human-readable rendering of the bound field} */
    @Override
    public String getFieldReference() {
        log.trace("Getting field reference for {}", address);
        return Fields.prettyColored((IField) resolvedField.fieldPath().getLast());
    }

    /** {@return the {@link Type} supplied for the field value} */
    @Override
    public Type getSuppliedType() {
        return valueSupplier.getSuppliedClass().getType();
    }

    /**
     * Supplies the current field value by delegating to {@link #getValue()}.
     *
     * @throws SupplyException if the value cannot be read
     */
    @Override
    public java.util.Optional<FieldType> supply() throws SupplyException {
        try {
            return java.util.Optional.ofNullable(this.getValue());
        } catch (ReflectionException e) {
            throw new SupplyException(e);
        }
    }

    /** {@return the supplied field value class} */
    @Override
    public IClass<FieldType> getSuppliedClass() {
        return (IClass<FieldType>) valueSupplier.getSuppliedClass();
    }
}
