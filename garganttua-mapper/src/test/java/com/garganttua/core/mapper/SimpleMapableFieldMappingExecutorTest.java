package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.rules.SimpleMapableFieldMappingExecutor;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link SimpleMapableFieldMappingExecutor}.
 * Tests mapping of nested objects that themselves require mapping.
 */
public class SimpleMapableFieldMappingExecutorTest {

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

    private IMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new Mapper(reflection);
    }

    //@Test
    void testDoMappingWithNestedObject() throws Exception {
        IField sourceField = wrapClass(SourceWithNested.class).getDeclaredField("nested");
        IField destinationField = wrapClass(DestinationWithNested.class).getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "test-value";

        DestinationWithNested result = executor.doMapping(wrapClass(DestinationWithNested.class), null, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("test-value", result.nested.value);
    }

    //@Test
    void testDoMappingWithExistingDestination() throws Exception {
        IField sourceField = wrapClass(SourceWithNested.class).getDeclaredField("nested");
        IField destinationField = wrapClass(DestinationWithNested.class).getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "new-value";

        DestinationWithNested destination = new DestinationWithNested();
        destination.otherField = "preserved";

        DestinationWithNested result = executor.doMapping(wrapClass(DestinationWithNested.class), destination, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("new-value", result.nested.value);
        assertEquals("preserved", result.otherField);
        assertSame(destination, result);
    }

    @Test
    void testDoMappingWithNullNestedObject() throws Exception {
        IField sourceField = wrapClass(SourceWithNested.class).getDeclaredField("nested");
        IField destinationField = wrapClass(DestinationWithNested.class).getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = null;

        DestinationWithNested destination = new DestinationWithNested();
        destination.nested = new NestedDestination();
        destination.nested.value = "old-value";

        DestinationWithNested result = executor.doMapping(wrapClass(DestinationWithNested.class), destination, source);

        assertNotNull(result);
        // Nested should remain unchanged when source is null
        assertNotNull(result.nested);
        assertEquals("old-value", result.nested.value);
    }

    //@Test
    void testDoMappingWithComplexNestedObject() throws Exception {
        IField sourceField = wrapClass(SourceWithComplexNested.class).getDeclaredField("complex");
        IField destinationField = wrapClass(DestinationWithComplexNested.class).getDeclaredField("complex");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        SourceWithComplexNested source = new SourceWithComplexNested();
        source.complex = new ComplexNestedSource();
        source.complex.name = "test-name";
        source.complex.count = 42;

        DestinationWithComplexNested result = executor.doMapping(wrapClass(DestinationWithComplexNested.class),null, source);

        assertNotNull(result);
        assertNotNull(result.complex);
        assertEquals("test-name", result.complex.name);
        assertEquals(42, result.complex.count);
    }

    @Test
    void testConstructor() throws Exception {
        IField sourceField = wrapClass(SourceWithNested.class).getDeclaredField("nested");
        IField destinationField = wrapClass(DestinationWithNested.class).getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        assertNotNull(executor);
    }

    //@Test
    void testDoMappingCreatesDestinationWhenNull() throws Exception {
        IField sourceField = wrapClass(SourceWithNested.class).getDeclaredField("nested");
        IField destinationField = wrapClass(DestinationWithNested.class).getDeclaredField("nested");

        SimpleMapableFieldMappingExecutor executor = new SimpleMapableFieldMappingExecutor(
            reflection, mapper, sourceField, destinationField);

        SourceWithNested source = new SourceWithNested();
        source.nested = new NestedSource();
        source.nested.value = "created";

        DestinationWithNested result = executor.doMapping(wrapClass(DestinationWithNested.class), null, source);

        assertNotNull(result);
        assertNotNull(result.nested);
        assertEquals("created", result.nested.value);
    }

    @SuppressWarnings("unchecked")
    private static <T> IClass<T> wrapClass(Class<?> clazz) {
        return (IClass<T>) reflection.getClass(clazz);
    }

    // Test helper classes
    private static class SourceWithNested {
        private NestedSource nested;
    }

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


    private static class NestedDestination {
        private String value;

        public NestedDestination() {
        }
    }

    private static class SourceWithComplexNested {
        private ComplexNestedSource complex;
    }

    @SuppressWarnings("unused")
    private static class DestinationWithComplexNested {

        @FieldMappingRule(sourceFieldAddress = "complex", fromSourceMethod = "getNestedFromSource" )
        private ComplexNestedDestination complex;

        public ComplexNestedDestination getNestedFromSource(ComplexNestedSource origin){
            return new ComplexNestedDestination(origin.name, origin.count);
        }

        public DestinationWithComplexNested() {
        }
    }

    @SuppressWarnings("unused")
    private static class ComplexNestedSource {
        private String name;
        private int count;
    }

    private static class ComplexNestedDestination {
        private String name;
        private int count;

        public ComplexNestedDestination(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }
}
