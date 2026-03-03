package com.garganttua.core.reflection.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.reflection.IAnnotatedType;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;

public class RuntimeField implements IField {

	private static final ConcurrentHashMap<Field, RuntimeField> CACHE = new ConcurrentHashMap<>();

	private final Field field;

	private RuntimeField(Field field) {
		this.field = field;
	}

	public static RuntimeField of(Field field) {
		return CACHE.computeIfAbsent(field, RuntimeField::new);
	}

	public Field unwrap() {
		return field;
	}

	public static Field unwrap(IField ifield) {
		if (ifield instanceof RuntimeField rf) return rf.field;
		throw new IllegalArgumentException("Cannot unwrap non-RuntimeField IField: " + ifield.getClass());
	}

	// --- Member ---

	@Override
	public IClass<?> getDeclaringClass() {
		return RuntimeClass.ofUnchecked(field.getDeclaringClass());
	}

	@Override
	public String getName() {
		return field.getName();
	}

	@Override
	public int getModifiers() {
		return field.getModifiers();
	}

	@Override
	public boolean isSynthetic() {
		return field.isSynthetic();
	}

	// --- AccessibleObject ---

	@Override
	public void setAccessible(boolean flag) {
		field.setAccessible(flag);
	}

	@Override
	public boolean trySetAccessible() {
		return field.trySetAccessible();
	}

	@Override
	public boolean canAccess(Object obj) {
		return field.canAccess(obj);
	}

	// --- Field metadata ---

	@Override
	public IClass<?> getType() {
		return RuntimeClass.ofUnchecked(field.getType());
	}

	@Override
	public Type getGenericType() {
		return field.getGenericType();
	}

	@Override
	public boolean isEnumConstant() {
		return field.isEnumConstant();
	}

	@Override
	public String toGenericString() {
		return field.toGenericString();
	}

	@Override
	public IAnnotatedType getAnnotatedType() {
		return new RuntimeAnnotatedType(field.getAnnotatedType());
	}

	// --- Object access ---

	@Override
	public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.get(obj);
	}

	@Override
	public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
		field.set(obj, value);
	}

	// --- Primitive access ---

	@Override
	public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getBoolean(obj);
	}

	@Override
	public void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException {
		field.setBoolean(obj, z);
	}

	@Override
	public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getByte(obj);
	}

	@Override
	public void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException {
		field.setByte(obj, b);
	}

	@Override
	public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getChar(obj);
	}

	@Override
	public void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException {
		field.setChar(obj, c);
	}

	@Override
	public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getShort(obj);
	}

	@Override
	public void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException {
		field.setShort(obj, s);
	}

	@Override
	public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getInt(obj);
	}

	@Override
	public void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException {
		field.setInt(obj, i);
	}

	@Override
	public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getLong(obj);
	}

	@Override
	public void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException {
		field.setLong(obj, l);
	}

	@Override
	public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getFloat(obj);
	}

	@Override
	public void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException {
		field.setFloat(obj, f);
	}

	@Override
	public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException {
		return field.getDouble(obj);
	}

	@Override
	public void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException {
		field.setDouble(obj, d);
	}

	// --- AnnotatedElement (IClass overloads) ---

	@Override
	public boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
		return field.isAnnotationPresent(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getAnnotation(IClass<T> annotationClass) {
		return field.getAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
		return field.getAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
		return field.getDeclaredAnnotation(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
		return field.getDeclaredAnnotationsByType(RuntimeClass.unwrapAnnotationClass(annotationClass));
	}

	@Override
	public Annotation[] getAnnotations() {
		return field.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return field.getDeclaredAnnotations();
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
		if (obj instanceof RuntimeField other) return field.equals(other.field);
		return false;
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public String toString() {
		return field.toString();
	}
}
