package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.RecordComponent}.
 *
 * <p>Runtime implementations wrap the actual {@code RecordComponent} object;
 * AOT implementations provide compile-time generated metadata.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IRecordComponent {

	String getName();

	IClass<?> getType();

	String getGenericSignature();

	Type getGenericType();

	AnnotatedType getAnnotatedType();

	IMethod getAccessor();

	IClass<?> getDeclaringRecord();

	// --- AnnotatedElement ---

	boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass);

	<T extends Annotation> T getAnnotation(IClass<T> annotationClass);

	Annotation[] getAnnotations();

	Annotation[] getDeclaredAnnotations();

	<T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass);

	<T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass);

	<T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass);
}
