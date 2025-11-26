package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Field;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualFieldBinder;
import com.garganttua.core.reflection.binders.FieldBinder;
import com.garganttua.core.reflection.binders.IFieldBinder;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFieldBinderBuilder<FieldType, OwnerType, Builder, Link>
        extends
        AbstractAutomaticLinkedBuilder<Builder, Link, IFieldBinder<OwnerType, FieldType>>
        implements IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> {

    private Class<OwnerType> ownerType;
    protected ObjectAddress address;
    private Class<?> expectedFieldType;
    protected IObjectSupplierBuilder<?, ?> valueSupplierBuilder;
    protected Class<FieldType> fieldType;
    protected IObjectSupplierBuilder<OwnerType, ? extends IObjectSupplier<OwnerType>> ownerSupplierBuilder;
    private Boolean allowNull = false;
    private IObjectQuery query;

    @Override
    public boolean equals(Object obj) {
        return this.address.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    protected AbstractFieldBinderBuilder(Link link,
            IObjectSupplierBuilder<OwnerType, ? extends IObjectSupplier<OwnerType>> ownerSupplierBuilder,
            Class<FieldType> fieldType) throws DslException {
        super(link);
        this.ownerSupplierBuilder = Objects.requireNonNull(ownerSupplierBuilder,
                "Owner supplier builder cannot be null");
        this.ownerType = ownerSupplierBuilder.getSuppliedType();
        this.fieldType = Objects.requireNonNull(fieldType, "Field Type cannot be null");
        try {
            this.query = ObjectQueryFactory.objectQuery(this.ownerType);
        } catch (ReflectionException e) {
            log.atError().log("[FieldBinderBuilder] Error creating objectQuery for class {}",
                    this.ownerSupplierBuilder.getSuppliedType(), e);
            throw new DslException(e.getMessage(), e);
        }
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            String fieldName) throws DslException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        try {
            this.address = FieldResolver.fieldByFieldName(fieldName, this.query, this.ownerType,
                    this.expectedFieldType);
        } catch (ReflectionException e) {
            log.error("Reflection error while resolving field by name: {}", fieldName, e);
            throw new DslException("Error resolving field '" + fieldName + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            Field field) throws DslException {
        Objects.requireNonNull(field, "field cannot be null");
        try {
            this.address = FieldResolver.fieldByField(field, this.ownerType, this.expectedFieldType);
        } catch (ReflectionException e) {
            log.error("Reflection error while resolving field: {}", field, e);
            throw new DslException("Error resolving field '" + field.getName() + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> field(
            ObjectAddress address) throws DslException {
        Objects.requireNonNull(address, "address cannot be null");
        this.address = address;
        try {
            FieldResolver.fieldByAddress(address, this.query, this.ownerType, this.expectedFieldType);
        } catch (ReflectionException e) {
            log.error("Reflection error while resolving field by address: {}", address, e);
            throw new DslException("Error resolving field at address '" + address + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    protected IFieldBinder<OwnerType, FieldType> doBuild() throws DslException {
        Objects.requireNonNull(this.address, "Address is not set");
        Objects.requireNonNull(this.valueSupplierBuilder, "Value supplier builder is not set");

        IObjectSupplier<FieldType> valueSupplier = (IObjectSupplier<FieldType>) AbstractConstructorBinderBuilder
                .createNullableObjectSupplier(this.valueSupplierBuilder, this.allowNull);

        if (this.buildContextual())
            return new ContextualFieldBinder<>(this.ownerSupplierBuilder.build(), this.address, valueSupplier);

        return new FieldBinder<>(this.ownerSupplierBuilder.build(), this.address,
                valueSupplier);
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> withValue(Object value) throws DslException {
        Objects.requireNonNull(value, "Value cannot be null");
        this.valueSupplierBuilder = FixedObjectSupplierBuilder.of(value);
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> withValue(
            IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> supplier) throws DslException {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.valueSupplierBuilder = supplier;
        return this;
    }

    private boolean buildContextual() {
        if (this.ownerSupplierBuilder.isContextual())
            return true;
        if (this.valueSupplierBuilder.isContextual())
            return true;
        return false;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Builder, Link> allowNull(boolean allowNull) throws DslException {
        this.allowNull = Objects.requireNonNull(allowNull, "Allow null cannot be emtpy");
        return this;
    }

    protected Field findField() throws DslException {
        if (this.address == null) {
            throw new DslException("Field is not set");
        }
        try {
            return (Field) this.query.find(this.address).getLast();
        } catch (ReflectionException e) {
            throw new DslException(e.getMessage(), e);
        }
    }

}
