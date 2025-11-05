package com.garganttua.core.reflection.binders.dsl;

import java.lang.reflect.Field;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.ContextualFieldBinder;
import com.garganttua.core.reflection.binders.FieldBinder;
import com.garganttua.core.reflection.binders.IFieldBinder;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFieldBinderBuilder<FieldType, OwnerType, Link>
        extends
        AbstractLinkedBuilder<Link, IFieldBinder<OwnerType, FieldType>>
        implements IFieldBinderBuilder<FieldType, OwnerType, Link> {

    private Class<OwnerType> ownerType;

    protected ObjectAddress address;
    private Class<?> expectedFieldType;

    @Override
    public boolean equals(Object obj) {
        return this.address.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    protected IObjectSupplierBuilder<?, ?> valueSupplierBuilder;

    protected Class<FieldType> fieldType;

    private IObjectSupplierBuilder<OwnerType, IObjectSupplier<OwnerType>> ownerSupplier;

    private Boolean allowNull;

    protected AbstractFieldBinderBuilder(Link link,
            IObjectSupplierBuilder<OwnerType, IObjectSupplier<OwnerType>> ownerSupplier, Class<FieldType> fieldType) {
        super(link);
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "Owner supplier cannot be null");
        this.ownerType = ownerSupplier.getSuppliedType();
        this.fieldType = Objects.requireNonNull(fieldType, "Field Type cannot be null");
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Link> field(
            String fieldName) throws DslException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        try {
            IObjectQuery query = ObjectQueryFactory.objectQuery(this.ownerType);
            this.address = FieldResolver.fieldByFieldName(fieldName, query, this.ownerType, this.expectedFieldType);
        } catch (ReflectionException e) {
            log.error("Reflection error while resolving field by name: {}", fieldName, e);
            throw new DslException("Error resolving field '" + fieldName + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Link> field(
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
    public IFieldBinderBuilder<FieldType, OwnerType, Link> field(
            ObjectAddress address) throws DslException {
        Objects.requireNonNull(address, "address cannot be null");
        this.address = address;
        try {
            IObjectQuery query = ObjectQueryFactory.objectQuery(this.ownerType);
            FieldResolver.fieldByAddress(address, query, this.ownerType, this.expectedFieldType);
        } catch (ReflectionException e) {
            log.error("Reflection error while resolving field by address: {}", address, e);
            throw new DslException("Error resolving field at address '" + address + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IFieldBinder<OwnerType, FieldType> build() throws DslException {
        Objects.requireNonNull(this.address, "Address is not set");
        Objects.requireNonNull(this.valueSupplierBuilder, "Value supplier builder is not set");

        IObjectSupplier<FieldType> valueSupplier = (IObjectSupplier<FieldType>) AbstractConstructorBinderBuilder
                .createNullableObjectSupplier(this.valueSupplierBuilder, this.allowNull);

        if (this.buildContextual())
            return new ContextualFieldBinder<>(this.ownerSupplier.build(), this.address, valueSupplier);

        return new FieldBinder<>(this.ownerSupplier.build(), this.address,
                valueSupplier);
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Link> withValue(Object value) throws DslException {
        Objects.requireNonNull(value, "Value cannot be null");
        this.valueSupplierBuilder = FixedObjectSupplierBuilder.of(value);
        return this;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Link> withValue(
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.valueSupplierBuilder = supplier;
        return this;
    }

    private boolean buildContextual() {
        if (this.ownerSupplier.isContextual())
            return true;
        if (this.valueSupplierBuilder.isContextual())
            return true;
        return false;
    }

    @Override
    public IFieldBinderBuilder<FieldType, OwnerType, Link> allowNull(boolean allowNull) throws DslException {
        this.allowNull = Objects.requireNonNull(allowNull, "Allow null cannot be emtpy");
        return this;
    }

}
