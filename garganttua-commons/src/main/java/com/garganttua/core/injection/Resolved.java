package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public record Resolved(boolean resolved, Class<?> elementType, IObjectSupplierBuilder<?, IObjectSupplier<?>> elementSupplier, boolean nullable) {

    public static Resolved notResolved(Class<?> elementType, AnnotatedElement annotatedElement){
        return new Resolved(false, elementType, null, IInjectableElementResolver.isNullable(annotatedElement));
    }

    public void ifResolved(ResolvedAction action){
        if( resolved )
            action.ifResolved(elementSupplier, nullable);
    }

    public void ifResolvedOrElse(ResolvedAction action, NotResolvedAction naction){
        if( resolved )
            action.ifResolved(elementSupplier, nullable);
        else 
            naction.ifNotResolved(nullable);
    }

}
