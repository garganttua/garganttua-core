package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.context.properties.PropertyProvider;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

/**
 * Test class for {@link PropertyProvider}.
 * Tests property retrieval, type conversion, mutability, and lifecycle operations.
 */
public class PropertyProviderTest {

    private PropertyProvider propertyProvider;
    private Map<String, Object> properties;

    @BeforeEach
    void setUp() {
        properties = new HashMap<>();
        properties.put("string.property", "test-value");
        properties.put("int.property", 42);
        properties.put("long.property", 123456789L);
        properties.put("double.property", 3.14159);
        properties.put("boolean.property", true);
        propertyProvider = new PropertyProvider(properties);
    }

    @Test
    void testGetPropertyString() throws DiException {
        Optional<String> result = propertyProvider.getProperty("string.property", String.class);

        assertTrue(result.isPresent());
        assertEquals("test-value", result.get());
    }

    @Test
    void testGetPropertyInteger() throws DiException {
        Optional<Integer> result = propertyProvider.getProperty("int.property", Integer.class);

        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void testGetPropertyLong() throws DiException {
        Optional<Long> result = propertyProvider.getProperty("long.property", Long.class);

        assertTrue(result.isPresent());
        assertEquals(123456789L, result.get());
    }

    @Test
    void testGetPropertyDouble() throws DiException {
        Optional<Double> result = propertyProvider.getProperty("double.property", Double.class);

        assertTrue(result.isPresent());
        assertEquals(3.14159, result.get(), 0.00001);
    }

    @Test
    void testGetPropertyBoolean() throws DiException {
        Optional<Boolean> result = propertyProvider.getProperty("boolean.property", Boolean.class);

        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    void testGetPropertyNotFound() throws DiException {
        Optional<String> result = propertyProvider.getProperty("non.existent.property", String.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetPropertyTypeConversionStringToInteger() throws DiException {
        properties.put("string.number", "999");
        propertyProvider = new PropertyProvider(properties);

        Optional<Integer> result = propertyProvider.getProperty("string.number", Integer.class);

        assertTrue(result.isPresent());
        assertEquals(999, result.get());
    }

    @Test
    void testGetPropertyTypeConversionStringToLong() throws DiException {
        properties.put("string.long", "888888888");
        propertyProvider = new PropertyProvider(properties);

        Optional<Long> result = propertyProvider.getProperty("string.long", Long.class);

        assertTrue(result.isPresent());
        assertEquals(888888888L, result.get());
    }

    @Test
    void testGetPropertyTypeConversionStringToDouble() throws DiException {
        properties.put("string.double", "2.71828");
        propertyProvider = new PropertyProvider(properties);

        Optional<Double> result = propertyProvider.getProperty("string.double", Double.class);

        assertTrue(result.isPresent());
        assertEquals(2.71828, result.get(), 0.00001);
    }

    @Test
    void testGetPropertyTypeConversionStringToBoolean() throws DiException {
        properties.put("string.boolean", "false");
        propertyProvider = new PropertyProvider(properties);

        Optional<Boolean> result = propertyProvider.getProperty("string.boolean", Boolean.class);

        assertTrue(result.isPresent());
        assertFalse(result.get());
    }

    @Test
    void testGetPropertyTypeConversionIntegerToString() throws DiException {
        Optional<String> result = propertyProvider.getProperty("int.property", String.class);

        assertTrue(result.isPresent());
        assertEquals("42", result.get());
    }

    @Test
    void testGetPropertyTypeConversionFailure() {
        properties.put("invalid.number", "not-a-number");
        propertyProvider = new PropertyProvider(properties);

        assertThrows(DiException.class, () -> {
            propertyProvider.getProperty("invalid.number", Integer.class);
        });
    }

    @Test
    void testSetProperty() throws DiException {
        propertyProvider.setProperty("new.property", "new-value");

        Optional<String> result = propertyProvider.getProperty("new.property", String.class);
        assertTrue(result.isPresent());
        assertEquals("new-value", result.get());
    }

    @Test
    void testSetPropertyNullKey() {
        assertThrows(DiException.class, () -> {
            propertyProvider.setProperty(null, "value");
        });
    }

    @Test
    void testSetPropertyBlankKey() {
        assertThrows(DiException.class, () -> {
            propertyProvider.setProperty("  ", "value");
        });
    }

    @Test
    void testIsMutable() {
        assertTrue(propertyProvider.isMutable());
    }

    @Test
    void testKeys() {
        Set<String> keys = propertyProvider.keys();

        assertNotNull(keys);
        assertEquals(5, keys.size());
        assertTrue(keys.contains("string.property"));
        assertTrue(keys.contains("int.property"));
        assertTrue(keys.contains("long.property"));
        assertTrue(keys.contains("double.property"));
        assertTrue(keys.contains("boolean.property"));
    }

    @Test
    void testKeysIsUnmodifiable() {
        Set<String> keys = propertyProvider.keys();

        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("new.key");
        });
    }

    @Test
    void testInit() throws LifecycleException {
        propertyProvider.onInit();
        assertTrue(propertyProvider.isInitialized());
    }

    @Test
    void testStart() throws LifecycleException {
        propertyProvider.onInit().onStart();
        assertTrue(propertyProvider.isStarted());
    }

    @Test
    void testFlush() throws LifecycleException, DiException {
        propertyProvider.onInit().onStart();
        propertyProvider.onStop().onFlush();

        Set<String> keys = propertyProvider.keys();
        assertEquals(0, keys.size());

        Optional<String> result = propertyProvider.getProperty("string.property", String.class);
        assertFalse(result.isPresent());
    }

    @Test
    void testStop() throws LifecycleException {
        propertyProvider.onInit().onStart();
        propertyProvider.onStop();
        assertTrue(propertyProvider.isStopped());
    }

    @Test
    void testCopy() throws CopyException, DiException {
        PropertyProvider copy = (PropertyProvider) propertyProvider.copy();

        assertNotNull(copy);
        assertNotSame(propertyProvider, copy);

        Optional<String> result = copy.getProperty("string.property", String.class);
        assertTrue(result.isPresent());
        assertEquals("test-value", result.get());
    }

    @Test
    void testCopyIsIndependent() throws CopyException, DiException {
        PropertyProvider copy = (PropertyProvider) propertyProvider.copy();

        copy.setProperty("new.property", "new-value");

        Optional<String> originalResult = propertyProvider.getProperty("new.property", String.class);
        assertFalse(originalResult.isPresent());

        Optional<String> copyResult = copy.getProperty("new.property", String.class);
        assertTrue(copyResult.isPresent());
        assertEquals("new-value", copyResult.get());
    }

    @Test
    void testConstructorWithNullProperties() {
        assertThrows(NullPointerException.class, () -> {
            new PropertyProvider(null);
        });
    }
}
