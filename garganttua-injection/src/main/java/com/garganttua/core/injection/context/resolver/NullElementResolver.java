package com.garganttua.core.injection.context.resolver;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullElementResolver implements IElementResolver {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) {
        log.atTrace().log("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.atDebug().log("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.atDebug().log("ElementType is not null: {}", elementType);

        IObjectSupplierBuilder<?, IObjectSupplier<?>> builder = new NullObjectSupplierBuilder(elementType);
        log.atInfo().log("Created NullObjectSupplierBuilder for elementType: {}", elementType.getSimpleName());

        boolean nullable = IInjectableElementResolver.isNullable(element);
        log.atDebug().log("Element {} nullable: {}", element, nullable);

        Resolved resolved = new Resolved(true, elementType, builder, nullable);
        log.atTrace().log("Exiting resolve with Resolved: {}", resolved);

        return resolved;
    }
}
