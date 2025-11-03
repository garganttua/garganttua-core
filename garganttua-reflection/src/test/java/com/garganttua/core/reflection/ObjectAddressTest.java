package com.garganttua.core.reflection;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ObjectAddressTest {
	
    @Test
    public void testValidAddressCreation() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("field1.field2.field3");
        assertNotNull(address);
        assertEquals(3, address.length());
        assertEquals("field1", address.getElement(0));
        assertEquals("field2", address.getElement(1));
        assertEquals("field3", address.getElement(2));
    }

    @Test
    public void testAddressToString() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("field1.field2.field3");
        assertEquals("field1.field2.field3", address.toString());
    }

    @Test
    public void testHashCodeAndEquals() throws ReflectionException {
        ObjectAddress address1 = new ObjectAddress("field1.field2.field3");
        ObjectAddress address2 = new ObjectAddress("field1.field2.field3");
        ObjectAddress address3 = new ObjectAddress("field1.field2.field4");

        assertEquals(address1.hashCode(), address2.hashCode());
        assertNotEquals(address1.hashCode(), address3.hashCode());

        assertTrue(address1.equals(address2));
        assertFalse(address1.equals(address3));
    }

    @Test
    public void testInvalidAddressCreation() {
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(".field1.field2"));
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress("field1.field2."));
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(""));
    }

    @Test
    public void testOutOfBoundsIndex() throws ReflectionException {
        ObjectAddress address = new ObjectAddress("field1.field2.field3");
        assertThrows(IllegalArgumentException.class, () -> address.getElement(-1));
        assertThrows(IllegalArgumentException.class, () -> address.getElement(3));
    }

	
}

	