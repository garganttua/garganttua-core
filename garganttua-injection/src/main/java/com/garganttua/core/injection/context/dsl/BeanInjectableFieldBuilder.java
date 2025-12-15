package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanInjectableFieldBuilder<FieldType, BeanType>
        extends
        AbstractFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>
        implements IBeanInjectableFieldBuilder<FieldType, BeanType> {

    public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link,
            IBeanFactoryBuilder<BeanType> beanSupplierBuilder, Class<FieldType> fieldType) throws DslException {
        super(link, beanSupplierBuilder, fieldType);
        log.atTrace().log("Entering BeanInjectableFieldBuilder constructor with link: {}, beanSupplierBuilder: {}, fieldType: {}",
                link, beanSupplierBuilder, fieldType);
        log.atInfo().log("BeanInjectableFieldBuilder initialized for fieldType: {} in beanClass: {}", fieldType,
                link.getSuppliedClass());
        log.atTrace().log("Exiting BeanInjectableFieldBuilder constructor");
    }

    @Override
    public Set<Class<?>> dependencies() {
        log.atTrace().log("Entering getDependencies() for injectable field of type: {}", this.fieldType);
        Set<Class<?>> dependencies = Set.of(this.fieldType);
        log.atDebug().log("Dependencies for injectable field: {}", dependencies);
        log.atTrace().log("Exiting getDependencies()");
        return dependencies;
    }

    @Override
    public IBeanInjectableFieldBuilder<FieldType, BeanType> valueSupplier(
            ISupplierBuilder<BeanType, ? extends ISupplier<BeanType>> valueSupplier) {
        log.atTrace().log("Entering valueSupplier() with valueSupplier: {}", valueSupplier);
        this.ownerSupplierBuilder = Objects.requireNonNull(valueSupplier, "Value supplier cannot be null");
        log.atDebug().log("Set ownerSupplierBuilder for fieldType: {} to supplier of type: {}", this.fieldType,
                valueSupplier.getSuppliedClass());
        log.atInfo().log("Value supplier set for fieldType: {} in beanClass: {}", this.fieldType,
                valueSupplier.getSuppliedClass());
        log.atTrace().log("Exiting valueSupplier()");
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() for fieldType: {} in beanClass: {}", this.fieldType,
                this.ownerSupplierBuilder.getSuppliedClass());

        log.atDebug().log("Finding field with address: {}", this.address);
        Field field = this.findField();
        if (field == null) {
            String message = "Field with address " + this.address + " not found in class "
                    + this.ownerSupplierBuilder.getSuppliedClass().getSimpleName();
            log.atError().log(message);
            throw new DslException(message);
        }

        log.atDebug().log("Found field: {} in class: {}", field.getName(), field.getDeclaringClass().getSimpleName());
        boolean nullable = IInjectableElementResolver.isNullable(field);
        this.allowNull(nullable);
        log.atInfo().log("Field {} auto-detected. Nullable: {}", field.getName(), nullable);
        log.atTrace().log("Exiting doAutoDetection()");
    }

    @Override
    public Field field() {
        log.atTrace().log("Entering field() for fieldType: {}", this.fieldType);
        Field foundField = this.findField();
        log.atDebug().log("Retrieved field: {}", foundField != null ? foundField.getName() : "null");
        log.atTrace().log("Exiting field()");
        return foundField;
    }
}