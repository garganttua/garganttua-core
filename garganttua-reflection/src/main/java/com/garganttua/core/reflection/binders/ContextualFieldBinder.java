package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

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
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        implements IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType> {

    private final ObjectAddress address;
    private final ISupplier<FieldType> valueSupplier;
    private final ISupplier<OnwerType> ownerSupplier;
    private final IReflectionProvider reflectionProvider;
    private final ResolvedField resolvedField;

    public ContextualFieldBinder(ISupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            ISupplier<FieldType> valueSupplier, IReflectionProvider reflectionProvider) throws ReflectionException {
        log.atTrace().log("Creating ContextualFieldBinder for fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.reflectionProvider = Objects.requireNonNull(reflectionProvider, "Reflection provider cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        this.resolvedField = FieldResolver.fieldByAddress(ownerSupplier.getSuppliedClass(), reflectionProvider, fieldAddress);
        log.atDebug().log("ContextualFieldBinder created for field {}", fieldAddress);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IClass<OwnerContextType> getOwnerContextType() {
        if (this.ownerSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (IClass<OwnerContextType>) contextual.getOwnerContextType();
        }
        return (IClass<OwnerContextType>) reflectionProvider.getClass(Void.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IClass<FieldContextType> getValueContextType() {
        if (this.valueSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (IClass<FieldContextType>) contextual.getOwnerContextType();
        }
        return (IClass<FieldContextType>) reflectionProvider.getClass(Void.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
            IFieldValue wrappedValue = SingleFieldValue.of(value, (IClass) valueSupplier.getSuppliedClass());
            new FieldAccessor(resolvedField).setValue(owner, wrappedValue);
            log.atDebug().log("Successfully set field {} value", address);

        } catch (SupplyException e) {
            log.atError().log("Supply error setting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public FieldType getValue(OwnerContextType ownerContext) throws ReflectionException {
        log.atTrace().log("getValue entry for field {}", address);
        try {

            if (ownerSupplier.supply().isEmpty()) {
                log.atError().log("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            log.atDebug().log("Getting field {} value", address);
            Object owner = ownerSupplier.supply().get();
            IFieldValue<?> fieldValue = new FieldAccessor<>(resolvedField).getValue(owner);
            FieldType value = (FieldType) fieldValue.first();
            log.atDebug().log("Successfully retrieved field {} value", address);
            return value;
        } catch (SupplyException e) {
            log.atError().log("Supply error getting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    @Override
    public String getFieldReference() {
        return Fields.prettyColored((IField) resolvedField.fieldPath().getLast());
    }

    @Override
    public Type getSuppliedType() {
        return valueSupplier.getSuppliedClass().getType();
    }

    @Override
    public Optional<FieldType> supply(OwnerContextType ownerContext, Object... otherContexts) throws SupplyException {
        return this.supply(ownerContext, otherContexts);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IClass<FieldType> getSuppliedClass() {
        return (IClass<FieldType>) valueSupplier.getSuppliedClass();
    }
}
