package com.garganttua.core.injection.context.resolver;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

public class NullElementResolver implements IElementResolver {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> resolve(Class<?> elementType,
            AnnotatedElement element) {
        Objects.requireNonNull(element, "Element cannot be null");
        Objects.requireNonNull(elementType, "ElementType cannot be null");
        IObjectSupplierBuilder<?, IObjectSupplier<?>> builder = new NullObjectSupplierBuilder(elementType);
        return Optional.ofNullable(builder);
    }

}
