package com.garganttua.core.aot.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import com.garganttua.core.reflection.IAnnotatedType;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;

/**
 * AOT implementation of {@link IField}.
 *
 * <p>Stores pre-computed field metadata. The actual {@link Field} is resolved
 * lazily for operations that require it (get/set, accessibility).</p>
 */
public class AOTField implements IField {

    private final String name;
    private final String declaringClassName;
    private final String typeName;
    private final int modifiers;
    private final Annotation[] annotations;
    private final Type genericType;

    // Lazy resolution
    private volatile IClass<?> resolvedDeclaringClass;
    private volatile IClass<?> resolvedType;
    private volatile Field resolvedField;

    public AOTField(String name, String declaringClassName, String typeName,
                    int modifiers, Annotation[] annotations, Type genericType) {
        this.name = name;
        this.declaringClassName = declaringClassName;
        this.typeName = typeName;
        this.modifiers = modifiers;
        this.annotations = annotations != null ? annotations.clone() : new Annotation[0];
        this.genericType = genericType;
    }

    // --- IMember ---

    @Override
    public IClass<?> getDeclaringClass() {
        IClass<?> cached = resolvedDeclaringClass;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedDeclaringClass == null) {
                try {
                    resolvedDeclaringClass = IClass.forName(declaringClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve declaring class: " + declaringClassName, e);
                }
            }
            return resolvedDeclaringClass;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public boolean isSynthetic() {
        return (modifiers & 0x00001000) != 0; // ACC_SYNTHETIC
    }

    // --- AccessibleObject ---

    @Override
    public void setAccessible(boolean flag) {
        resolveField().setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return resolveField().trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return resolveField().canAccess(obj);
    }

    // --- Field metadata ---

    @Override
    public IClass<?> getType() {
        IClass<?> cached = resolvedType;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedType == null) {
                try {
                    resolvedType = IClass.forName(typeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot resolve field type: " + typeName, e);
                }
            }
            return resolvedType;
        }
    }

    @Override
    public Type getGenericType() {
        if (genericType != null) return genericType;
        return (Type) getType().getType();
    }

    @Override
    public boolean isEnumConstant() {
        return (modifiers & 0x00004000) != 0; // ACC_ENUM
    }

    @Override
    public String toGenericString() {
        StringBuilder sb = new StringBuilder();
        String mod = Modifier.toString(modifiers);
        if (!mod.isEmpty()) sb.append(mod).append(' ');
        sb.append(typeName).append(' ');
        sb.append(declaringClassName).append('.').append(name);
        return sb.toString();
    }

    @Override
    public IAnnotatedType getAnnotatedType() {
        return null;
    }

    // --- Object access (delegate to lazy Field) ---

    @Override
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().get(obj);
    }

    @Override
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        resolveField().set(obj, value);
    }

    @Override
    public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getBoolean(obj);
    }

    @Override
    public void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setBoolean(obj, z);
    }

    @Override
    public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getByte(obj);
    }

    @Override
    public void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setByte(obj, b);
    }

    @Override
    public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getChar(obj);
    }

    @Override
    public void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setChar(obj, c);
    }

    @Override
    public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getShort(obj);
    }

    @Override
    public void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setShort(obj, s);
    }

    @Override
    public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getInt(obj);
    }

    @Override
    public void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setInt(obj, i);
    }

    @Override
    public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getLong(obj);
    }

    @Override
    public void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setLong(obj, l);
    }

    @Override
    public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getFloat(obj);
    }

    @Override
    public void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setFloat(obj, f);
    }

    @Override
    public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return resolveField().getDouble(obj);
    }

    @Override
    public void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException {
        resolveField().setDouble(obj, d);
    }

    // --- AnnotatedElement ---

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
        for (Annotation a : annotations) {
            if (a.annotationType().getName().equals(annotationClass.getName())) {
                return (T) a;
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.clone();
    }

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }

    // --- Lazy Field resolution ---

    private Field resolveField() {
        Field cached = resolvedField;
        if (cached != null) return cached;
        synchronized (this) {
            if (resolvedField == null) {
                try {
                    Class<?> clazz = Class.forName(declaringClassName);
                    resolvedField = clazz.getDeclaredField(name);
                } catch (ClassNotFoundException | NoSuchFieldException e) {
                    throw new IllegalStateException(
                            "Cannot resolve field: " + declaringClassName + "." + name, e);
                }
            }
            return resolvedField;
        }
    }

    // --- Object overrides ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof AOTField other) {
            return declaringClassName.equals(other.declaringClassName) && name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * declaringClassName.hashCode() + name.hashCode();
    }

    @Override
    public String toString() {
        return declaringClassName + "." + name;
    }
}
