package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Resolver(annotations = { Code.class })
@NoArgsConstructor
public class CodeElementResolver implements IElementResolver {

        @Override
        public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

                log.atTrace()
                                .log("Resolving code element");

                if (!Integer.class.isAssignableFrom(elementType)) {
                        log.atError()
                                        .log("Injectable is not an Integer, throwing exception");
                        throw new DiException("Injectable is not an Integer : " + elementType.getSimpleName());
                }

                log.atDebug()
                                .log("Element type is valid Integer, preparing supplier");

                ISupplierBuilder<Integer, IContextualSupplier<Integer, IRuntimeContext<Object, Object>>> s = code();

                boolean nullable = isNullable(element);

                log.atDebug()
                                .log("Resolved code element successfully");

                return new Resolved(true, elementType, s, nullable);
        }
}