package com.garganttua.core.reflection;

import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.Field}.
 *
 * <p>Runtime implementations wrap the actual {@code Field} object;
 * AOT implementations provide compile-time generated metadata and direct access.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IField  extends IMember, IAnnotatedElement {

	// --- AccessibleObject ---

	void setAccessible(boolean flag);

	boolean trySetAccessible();
	
	boolean canAccess(Object obj);

	// --- Field metadata ---

	IClass<?> getType();

	Type getGenericType();

	boolean isEnumConstant();

	String toGenericString();

	IAnnotatedType getAnnotatedType();

	// --- Object access ---

	Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;

	// --- Primitive access ---

	boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException;

	byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException;

	char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException;

	short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException;

	int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException;

	long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException;

	float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException;

	double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException;

	void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException;

}
