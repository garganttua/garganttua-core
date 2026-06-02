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

	/**
	 * Returns whether the parameter has a name according to the class file.
	 *
	 * @return {@code true} if the parameter name is present in the class file
	 */
	boolean isNamePresent();

	/**
	 * Returns the name of this parameter.
	 *
	 * @return the parameter name, or a synthesized name (e.g. {@code arg0}) when not present
	 */
	String getName();

	/**
	 * Returns the modifier flags for this parameter.
	 *
	 * @return the parameter modifiers encoded as an {@code int}
	 */
	int getModifiers();

	/**
	 * Returns the declared type of this parameter.
	 *
	 * @return the parameter type as an {@link IClass}
	 */
	IClass<?> getType();

	/**
	 * Returns the generic type of this parameter.
	 *
	 * @return the parameterized {@link Type} of this parameter
	 */
	Type getParameterizedType();

	/**
	 * Returns whether this parameter is implicitly declared in source code.
	 *
	 * @return {@code true} if the parameter is implicit
	 */
	boolean isImplicit();

	/**
	 * Returns whether this parameter is synthetic (compiler-generated).
	 *
	 * @return {@code true} if the parameter is synthetic
	 */
	boolean isSynthetic();

	/**
	 * Returns whether this parameter represents a variable-arity argument.
	 *
	 * @return {@code true} if this is a varargs parameter
	 */
	boolean isVarArgs();

	/**
	 * Returns the annotated type of this parameter.
	 *
	 * @return the {@link IAnnotatedType} describing the use of the parameter's type
	 */
	IAnnotatedType getAnnotatedType();

	// --- AnnotatedElement (abstract in IAnnotatedElement) ---

	@Override
	<T extends Annotation> T getAnnotation(IClass<T> annotationClass);

	@Override
	Annotation[] getAnnotations();

	@Override
	Annotation[] getDeclaredAnnotations();
}
