package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

@FunctionalInterface
public interface IElementResolver {

    Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> resolve(Class<?> elementType, AnnotatedElement element) throws DiException;

}
