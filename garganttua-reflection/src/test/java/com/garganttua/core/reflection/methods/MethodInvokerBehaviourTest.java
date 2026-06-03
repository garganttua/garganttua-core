package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.SupplyException;

/**
 * Behaviour tests for {@link MethodInvoker} exercising direct (single-element path)
 * instance and static invocation, exception capture, the lazy
 * {@link com.garganttua.core.supply.ISupplier} path, and the construction / argument
 * guards. These mirror how production code (MethodDelegate) builds invokers: the
 * resolved method always carries a single-element path resolved against the runtime
 * object's own class.
 */
public class MethodInvokerBehaviourTest {

    private static final IReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUpReflection() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    // --- Domain ---

    public static class Greeter {
        public Greeter() {
        }

        public String greet(String who) {
            return "hi " + who;
        }

        public String boom() {
            throw new IllegalStateException("kaboom");
        }

        public static String shout(String s) {
            return s.toUpperCase();
        }

        public static String noisy() {
            throw new RuntimeException("static-fail");
        }
    }

    public static class StaticNoArg {
        public static String ping() {
            return "pong";
        }
    }

    private static ResolvedMethod resolveByName(Class<?> owner, String name) throws ReflectionException {
        return MethodResolver.methodByName(RuntimeClass.of(owner), PROVIDER, name);
    }

    // ========================================================================
    // Direct instance invocation
    // ========================================================================

    @Test
    public void invokeReturnsSingleValueFromInstanceMethod() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "greet"), true);

        IMethodReturn<String> result = invoker.invoke(new Greeter(), "world");

        assertTrue(result.isSingle());
        assertFalse(result.hasException());
        assertEquals("hi world", result.single());
    }

    @Test
    public void invokeCapturesTargetExceptionAsMethodReturn() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "boom"), true);

        IMethodReturn<String> result = invoker.invoke(new Greeter());

        assertTrue(result.hasException());
        assertInstanceOf(IllegalStateException.class, result.getException());
        assertEquals("kaboom", result.getException().getMessage());
    }

    @Test
    public void invokeNullTargetOnInstanceMethodThrows() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "greet"), true);

        ReflectionException ex = assertThrows(ReflectionException.class, () -> invoker.invoke(null, "x"));
        assertTrue(ex.getMessage().contains("object is null"));
    }

    @Test
    public void invokeWrongTargetTypeThrows() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "greet"), true);

        ReflectionException ex = assertThrows(ReflectionException.class, () -> invoker.invoke("not a greeter", "x"));
        assertTrue(ex.getMessage().contains("not of type"));
    }

    // ========================================================================
    // Static invocation
    // ========================================================================

    @Test
    public void invokeStaticMethodWithNullTargetSucceeds() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "shout"), true);

        IMethodReturn<String> result = invoker.invoke(null, "loud");

        assertEquals("LOUD", result.single());
    }

    // ========================================================================
    // ISupplier path (no-arg static invocation)
    // ========================================================================

    @Test
    public void supplyInvokesNoArgStaticAndReturnsValue() throws ReflectionException, SupplyException {
        MethodInvoker<StaticNoArg, String> invoker = new MethodInvoker<>(resolveByName(StaticNoArg.class, "ping"), true);

        IMethodReturn<String> result = invoker.supply().orElseThrow();

        assertEquals("pong", result.single());
    }

    @Test
    public void supplyWrapsThrownExceptionInSupplyException() throws ReflectionException {
        MethodInvoker<Greeter, String> invoker = new MethodInvoker<>(resolveByName(Greeter.class, "noisy"), true);

        SupplyException ex = assertThrows(SupplyException.class, invoker::supply);
        assertNotNull(ex.getCause());
    }

    @Test
    public void suppliedClassAndTypeAreMethodReturn() throws ReflectionException {
        MethodInvoker<StaticNoArg, String> invoker = new MethodInvoker<>(resolveByName(StaticNoArg.class, "ping"), true);

        assertEquals(IMethodReturn.class, invoker.getSuppliedClass().getType());
        assertEquals(IMethodReturn.class, invoker.getSuppliedType());
    }

    // ========================================================================
    // Construction guard
    // ========================================================================

    @Test
    public void constructorRejectsNullResolvedMethod() {
        assertThrows(NullPointerException.class, () -> new MethodInvoker<>(null));
    }
}
