package com.garganttua.core.supply;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IRecordComponent;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ITypeVariable;

/**
 * Test-only wrapper that adapts a JDK {@link Class} to the {@link IClass} interface.
 *
 * <p>
 * This is a minimal implementation for garganttua-supply tests only.
 * Member-level introspection methods throw {@link UnsupportedOperationException}.
 * </p>
 */
public final class TestIClass<T> implements IClass<T> {

    private final Class<T> wrapped;

    private TestIClass(Class<T> wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    public static <T> IClass<T> of(Class<T> clazz) {
        return new TestIClass<>(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> IClass<T> ofUnchecked(Class<?> clazz) {
        return new TestIClass<>((Class<T>) clazz);
    }

    // --- Naming ---

    @Override public String getName() { return wrapped.getName(); }
    @Override public String getSimpleName() { return wrapped.getSimpleName(); }
    @Override public String getCanonicalName() { return wrapped.getCanonicalName(); }
    @Override public String getTypeName() { return wrapped.getTypeName(); }
    @Override public String getPackageName() { return wrapped.getPackageName(); }
    @Override public String toGenericString() { return wrapped.toGenericString(); }
    @Override public String descriptorString() { return wrapped.descriptorString(); }

    // --- Modifiers & properties ---

    @Override public int getModifiers() { return wrapped.getModifiers(); }
    @Override public boolean isInterface() { return wrapped.isInterface(); }
    @Override public boolean isArray() { return wrapped.isArray(); }
    @Override public boolean isPrimitive() { return wrapped.isPrimitive(); }
    @Override public boolean isAnnotation() { return wrapped.isAnnotation(); }
    @Override public boolean isSynthetic() { return wrapped.isSynthetic(); }
    @Override public boolean isEnum() { return wrapped.isEnum(); }
    @Override public boolean isRecord() { return wrapped.isRecord(); }
    @Override public boolean isSealed() { return wrapped.isSealed(); }
    @Override public boolean isHidden() { return wrapped.isHidden(); }
    @Override public boolean isMemberClass() { return wrapped.isMemberClass(); }
    @Override public boolean isLocalClass() { return wrapped.isLocalClass(); }
    @Override public boolean isAnonymousClass() { return wrapped.isAnonymousClass(); }

    // --- Type hierarchy ---

    @Override
    @SuppressWarnings("unchecked")
    public IClass<? super T> getSuperclass() {
        Class<? super T> s = wrapped.getSuperclass();
        return s == null ? null : (IClass<? super T>) of(s);
    }

    @Override
    public IClass<?>[] getInterfaces() {
        Class<?>[] ifaces = wrapped.getInterfaces();
        IClass<?>[] result = new IClass<?>[ifaces.length];
        for (int i = 0; i < ifaces.length; i++) result[i] = ofUnchecked(ifaces[i]);
        return result;
    }

    @Override public Type getGenericSuperclass() { return wrapped.getGenericSuperclass(); }
    @Override public Type[] getGenericInterfaces() { return wrapped.getGenericInterfaces(); }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ITypeVariable<IClass<T>>[] getTypeParameters() {
        // Stub: return empty array (not needed by supply tests)
        return new ITypeVariable[0];
    }

    // --- Type checks ---

    @Override
    public boolean isAssignableFrom(IClass<?> cls) {
        if (cls instanceof TestIClass<?> t) {
            return wrapped.isAssignableFrom(t.wrapped);
        }
        throw new UnsupportedOperationException("Cannot check assignability with non-TestIClass: " + cls);
    }

    @Override
    public boolean isAssignableFrom(Class<?> cls) {
        return wrapped.isAssignableFrom(cls);
    }

    @Override
    public boolean isInstance(Object obj) {
        return wrapped.isInstance(obj);
    }

    // --- Array / OfField ---

    @Override public IClass<?> getComponentType() {
        Class<?> c = wrapped.getComponentType();
        return c == null ? null : ofUnchecked(c);
    }
    @Override public IClass<?> componentType() { return getComponentType(); }
    @Override public IClass<?> arrayType() { return ofUnchecked(wrapped.arrayType()); }

    // --- Declared members - not supported in test helper ---

    @Override public IField[] getDeclaredFields() { throw new UnsupportedOperationException(); }
    @Override public IMethod[] getDeclaredMethods() { throw new UnsupportedOperationException(); }
    @Override public IConstructor<?>[] getDeclaredConstructors() { throw new UnsupportedOperationException(); }
    @Override public IField getDeclaredField(String name) { throw new UnsupportedOperationException(); }
    @Override public IMethod getDeclaredMethod(String name, IClass<?>... parameterTypes) { throw new UnsupportedOperationException(); }
    @Override public IConstructor<T> getDeclaredConstructor(IClass<?>... parameterTypes) { throw new UnsupportedOperationException(); }

    // --- Public members - not supported in test helper ---

    @Override public IField[] getFields() { throw new UnsupportedOperationException(); }
    @Override public IMethod[] getMethods() { throw new UnsupportedOperationException(); }
    @Override public IConstructor<?>[] getConstructors() { throw new UnsupportedOperationException(); }
    @Override public IField getField(String name) { throw new UnsupportedOperationException(); }
    @Override public IMethod getMethod(String name, IClass<?>... parameterTypes) { throw new UnsupportedOperationException(); }
    @Override public IConstructor<T> getConstructor(IClass<?>... parameterTypes) { throw new UnsupportedOperationException(); }

    // --- Record components ---

    @Override public IRecordComponent[] getRecordComponents() { throw new UnsupportedOperationException(); }

    // --- Nesting & enclosing ---

    @Override public IClass<?> getEnclosingClass() {
        Class<?> c = wrapped.getEnclosingClass();
        return c == null ? null : ofUnchecked(c);
    }
    @Override public IClass<?> getDeclaringClass() {
        Class<?> c = wrapped.getDeclaringClass();
        return c == null ? null : ofUnchecked(c);
    }
    @Override public IMethod getEnclosingMethod() { throw new UnsupportedOperationException(); }
    @Override public IConstructor<?> getEnclosingConstructor() { throw new UnsupportedOperationException(); }
    @Override public IClass<?> getNestHost() { return ofUnchecked(wrapped.getNestHost()); }
    @Override public IClass<?>[] getNestMembers() { throw new UnsupportedOperationException(); }
    @Override public boolean isNestmateOf(IClass<?> c) { throw new UnsupportedOperationException(); }

    // --- Inner classes ---

    @Override public IClass<?>[] getClasses() { throw new UnsupportedOperationException(); }
    @Override public IClass<?>[] getDeclaredClasses() { throw new UnsupportedOperationException(); }

    // --- Sealed ---

    @Override public IClass<?>[] getPermittedSubclasses() { throw new UnsupportedOperationException(); }

    // --- Enum ---

    @Override public T[] getEnumConstants() { return wrapped.getEnumConstants(); }

    // --- Annotations ---

    @Override public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) { throw new UnsupportedOperationException(); }
    @Override public <A extends Annotation> A getAnnotation(IClass<A> annotationClass) { throw new UnsupportedOperationException(); }
    @Override public Annotation[] getAnnotations() { return wrapped.getAnnotations(); }
    @Override public Annotation[] getDeclaredAnnotations() { return wrapped.getDeclaredAnnotations(); }
    @Override public <A extends Annotation> A[] getAnnotationsByType(IClass<A> annotationClass) { throw new UnsupportedOperationException(); }
    @Override public <A extends Annotation> A getDeclaredAnnotation(IClass<A> annotationClass) { throw new UnsupportedOperationException(); }
    @Override public <A extends Annotation> A[] getDeclaredAnnotationsByType(IClass<A> annotationClass) { throw new UnsupportedOperationException(); }
    @Override public AnnotatedType getAnnotatedSuperclass() { return wrapped.getAnnotatedSuperclass(); }
    @Override public AnnotatedType[] getAnnotatedInterfaces() { return wrapped.getAnnotatedInterfaces(); }

    @Override
    public IReflection reflection() {
        throw new UnsupportedOperationException("TestIClass is a test-only stub without IReflection");
    }

    // --- Cast ---

    @Override public T cast(Object obj) { return wrapped.cast(obj); }

    @Override
    @SuppressWarnings("unchecked")
    public <U> IClass<? extends U> asSubclass(IClass<U> clazz) {
        if (clazz instanceof TestIClass<U> t) {
            return (IClass<? extends U>) of(wrapped.asSubclass(t.wrapped));
        }
        throw new UnsupportedOperationException();
    }

    // --- Runtime info ---

    @Override public Package getPackage() { return wrapped.getPackage(); }
    @Override public Module getModule() { return wrapped.getModule(); }
    @Override public ClassLoader getClassLoader() { return wrapped.getClassLoader(); }
    @Override public Type getType() { return wrapped; }

    // --- Constable ---

    @Override public Optional<ClassDesc> describeConstable() { return wrapped.describeConstable(); }

    // --- Object ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof TestIClass<?> that) return wrapped.equals(that.wrapped);
        return false;
    }

    @Override public int hashCode() { return wrapped.hashCode(); }
    @Override public String toString() { return wrapped.toString(); }
}
