package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IParameter;

public class RuntimeConstructor<T> implements IConstructor<T> {

	private static final ConcurrentHashMap<Constructor<?>, RuntimeConstructor<?>> CACHE = new ConcurrentHashMap<>();

	private final Constructor<T> constructor;

	private RuntimeConstructor(Constructor<T> constructor) {
		this.constructor = constructor;
	}

	@SuppressWarnings("unchecked")
	public static <T> RuntimeConstructor<T> of(Constructor<T> constructor) {
		return (RuntimeConstructor<T>) CACHE.computeIfAbsent(constructor, k -> new RuntimeConstructor<>(constructor));
	}

	@SuppressWarnings("unchecked")
	public static RuntimeConstructor<?> ofUnchecked(Constructor<?> constructor) {
		return CACHE.computeIfAbsent(constructor, k -> new RuntimeConstructor<>(k));
	}

	public Constructor<T> unwrap() {
		return constructor;
	}

	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> unwrap(IConstructor<T> iconstructor) {
		if (iconstructor instanceof RuntimeConstructor<T> rc) return rc.constructor;
		throw new IllegalArgumentException("Cannot unwrap non-RuntimeConstructor IConstructor: " + iconstructor.getClass());
	}

	// --- Member ---

	@Override
	@SuppressWarnings("unchecked")
	public IClass<T> getDeclaringClass() {
		return (IClass<T>) RuntimeClass.ofUnchecked(constructor.getDeclaringClass());
	}

	@Override
	public String getName() {
		return constructor.getName();
	}

	@Override
	public int getModifiers() {
		return constructor.getModifiers();
	}

	@Override
	public boolean isSynthetic() {
		return constructor.isSynthetic();
	}

	// --- AccessibleObject ---

	@Override
	public void setAccessible(boolean flag) {
		constructor.setAccessible(flag);
	}

	@Override
	public boolean trySetAccessible() {
		return constructor.trySetAccessible();
	}

	@Override
	public boolean canAccess(Object obj) {
		return constructor.canAccess(obj);
	}

	// --- GenericDeclaration ---

	@Override
	public TypeVariable<?>[] getTypeParameters() {
		return constructor.getTypeParameters();
	}

	// --- Parameters ---

	@Override
	public IClass<?>[] getParameterTypes() {
		return Arrays.stream(constructor.getParameterTypes())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public Type[] getGenericParameterTypes() {
		return constructor.getGenericParameterTypes();
	}

	@Override
	public int getParameterCount() {
		return constructor.getParameterCount();
	}

	@Override
	public IParameter[] getParameters() {
		return Arrays.stream(constructor.getParameters())
				.map(RuntimeParameter::of)
				.toArray(IParameter[]::new);
	}

	// --- Exceptions ---

	@Override
	public IClass<?>[] getExceptionTypes() {
		return Arrays.stream(constructor.getExceptionTypes())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public Type[] getGenericExceptionTypes() {
		return constructor.getGenericExceptionTypes();
	}

	// --- Constructor properties ---

	@Override
	public boolean isVarArgs() {
		return constructor.isVarArgs();
	}

	@Override
	public String toGenericString() {
		return constructor.toGenericString();
	}

	// --- Instantiation ---

	@Override
	public T newInstance(Object... initargs)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return constructor.newInstance(initargs);
	}

	// --- Annotated types ---

	@Override
	public AnnotatedType getAnnotatedReturnType() {
		return constructor.getAnnotatedReturnType();
	}

	@Override
	public AnnotatedType[] getAnnotatedParameterTypes() {
		return constructor.getAnnotatedParameterTypes();
	}

	@Override
	public AnnotatedType[] getAnnotatedExceptionTypes() {
		return constructor.getAnnotatedExceptionTypes();
	}

	@Override
	public AnnotatedType getAnnotatedReceiverType() {
		return constructor.getAnnotatedReceiverType();
	}

	// --- AnnotatedElement (IClass overloads) ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return constructor.isAnnotationPresent(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) {
		return constructor.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A[] getAnnotationsByType(IClass<A> annotationClass) {
		return constructor.getAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A getDeclaredAnnotation(IClass<A> annotationClass) {
		return constructor.getDeclaredAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A[] getDeclaredAnnotationsByType(IClass<A> annotationClass) {
		return constructor.getDeclaredAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	// --- AnnotatedElement (Class overloads from java.lang.reflect.AnnotatedElement) ---

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return constructor.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return constructor.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return constructor.getDeclaredAnnotations();
	}

	// --- Object overrides ---

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof RuntimeConstructor<?> other) return constructor.equals(other.constructor);
		return false;
	}

	@Override
	public int hashCode() {
		return constructor.hashCode();
	}

	@Override
	public String toString() {
		return constructor.toString();
	}
}
