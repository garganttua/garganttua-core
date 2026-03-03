package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.rules.MethodMappingExecutor;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link MethodMappingExecutor}.
 * Tests method-based field mapping in both regular and reverse directions.
 */
public class MethodMappingExecutorTest {

    private static IReflection reflection;

    @BeforeAll
    static void setUpReflection() throws Exception {
        reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDownReflection() {
        IClass.setReflection(null);
    }

    @Test
    void testDoMappingWithRegularDirection() throws Exception {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("value");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("transformedValue");
        IMethod method = wrapClass(DestinationClass.class).getDeclaredMethod("transformValue", wrapClass(String.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertNotNull(result);
        assertEquals("TRANSFORMED:test", result.transformedValue);
    }

    @Test
    void testDoMappingWithReverseDirection() throws Exception {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("value");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("transformedValue");
        IMethod method = wrapClass(SourceClass.class).getDeclaredMethod("reverseTransform", wrapClass(String.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REVERSE);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertNotNull(result);
        assertEquals("REVERSE:test", result.transformedValue);
    }

    @Test
    void testDoMappingWithNullSourceValue() throws Exception {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("value");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("transformedValue");
        IMethod method = wrapClass(DestinationClass.class).getDeclaredMethod("transformValue", wrapClass(String.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = null;

        DestinationClass destination = new DestinationClass();
        destination.transformedValue = "old-value";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertNotNull(result);
        assertEquals("old-value", result.transformedValue); // Should remain unchanged
    }

    @Test
    void testDoMappingWithIntegerTransformation() throws Exception {
        IField sourceField = wrapClass(SourceClassWithInt.class).getDeclaredField("number");
        IField destinationField = wrapClass(DestinationClassWithInt.class).getDeclaredField("doubledNumber");
        IMethod method = wrapClass(DestinationClassWithInt.class).getDeclaredMethod("doubleValue", wrapClass(Integer.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClassWithInt source = new SourceClassWithInt();
        source.number = 42;

        DestinationClassWithInt destination = new DestinationClassWithInt();

        DestinationClassWithInt result = executor.doMapping(wrapClass(DestinationClassWithInt.class), destination, source);

        assertNotNull(result);
        assertEquals(84, result.doubledNumber);
    }

    @Test
    void testDoMappingRegularPreservesExistingFields() throws Exception {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("value");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("transformedValue");
        IMethod method = wrapClass(DestinationClass.class).getDeclaredMethod("transformValue", wrapClass(String.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        SourceClass source = new SourceClass();
        source.value = "test";

        DestinationClass destination = new DestinationClass();
        destination.otherField = "preserved";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertEquals("TRANSFORMED:test", result.transformedValue);
        assertEquals("preserved", result.otherField);
    }

    @Test
    void testConstructor() throws Exception {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("value");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("transformedValue");
        IMethod method = wrapClass(DestinationClass.class).getDeclaredMethod("transformValue", wrapClass(String.class));

        MethodMappingExecutor executor = new MethodMappingExecutor(
            method, sourceField, destinationField, MappingDirection.REGULAR);

        assertNotNull(executor);
    }

    @SuppressWarnings("unchecked")
    private static <T> IClass<T> wrapClass(Class<?> clazz) {
        return (IClass<T>) reflection.getClass(clazz);
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
