package com.garganttua.core.injection.context.dsl;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.reflection.binders.dsl.AbstractFieldBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class BeanInjectableFieldBuilder<FieldType, BeanType>
        extends AbstractFieldBinderBuilder<FieldType, BeanType, IBeanFactoryBuilder<BeanType>>
        implements IBeanInjectableFieldBuilder<FieldType, BeanType> {

    public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link,
            IBeanFactoryBuilder<BeanType> beanSupplierBuilder, Class<FieldType> fieldType) {
        super(link, beanSupplierBuilder, fieldType);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of(this.fieldType);
    }

    @Override
    public IBeanInjectableFieldBuilder<FieldType, BeanType> setBean(IObjectSupplierBuilder<BeanType, ? extends IObjectSupplier<BeanType>> beanSupplier) {
        this.ownerSupplierBuilder = Objects.requireNonNull(beanSupplier,
                "Bean supplier cannot be null");
        return this;
    }

}
