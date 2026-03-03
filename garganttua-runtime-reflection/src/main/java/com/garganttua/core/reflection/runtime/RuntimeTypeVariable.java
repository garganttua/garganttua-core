package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IGenericDeclaration;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * Runtime implementation of {@link ITypeVariable} that wraps a JDK
 * {@link TypeVariable}.
 *
 * @param <D> the type of generic declaration that declared the underlying type variable
 */
public class RuntimeTypeVariable<D extends IGenericDeclaration> implements ITypeVariable<D> {

	private final TypeVariable<?> delegate;
	private final D declaration;

	public RuntimeTypeVariable(TypeVariable<?> delegate, D declaration) {
		this.delegate = delegate;
		this.declaration = declaration;
	}

	@Override
	public Type[] getBounds() {
		return delegate.getBounds();
	}

	@Override
	public D getGenericDeclaration() {
		return declaration;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public AnnotatedType[] getAnnotatedBounds() {
		return delegate.getAnnotatedBounds();
	}

	@Override
	public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) {
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

	@Override
	public String getTypeName() {
		return delegate.getTypeName();
	}
}
