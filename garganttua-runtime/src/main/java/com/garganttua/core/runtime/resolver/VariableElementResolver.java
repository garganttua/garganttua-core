package com.garganttua.core.runtime.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;
import static com.garganttua.core.runtime.RuntimeContext.*;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class VariableElementResolver implements IElementResolver {

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {

        Variable annotation = element.getAnnotation(Variable.class);

        if (annotation == null)
            throw new DiException("Injectable is not annotated with @Variable");

        String name = annotation.name();

        IObjectSupplierBuilder<?, ?> s = variable(name, elementType);

        return new Resolved(true, elementType, s, isNullable(element));
    }

}
