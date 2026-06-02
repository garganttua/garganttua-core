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

	/**
	 * Sets the accessibility flag for this field.
	 *
	 * @param flag {@code true} to suppress access checks
	 */
	void setAccessible(boolean flag);

	/**
	 * Attempts to make this field accessible, returning the outcome.
	 *
	 * @return {@code true} if accessibility was successfully enabled
	 */
	boolean trySetAccessible();

	/**
	 * Tests whether the caller can access this field for the given object.
	 *
	 * @param obj the instance to test against, or {@code null} for a static field
	 * @return {@code true} if access is permitted
	 */
	boolean canAccess(Object obj);

	// --- Field metadata ---

	/**
	 * @return the declared type of this field
	 */
	IClass<?> getType();

	/**
	 * @return the generic declared type of this field
	 */
	Type getGenericType();

	/**
	 * @return {@code true} if this field represents an enum constant
	 */
	boolean isEnumConstant();

	/**
	 * @return a string describing this field including generic type information
	 */
	String toGenericString();

	/**
	 * @return the annotated declared type of this field
	 */
	IAnnotatedType getAnnotatedType();

	// --- Object access ---

	/**
	 * Reads the value of this field from the given object.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the current field value
	 * @throws IllegalArgumentException if {@code obj} is not an instance of the declaring class
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a value to this field on the given object.
	 *
	 * @param obj   the instance to write to, or {@code null} for a static field
	 * @param value the value to set
	 * @throws IllegalArgumentException if {@code obj} or {@code value} is incompatible with the field
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;

	// --- Primitive access ---

	/**
	 * Reads this field as a {@code boolean}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code boolean} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param z   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code byte}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code byte} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param b   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code char}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code char} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param c   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code short}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code short} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param s   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as an {@code int}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes an {@code int} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param i   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code long}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code long} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param l   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code float}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code float} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param f   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Reads this field as a {@code double}.
	 *
	 * @param obj the instance to read from, or {@code null} for a static field
	 * @return the field value
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible
	 */
	double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Writes a {@code double} value to this field.
	 *
	 * @param obj the instance to write to, or {@code null} for a static field
	 * @param d   the value to set
	 * @throws IllegalArgumentException if the field type is incompatible
	 * @throws IllegalAccessException   if this field is not accessible or is final
	 */
	void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException;

}
