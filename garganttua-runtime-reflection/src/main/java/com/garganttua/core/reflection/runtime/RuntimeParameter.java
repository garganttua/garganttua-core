package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IParameter;

public class RuntimeParameter implements IParameter {

	private final Parameter parameter;

	private RuntimeParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	public static RuntimeParameter of(Parameter parameter) {
		return new RuntimeParameter(parameter);
	}

	public Parameter unwrap() {
		return parameter;
	}

	@Override
	public boolean isNamePresent() {
		return parameter.isNamePresent();
	}

	@Override
	public String getName() {
		return parameter.getName();
	}

	@Override
	public int getModifiers() {
		return parameter.getModifiers();
	}

	@Override
	public IClass<?> getType() {
		return RuntimeClass.ofUnchecked(parameter.getType());
	}

	@Override
	public Type getParameterizedType() {
		return parameter.getParameterizedType();
	}

	@Override
	public boolean isImplicit() {
		return parameter.isImplicit();
	}

	@Override
	public boolean isSynthetic() {
		return parameter.isSynthetic();
	}

	@Override
	public boolean isVarArgs() {
		return parameter.isVarArgs();
	}

	@Override
	public AnnotatedType getAnnotatedType() {
		return parameter.getAnnotatedType();
	}

	// --- AnnotatedElement ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return parameter.isAnnotationPresent(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
		return parameter.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return parameter.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return parameter.getDeclaredAnnotations();
	}

	@Override
	public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
		return parameter.getAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
		return parameter.getDeclaredAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
		return parameter.getDeclaredAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof RuntimeParameter other) return parameter.equals(other.parameter);
		return false;
	}

	@Override
	public int hashCode() {
		return parameter.hashCode();
	}

	@Override
	public String toString() {
		return parameter.toString();
	}
}
