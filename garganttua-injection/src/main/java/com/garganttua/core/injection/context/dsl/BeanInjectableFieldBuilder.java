package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanInjectableFieldBuilder<FieldType, BeanType>
        extends
        AbstractFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>
        implements IBeanInjectableFieldBuilder<FieldType, BeanType> {

    public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link,
            IBeanFactoryBuilder<BeanType> beanSupplierBuilder, Class<FieldType> fieldType) throws DslException {
        super(link, beanSupplierBuilder, fieldType);
        log.atTrace().log("Initialized BeanInjectableFieldBuilder for fieldType: {} in beanClass: {}", fieldType,
                link.getSuppliedType());
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Getting dependencies for injectable field of type: {}", this.fieldType);
        return Set.of(this.fieldType);
    }

    @Override
    public IBeanInjectableFieldBuilder<FieldType, BeanType> setBean(
            IObjectSupplierBuilder<BeanType, ? extends IObjectSupplier<BeanType>> beanSupplier) {
        this.ownerSupplierBuilder = Objects.requireNonNull(beanSupplier, "Bean supplier cannot be null");
        log.atInfo().log("Set bean supplier for fieldType: {} in beanClass: {}", this.fieldType,
                beanSupplier.getSuppliedType());
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Starting auto-detection for fieldType: {} in beanClass: {}", this.fieldType,
                this.ownerSupplierBuilder.getSuppliedType());

        Field field = this.findField();
        if (field == null) {
            String message = "Field with address " + this.address + " not found in class "
                    + this.ownerSupplierBuilder.getSuppliedType().getSimpleName();
            log.atError().log(message);
            throw new DslException(message);
        }

        boolean nullable = IInjectableElementResolver.isNullable(field);
        this.allowNull(nullable);
        log.atInfo().log("Field {} auto-detected. Nullable: {}", field.getName(), nullable);
    }
}