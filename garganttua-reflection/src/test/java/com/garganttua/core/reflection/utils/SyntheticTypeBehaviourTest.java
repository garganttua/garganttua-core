package com.garganttua.core.reflection.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for the synthetic {@link ParameterizedTypeImpl} and
 * {@link WildcardTypeImpl} runtime type constructors.
 */
public class SyntheticTypeBehaviourTest {

    // ===== ParameterizedTypeImpl =====

    @Test
    public void parameterized_exposesRawAndArguments() {
        Type[] args = { String.class };
        ParameterizedTypeImpl pt = new ParameterizedTypeImpl(java.util.List.class, args);
        assertSame(java.util.List.class, pt.getRawType());
        assertArrayEquals(args, pt.getActualTypeArguments());
        assertNull(pt.getOwnerType());
    }

    @Test
    public void parameterized_ownerTypeRetained() {
        ParameterizedTypeImpl pt = new ParameterizedTypeImpl(
                java.util.Map.class, new Type[] { String.class, Integer.class }, Object.class);
        assertSame(Object.class, pt.getOwnerType());
        assertEquals(2, pt.getActualTypeArguments().length);
    }

    @Test
    public void parameterized_toStringRendersGenerics() {
        ParameterizedTypeImpl pt = new ParameterizedTypeImpl(
                java.util.Map.class, new Type[] { String.class, Integer.class });
        String s = pt.toString();
        assertTrue(s.startsWith("java.util.Map<"));
        assertTrue(s.contains("java.lang.String"));
        assertTrue(s.contains("java.lang.Integer"));
        assertTrue(s.contains(", "));
    }

    // ===== WildcardTypeImpl =====

    @Test
    public void wildcard_extendsHasUpperBoundAndNoLower() {
        WildcardType w = WildcardTypeImpl.extends_(Number.class);
        assertArrayEquals(new Type[] { Number.class }, w.getUpperBounds());
        assertEquals(0, w.getLowerBounds().length);
        assertEquals("? extends java.lang.Number", w.toString());
    }

    @Test
    public void wildcard_superHasLowerBound() {
        WildcardType w = WildcardTypeImpl.super_(Integer.class);
        assertArrayEquals(new Type[] { Integer.class }, w.getLowerBounds());
        assertArrayEquals(new Type[] { Object.class }, w.getUpperBounds());
        assertEquals("? super java.lang.Integer", w.toString());
    }

    @Test
    public void wildcard_unboundedRendersQuestionMark() {
        WildcardType w = WildcardTypeImpl.unbounded();
        assertEquals("?", w.toString());
        assertArrayEquals(new Type[] { Object.class }, w.getUpperBounds());
        assertEquals(0, w.getLowerBounds().length);
    }

    @Test
    public void wildcard_nullBoundsDefaultToObjectAndEmpty() {
        WildcardTypeImpl w = new WildcardTypeImpl(null, null);
        assertArrayEquals(new Type[] { Object.class }, w.getUpperBounds());
        assertEquals(0, w.getLowerBounds().length);
    }

    @Test
    public void wildcard_getBoundsReturnsDefensiveCopy() {
        WildcardType w = WildcardTypeImpl.extends_(Number.class);
        Type[] first = w.getUpperBounds();
        first[0] = String.class;
        // mutation of the returned array must not affect the internal state
        assertArrayEquals(new Type[] { Number.class }, w.getUpperBounds());
        assertNotSame(first, w.getUpperBounds());
    }

    @Test
    public void wildcard_equalsAndHashCodeByBounds() {
        WildcardType a = WildcardTypeImpl.extends_(Number.class);
        WildcardType b = WildcardTypeImpl.extends_(Number.class);
        WildcardType c = WildcardTypeImpl.extends_(String.class);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void wildcard_notEqualToNonWildcard() {
        assertNotEquals(WildcardTypeImpl.unbounded(), "not a wildcard");
    }
}
