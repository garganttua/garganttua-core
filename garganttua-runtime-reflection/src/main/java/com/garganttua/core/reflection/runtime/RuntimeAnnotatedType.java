package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IAnnotatedType;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

/**
 * Runtime implementation of {@link IAnnotatedType} that wraps a JDK
 * {@link AnnotatedType}.
 */
public class RuntimeAnnotatedType implements IAnnotatedType {

	private final AnnotatedType delegate;

	public RuntimeAnnotatedType(AnnotatedType delegate) {
		this.delegate = delegate;
	}

	@Override
	public Type getType() {
		return delegate.getType();
	}

	@Override
	public AnnotatedType getAnnotatedOwnerType() {
		return delegate.getAnnotatedOwnerType();
	}

	@Override
	public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
		return delegate.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return delegate.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return delegate.getDeclaredAnnotations();
	}

	@Override
	public IReflection reflection() {
		return IClass.getReflection();
	}
}
