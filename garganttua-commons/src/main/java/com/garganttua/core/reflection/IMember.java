package com.garganttua.core.reflection;

import java.lang.reflect.AccessFlag;
import java.util.Set;

public interface IMember {

    /**
     * Identifies the set of all public members of a class or interface,
     * including inherited members.
     */
    public static final int PUBLIC = 0;

    /**
     * Identifies the set of declared members of a class or interface.
     * Inherited members are not included.
     */
    public static final int DECLARED = 1;

    /**
     * Returns the Class object representing the class or interface
     * that declares the member or constructor represented by this Member.
     *
     * @return an object representing the declaring class of the
     * underlying member
     */
    public IClass<?> getDeclaringClass();

    /**
     * Returns the simple name of the underlying member or constructor
     * represented by this Member.
     *
     * @return the simple name of the underlying member
     */
    public String getName();

    /**
     * Returns the Java language modifiers for the member or
     * constructor represented by this Member, as an integer.  The
     * Modifier class should be used to decode the modifiers in
     * the integer.
     *
     * @return the Java language modifiers for the underlying member
     * @see java.lang.reflect.Modifier
     * @see #accessFlags()
     */
    public int getModifiers();


    /**
     * {@return an unmodifiable set of the {@linkplain AccessFlag
     * access flags} for this member, possibly empty}
     *
     * <p><b>Implementation Note:</b>
     * The default implementation throws {@link
     * UnsupportedOperationException}.</p>
     * @see #getModifiers()
     * @since 20
     */
    public default Set<AccessFlag> accessFlags() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this member was introduced by
     * the compiler; returns {@code false} otherwise.
     *
     * @return true if and only if this member was introduced by
     * the compiler.
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1">JLS 13.1 The Form of a Binary</a>
     * @since 1.5
     */
    public boolean isSynthetic();
}
