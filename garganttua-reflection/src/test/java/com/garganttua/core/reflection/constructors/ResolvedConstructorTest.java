package com.garganttua.core.reflection.constructors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.runtime.RuntimeClass;

public class ResolvedConstructorTest {

    public static class SimpleClass {
        public SimpleClass() {}
        public SimpleClass(String name) {}
        public SimpleClass(String name, int value) {}
    }

    @Test
    void testConstructedType() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertEquals(RuntimeClass.of(SimpleClass.class), resolved.constructedType());
    }

    @Test
    void testParameterCount() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertEquals(2, resolved.parameterCount());
    }

    @Test
    void testMatchesNoArgs() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertTrue(resolved.matches());
        assertFalse(resolved.matches(RuntimeClass.of(String.class)));
    }

    @Test
    void testMatchesWithParameterTypes() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertTrue(resolved.matches(RuntimeClass.of(String.class), RuntimeClass.of(int.class)));
        assertFalse(resolved.matches(RuntimeClass.of(int.class), RuntimeClass.of(String.class)));
        assertFalse(resolved.matches(RuntimeClass.of(String.class)));
    }

    @Test
    void testMatchesConstructor() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertTrue(resolved.matches(ctor));
    }

    @Test
    void testDelegatesGetDeclaringClass() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertEquals(ctor.getDeclaringClass(), resolved.getDeclaringClass());
    }

    @Test
    void testDelegatesGetParameterTypes() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class));
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        assertArrayEquals(ctor.getParameterTypes(), resolved.getParameterTypes());
    }

    @Test
    void testEqualsAndHashCode() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ResolvedConstructor<SimpleClass> r1 = new ResolvedConstructor<>(ctor);
        ResolvedConstructor<SimpleClass> r2 = new ResolvedConstructor<>(ctor);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testNotEqualsDifferentConstructors() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor1 = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        IConstructor<SimpleClass> ctor2 = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class));

        ResolvedConstructor<SimpleClass> r1 = new ResolvedConstructor<>(ctor1);
        ResolvedConstructor<SimpleClass> r2 = new ResolvedConstructor<>(ctor2);

        assertNotEquals(r1, r2);
    }

    @Test
    void testToString() throws NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        ResolvedConstructor<SimpleClass> resolved = new ResolvedConstructor<>(ctor);

        String str = resolved.toString();
        assertTrue(str.contains("ResolvedConstructor"));
        assertTrue(str.contains("SimpleClass"));
        assertTrue(str.contains("String"));
        assertTrue(str.contains("int"));
    }
}
