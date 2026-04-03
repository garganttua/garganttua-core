package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.constant.Constable;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.Class}.
 *
 * <p>
 * Runtime implementations wrap the actual {@code Class} object;
 * AOT implementations provide compile-time generated metadata and member access
 * without runtime introspection.
 * </p>
 *
 * @param <T> the type represented by this class descriptor
 * @since 2.0.0-ALPHA01
 */
public interface IClass<T> extends IGenericDeclaration, Type,
		TypeDescriptor.OfField<IClass<?>>,
		Constable {

	// --- Static factory (mirrors Class.forName) ---

	static <T> IClass<T> forName(String className) throws ClassNotFoundException {
		return ReflectionHolder.reflection().forName(className);
	}

	static <T> IClass<T> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		return ReflectionHolder.reflection().forName(name, initialize, loader);
	}

	static void setReflection(IReflection reflection) {
		ReflectionHolder.globalDefault = reflection;
	}

	static IReflection getReflection() {
		return ReflectionHolder.reflection();
	}

	static void setThreadReflection(IReflection reflection) {
		ReflectionHolder.threadLocal.set(reflection);
	}

	static void clearThreadReflection() {
		ReflectionHolder.threadLocal.remove();
	}

	static <T> IClass<T> getClass(Class<T> clazz) {
		return ReflectionHolder.reflection().getClass(clazz);
	}

	/**
	 * Internal holder for the default {@link IReflection} used by static factory
	 * methods. ThreadLocal reflection takes precedence over the global default.
	 */
	class ReflectionHolder {
		static volatile IReflection globalDefault;
		static final ThreadLocal<IReflection> threadLocal = new ThreadLocal<>();

		private ReflectionHolder() {
		}

		static IReflection reflection() {
			IReflection tl = threadLocal.get();
			if (tl != null)
				return tl;
			if (globalDefault != null)
				return globalDefault;

			throw new IllegalStateException(
					"No IReflection available. Call IClass.setReflection()");
		}
	}

	// --- Naming ---

	String getName();

	String getSimpleName();

	String getCanonicalName();

	String getTypeName();

	String getPackageName();

	String toGenericString();

	String descriptorString();

	// --- Modifiers & properties ---

	int getModifiers();

	boolean isInterface();

	boolean isArray();

	boolean isPrimitive();

	boolean isAnnotation();

	boolean isSynthetic();

	boolean isEnum();

	boolean isRecord();

	boolean isSealed();

	boolean isHidden();

	boolean isMemberClass();

	boolean isLocalClass();

	boolean isAnonymousClass();

	// --- Type hierarchy ---

	IClass<? super T> getSuperclass();

	IClass<?>[] getInterfaces();

	Type getGenericSuperclass();

	Type[] getGenericInterfaces();

	ITypeVariable<IClass<T>>[] getTypeParameters();

	// --- Type checks ---

	boolean isAssignableFrom(IClass<?> cls);

	boolean isAssignableFrom(Class<?> cls);

	boolean isInstance(Object obj);

	/**
	 * Checks whether this {@code IClass} represents the given raw {@code Class}.
	 *
	 * <p>
	 * This is the recommended way to compare an {@code IClass} with a raw {@code Class<?>}.
	 * Unlike {@code equals(Class)}, this method is explicitly directional and does not
	 * interfere with the symmetric contract of {@link Object#equals(Object)}.
	 * </p>
	 *
	 * @param cls the raw class to compare against
	 * @return {@code true} if this IClass represents the same type as {@code cls}
	 */
	default boolean represents(Class<?> cls) {
		return cls != null && cls.equals(getType());
	}

	// --- Array ---

	IClass<?> getComponentType();

	IClass<?> arrayType();

	// --- Declared members (own declarations only) ---

	IField[] getDeclaredFields();

	IMethod[] getDeclaredMethods();

	IConstructor<?>[] getDeclaredConstructors();

	IField getDeclaredField(String name) throws NoSuchFieldException, SecurityException;

	IMethod getDeclaredMethod(String name, IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException;

	IConstructor<T> getDeclaredConstructor(IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException;

	// --- Public members (including inherited) ---

	IField[] getFields();

	IMethod[] getMethods();

	IConstructor<?>[] getConstructors();

	IField getField(String name) throws NoSuchFieldException, SecurityException;

	IMethod getMethod(String name, IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException;

	IConstructor<T> getConstructor(IClass<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException;

	// --- Record components ---

	IRecordComponent[] getRecordComponents();

	// --- Nesting & enclosing ---

	IClass<?> getEnclosingClass();

	IClass<?> getDeclaringClass();

	IMethod getEnclosingMethod();

	IConstructor<?> getEnclosingConstructor();

	IClass<?> getNestHost();

	IClass<?>[] getNestMembers();

	boolean isNestmateOf(IClass<?> c);

	// --- Inner classes ---

	IClass<?>[] getClasses();

	IClass<?>[] getDeclaredClasses();

	// --- Sealed ---

	IClass<?>[] getPermittedSubclasses();

	// --- Enum ---

	T[] getEnumConstants();

	// --- Annotations ---

	<A extends Annotation> A getAnnotation(IClass<A> annotationClass);

	Annotation[] getAnnotations();

	Annotation[] getDeclaredAnnotations();

	AnnotatedType getAnnotatedSuperclass();

	AnnotatedType[] getAnnotatedInterfaces();

	// --- Cast ---

	T cast(Object obj);

	<U> IClass<? extends U> asSubclass(IClass<U> clazz);

	// --- Runtime info ---

	Package getPackage();

	Module getModule();

	ClassLoader getClassLoader();

	Type getType();

}
