package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IParameter;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

public class RuntimeMethod implements IMethod {

	private static final ConcurrentHashMap<Method, RuntimeMethod> CACHE = new ConcurrentHashMap<>();

	private final Method method;

	private RuntimeMethod(Method method) {
		this.method = method;
	}

	public static RuntimeMethod of(Method method) {
		return CACHE.computeIfAbsent(method, RuntimeMethod::new);
	}

	public Method unwrap() {
		return method;
	}

	public static Method unwrap(IMethod imethod) {
		if (imethod instanceof RuntimeMethod rm) return rm.method;
		throw new IllegalArgumentException("Cannot unwrap non-RuntimeMethod IMethod: " + imethod.getClass());
	}

	// --- Member ---

	@Override
	public IClass<?> getDeclaringClass() {
		return RuntimeClass.ofUnchecked(method.getDeclaringClass());
	}

	@Override
	public String getName() {
		return method.getName();
	}

	@Override
	public int getModifiers() {
		return method.getModifiers();
	}

	@Override
	public boolean isSynthetic() {
		return method.isSynthetic();
	}

	// --- AccessibleObject ---

	@Override
	public void setAccessible(boolean flag) {
		method.setAccessible(flag);
	}

	@Override
	public boolean trySetAccessible() {
		return method.trySetAccessible();
	}

	@Override
	public boolean canAccess(Object obj) {
		return method.canAccess(obj);
	}

	// --- GenericDeclaration ---

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public ITypeVariable<?>[] getTypeParameters() {
		TypeVariable<?>[] jdkVars = method.getTypeParameters();
		ITypeVariable<?>[] result = new ITypeVariable[jdkVars.length];
		for (int i = 0; i < jdkVars.length; i++) {
			result[i] = new RuntimeTypeVariable<>(jdkVars[i], this);
		}
		return result;
	}

	// --- Return type ---

	@Override
	public IClass<?> getReturnType() {
		return RuntimeClass.ofUnchecked(method.getReturnType());
	}

	@Override
	public Type getGenericReturnType() {
		return method.getGenericReturnType();
	}

	// --- Parameters ---

	@Override
	public IClass<?>[] getParameterTypes() {
		return Arrays.stream(method.getParameterTypes())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public Type[] getGenericParameterTypes() {
		return method.getGenericParameterTypes();
	}

	@Override
	public int getParameterCount() {
		return method.getParameterCount();
	}

	@Override
	public IParameter[] getParameters() {
		return Arrays.stream(method.getParameters())
				.map(RuntimeParameter::of)
				.toArray(IParameter[]::new);
	}

	// --- Exceptions ---

	@Override
	public IClass<?>[] getExceptionTypes() {
		return Arrays.stream(method.getExceptionTypes())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public Type[] getGenericExceptionTypes() {
		return method.getGenericExceptionTypes();
	}

	// --- Method properties ---

	@Override
	public boolean isVarArgs() {
		return method.isVarArgs();
	}

	@Override
	public boolean isBridge() {
		return method.isBridge();
	}

	@Override
	public boolean isDefault() {
		return method.isDefault();
	}

	@Override
	public Object getDefaultValue() {
		return method.getDefaultValue();
	}

	@Override
	public String toGenericString() {
		return method.toGenericString();
	}

	// --- Invocation ---

	@Override
	public Object invoke(Object obj, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return method.invoke(obj, args);
	}

	// --- Annotated types ---

	@Override
	public AnnotatedType getAnnotatedReturnType() {
		return method.getAnnotatedReturnType();
	}

	@Override
	public AnnotatedType[] getAnnotatedParameterTypes() {
		return method.getAnnotatedParameterTypes();
	}

	@Override
	public AnnotatedType[] getAnnotatedExceptionTypes() {
		return method.getAnnotatedExceptionTypes();
	}

	@Override
	public AnnotatedType getAnnotatedReceiverType() {
		return method.getAnnotatedReceiverType();
	}

	// --- AnnotatedElement (IClass overloads) ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return method.isAnnotationPresent(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
		return method.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
		return method.getAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
		return method.getDeclaredAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
		return method.getDeclaredAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return method.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return method.getDeclaredAnnotations();
	}

	// --- IAnnotatedElement ---

	@Override
	public IReflection reflection() {
		return IClass.getReflection();
	}

	// --- Object overrides ---

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof RuntimeMethod other) return method.equals(other.method);
		return false;
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}

	@Override
	public String toString() {
		return method.toString();
	}
}
