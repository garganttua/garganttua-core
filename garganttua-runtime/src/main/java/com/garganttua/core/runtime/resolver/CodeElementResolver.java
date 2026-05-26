package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.NoArgsConstructor;

@Resolver(annotations = { Code.class })
@NoArgsConstructor
public class CodeElementResolver implements IElementResolver {
    private static final IDiagnostic log = Diagnostics.of(CodeElementResolver.class);

        @Override
        public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {

                log.trace("Resolving code element");

                if (!IClass.getClass(Integer.class).isAssignableFrom(elementType)) {
                        log.error("Injectable is not an Integer, throwing exception");
                        throw new DiException("Injectable is not an Integer : " + elementType.getSimpleName());
                }

                log.debug("Element type is valid Integer, preparing supplier");

                ISupplierBuilder<Integer, IContextualSupplier<Integer, IRuntimeContext<Object, Object>>> s = code();

                boolean nullable = isNullable(element);

                log.debug("Resolved code element successfully");

                return new Resolved(true, elementType, s, nullable);
        }
}