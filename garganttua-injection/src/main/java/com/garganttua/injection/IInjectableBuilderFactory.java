package com.garganttua.injection;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;

@FunctionalInterface
public interface IInjectableBuilderFactory {

    Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> createBuilder(Class<?> elementType, AnnotatedElement element);

}
