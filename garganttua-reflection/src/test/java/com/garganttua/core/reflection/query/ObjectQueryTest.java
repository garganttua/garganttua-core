package com.garganttua.core.reflection.query;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;

public class ObjectQueryTest {

    // Test class with overloaded methods
    public static class TestClass {
        private String name;
        private int value;

        public void testMethod() {
        }

        public void testMethod(String arg) {
        }

        public void testMethod(String arg1, int arg2) {
        }

        public void testMethod(int arg) {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    public void testFindSingleMethod() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // find() should return only the first method found
        List<Object> results = query.find("testMethod");

        assertEquals(1, results.size(), "find() should return only one method");
        assertTrue(results.get(0) instanceof Method, "Result should be a Method");
        assertEquals("testMethod", ((Method) results.get(0)).getName());
    }

    @Test
    public void testFindAllOverloadedMethods() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // findAll() should return all overloaded methods
        List<Object> results = query.findAll("testMethod");

        assertEquals(4, results.size(), "findAll() should return all 4 overloaded methods");

        // Verify all results are Methods with the correct name
        for (Object obj : results) {
            assertTrue(obj instanceof Method, "Each result should be a Method");
            assertEquals("testMethod", ((Method) obj).getName());
        }

        // Verify different parameter counts
        boolean noArgs = false;
        boolean oneStringArg = false;
        boolean twoArgs = false;
        boolean oneIntArg = false;

        for (Object obj : results) {
            Method m = (Method) obj;
            int paramCount = m.getParameterCount();
            Class<?>[] paramTypes = m.getParameterTypes();

            if (paramCount == 0) {
                noArgs = true;
            } else if (paramCount == 1 && paramTypes[0] == String.class) {
                oneStringArg = true;
            } else if (paramCount == 1 && paramTypes[0] == int.class) {
                oneIntArg = true;
            } else if (paramCount == 2) {
                twoArgs = true;
            }
        }

        assertTrue(noArgs, "Should find no-args variant");
        assertTrue(oneStringArg, "Should find String arg variant");
        assertTrue(oneIntArg, "Should find int arg variant");
        assertTrue(twoArgs, "Should find two-args variant");
    }

    @Test
    public void testFindAllWithStringAddress() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test findAll with String parameter
        List<Object> results = query.findAll("testMethod");

        assertEquals(4, results.size(), "findAll(String) should return all 4 overloaded methods");
    }

    @Test
    public void testFindAllWithObjectAddress() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test findAll with ObjectAddress parameter
        ObjectAddress address = new ObjectAddress("testMethod", true);
        List<Object> results = query.findAll(address);

        assertEquals(4, results.size(), "findAll(ObjectAddress) should return all 4 overloaded methods");
    }

    @Test
    public void testFindAllWithNonOverloadedMethod() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test with a method that has no overloads
        List<Object> results = query.findAll("getName");

        assertEquals(1, results.size(), "findAll() should return 1 method when there's no overload");
        assertTrue(results.get(0) instanceof Method);
        assertEquals("getName", ((Method) results.get(0)).getName());
    }

    @Test
    public void testFindField() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Find a field (behavior should be the same for find() and findAll())
        List<Object> findResults = query.find("name");
        List<Object> findAllResults = query.findAll("name");

        assertEquals(1, findResults.size(), "find() should return the field");
        assertEquals(1, findAllResults.size(), "findAll() should return the field");
        assertEquals(findResults.get(0), findAllResults.get(0), "find() and findAll() should return same field");
    }

    @Test
    public void testAddressSingleMethod() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // address() should return the first method found
        ObjectAddress address = query.address("testMethod");

        assertNotNull(address, "address() should return an ObjectAddress");
        assertEquals("testMethod", address.toString(), "Address should be 'testMethod'");
    }

    @Test
    public void testAddressesOverloadedMethods() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // addresses() should return all ObjectAddress instances for overloaded methods
        List<ObjectAddress> addresses = query.addresses("testMethod");

        assertEquals(4, addresses.size(), "addresses() should return 4 ObjectAddress instances for overloaded methods");

        // Verify all addresses have the same string representation
        for (ObjectAddress addr : addresses) {
            assertEquals("testMethod", addr.toString(), "Each address should be 'testMethod'");
        }
    }

    @Test
    public void testAddressesNonOverloadedMethod() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test with a method that has no overloads
        List<ObjectAddress> addresses = query.addresses("getName");

        assertEquals(1, addresses.size(), "addresses() should return 1 address when there's no overload");
        assertEquals("getName", addresses.get(0).toString());
    }

    @Test
    public void testAddressesField() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test with a field (should return single address)
        ObjectAddress singleAddress = query.address("name");
        List<ObjectAddress> multipleAddresses = query.addresses("name");

        assertEquals(1, multipleAddresses.size(), "addresses() should return 1 address for a field");
        assertEquals(singleAddress.toString(), multipleAddresses.get(0).toString(),
                "address() and addresses() should return same address for a field");
    }

    @Test
    public void testAddressesEmptyResult() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // Test with a non-existent element
        List<ObjectAddress> addresses = query.addresses("nonExistent");

        assertNotNull(addresses, "addresses() should return non-null list");
        assertTrue(addresses.isEmpty(), "addresses() should return empty list for non-existent element");
    }

    @Test
    public void testAddressVsAddressesConsistency() throws ReflectionException {
        IObjectQuery query = new ObjectQuery(TestClass.class);

        // For overloaded methods, address() returns first, addresses() returns all
        ObjectAddress singleAddress = query.address("testMethod");
        List<ObjectAddress> multipleAddresses = query.addresses("testMethod");

        assertNotNull(singleAddress, "address() should return an ObjectAddress");
        assertEquals(4, multipleAddresses.size(), "addresses() should return all 4 overloads");

        // The single address should have the same string representation as the addresses in the list
        // (all should be "testMethod")
        assertEquals(singleAddress.toString(), multipleAddresses.get(0).toString(),
                "Single address should match the address from the list");
    }
}
