package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeMethod;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

public class MethodResolverTest {

    private static final IReflectionProvider provider = new RuntimeReflectionProvider();

    // Test class with methods - avoid complex overloads
    public static class TestService {

        public String greet() {
            return "hello";
        }

        public String echo(String input) {
            return input;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public double multiply(double a, double b) {
            return a * b;
        }

        public void doNothing() {
            // Does nothing
        }

        // Overloaded methods - for testing multiple overload detection
        public String process() {
            return "no args";
        }

        public String process(String input) {
            return "string: " + input;
        }
    }

    @Test
    public void testMethodByNameWithNoParameters() throws ReflectionException {
        // Should find the no-args greet method
        ResolvedMethod resolved = MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "greet",
                RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("greet", resolved.getName());
        assertEquals(0, resolved.getParameterTypes().length);
    }

    @Test
    public void testMethodByNameWithStringParameter() throws ReflectionException {
        // Should find the String parameter version
        ResolvedMethod resolved = MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "echo",
                RuntimeClass.of(String.class), RuntimeClass.of(String.class));

        assertNotNull(resolved);
        assertEquals("echo", resolved.getName());
        assertEquals(1, resolved.getParameterTypes().length);
        assertEquals(RuntimeClass.of(String.class), resolved.getParameterTypes()[0]);
    }

    @Test
    public void testMethodByNameWithTwoIntParameters() throws ReflectionException {
        // Should find the two-int-parameter version
        ResolvedMethod resolved = MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "add",
                RuntimeClass.of(int.class), RuntimeClass.of(int.class), RuntimeClass.of(int.class));

        assertNotNull(resolved);
        assertEquals("add", resolved.getName());
        assertEquals(2, resolved.getParameterTypes().length);
        assertEquals(RuntimeClass.of(int.class), resolved.getParameterTypes()[0]);
        assertEquals(RuntimeClass.of(int.class), resolved.getParameterTypes()[1]);
    }

    @Test
    public void testMethodByNameWithTwoDoubleParameters() throws ReflectionException {
        // Should find the double method
        ResolvedMethod resolved = MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "multiply",
                RuntimeClass.of(double.class), RuntimeClass.of(double.class), RuntimeClass.of(double.class));

        assertNotNull(resolved);
        assertEquals("multiply", resolved.getName());
        assertEquals(RuntimeClass.of(double.class), resolved.returnType());
    }

    @Test
    public void testMethodByAddressSimple() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("greet", true);

        ResolvedMethod result = MethodResolver.methodByAddress(RuntimeClass.of(TestService.class), provider, address,
                RuntimeClass.of(String.class));

        assertNotNull(result);
        assertEquals("greet", result.getName());
    }

    @Test
    public void testMethodByNameNoMatchingSignature() {
        // Should throw exception when no matching signature is found
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "echo",
                    RuntimeClass.of(void.class), RuntimeClass.of(double.class)); // No such signature exists
        });
    }

    @Test
    public void testMethodByNameVoidReturn() throws ReflectionException {
        // Should find void method
        ResolvedMethod resolved = MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "doNothing",
                RuntimeClass.of(void.class));

        assertNotNull(resolved);
        assertEquals("doNothing", resolved.getName());
    }

    @Test
    public void testMethodByNameNonExistentMethod() {
        // Should throw exception for non-existent method
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "nonExistent");
        });
    }

    @Test
    public void testMethodByNameWithWrongParameterCount() {
        // Should throw exception when parameter count doesn't match
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "echo",
                    RuntimeClass.of(String.class), RuntimeClass.of(String.class), RuntimeClass.of(int.class)); // Too many parameters
        });
    }

    @Test
    public void testMethodByMethodSimple() throws ReflectionException, NoSuchMethodException {
        // Get a specific method
        Method specificMethod = TestService.class.getDeclaredMethod("echo", String.class);

        // Should find the exact method
        ResolvedMethod resolved = MethodResolver.methodByMethod(RuntimeClass.of(TestService.class), provider, RuntimeMethod.of(specificMethod));

        assertNotNull(resolved);
        assertEquals("echo", resolved.getName());
        assertEquals(1, resolved.getParameterTypes().length);
    }

    @Test
    public void testMethodByMethodAdd() throws ReflectionException, NoSuchMethodException {
        Method addMethod = TestService.class.getDeclaredMethod("add", int.class, int.class);

        ResolvedMethod resolved = MethodResolver.methodByMethod(RuntimeClass.of(TestService.class), provider, RuntimeMethod.of(addMethod));

        assertNotNull(resolved);
        assertEquals("add", resolved.getName());
        assertEquals(RuntimeClass.of(int.class), resolved.returnType());
    }

    @Test
    public void testMethodByMethodMultiply() throws ReflectionException, NoSuchMethodException {
        Method multiplyMethod = TestService.class.getDeclaredMethod("multiply", double.class, double.class);

        ResolvedMethod resolved = MethodResolver.methodByMethod(RuntimeClass.of(TestService.class), provider, RuntimeMethod.of(multiplyMethod));

        assertNotNull(resolved);
        assertEquals("multiply", resolved.getName());
        assertEquals(RuntimeClass.of(double.class), resolved.returnType());
    }

    @Test
    public void testMultipleOverloadsDetected() {
        // When multiple overloads exist, resolver should throw if signature matches multiple
        // The process() method has overloads with same return type
        assertThrows(ReflectionException.class, () -> {
            // This should fail because there are multiple "process" methods
            MethodResolver.methodByName(RuntimeClass.of(TestService.class), provider, "process");
        });
    }

}
