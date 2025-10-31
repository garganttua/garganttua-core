package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

public class SingletonBuilderFactory extends BeanBuilderFactory
        implements IInjectableBuilderFactory {

    public SingletonBuilderFactory(Set<Class<? extends Annotation>> qualifiers) {
        super(qualifiers);
    }

    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType,
            AnnotatedElement element) {
        return this.createBuilder(elementType, element, BeanStrategy.singleton);
    }
}
