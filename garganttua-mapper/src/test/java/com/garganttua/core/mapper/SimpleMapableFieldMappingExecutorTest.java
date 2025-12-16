package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.rules.SimpleMapableFieldMappingExecutor;

/**
 * Test class for {@link SimpleMapableFieldMappingExecutor}.
 * Tests mapping of nested objects that themselves require mapping.
 */
public class SimpleMapableFieldMappingExecutorTest {

    private IMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new Mapper();
    }

    @Test
    void testDoMappingWithNestedObject() throws Exception {
        Field sourceField = SourceWithNested.class.getDeclaredField("nested");
        Field destinationField = DestinationWithNested.class.getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "test-value";

        DestinationWithNested result = executor.doMapping(DestinationWithNested.class, null, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("test-value", result.nested.value);
    }

    @Test
    void testDoMappingWithExistingDestination() throws Exception {
        Field sourceField = SourceWithNested.class.getDeclaredField("nested");
        Field destinationField = DestinationWithNested.class.getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "new-value";

        DestinationWithNested destination = new DestinationWithNested();
        destination.otherField = "preserved";

        DestinationWithNested result = executor.doMapping(DestinationWithNested.class, destination, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("new-value", result.nested.value);
        assertEquals("preserved", result.otherField);
        assertSame(destination, result);
    }

    @Test
    void testDoMappingWithNullNestedObject() throws Exception {
        Field sourceField = SourceWithNested.class.getDeclaredField("nested");
        Field destinationField = DestinationWithNested.class.getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = null;

        DestinationWithNested destination = new DestinationWithNested();
        destination.nested = new NestedDestination();
        destination.nested.value = "old-value";

        DestinationWithNested result = executor.doMapping(DestinationWithNested.class, destination, source);

        assertNotNull(result);
        // Nested should remain unchanged when source is null
        assertNotNull(result.nested);
        assertEquals("old-value", result.nested.value);
    }

    @Test
    void testDoMappingWithComplexNestedObject() throws Exception {
        Field sourceField = SourceWithComplexNested.class.getDeclaredField("complex");
        Field destinationField = DestinationWithComplexNested.class.getDeclaredField("complex");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        SourceWithComplexNested source = new SourceWithComplexNested();
        source.complex = new ComplexNestedSource();
        source.complex.name = "test-name";
        source.complex.count = 42;

        DestinationWithComplexNested result = executor.doMapping(DestinationWithComplexNested.class, null, source);

        assertNotNull(result);
        assertNotNull(result.complex);
        assertEquals("test-name", result.complex.name);
        assertEquals(42, result.complex.count);
    }

    @Test
    void testConstructor() throws Exception {
        Field sourceField = SourceWithNested.class.getDeclaredField("nested");
        Field destinationField = DestinationWithNested.class.getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        assertNotNull(executor);
    }

    @Test
    void testDoMappingCreatesDestinationWhenNull() throws Exception {
        Field sourceField = SourceWithNested.class.getDeclaredField("nested");
        Field destinationField = DestinationWithNested.class.getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "created";

        DestinationWithNested result = executor.doMapping(DestinationWithNested.class, null, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("created", result.nested.value);
    }

    // Test helper classes
    @SuppressWarnings("unused")
    private static class SourceWithNested {
        private NestedSource nested;
    }

    @SuppressWarnings("unused")
    private static class DestinationWithNested {
        private NestedDestination nested;
        private String otherField;

        public DestinationWithNested() {
        }
    }

    @SuppressWarnings("unused")
    private static class NestedSource {
        private String value;
    }

    @SuppressWarnings("unused")
    private static class NestedDestination {
        private String value;

        public NestedDestination() {
        }
    }

    @SuppressWarnings("unused")
    private static class SourceWithComplexNested {
        private ComplexNestedSource complex;
    }

    @SuppressWarnings("unused")
    private static class DestinationWithComplexNested {
        private ComplexNestedDestination complex;

        public DestinationWithComplexNested() {
        }
    }

    @SuppressWarnings("unused")
    private static class ComplexNestedSource {
        private String name;
        private int count;
    }

    @SuppressWarnings("unused")
    private static class ComplexNestedDestination {
        private String name;
        private int count;

        public ComplexNestedDestination() {
        }
    }
}
