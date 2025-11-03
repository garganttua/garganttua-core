package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IInjectableBuilderRegistry {

    void registerFactory(Class<? extends Annotation> annotation, IInjectableBuilderFactory factory);
    
    Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType, AnnotatedElement element);

}
