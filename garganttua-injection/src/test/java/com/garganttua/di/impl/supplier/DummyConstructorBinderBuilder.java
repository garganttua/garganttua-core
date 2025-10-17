package com.garganttua.di.impl.supplier;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.supplier.builder.binder.AbstractConstructorBinderBuilder;

public class DummyConstructorBinderBuilder<TargetClass>
        extends AbstractConstructorBinderBuilder<TargetClass, DummyConstructorBinderBuilder<TargetClass>, Object> {

    public DummyConstructorBinderBuilder(Class<TargetClass> objectClass) {
        super(new Object(), objectClass);
    }

    @Override
    public DummyConstructorBinderBuilder<TargetClass> autoDetect(boolean b) throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'autoDetect'");
    }

    @Override
    protected DummyConstructorBinderBuilder<TargetClass> getBuilder() {
        return this;
    }

}