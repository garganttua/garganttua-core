package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;

public class RuntimeRecordComponent implements IRecordComponent {

	private final RecordComponent component;

	private RuntimeRecordComponent(RecordComponent component) {
		this.component = component;
	}

	public static RuntimeRecordComponent of(RecordComponent component) {
		return new RuntimeRecordComponent(component);
	}

	public RecordComponent unwrap() {
		return component;
	}

	@Override
	public String getName() {
		return component.getName();
	}

	@Override
	public IClass<?> getType() {
		return RuntimeClass.ofUnchecked(component.getType());
	}

	@Override
	public String getGenericSignature() {
		return component.getGenericSignature();
	}

	@Override
	public Type getGenericType() {
		return component.getGenericType();
	}

	@Override
	public AnnotatedType getAnnotatedType() {
		return component.getAnnotatedType();
	}

	@Override
	public IMethod getAccessor() {
		return RuntimeMethod.of(component.getAccessor());
	}

	@Override
	public IClass<?> getDeclaringRecord() {
		return RuntimeClass.ofUnchecked(component.getDeclaringRecord());
	}

	// --- AnnotatedElement ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return component.isAnnotationPresent(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
		return component.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return component.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return component.getDeclaredAnnotations();
	}

	@Override
	public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
		return component.getAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
		return component.getDeclaredAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
		return component.getDeclaredAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof RuntimeRecordComponent other) return component.equals(other.component);
		return false;
	}

	@Override
	public int hashCode() {
		return component.hashCode();
	}

	@Override
	public String toString() {
		return component.toString();
	}
}
