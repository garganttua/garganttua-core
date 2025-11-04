package com.garganttua.di.impl.supplier;

import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;

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