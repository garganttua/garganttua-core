package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectableBuilderFactory;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class PrototypeBuilderFactory extends BeanBuilderFactory
        implements IInjectableBuilderFactory {

    public PrototypeBuilderFactory(Set<Class<? extends Annotation>> qualifiers) {
        super(qualifiers);
    }

    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType, AnnotatedElement parameter) {
        return this.createBuilder(elementType, parameter, BeanStrategy.prototype);
    }
}