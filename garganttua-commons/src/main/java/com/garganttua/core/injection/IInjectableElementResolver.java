package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import lombok.NonNull;

public interface IInjectableElementResolver {

    Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException;

    Set<Resolved> resolve(Executable method) throws DiException;

    public static boolean isNullable(AnnotatedElement annotatedElement) {
        if (annotatedElement.getAnnotation(Nullable.class) != null)
            return true;
        if (annotatedElement.getAnnotation(NonNull.class) != null)
            return false;
        return false;
    }

}
