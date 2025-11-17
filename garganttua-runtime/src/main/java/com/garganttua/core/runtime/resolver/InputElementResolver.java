package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class InputElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
                IObjectSupplierBuilder<?,?> s = input(elementType);

        return new Resolved(true, elementType, s, isNullable(element));
    }

}
