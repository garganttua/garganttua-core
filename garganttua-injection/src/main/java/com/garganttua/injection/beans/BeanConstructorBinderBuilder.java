package com.garganttua.injection.beans;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.supplier.builder.binder.AbstractConstructorBinderBuilder;

public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType) {
        super(link, beanType);
    }

    @Override
    protected IBeanConstructorBinderBuilder<Bean> getBuilder() {
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
    
    }
}