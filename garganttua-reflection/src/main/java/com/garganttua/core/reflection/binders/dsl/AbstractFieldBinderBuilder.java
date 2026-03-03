package com.garganttua.core.reflection.binders.dsl;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualFieldBinder;
import com.garganttua.core.reflection.binders.FieldBinder;
import com.garganttua.core.reflection.binders.IFieldBinder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFieldBinderBuilder<FieldType, OwnerType, Builder extends IFieldBinderBuilder<FieldType, OwnerType, Builder, Link>, Link>
        extends
        AbstractAutomaticLinkedDependentBuilder<Builder, Link, IFieldBinder<OwnerType, FieldType>>
        implements IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> {

    private IClass<OwnerType> ownerType;
    protected ObjectAddress address;
    private IClass<?> expectedFieldType;
    protected ISupplierBuilder<?, ?> valueSupplierBuilder;
    protected IClass<FieldType> fieldType;
    protected ISupplierBuilder<OwnerType, ? extends ISupplier<OwnerType>> ownerSupplierBuilder;
    private Boolean allowNull = false;
    private IReflection reflection;

    // Pending field config — resolved in doBuild()
    private String pendingFieldName;
    private IField pendingField;
    private ObjectAddress pendingAddress;
    private Object pendingRawValue;

    @Override
    public boolean equals(Object obj) {
        if (this.address != null) return this.address.equals(obj);
        if (this.pendingAddress != null) return this.pendingAddress.equals(obj);
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        if (this.address != null) return this.address.hashCode();
        if (this.pendingAddress != null) return this.pendingAddress.hashCode();
        return super.hashCode();
    }

    protected AbstractFieldBinderBuilder(Link link,
            ISupplierBuilder<OwnerType, ? extends ISupplier<OwnerType>> ownerSupplierBuilder,
            IClass<FieldType> fieldType, Set<DependencySpec> dependencies) throws DslException {
        super(
                link,
                Stream.concat(
                        dependencies.stream(),
                        Stream.of(DependencySpec.use(IReflectionBuilder.class, DependencyPhase.BUILD)))
                        .collect(Collectors.toUnmodifiableSet()));
        this.ownerSupplierBuilder = Objects.requireNonNull(ownerSupplierBuilder,
                "Owner supplier builder cannot be null");
        this.ownerType = ownerSupplierBuilder.getSuppliedClass();
        this.fieldType = Objects.requireNonNull(fieldType, "Field Type cannot be null");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IReflection ref) {
            this.reflection = ref;
        }
        this.doPreBuildWithDependency_(dependency);
    }

    protected abstract void doPreBuildWithDependency_(Object dependency);

    private IReflection effectiveReflection() {
        return this.reflection != null ? this.reflection : IClass.getReflection();
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            String fieldName) throws DslException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        this.pendingFieldName = fieldName;
        this.pendingField = null;
        this.pendingAddress = null;
        this.address = null;
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            IField field) throws DslException {
        Objects.requireNonNull(field, "field cannot be null");
        this.pendingField = field;
        this.pendingFieldName = null;
        this.pendingAddress = null;
        this.address = null;
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            ObjectAddress address) throws DslException {
        Objects.requireNonNull(address, "address cannot be null");
        this.pendingAddress = address;
        this.pendingFieldName = null;
        this.pendingField = null;
        this.address = null;
        return this;
    }

    private void resolveFieldAddress() throws DslException {
        if (this.address != null) return; // Already resolved

        try {
            ResolvedField resolved;

            IReflection reflection = effectiveReflection();
            if (this.pendingFieldName != null) {
                resolved = FieldResolver.fieldByFieldName(this.ownerType, reflection,
                        this.pendingFieldName, this.expectedFieldType);
                this.address = resolved.address();
            } else if (this.pendingField != null) {
                resolved = FieldResolver.fieldByField(this.ownerType, reflection,
                        this.pendingField, this.expectedFieldType);
                this.address = resolved.address();
            } else if (this.pendingAddress != null) {
                resolved = FieldResolver.fieldByAddress(this.ownerType, reflection,
                        this.pendingAddress, this.expectedFieldType);
                this.address = resolved.address();
            }
        } catch (ReflectionException e) {
            String desc = this.pendingFieldName != null ? this.pendingFieldName
                    : this.pendingField != null ? this.pendingField.getName()
                            : String.valueOf(this.pendingAddress);
            log.error("Reflection error while resolving field: {}", desc, e);
            throw new DslException("Error resolving field '" + desc + "' for " + ownerType.getName(), e);
        }
    }

    private void resolveValueSupplier() {
        if (this.pendingRawValue != null && this.valueSupplierBuilder == null) {
            IClass<?> clz = effectiveReflection().getClass(this.pendingRawValue.getClass());
            this.valueSupplierBuilder = new FixedSupplierBuilder(this.pendingRawValue, clz);
        }
    }

    @Override
    protected IFieldBinder<OwnerType, FieldType> doBuild() throws DslException {

        // Resolve field address using reflection
        resolveFieldAddress();
        // Resolve pending raw value to supplier
        resolveValueSupplier();

        Objects.requireNonNull(this.address, "Address is not set");
        Objects.requireNonNull(this.valueSupplierBuilder, "Value supplier builder is not set");

        ISupplier<FieldType> valueSupplier = (ISupplier<FieldType>) AbstractConstructorBinderBuilder
                .createNullableObjectSupplier(this.valueSupplierBuilder, this.allowNull);

        try {
            IReflection reflection = effectiveReflection();
            if (this.buildContextual())
                return new ContextualFieldBinder<>(this.ownerSupplierBuilder.build(), this.address, valueSupplier,
                        reflection);

            return new FieldBinder<>(this.ownerSupplierBuilder.build(), this.address,
                    valueSupplier, reflection);
        } catch (ReflectionException e) {
            throw new DslException("Error resolving field '" + this.address + "' for " + ownerType.getName(), e);
        }
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> withValue(Object value) throws DslException {
        Objects.requireNonNull(value, "Value cannot be null");
        // Store raw value — will be resolved in doBuild() using reflection
        this.pendingRawValue = value;
        this.valueSupplierBuilder = null;
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> withValue(
            ISupplierBuilder<?, ? extends ISupplier<?>> supplier) throws DslException {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.valueSupplierBuilder = supplier;
        this.pendingRawValue = null;
        return this;
    }

    private boolean buildContextual() {
        if (this.ownerSupplierBuilder.isContextual())
            return true;
        return false;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> allowNull(boolean allowNull) throws DslException {
        this.allowNull = Objects.requireNonNull(allowNull, "Allow null cannot be emtpy");
        return this;
    }

    protected IField findField() throws DslException {
        if (this.address == null) {
            throw new DslException("Field is not set");
        }
        try {
            ResolvedField resolved = FieldResolver.fieldByAddress(this.ownerType, effectiveReflection(), this.address);
            return resolved;
        } catch (ReflectionException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

}
