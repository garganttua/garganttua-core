package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

public class RuntimeClass<T> implements IClass<T> {

	private static final ConcurrentHashMap<Class<?>, RuntimeClass<?>> CACHE = new ConcurrentHashMap<>();

	private final Class<T> clazz;

	private RuntimeClass(Class<T> clazz) {
		this.clazz = clazz;
	}


	public static <T> RuntimeClass<T> of(Class<T> clazz) {
		return (RuntimeClass<T>) CACHE.computeIfAbsent(clazz, RuntimeClass::new);
	}

	public static RuntimeClass<?> ofUnchecked(Class<?> clazz) {
		return CACHE.computeIfAbsent(clazz, k -> new RuntimeClass<>(k));
	}

	public Class<T> unwrap() {
		return clazz;
	}

	// --- Naming ---

	@Override
	public String getName() {
		return clazz.getName();
	}

	@Override
	public String getSimpleName() {
		return clazz.getSimpleName();
	}

	@Override
	public String getCanonicalName() {
		return clazz.getCanonicalName();
	}

	@Override
	public String getTypeName() {
		return clazz.getTypeName();
	}

	@Override
	public String getPackageName() {
		return clazz.getPackageName();
	}

	@Override
	public String toGenericString() {
		return clazz.toGenericString();
	}

	@Override
	public String descriptorString() {
		return clazz.descriptorString();
	}

	// --- Modifiers & properties ---

	@Override
	public int getModifiers() {
		return clazz.getModifiers();
	}

	@Override
	public boolean isInterface() {
		return clazz.isInterface();
	}

	@Override
	public boolean isArray() {
		return clazz.isArray();
	}

	@Override
	public boolean isPrimitive() {
		return clazz.isPrimitive();
	}

	@Override
	public boolean isAnnotation() {
		return clazz.isAnnotation();
	}

	@Override
	public boolean isSynthetic() {
		return clazz.isSynthetic();
	}

	@Override
	public boolean isEnum() {
		return clazz.isEnum();
	}

	@Override
	public boolean isRecord() {
		return clazz.isRecord();
	}

	@Override
	public boolean isSealed() {
		return clazz.isSealed();
	}

	@Override
	public boolean isHidden() {
		return clazz.isHidden();
	}

	@Override
	public boolean isMemberClass() {
		return clazz.isMemberClass();
	}

	@Override
	public boolean isLocalClass() {
		return clazz.isLocalClass();
	}

	@Override
	public boolean isAnonymousClass() {
		return clazz.isAnonymousClass();
	}

	// --- Type hierarchy ---

	@Override
	@SuppressWarnings("unchecked")
	public IClass<? super T> getSuperclass() {
		Class<? super T> superclass = clazz.getSuperclass();
		return superclass == null ? null : (IClass<? super T>) RuntimeClass.ofUnchecked(superclass);
	}

	@Override
	public IClass<?>[] getInterfaces() {
		return Arrays.stream(clazz.getInterfaces())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public Type getGenericSuperclass() {
		return clazz.getGenericSuperclass();
	}

	@Override
	public Type[] getGenericInterfaces() {
		return clazz.getGenericInterfaces();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public ITypeVariable<IClass<T>>[] getTypeParameters() {
		TypeVariable<Class<T>>[] jdkVars = clazz.getTypeParameters();
		ITypeVariable<IClass<T>>[] result = new ITypeVariable[jdkVars.length];
		for (int i = 0; i < jdkVars.length; i++) {
			result[i] = new RuntimeTypeVariable<>(jdkVars[i], this);
		}
		return result;
	}

	// --- Type checks ---

	@Override
	public boolean isAssignableFrom(IClass<?> cls) {
		return clazz.isAssignableFrom(unwrapClass(cls));
	}

	@Override
	public boolean isAssignableFrom(Class<?> cls) {
		return clazz.isAssignableFrom(cls);
	}

	@Override
	public boolean isInstance(Object obj) {
		return clazz.isInstance(obj);
	}

	// --- Array ---

	@Override
	public IClass<?> getComponentType() {
		Class<?> ct = clazz.getComponentType();
		return ct == null ? null : RuntimeClass.ofUnchecked(ct);
	}

	@Override
	public IClass<?> componentType() {
		return getComponentType();
	}

	@Override
	public IClass<?> arrayType() {
		return RuntimeClass.ofUnchecked(clazz.arrayType());
	}

	@Override
	public Optional<ClassDesc> describeConstable() {
		return clazz.describeConstable();
	}

	// --- Declared members ---

	@Override
	public IField[] getDeclaredFields() {
		return Arrays.stream(clazz.getDeclaredFields())
				.map(RuntimeField::of)
				.toArray(IField[]::new);
	}

	@Override
	public IMethod[] getDeclaredMethods() {
		return Arrays.stream(clazz.getDeclaredMethods())
				.map(RuntimeMethod::of)
				.toArray(IMethod[]::new);
	}

	@Override
	public IConstructor<?>[] getDeclaredConstructors() {
		return Arrays.stream(clazz.getDeclaredConstructors())
				.map(RuntimeConstructor::ofUnchecked)
				.toArray(IConstructor<?>[]::new);
	}

	@Override
	public IField getDeclaredField(String name) throws NoSuchFieldException, SecurityException {
		return RuntimeField.of(clazz.getDeclaredField(name));
	}

	@Override
	public IMethod getDeclaredMethod(String name, IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		Class<?>[] rawTypes = unwrapClasses(parameterTypes);
		return RuntimeMethod.of(clazz.getDeclaredMethod(name, rawTypes));
	}

	@Override
	@SuppressWarnings("unchecked")
	public IConstructor<T> getDeclaredConstructor(IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		Class<?>[] rawTypes = unwrapClasses(parameterTypes);
		return (IConstructor<T>) RuntimeConstructor.ofUnchecked(clazz.getDeclaredConstructor(rawTypes));
	}

	// --- Public members ---

	@Override
	public IField[] getFields() {
		return Arrays.stream(clazz.getFields())
				.map(RuntimeField::of)
				.toArray(IField[]::new);
	}

	@Override
	public IMethod[] getMethods() {
		return Arrays.stream(clazz.getMethods())
				.map(RuntimeMethod::of)
				.toArray(IMethod[]::new);
	}

	@Override
	public IConstructor<?>[] getConstructors() {
		return Arrays.stream(clazz.getConstructors())
				.map(RuntimeConstructor::ofUnchecked)
				.toArray(IConstructor<?>[]::new);
	}

	@Override
	public IField getField(String name) throws NoSuchFieldException, SecurityException {
		return RuntimeField.of(clazz.getField(name));
	}

	@Override
	public IMethod getMethod(String name, IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		Class<?>[] rawTypes = unwrapClasses(parameterTypes);
		return RuntimeMethod.of(clazz.getMethod(name, rawTypes));
	}

	@Override
	@SuppressWarnings("unchecked")
	public IConstructor<T> getConstructor(IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		Class<?>[] rawTypes = unwrapClasses(parameterTypes);
		return (IConstructor<T>) RuntimeConstructor.ofUnchecked(clazz.getConstructor(rawTypes));
	}

	// --- Record components ---

	@Override
	public IRecordComponent[] getRecordComponents() {
		RecordComponent[] rcs = clazz.getRecordComponents();
		if (rcs == null) return null;
		return Arrays.stream(rcs)
				.map(RuntimeRecordComponent::of)
				.toArray(IRecordComponent[]::new);
	}

	// --- Nesting & enclosing ---

	@Override
	public IClass<?> getEnclosingClass() {
		Class<?> ec = clazz.getEnclosingClass();
		return ec == null ? null : RuntimeClass.ofUnchecked(ec);
	}

	@Override
	public IClass<?> getDeclaringClass() {
		Class<?> dc = clazz.getDeclaringClass();
		return dc == null ? null : RuntimeClass.ofUnchecked(dc);
	}

	@Override
	public IMethod getEnclosingMethod() {
		Method em = clazz.getEnclosingMethod();
		return em == null ? null : RuntimeMethod.of(em);
	}

	@Override
	public IConstructor<?> getEnclosingConstructor() {
		Constructor<?> ec = clazz.getEnclosingConstructor();
		return ec == null ? null : RuntimeConstructor.ofUnchecked(ec);
	}

	@Override
	public IClass<?> getNestHost() {
		return RuntimeClass.ofUnchecked(clazz.getNestHost());
	}

	@Override
	public IClass<?>[] getNestMembers() {
		return Arrays.stream(clazz.getNestMembers())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public boolean isNestmateOf(IClass<?> c) {
		return clazz.isNestmateOf(unwrapClass(c));
	}

	// --- Inner classes ---

	@Override
	public IClass<?>[] getClasses() {
		return Arrays.stream(clazz.getClasses())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	@Override
	public IClass<?>[] getDeclaredClasses() {
		return Arrays.stream(clazz.getDeclaredClasses())
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	// --- Sealed ---

	@Override
	public IClass<?>[] getPermittedSubclasses() {
		Class<?>[] ps = clazz.getPermittedSubclasses();
		if (ps == null) return null;
		return Arrays.stream(ps)
				.map(RuntimeClass::ofUnchecked)
				.toArray(IClass<?>[]::new);
	}

	// --- Enum ---

	@Override
	public T[] getEnumConstants() {
		return clazz.getEnumConstants();
	}

	// --- Annotations (IClass-based) ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return clazz.isAnnotationPresent(unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) {
		return clazz.getAnnotation(unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return clazz.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return clazz.getDeclaredAnnotations();
	}

	@Override
	public <A extends Annotation> A[] getAnnotationsByType(IClass<A> annotationClass) {
		return clazz.getAnnotationsByType(unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A getDeclaredAnnotation(IClass<A> annotationClass) {
		return clazz.getDeclaredAnnotation(unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <A extends Annotation> A[] getDeclaredAnnotationsByType(IClass<A> annotationClass) {
		return clazz.getDeclaredAnnotationsByType(unwrapAnnotationClass(annotationClass));
	}

	@Override
	public AnnotatedType getAnnotatedSuperclass() {
		return clazz.getAnnotatedSuperclass();
	}

	@Override
	public AnnotatedType[] getAnnotatedInterfaces() {
		return clazz.getAnnotatedInterfaces();
	}

	// --- IAnnotatedElement ---

	@Override
	public IReflection reflection() {
		return IClass.getReflection();
	}

	// --- Cast ---

	@Override
	public T cast(Object obj) {
		return clazz.cast(obj);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <U> IClass<? extends U> asSubclass(IClass<U> other) {
		Class<? extends U> sub = clazz.asSubclass((Class) unwrapClass(other));
		return (IClass<? extends U>) RuntimeClass.ofUnchecked(sub);
	}

	// --- Runtime info ---

	@Override
	public Package getPackage() {
		return clazz.getPackage();
	}

	@Override
	public Module getModule() {
		return clazz.getModule();
	}

	@Override
	public ClassLoader getClassLoader() {
		return clazz.getClassLoader();
	}

	@Override
	public Type getType() {
		return clazz;
	}

	// --- Object overrides ---

	/**
	 * {@inheritDoc}
	 *
	 * <p><b>Note:</b> This method also accepts raw {@link Class} objects for
	 * backward compatibility, but this usage is <b>deprecated</b> because it
	 * violates the symmetry contract of {@code equals} ({@code iclass.equals(clazz)}
	 * returns {@code true} while {@code clazz.equals(iclass)} returns {@code false}).
	 * Use {@link #represents(Class)} instead for comparing an {@code IClass} with
	 * a raw {@code Class<?>}.</p>
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof RuntimeClass<?> other) return clazz.equals(other.clazz);
		if (obj instanceof Class<?> other) return clazz.equals(other);
		return false;
	}

	@Override
	public int hashCode() {
		return clazz.hashCode();
	}

	@Override
	public String toString() {
		return clazz.toString();
	}

	// --- Utility ---

	public static Class<?> unwrapClass(IClass<?> iclass) {
		if (iclass instanceof RuntimeClass<?> rc) return rc.clazz;
		throw new IllegalArgumentException("Cannot unwrap non-RuntimeClass IClass: " + iclass.getClass());
	}

	public static Class<?>[] unwrapClasses(IClass<?>[] iclasses) {
		if (iclasses == null) return new Class<?>[0];
		Class<?>[] result = new Class<?>[iclasses.length];
		for (int i = 0; i < iclasses.length; i++) {
			result[i] = unwrapClass(iclasses[i]);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <A extends Annotation> Class<A> unwrapAnnotationClass(IClass<A> iclass) {
		return (Class<A>) unwrapClass(iclass);
	}
}
