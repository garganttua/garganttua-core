package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.Parameter}.
 *
 * <p>Runtime implementations wrap the actual {@code Parameter} object;
 * AOT implementations provide compile-time generated metadata.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IParameter {

	boolean isNamePresent();

	String getName();

	int getModifiers();

	IClass<?> getType();

	Type getParameterizedType();

	boolean isImplicit();

	boolean isSynthetic();

	boolean isVarArgs();

	AnnotatedType getAnnotatedType();

	// --- AnnotatedElement ---

	boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass);

	<T extends Annotation> T getAnnotation(IClass<T> annotationClass);

	Annotation[] getAnnotations();

	Annotation[] getDeclaredAnnotations();

	<T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass);

	<T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass);

	<T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass);
}
