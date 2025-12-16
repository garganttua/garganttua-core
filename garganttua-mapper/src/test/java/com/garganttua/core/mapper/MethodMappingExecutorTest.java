package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.rules.MethodMappingExecutor;

/**
 * Test class for {@link MethodMappingExecutor}.
 * Tests method-based field mapping in both regular and reverse directions.
 */
public class MethodMappingExecutorTest {

    @Test
    void testDoMappingWithRegularDirection() throws Exception {
        Field sourceField = SourceClass.class.getDeclaredField("value");
        Field destinationField = DestinationClass.class.getDeclaredField("transformedValue");
        Method method = DestinationClass.class.getDeclaredMethod("transformValue", String.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();

        DestinationClass result = executor.doMapping(DestinationClass.class, destination, source);

        assertNotNull(result);
        assertEquals("TRANSFORMED:test", result.transformedValue);
    }

    @Test
    void testDoMappingWithReverseDirection() throws Exception {
        Field sourceField = SourceClass.class.getDeclaredField("value");
        Field destinationField = DestinationClass.class.getDeclaredField("transformedValue");
        Method method = SourceClass.class.getDeclaredMethod("reverseTransform", String.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REVERSE);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();

        DestinationClass result = executor.doMapping(DestinationClass.class, destination, source);

        assertNotNull(result);
        assertEquals("REVERSE:test", result.transformedValue);
    }

    @Test
    void testDoMappingWithNullSourceValue() throws Exception {
        Field sourceField = SourceClass.class.getDeclaredField("value");
        Field destinationField = DestinationClass.class.getDeclaredField("transformedValue");
        Method method = DestinationClass.class.getDeclaredMethod("transformValue", String.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = null;

        DestinationClass destination = new DestinationClass();
        destination.transformedValue = "old-value";

        DestinationClass result = executor.doMapping(DestinationClass.class, destination, source);

        assertNotNull(result);
        assertEquals("old-value", result.transformedValue); // Should remain unchanged
    }

    @Test
    void testDoMappingWithIntegerTransformation() throws Exception {
        Field sourceField = SourceClassWithInt.class.getDeclaredField("number");
        Field destinationField = DestinationClassWithInt.class.getDeclaredField("doubledNumber");
        Method method = DestinationClassWithInt.class.getDeclaredMethod("doubleValue", Integer.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClassWithInt source = new SourceClassWithInt();
        source.number = 42;

        DestinationClassWithInt destination = new DestinationClassWithInt();

        DestinationClassWithInt result = executor.doMapping(DestinationClassWithInt.class, destination, source);

        assertNotNull(result);
        assertEquals(84, result.doubledNumber);
    }

    @Test
    void testDoMappingRegularPreservesExistingFields() throws Exception {
        Field sourceField = SourceClass.class.getDeclaredField("value");
        Field destinationField = DestinationClass.class.getDeclaredField("transformedValue");
        Method method = DestinationClass.class.getDeclaredMethod("transformValue", String.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();
        destination.otherField = "preserved";

        DestinationClass result = executor.doMapping(DestinationClass.class, destination, source);

        assertEquals("TRANSFORMED:test", result.transformedValue);
        assertEquals("preserved", result.otherField);
    }

    @Test
    void testConstructor() throws Exception {
        Field sourceField = SourceClass.class.getDeclaredField("value");
        Field destinationField = DestinationClass.class.getDeclaredField("transformedValue");
        Method method = DestinationClass.class.getDeclaredMethod("transformValue", String.class);

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        assertNotNull(executor);
    }

    // Test helper classes
    @SuppressWarnings("unused")
    private static class SourceClass {
        private String value;

        @SuppressWarnings("unused")
        private String reverseTransform(String input) {
            if (input == null) return null;
            return "REVERSE:" + input;
        }
    }

    @SuppressWarnings("unused")
    private static class DestinationClass {
        private String transformedValue;
        private String otherField;

        @SuppressWarnings("unused")
        private String transformValue(String input) {
            if (input == null) return null;
            return "TRANSFORMED:" + input;
        }
    }

    @SuppressWarnings("unused")
    private static class SourceClassWithInt {
        private Integer number;
    }

    @SuppressWarnings("unused")
    private static class DestinationClassWithInt {
        private Integer doubledNumber;

        @SuppressWarnings("unused")
        private Integer doubleValue(Integer input) {
            if (input == null) return null;
            return input * 2;
        }
    }
}
