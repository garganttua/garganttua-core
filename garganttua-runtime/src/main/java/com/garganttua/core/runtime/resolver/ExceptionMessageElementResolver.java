package com.garganttua.core.runtime.resolver;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import static com.garganttua.core.runtime.RuntimeContext.*;
import static com.garganttua.core.injection.IInjectableElementResolver.*;

public class ExceptionMessageElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        if( !String.class.isAssignableFrom(elementType) )
            throw new DiException("Injectable is not a String : "+elementType.getSimpleName());

        IObjectSupplierBuilder<String, IContextualObjectSupplier<String, IRuntimeContext<Object, Object>>> s = exceptionMessage();

        return new Resolved(true, elementType, s, isNullable(element));
    }

}
