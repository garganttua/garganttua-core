package com.garganttua.core.configuration.populator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.configuration.populator.MethodMapping;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;

class MethodMappingTest {

    // Test builder class with various method signatures
    @SuppressWarnings("unused")
    public static class TestBuilder {
        public TestBuilder name(String name) { return this; }
        public TestBuilder withPort(int port) { return this; }
        public TestBuilder withTimeout(String timeout) { return this; }
        public TestBuilder maxRetries(int retries) { return this; }
        @ConfigProperty("custom-key")
        public TestBuilder customMethod(String value) { return this; }
        @ConfigIgnore
        public TestBuilder ignoredMethod(String value) { return this; }
        public TestBuilder connectionTimeout(String value) { return this; }
    }

    @Test
    void testDirectNameMatch() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "name");
        assertTrue(method.isPresent());
        assertEquals("name", method.get().getName());
    }

    @Test
    void testWithPrefixMatch() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "port");
        assertTrue(method.isPresent());
        assertEquals("withPort", method.get().getName());
    }

    @Test
    void testAnnotationMatch() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "custom-key");
        assertTrue(method.isPresent());
        assertEquals("customMethod", method.get().getName());
    }

    @Test
    void testIgnoredMethod() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "ignoredMethod");
        assertTrue(method.isEmpty());
    }

    @Test
    void testKebabCaseMatch() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "connection-timeout");
        assertTrue(method.isPresent());
        assertEquals("connectionTimeout", method.get().getName());
    }

    @Test
    void testUnknownKey() {
        var mapping = new MethodMapping(MethodMappingStrategy.SMART);
        var method = mapping.resolve(TestBuilder.class, "nonexistent");
        assertTrue(method.isEmpty());
    }

    @Test
    void testDirectStrategyNoFallback() {
        var mapping = new MethodMapping(MethodMappingStrategy.DIRECT);
        // Should find direct match
        var method = mapping.resolve(TestBuilder.class, "name");
        assertTrue(method.isPresent());

        // Should find withPrefix match (still works in DIRECT)
        var method2 = mapping.resolve(TestBuilder.class, "port");
        assertTrue(method2.isPresent());

        // Should NOT find kebab-case (not in DIRECT)
        var method3 = mapping.resolve(TestBuilder.class, "connection-timeout");
        assertTrue(method3.isEmpty());
    }

    @Test
    void testCamelCaseConversion() {
        assertEquals("myProperty", MethodMapping.toCamelCase("my_property"));
        assertEquals("myLongProperty", MethodMapping.toCamelCase("my_long_property"));
    }

    @Test
    void testKebabToCamelCase() {
        assertEquals("connectionTimeout", MethodMapping.kebabToCamelCase("connection-timeout"));
        assertEquals("maxRetryCount", MethodMapping.kebabToCamelCase("max-retry-count"));
    }

    @Test
    void testCapitalize() {
        assertEquals("Hello", MethodMapping.capitalize("hello"));
        assertEquals("A", MethodMapping.capitalize("a"));
        assertEquals("", MethodMapping.capitalize(""));
    }
}
