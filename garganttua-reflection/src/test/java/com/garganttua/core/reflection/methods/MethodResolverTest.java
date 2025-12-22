package com.garganttua.core.reflection.methods;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

public class MethodResolverTest {

    // Test class with overloaded methods
    public static class TestService {

        public String process() {
            return "no args";
        }

        public String process(String input) {
            return "string: " + input;
        }

        public String process(int value) {
            return "int: " + value;
        }

        public String process(String input, int value) {
            return "both: " + input + ", " + value;
        }

        public int calculate(int a, int b) {
            return a + b;
        }

        public double calculate(double a, double b) {
            return a + b;
        }

        public void voidMethod() {
            // Does nothing
        }
    }

    @Test
    public void testMethodByNameWithNoParameters() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find the no-args version
        ObjectAddress address = MethodResolver.methodByName("process", query, TestService.class,
                String.class);

        assertNotNull(address);
        assertEquals("process", address.getElement(0));
    }

    @Test
    public void testMethodByNameWithStringParameter() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find the String parameter version
        ObjectAddress address = MethodResolver.methodByName("process", query, TestService.class,
                String.class, String.class);

        assertNotNull(address);
        assertEquals("process", address.getElement(0));
    }

    @Test
    public void testMethodByNameWithIntParameter() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find the int parameter version
        ObjectAddress address = MethodResolver.methodByName("process", query, TestService.class,
                String.class, int.class);

        assertNotNull(address);
        assertEquals("process", address.getElement(0));
    }

    @Test
    public void testMethodByNameWithTwoParameters() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find the two-parameter version
        ObjectAddress address = MethodResolver.methodByName("process", query, TestService.class,
                String.class, String.class, int.class);

        assertNotNull(address);
        assertEquals("process", address.getElement(0));
    }

    @Test
    public void testMethodByNameWithDifferentReturnTypes() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find the int version based on return type
        ObjectAddress address1 = MethodResolver.methodByName("calculate", query, TestService.class,
                int.class, int.class, int.class);

        assertNotNull(address1);

        // Should find the double version based on return type
        ObjectAddress address2 = MethodResolver.methodByName("calculate", query, TestService.class,
                double.class, double.class, double.class);

        assertNotNull(address2);
    }

    @Test
    public void testMethodByAddressWithOverload() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);
        ObjectAddress address = new ObjectAddress("process", true);

        // With signature specified, should find the correct overload
        ObjectAddress result = MethodResolver.methodByAddress(address, query, TestService.class,
                String.class, String.class);

        assertNotNull(result);
        assertEquals("process", result.getElement(0));
    }

    @Test
    public void testMethodByNameNoMatchingSignature() {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should throw exception when no matching signature is found
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName("process", query, TestService.class,
                    void.class, double.class); // No such signature exists
        });
    }

    @Test
    public void testMethodByNameVoidReturn() throws ReflectionException {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should find void method
        ObjectAddress address = MethodResolver.methodByName("voidMethod", query, TestService.class,
                void.class);

        assertNotNull(address);
        assertEquals("voidMethod", address.getElement(0));
    }

    @Test
    public void testMethodByNameNonExistentMethod() {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should throw exception for non-existent method
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName("nonExistent", query, TestService.class);
        });
    }

    @Test
    public void testMethodByNameWithWrongParameterCount() {
        IObjectQuery query = ObjectQueryFactory.objectQuery(TestService.class);

        // Should throw exception when parameter count doesn't match
        assertThrows(ReflectionException.class, () -> {
            MethodResolver.methodByName("process", query, TestService.class,
                    String.class, String.class, int.class, String.class); // Too many parameters
        });
    }

    @Test
    public void testMethodByMethodWithOverload() throws ReflectionException, NoSuchMethodException {
        // Get a specific overloaded method
        Method specificMethod = TestService.class.getDeclaredMethod("process", String.class);

        // Should find the exact method
        ObjectAddress address = MethodResolver.methodByMethod(specificMethod, TestService.class);

        assertNotNull(address);
        assertEquals("process", address.getElement(0));
    }

    @Test
    public void testMethodByMethodWithDifferentOverloads() throws ReflectionException, NoSuchMethodException {
        // Get different overloads
        Method noArgsMethod = TestService.class.getDeclaredMethod("process");
        Method stringArgMethod = TestService.class.getDeclaredMethod("process", String.class);
        Method intArgMethod = TestService.class.getDeclaredMethod("process", int.class);

        // Should find each specific method
        ObjectAddress address1 = MethodResolver.methodByMethod(noArgsMethod, TestService.class);
        ObjectAddress address2 = MethodResolver.methodByMethod(stringArgMethod, TestService.class);
        ObjectAddress address3 = MethodResolver.methodByMethod(intArgMethod, TestService.class);

        assertNotNull(address1);
        assertNotNull(address2);
        assertNotNull(address3);

        // All should point to "process"
        assertEquals("process", address1.getElement(0));
        assertEquals("process", address2.getElement(0));
        assertEquals("process", address3.getElement(0));
    }

    @Test
    public void testMethodByMethodWithCalculateOverloads() throws ReflectionException, NoSuchMethodException {
        // Get different calculate overloads
        Method intCalculate = TestService.class.getDeclaredMethod("calculate", int.class, int.class);
        Method doubleCalculate = TestService.class.getDeclaredMethod("calculate", double.class, double.class);

        // Should find each specific method
        ObjectAddress address1 = MethodResolver.methodByMethod(intCalculate, TestService.class);
        ObjectAddress address2 = MethodResolver.methodByMethod(doubleCalculate, TestService.class);

        assertNotNull(address1);
        assertNotNull(address2);

        assertEquals("calculate", address1.getElement(0));
        assertEquals("calculate", address2.getElement(0));
    }
}
