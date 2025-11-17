package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class ContextElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        if (!IRuntimeContext.class.isAssignableFrom(elementType))
            throw new DiException("Injectable is not a IRuntimeContext : " + elementType.getSimpleName());
        IObjectSupplierBuilder<IRuntimeContext<Object, Object>, IContextualObjectSupplier<IRuntimeContext<Object, Object>, IRuntimeContext<Object, Object>>> s = context();

        return new Resolved(true, elementType, s, isNullable(element));
    }

}
