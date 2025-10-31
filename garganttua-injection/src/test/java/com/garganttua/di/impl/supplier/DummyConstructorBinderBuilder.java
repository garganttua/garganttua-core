package com.garganttua.di.impl.supplier;

import java.util.Set;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.supplier.builder.binder.AbstractConstructorBinderBuilder;

public class DummyConstructorBinderBuilder<TargetClass>
        extends AbstractConstructorBinderBuilder<TargetClass, DummyConstructorBinderBuilder<TargetClass>, Object> {

    public DummyConstructorBinderBuilder(Class<TargetClass> objectClass) {
        super(new Object(), objectClass);
    }


    @Override
    protected DummyConstructorBinderBuilder<TargetClass> getBuilder() {
        return this;
    }


    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }


    @Override
    public Set<Class<?>> getDependencies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDependencies'");
    }

}