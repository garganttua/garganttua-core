package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BeanInjectableFieldBuilder<FieldType, BeanType>
        extends
        AbstractFieldBinderBuilder<FieldType, BeanType, IBeanInjectableFieldBuilder<FieldType, BeanType>, IBeanFactoryBuilder<BeanType>>
        implements IBeanInjectableFieldBuilder<FieldType, BeanType> {

    public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link,
            IBeanFactoryBuilder<BeanType> beanSupplierBuilder, Class<FieldType> fieldType) throws DslException {
        super(link, beanSupplierBuilder, fieldType);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of(this.fieldType);
    }

    @Override
    public IBeanInjectableFieldBuilder<FieldType, BeanType> setBean(
            IObjectSupplierBuilder<BeanType, ? extends IObjectSupplier<BeanType>> beanSupplier) {
        this.ownerSupplierBuilder = Objects.requireNonNull(beanSupplier,
                "Bean supplier cannot be null");
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {

        Field field = this.findField();
        if (field == null)
            throw new DslException("Field with address " + this.address + " not found in class "
                    + this.ownerSupplierBuilder.getSuppliedType().getSimpleName());

        if (field.getAnnotation(Nullable.class) != null)
            this.allowNull(true);
        if (field.getAnnotation(Nonnull.class) != null)
            this.allowNull(false);

    }

}
