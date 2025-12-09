package com.garganttua.core.injection.context.resolver;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={Null.class})
public class NullElementResolver implements IElementResolver {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) {
        log.atTrace().log("Entering resolve with elementType: {} and element: {}", elementType, element);

        Objects.requireNonNull(element, "Element cannot be null");
        log.atDebug().log("Element is not null: {}", element);

        Objects.requireNonNull(elementType, "ElementType cannot be null");
        log.atDebug().log("ElementType is not null: {}", elementType);

        ISupplierBuilder<?, ISupplier<?>> builder = new NullSupplierBuilder(elementType);
        log.atInfo().log("Created NullSupplierBuilder for elementType: {}", elementType.getSimpleName());

        boolean nullable = IInjectableElementResolver.isNullable(element);
        log.atDebug().log("Element {} nullable: {}", element, nullable);

        Resolved resolved = new Resolved(true, elementType, builder, nullable);
        log.atTrace().log("Exiting resolve with Resolved: {}", resolved);

        return resolved;
    }
}
