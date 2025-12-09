package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations={Input.class})
public class InputElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        log.atTrace()
                .log("Resolving input element");

        log.atDebug()
                .log("Preparing input supplier");

        ISupplierBuilder<?, ?> s = input(elementType);

        boolean nullable = isNullable(element);

        log.atInfo()
                .log("Resolved input element successfully");

        return new Resolved(true, elementType, s, nullable);
    }
}