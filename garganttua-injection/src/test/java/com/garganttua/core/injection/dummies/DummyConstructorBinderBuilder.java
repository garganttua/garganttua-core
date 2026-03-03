package com.garganttua.core.injection.dummies;

import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;

public class DummyConstructorBinderBuilder<TargetClass>
        extends AbstractConstructorBinderBuilder<TargetClass, DummyConstructorBinderBuilder<TargetClass>, Object> {

    public DummyConstructorBinderBuilder(Class<TargetClass> objectClass) {
        super(new Object(), IClass.getClass(objectClass), Set.of());
    }

    @Override
    protected void doAutoDetection() throws DslException {
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

    @Override
    protected void doPreBuildWithDependency_(Object dependency) {
        // No-op for test dummy
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No-op for test dummy
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // No-op for test dummy
    }

}
