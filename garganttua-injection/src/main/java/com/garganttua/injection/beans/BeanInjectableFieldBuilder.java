package com.garganttua.injection.beans;

import java.util.Set;

import com.garganttua.injection.supplier.builder.binder.AbstractInjectableFieldBuilder;

public class BeanInjectableFieldBuilder<FieldType, BeanType> extends AbstractInjectableFieldBuilder<FieldType, BeanType, IBeanFactoryBuilder<BeanType>> implements IBeanInjectableFieldBuilder<FieldType, BeanType> {

    public BeanInjectableFieldBuilder(IBeanFactoryBuilder<BeanType> link, Class<BeanType> beanType, Class<FieldType> fieldType) {
        super(link, beanType, fieldType);
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of(this.fieldType);
    }

}
