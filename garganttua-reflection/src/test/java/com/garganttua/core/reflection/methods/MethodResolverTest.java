package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;

public class MethodResolverTest {

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
        ResolvedMethod resolved = MethodResolver.methodByName(TestService.class, "greet",
                String.class);

        assertNotNull(resolved);
        assertEquals("greet", resolved.name());
        assertEquals(0, resolved.parameterTypes().length);
    }

    @Test
    public void testMethodByNameWithStringParameter() throws ReflectionException {
        // Should find the String parameter version
        ResolvedMethod resolved = MethodResolver.methodByName(TestService.class, "echo",
                String.class, String.class);

        assertNotNull(resolved);
        assertEquals("echo", resolved.name());
        assertEquals(1, resolved.parameterTypes().length);
        assertEquals(String.class, resolved.parameterTypes()[0]);
    }

    @Test
    public void testMethodByNameWithTwoIntParameters() throws ReflectionException {
        // Should find the two-int-parameter version
        ResolvedMethod resolved = MethodResolver.methodByName(TestService.class, "add",
                int.class, int.class, int.class);

        assertNotNull(resolved);
        assertEquals("add", resolved.name());
        assertEquals(2, resolved.parameterTypes().length);
        assertEquals(int.class, resolved.parameterTypes()[0]);
        assertEquals(int.class, resolved.parameterTypes()[1]);
    }

    @Test
    public void testMethodByNameWithTwoDoubleParameters() throws ReflectionException {
        // Should find the double method
        ResolvedMethod resolved = MethodResolver.methodByName(TestService.class, "multiply",
                double.class, double.class, double.class);

        assertNotNull(resolved);
        assertEquals("multiply", resolved.name());
        assertEquals(double.class, resolved.returnType());
    }

    @Test
    public void testMethodByAddressSimple() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("greet", true);

        ResolvedMethod result = MethodResolver.methodByAddress(TestService.class, address,
                String.class);

        assertNotNull(result);
        assertEquals("greet", result.name());
    }

    @Test
    public void testMethodByNameNoMatchingSignature() {
        // Should throw exception when no matching signature is found
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(TestService.class, "echo",
                    void.class, double.class); // No such signature exists
        });
    }

    @Test
    public void testMethodByNameVoidReturn() throws ReflectionException {
        // Should find void method
        ResolvedMethod resolved = MethodResolver.methodByName(TestService.class, "doNothing",
                void.class);

        assertNotNull(resolved);
        assertEquals("doNothing", resolved.name());
    }

    @Test
    public void testMethodByNameNonExistentMethod() {
        // Should throw exception for non-existent method
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(TestService.class, "nonExistent");
        });
    }

    @Test
    public void testMethodByNameWithWrongParameterCount() {
        // Should throw exception when parameter count doesn't match
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName(TestService.class, "echo",
                    String.class, String.class, int.class); // Too many parameters
        });
    }

    @Test
    public void testMethodByMethodSimple() throws ReflectionException, NoSuchMethodException {
        // Get a specific method
        Method specificMethod = TestService.class.getDeclaredMethod("echo", String.class);

        // Should find the exact method
        ResolvedMethod resolved = MethodResolver.methodByMethod(TestService.class, specificMethod);

        assertNotNull(resolved);
        assertEquals("echo", resolved.name());
        assertEquals(1, resolved.parameterTypes().length);
    }

    @Test
    public void testMethodByMethodAdd() throws ReflectionException, NoSuchMethodException {
        Method addMethod = TestService.class.getDeclaredMethod("add", int.class, int.class);

        ResolvedMethod resolved = MethodResolver.methodByMethod(TestService.class, addMethod);

        assertNotNull(resolved);
        assertEquals("add", resolved.name());
        assertEquals(int.class, resolved.returnType());
    }

    @Test
    public void testMethodByMethodMultiply() throws ReflectionException, NoSuchMethodException {
        Method multiplyMethod = TestService.class.getDeclaredMethod("multiply", double.class, double.class);

        ResolvedMethod resolved = MethodResolver.methodByMethod(TestService.class, multiplyMethod);

        assertNotNull(resolved);
        assertEquals("multiply", resolved.name());
        assertEquals(double.class, resolved.returnType());
    }

    @Test
    public void testMultipleOverloadsDetected() {
        // When multiple overloads exist, resolver should throw if signature matches multiple
        // The process() method has overloads with same return type
        assertThrows(ReflectionException.class, () -> {
            // This should fail because there are multiple "process" methods
            MethodResolver.methodByName(TestService.class, "process");
        });
    }

}
