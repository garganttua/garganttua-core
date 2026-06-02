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

	/**
	 * Returns the name of this record component.
	 *
	 * @return the component name as declared in the record header
	 */
	String getName();

	/**
	 * Returns the declared type of this record component.
	 *
	 * @return the component type
	 */
	IClass<?> getType();

	/**
	 * Returns the generic signature of this record component, if any.
	 *
	 * @return the generic signature string, or {@code null} when not generic
	 */
	String getGenericSignature();

	/**
	 * Returns the generic type of this record component.
	 *
	 * @return the resolved generic {@link Type}
	 */
	Type getGenericType();

	/**
	 * Returns the annotated type of this record component.
	 *
	 * @return the {@link AnnotatedType} of the component
	 */
	AnnotatedType getAnnotatedType();

	/**
	 * Returns the accessor method that reads this component.
	 *
	 * @return the accessor {@link IMethod}
	 */
	IMethod getAccessor();

	/**
	 * Returns the record type declaring this component.
	 *
	 * @return the declaring record class
	 */
	IClass<?> getDeclaringRecord();

	// --- AnnotatedElement ---

	/**
	 * Checks whether the given annotation is present on this component.
	 *
	 * @param annotationClass the annotation type to look for
	 * @return {@code true} if the annotation is present
	 */
	boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass);

	/**
	 * Returns the component's annotation of the given type, if present.
	 *
	 * @param <T>             the annotation type
	 * @param annotationClass the annotation type to retrieve
	 * @return the annotation instance, or {@code null} if absent
	 */
	<T extends Annotation> T getAnnotation(IClass<T> annotationClass);

	/**
	 * Returns all annotations present on this component.
	 *
	 * @return the annotations array (never {@code null}, may be empty)
	 */
	Annotation[] getAnnotations();

	/**
	 * Returns the annotations directly declared on this component.
	 *
	 * @return the declared annotations array (never {@code null}, may be empty)
	 */
	Annotation[] getDeclaredAnnotations();

	/**
	 * Returns all annotations of the given (possibly repeatable) type present on this component.
	 *
	 * @param <T>             the annotation type
	 * @param annotationClass the annotation type to retrieve
	 * @return the matching annotations (never {@code null}, may be empty)
	 */
	<T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass);

	/**
	 * Returns the directly declared annotation of the given type, if present.
	 *
	 * @param <T>             the annotation type
	 * @param annotationClass the annotation type to retrieve
	 * @return the declared annotation instance, or {@code null} if absent
	 */
	<T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass);

	/**
	 * Returns the directly declared annotations of the given (possibly repeatable) type.
	 *
	 * @param <T>             the annotation type
	 * @param annotationClass the annotation type to retrieve
	 * @return the matching declared annotations (never {@code null}, may be empty)
	 */
	<T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass);
}
