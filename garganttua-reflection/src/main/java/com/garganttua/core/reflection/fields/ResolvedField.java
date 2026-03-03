package com.garganttua.core.reflection.fields;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

import com.garganttua.core.reflection.IAnnotatedType;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;

public record ResolvedField(ObjectAddress address, List<Object> fieldPath) implements IField {

    private IField field() {
        return (IField) fieldPath.getLast();
    }

    /**
     * Returns the owner type (declaring class) of the resolved field.
     */
    public IClass<?> ownerType() {
        return field().getDeclaringClass();
    }

    /**
     * Returns the type of the resolved field.
     */
    public IClass<?> fieldType() {
        return field().getType();
    }

    /**
     * Returns whether the resolved field is static.
     */
    public boolean isStatic() {
        return Modifier.isStatic(field().getModifiers());
    }

    boolean matches(IField other) {
        return this.field().equals(other);
    }

    boolean matches(IClass<?> ownerType, IClass<?> fieldType) {
        if (!field().getDeclaringClass().isAssignableFrom(ownerType))
            return false;

        if (fieldType != null && !field().getType().isAssignableFrom(fieldType)) {
            return false;
        }
        return true;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResolvedField(ObjectAddress otherAddress, List<Object> otherFieldPath))) return false;
        return java.util.Objects.equals(address, otherAddress)
                && java.util.Objects.equals(fieldPath, otherFieldPath);
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(address, fieldPath);
    }

    // --- Member ---

    @Override
    public IClass<?> getDeclaringClass() {
        return field().getDeclaringClass();
    }

    @Override
    public String getName() {
        return field().getName();
    }

    @Override
    public int getModifiers() {
        return field().getModifiers();
    }

    @Override
    public boolean isSynthetic() {
        return field().isSynthetic();
    }

    // --- AccessibleObject ---

    @Override
    public void setAccessible(boolean flag) {
        field().setAccessible(flag);
    }

    @Override
    public boolean trySetAccessible() {
        return field().trySetAccessible();
    }

    @Override
    public boolean canAccess(Object obj) {
        return field().canAccess(obj);
    }

    // --- Field metadata ---

    @Override
    public IClass<?> getType() {
        return field().getType();
    }

    @Override
    public Type getGenericType() {
        return field().getGenericType();
    }

    @Override
    public boolean isEnumConstant() {
        return field().isEnumConstant();
    }

    @Override
    public String toGenericString() {
        return field().toGenericString();
    }

    @Override
    public IAnnotatedType getAnnotatedType() {
        return field().getAnnotatedType();
    }

    // --- Object access ---

    @Override
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().get(obj);
    }

    @Override
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        field().set(obj, value);
    }

    // --- Primitive access ---

    @Override
    public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getBoolean(obj);
    }

    @Override
    public void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException {
        field().setBoolean(obj, z);
    }

    @Override
    public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getByte(obj);
    }

    @Override
    public void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException {
        field().setByte(obj, b);
    }

    @Override
    public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getChar(obj);
    }

    @Override
    public void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException {
        field().setChar(obj, c);
    }

    @Override
    public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getShort(obj);
    }

    @Override
    public void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException {
        field().setShort(obj, s);
    }

    @Override
    public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getInt(obj);
    }

    @Override
    public void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException {
        field().setInt(obj, i);
    }

    @Override
    public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getLong(obj);
    }

    @Override
    public void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException {
        field().setLong(obj, l);
    }

    @Override
    public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getFloat(obj);
    }

    @Override
    public void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException {
        field().setFloat(obj, f);
    }

    @Override
    public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field().getDouble(obj);
    }

    @Override
    public void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException {
        field().setDouble(obj, d);
    }

    // --- AnnotatedElement (IClass overloads) ---

    @Override
    public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
        return field().isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
        return field().getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
        return field().getAnnotationsByType(annotationClass);
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
        return field().getDeclaredAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
        return field().getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return field().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return field().getDeclaredAnnotations();
    }

    // --- IAnnotatedElement ---

    @Override
    public IReflection reflection() {
        return field().reflection();
    }

    @Override
    public String toString() {
        return "ResolvedField[" + address + " -> " + field() + "]";
    }
}
