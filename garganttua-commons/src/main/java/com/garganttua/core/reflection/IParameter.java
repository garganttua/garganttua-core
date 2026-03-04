package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.Parameter}.
 *
 * <p>Runtime implementations wrap the actual {@code Parameter} object;
 * AOT implementations provide compile-time generated metadata.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IParameter extends IAnnotatedElement {

	boolean isNamePresent();

	String getName();

	int getModifiers();

	IClass<?> getType();

	Type getParameterizedType();

	boolean isImplicit();

	boolean isSynthetic();

	boolean isVarArgs();

	IAnnotatedType getAnnotatedType();

	// --- AnnotatedElement (abstract in IAnnotatedElement) ---

	@Override
	<T extends Annotation> T getAnnotation(IClass<T> annotationClass);

	@Override
	Annotation[] getAnnotations();

	@Override
	Annotation[] getDeclaredAnnotations();
}
