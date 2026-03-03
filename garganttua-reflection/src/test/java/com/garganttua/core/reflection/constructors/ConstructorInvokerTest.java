package com.garganttua.core.reflection.constructors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.supply.SupplyException;

public class ConstructorInvokerTest {

    public static class SimpleClass {
        public final String name;
        public final int value;

        public SimpleClass() {
            this.name = "default";
            this.value = 0;
        }

        public SimpleClass(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class ThrowingClass {
        public ThrowingClass() {
            throw new IllegalStateException("Constructor failed");
        }
    }

    @Test
    void testNewInstanceNoArgs() throws ReflectionException, NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ConstructorInvoker<SimpleClass> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(ctor));

        IMethodReturn<SimpleClass> result = invoker.newInstance();

        assertFalse(result.hasException());
        assertNotNull(result.single());
        assertEquals("default", result.single().name);
        assertEquals(0, result.single().value);
    }

    @Test
    void testNewInstanceWithArgs() throws ReflectionException, NoSuchMethodException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class)
                .getDeclaredConstructor(RuntimeClass.of(String.class), RuntimeClass.of(int.class));
        ConstructorInvoker<SimpleClass> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(ctor));

        IMethodReturn<SimpleClass> result = invoker.newInstance("hello", 42);

        assertFalse(result.hasException());
        assertNotNull(result.single());
        assertEquals("hello", result.single().name);
        assertEquals(42, result.single().value);
    }

    @Test
    void testNewInstanceExceptionWrapped() throws ReflectionException, NoSuchMethodException {
        IConstructor<ThrowingClass> ctor = RuntimeClass.of(ThrowingClass.class).getDeclaredConstructor();
        ConstructorInvoker<ThrowingClass> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(ctor));

        IMethodReturn<ThrowingClass> result = invoker.newInstance();

        assertTrue(result.hasException());
        assertInstanceOf(IllegalStateException.class, result.getException());
        assertEquals("Constructor failed", result.getException().getMessage());
    }

    @Test
    void testSupplyNoArgs() throws ReflectionException, NoSuchMethodException, SupplyException {
        IConstructor<SimpleClass> ctor = RuntimeClass.of(SimpleClass.class).getDeclaredConstructor();
        ConstructorInvoker<SimpleClass> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(ctor));

        Optional<IMethodReturn<SimpleClass>> result = invoker.supply();

        assertTrue(result.isPresent());
        assertFalse(result.get().hasException());
        assertEquals("default", result.get().single().name);
    }

    @Test
    void testSupplyThrowsOnException() throws NoSuchMethodException {
        IConstructor<ThrowingClass> ctor = RuntimeClass.of(ThrowingClass.class).getDeclaredConstructor();
        ConstructorInvoker<ThrowingClass> invoker = new ConstructorInvoker<>(new ResolvedConstructor<>(ctor));

        assertThrows(SupplyException.class, invoker::supply);
    }

    @Test
    void testNullConstructorThrows() {
        assertThrows(NullPointerException.class, () -> new ConstructorInvoker<>(null));
    }
}
