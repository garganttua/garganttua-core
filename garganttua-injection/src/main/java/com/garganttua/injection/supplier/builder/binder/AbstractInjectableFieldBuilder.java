package com.garganttua.injection.supplier.builder.binder;

import java.lang.reflect.Field;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.GGObjectAddress;
import com.garganttua.core.reflection.GGReflectionException;
import com.garganttua.core.reflection.IGGObjectQuery;
import com.garganttua.core.reflection.binders.IInjectableField;
import com.garganttua.core.reflection.binders.dsl.IInjectableFieldBuilder;
import com.garganttua.core.reflection.fields.FieldResolver;
import com.garganttua.core.reflection.query.GGObjectQueryFactory;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.dsl.AbstractLinkedBuilder;
import com.garganttua.injection.supplier.binder.InjectableField;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractInjectableFieldBuilder<FieldType, OwnerType, Link>
        extends
        AbstractLinkedBuilder<Link, IInjectableField<OwnerType>>
        implements IInjectableFieldBuilder<FieldType, OwnerType, Link> {

    private Class<OwnerType> ownerType;

    protected GGObjectAddress address;
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

    protected AbstractInjectableFieldBuilder(Link link, Class<OwnerType> ownerType, Class<FieldType> fieldType) {
        super(link);
        this.ownerType = Objects.requireNonNull(ownerType, "OwnerType Type cannot be null");
        this.fieldType = Objects.requireNonNull(fieldType, "Field Type cannot be null");
    }

    @Override
    public IInjectableFieldBuilder<FieldType, OwnerType, Link> field(
            String fieldName) throws DslException {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        try {
            IGGObjectQuery query = GGObjectQueryFactory.objectQuery(this.ownerType);
            this.address = FieldResolver.fieldByFieldName(fieldName, query, this.ownerType, this.expectedFieldType);
        } catch (GGReflectionException | DiException e) {
            log.error("Reflection error while resolving field by name: {}", fieldName, e);
            throw new DslException("Error resolving field '" + fieldName + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IInjectableFieldBuilder<FieldType, OwnerType, Link> field(
            Field field) throws DslException {
        Objects.requireNonNull(field, "field cannot be null");
        try {
            this.address = FieldResolver.fieldByField(field, this.ownerType, this.expectedFieldType);
        } catch (DiException e) {
            log.error("Reflection error while resolving field: {}", field, e);
            throw new DslException("Error resolving field '" + field.getName() + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IInjectableFieldBuilder<FieldType, OwnerType, Link> field(
            GGObjectAddress address) throws DslException {
        Objects.requireNonNull(address, "address cannot be null");
        this.address = address;
        try {
            IGGObjectQuery query = GGObjectQueryFactory.objectQuery(this.ownerType);
            FieldResolver.fieldByAddress(address, query, this.ownerType, this.expectedFieldType);
        } catch (GGReflectionException | DiException e) {
            log.error("Reflection error while resolving field by address: {}", address, e);
            throw new DslException("Error resolving field at address '" + address + "' for " + ownerType.getName(), e);
        }
        return this;
    }

    @Override
    public IInjectableField<OwnerType> build() throws DslException {
        Objects.requireNonNull(this.address, "Address is not set");
        Objects.requireNonNull(this.valueSupplierBuilder, "Value supplier builder is not set");

        IInjectableField<OwnerType> result = new InjectableField<>(this.fieldType, this.ownerType, this.address,
                this.valueSupplierBuilder.build());

        return result;
    }

    @Override
    public IInjectableFieldBuilder<FieldType, OwnerType, Link> withValue(Object value) throws DslException {
        Objects.requireNonNull(value, "Value cannot be null");
        this.valueSupplierBuilder = FixedObjectSupplierBuilder.of(value);
        return this;
    }

    @Override
    public IInjectableFieldBuilder<FieldType, OwnerType, Link> withValue(
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.valueSupplierBuilder = supplier;
        return this;
    }

}
