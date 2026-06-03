package com.garganttua.core.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class TypeUtilsBehaviourTest {

    // --- wrapperFor (Class-based) ---

    @Test
    void wrapperForReturnsWrapperForEachPrimitive() {
        assertEquals(Boolean.class, TypeUtils.wrapperFor(boolean.class));
        assertEquals(Byte.class, TypeUtils.wrapperFor(byte.class));
        assertEquals(Character.class, TypeUtils.wrapperFor(char.class));
        assertEquals(Short.class, TypeUtils.wrapperFor(short.class));
        assertEquals(Integer.class, TypeUtils.wrapperFor(int.class));
        assertEquals(Long.class, TypeUtils.wrapperFor(long.class));
        assertEquals(Float.class, TypeUtils.wrapperFor(float.class));
        assertEquals(Double.class, TypeUtils.wrapperFor(double.class));
        assertEquals(Void.class, TypeUtils.wrapperFor(void.class));
    }

    @Test
    void wrapperForReturnsNullForNonPrimitive() {
        assertNull(TypeUtils.wrapperFor(String.class));
        assertNull(TypeUtils.wrapperFor(Integer.class));
        assertNull(TypeUtils.wrapperFor(Object.class));
    }

    // --- wrapperNameFor (String-based) ---

    @Test
    void wrapperNameForMapsPrimitiveNamesToWrapperFqn() {
        assertEquals("java.lang.Integer", TypeUtils.wrapperNameFor("int"));
        assertEquals("java.lang.Boolean", TypeUtils.wrapperNameFor("boolean"));
        assertEquals("java.lang.Character", TypeUtils.wrapperNameFor("char"));
        assertEquals("java.lang.Void", TypeUtils.wrapperNameFor("void"));
    }

    @Test
    void wrapperNameForReturnsNullForUnknownName() {
        assertNull(TypeUtils.wrapperNameFor("java.lang.Integer"));
        assertNull(TypeUtils.wrapperNameFor("String"));
        assertNull(TypeUtils.wrapperNameFor(""));
    }

    // --- isWrapperType ---

    @Test
    void isWrapperTypeRecognizesWrapperFqns() {
        assertTrue(TypeUtils.isWrapperType("java.lang.Integer"));
        assertTrue(TypeUtils.isWrapperType("java.lang.Void"));
        assertTrue(TypeUtils.isWrapperType("java.lang.Character"));
    }

    @Test
    void isWrapperTypeRejectsNonWrappers() {
        assertFalse(TypeUtils.isWrapperType("int"));
        assertFalse(TypeUtils.isWrapperType("java.lang.String"));
        assertFalse(TypeUtils.isWrapperType("java.util.Date"));
        assertFalse(TypeUtils.isWrapperType("unknown"));
    }

    // --- isAssignable ---

    @Test
    void isAssignableDirectHierarchy() {
        IClass<?> number = JdkClass.of(Number.class);
        IClass<?> integer = JdkClass.of(Integer.class);
        assertTrue(TypeUtils.isAssignable(number, integer));
        assertFalse(TypeUtils.isAssignable(integer, number));
    }

    @Test
    void isAssignablePrimitiveFormalAcceptsWrapperActual() {
        IClass<?> primInt = JdkClass.of(int.class);
        IClass<?> wrapInt = JdkClass.of(Integer.class);
        assertTrue(TypeUtils.isAssignable(primInt, wrapInt));
    }

    @Test
    void isAssignableWrapperFormalAcceptsPrimitiveActual() {
        IClass<?> wrapInt = JdkClass.of(Integer.class);
        IClass<?> primInt = JdkClass.of(int.class);
        assertTrue(TypeUtils.isAssignable(wrapInt, primInt));
    }

    @Test
    void isAssignableMismatchedPrimitiveWrapperIsFalse() {
        IClass<?> primInt = JdkClass.of(int.class);
        IClass<?> wrapLong = JdkClass.of(Long.class);
        assertFalse(TypeUtils.isAssignable(primInt, wrapLong));
    }

    @Test
    void isAssignableUnrelatedTypesIsFalse() {
        IClass<?> str = JdkClass.of(String.class);
        IClass<?> integer = JdkClass.of(Integer.class);
        assertFalse(TypeUtils.isAssignable(str, integer));
    }

    // --- isComplexType ---

    @Test
    void isComplexTypeTrueForUserType() {
        assertTrue(TypeUtils.isComplexType(JdkClass.of(TypeUtilsBehaviourTest.class)));
    }

    @Test
    void isComplexTypeFalseForPrimitive() {
        assertFalse(TypeUtils.isComplexType(JdkClass.of(int.class)));
    }

    @Test
    void isComplexTypeFalseForSimpleTypes() {
        assertFalse(TypeUtils.isComplexType(JdkClass.of(String.class)));
        assertFalse(TypeUtils.isComplexType(JdkClass.of(Integer.class)));
        assertFalse(TypeUtils.isComplexType(JdkClass.of(Date.class)));
    }

    @Test
    void isComplexTypeFalseForJdkType() {
        assertFalse(TypeUtils.isComplexType(JdkClass.of(List.class)));
        assertFalse(TypeUtils.isComplexType(JdkClass.of(Thread.class)));
    }

    // --- isNotPrimitive ---

    @Test
    void isNotPrimitiveFalseForPrimitiveAndSimple() {
        assertFalse(TypeUtils.isNotPrimitive(JdkClass.of(int.class)));
        assertFalse(TypeUtils.isNotPrimitive(JdkClass.of(Integer.class)));
        assertFalse(TypeUtils.isNotPrimitive(JdkClass.of(String.class)));
        assertFalse(TypeUtils.isNotPrimitive(JdkClass.of(Date.class)));
    }

    @Test
    void isNotPrimitiveTrueForComplexAndJdkCollections() {
        assertTrue(TypeUtils.isNotPrimitive(JdkClass.of(TypeUtilsBehaviourTest.class)));
        // List is NOT in SIMPLE_TYPE_NAMES, so isNotPrimitive is true even though isComplexType is false
        assertTrue(TypeUtils.isNotPrimitive(JdkClass.of(List.class)));
    }

    // --- isNotPrimitiveOrInternal delegates to isComplexType ---

    @Test
    void isNotPrimitiveOrInternalEqualsIsComplexType() {
        IClass<?> user = JdkClass.of(TypeUtilsBehaviourTest.class);
        IClass<?> jdk = JdkClass.of(List.class);
        assertEquals(TypeUtils.isComplexType(user), TypeUtils.isNotPrimitiveOrInternal(user));
        assertEquals(TypeUtils.isComplexType(jdk), TypeUtils.isNotPrimitiveOrInternal(jdk));
        assertTrue(TypeUtils.isNotPrimitiveOrInternal(user));
        assertFalse(TypeUtils.isNotPrimitiveOrInternal(jdk));
    }
}
