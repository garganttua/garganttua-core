package com.garganttua.core.reflection.binders;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

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
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.Supplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Context-aware {@link FieldBinder} variant. Resolves the owner and value via contextual suppliers
 * and reads or writes a field reached through an {@link ObjectAddress}.
 *
 * @param <OnwerType>        the type owning the field
 * @param <FieldType>        the field value type
 * @param <OwnerContextType> the context type required to supply the owner
 * @param <FieldContextType> the context type required to supply the value
 */
public class ContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType>
        implements IContextualFieldBinder<OnwerType, FieldType, OwnerContextType, FieldContextType> {
    private static final Logger log = Logger.getLogger(ContextualFieldBinder.class);

    private final ObjectAddress address;
    private final ISupplier<FieldType> valueSupplier;
    private final ISupplier<OnwerType> ownerSupplier;
    private final IReflectionProvider reflectionProvider;
    private final ResolvedField resolvedField;

    /**
     * Creates a contextual field binder and resolves the field located at {@code fieldAddress}.
     *
     * @param ownerSupplier      supplier of the object owning the field
     * @param fieldAddress       dotted path locating the field within the owner
     * @param valueSupplier      supplier of the value to write
     * @param reflectionProvider provider used to resolve the field
     * @throws ReflectionException if the field cannot be resolved
     */
    public ContextualFieldBinder(ISupplier<OnwerType> ownerSupplier, ObjectAddress fieldAddress,
            ISupplier<FieldType> valueSupplier, IReflectionProvider reflectionProvider) throws ReflectionException {
        log.trace("Creating ContextualFieldBinder for fieldAddress={}", fieldAddress);
        this.address = Objects.requireNonNull(fieldAddress, "Address cannot be null");
        this.reflectionProvider = Objects.requireNonNull(reflectionProvider, "Reflection provider cannot be null");
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        this.resolvedField = FieldResolver.fieldByAddress(ownerSupplier.getSuppliedClass(), reflectionProvider, fieldAddress);
        log.debug("ContextualFieldBinder created for field {}", fieldAddress);
    }

    /** {@return the context type required to supply the owner, or {@code Void} when non-contextual} */
    @SuppressWarnings("unchecked")
    @Override
    public IClass<OwnerContextType> getOwnerContextType() {
        if (this.ownerSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (IClass<OwnerContextType>) contextual.getOwnerContextType();
        }
        return (IClass<OwnerContextType>) reflectionProvider.getClass(Void.class);
    }

    /** {@return the context type required to supply the value, or {@code Void} when non-contextual} */
    @SuppressWarnings("unchecked")
    @Override
    public IClass<FieldContextType> getValueContextType() {
        if (this.valueSupplier instanceof IContextualSupplier<?, ?> contextual) {
            return (IClass<FieldContextType>) contextual.getOwnerContextType();
        }
        return (IClass<FieldContextType>) reflectionProvider.getClass(Void.class);
    }

    /**
     * Writes the supplied value into the field, resolving owner and value from the given contexts.
     *
     * @param ownerContext context used to supply the owner
     * @param valueContext context used to supply the value
     * @throws ReflectionException if the owner cannot be supplied or the write fails
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setValue(OwnerContextType ownerContext, FieldContextType valueContext) throws ReflectionException {
        log.trace("setValue entry for field {}", address);
        try {
            OnwerType owner = Supplier.contextualSupply(this.ownerSupplier, ownerContext);
            FieldType value = Supplier.contextualSupply(this.valueSupplier, valueContext);

            if (owner == null) {
                log.error("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            log.debug("Setting field {} value", address);
            IFieldValue wrappedValue = SingleFieldValue.of(value, (IClass) valueSupplier.getSuppliedClass());
            new FieldAccessor(resolvedField).setValue(owner, wrappedValue);
            log.debug("Successfully set field {} value", address);

        } catch (SupplyException e) {
            log.error("Supply error setting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    /**
     * Reads the field value from the owner supplied for the given context.
     *
     * @param ownerContext context used to supply the owner
     * @return the current field value
     * @throws ReflectionException if the owner cannot be supplied or the read fails
     */
    @SuppressWarnings("unchecked")
    @Override
    public FieldType getValue(OwnerContextType ownerContext) throws ReflectionException {
        log.trace("getValue entry for field {}", address);
        try {

            if (ownerSupplier.supply().isEmpty()) {
                log.error("Owner supplier did not supply any object for field {}", address);
                throw new ReflectionException("Owner supplier did not supply any object");
            }

            log.debug("Getting field {} value", address);
            Object owner = ownerSupplier.supply().get();
            IFieldValue<?> fieldValue = new FieldAccessor<>(resolvedField).getValue(owner);
            FieldType value = (FieldType) fieldValue.first();
            log.debug("Successfully retrieved field {} value", address);
            return value;
        } catch (SupplyException e) {
            log.error("Supply error getting field {}", address, e);
            throw new ReflectionException(e);
        }
    }

    /** {@return a colored, human-readable rendering of the bound field} */
    @Override
    public String getFieldReference() {
        return Fields.prettyColored((IField) resolvedField.fieldPath().getLast());
    }

    /** {@return the {@link Type} supplied for the field value} */
    @Override
    public Type getSuppliedType() {
        return valueSupplier.getSuppliedClass().getType();
    }

    /**
     * Supplies the value to be written into the field, resolved from the given contexts.
     *
     * @param ownerContext  context used to supply the owner
     * @param otherContexts additional contexts forwarded to the value supplier
     * @throws SupplyException if the value cannot be supplied
     */
    @Override
    public Optional<FieldType> supply(OwnerContextType ownerContext, Object... otherContexts) throws SupplyException {
        Object[] contexts = new Object[otherContexts.length + 1];
        contexts[0] = ownerContext;
        System.arraycopy(otherContexts, 0, contexts, 1, otherContexts.length);
        return Optional.ofNullable(Supplier.contextualSupply(this.valueSupplier, contexts));
    }

    /** {@return the supplied field value class} */
    @Override
    @SuppressWarnings("unchecked")
    public IClass<FieldType> getSuppliedClass() {
        return (IClass<FieldType>) valueSupplier.getSuppliedClass();
    }
}
