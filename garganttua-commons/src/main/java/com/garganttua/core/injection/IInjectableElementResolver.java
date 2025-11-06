package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IInjectableElementResolver {

    Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> resolve(Class<?> elementType, AnnotatedElement element);

}
