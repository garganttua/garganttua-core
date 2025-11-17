package com.garganttua.core.injection.dummies;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;

public class DummyConstructorBinderBuilder<TargetClass>
        extends AbstractConstructorBinderBuilder<TargetClass, DummyConstructorBinderBuilder<TargetClass>, Object> {

    public DummyConstructorBinderBuilder(Class<TargetClass> objectClass) {
        super(new Object(), objectClass);
    }



    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

}