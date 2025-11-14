package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;

@FunctionalInterface
public interface IElementResolver {

    Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException;

}
