package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class ExceptionElementResolver implements IElementResolver {

    @SuppressWarnings("unchecked")
    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        if( !Throwable.class.isAssignableFrom(elementType) )
            throw new DiException("Injectable is not a Throwable "+elementType.getSimpleName());

        Class<? extends Throwable> exceptionType = (Class<? extends Throwable>) elementType;
        IObjectSupplierBuilder<? extends Throwable,?> s = exception(exceptionType);
        return new Resolved(true, elementType, s, isNullable(element));
    }

}
