package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mapper.rules.SimpleFieldMappingExecutor;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link SimpleFieldMappingExecutor}.
 * Tests simple field-to-field mapping.
 */
public class SimpleFieldMappingExecutorTest {

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

    private SimpleFieldMappingExecutor executor;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("name");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("name");
        executor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);
    }

    @Test
    void testDoMappingWithExistingDestinationObject() throws MapperException, NoSuchFieldException {
        SourceClass source = new SourceClass();
        source.name = "test-value";

        DestinationClass destination = new DestinationClass();
        destination.name = "old-value";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertNotNull(result);
        assertEquals("test-value", result.name);
        assertSame(destination, result);
    }

    @Test
    void testDoMappingWithNullDestinationObject() throws MapperException {
        SourceClass source = new SourceClass();
        source.name = "test-value";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertEquals("test-value", result.name);
    }

    @Test
    void testDoMappingWithNullSourceValue() throws MapperException {
        SourceClass source = new SourceClass();
        source.name = null;

        DestinationClass destination = new DestinationClass();
        destination.name = "old-value";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertNotNull(result);
        assertNull(result.name);
    }

    @Test
    void testDoMappingWithIntegerField() throws MapperException, NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("age");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("age");
        SimpleFieldMappingExecutor intExecutor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);

        SourceClass source = new SourceClass();
        source.age = 42;

        DestinationClass result = intExecutor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertEquals(42, result.age);
    }

    @Test
    void testDoMappingWithBooleanField() throws MapperException, NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("active");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("active");
        SimpleFieldMappingExecutor boolExecutor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);

        SourceClass source = new SourceClass();
        source.active = true;

        DestinationClass result = boolExecutor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertTrue(result.active);
    }

    @Test
    void testDoMappingWithDoubleField() throws MapperException, NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("score");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("score");
        SimpleFieldMappingExecutor doubleExecutor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);

        SourceClass source = new SourceClass();
        source.score = 99.5;

        DestinationClass result = doubleExecutor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertEquals(99.5, result.score, 0.001);
    }

    @Test
    void testDoMappingPreservesOtherFields() throws MapperException {
        SourceClass source = new SourceClass();
        source.name = "new-name";

        DestinationClass destination = new DestinationClass();
        destination.name = "old-name";
        destination.age = 25;
        destination.active = true;

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), destination, source);

        assertEquals("new-name", result.name);
        assertEquals(25, result.age); // preserved
        assertTrue(result.active); // preserved
    }

    @Test
    void testDoMappingWithZeroValues() throws MapperException, NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("age");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("age");
        SimpleFieldMappingExecutor intExecutor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);

        SourceClass source = new SourceClass();
        source.age = 0;

        DestinationClass result = intExecutor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertEquals(0, result.age);
    }

    @Test
    void testDoMappingWithEmptyString() throws MapperException {
        SourceClass source = new SourceClass();
        source.name = "";

        DestinationClass result = executor.doMapping(wrapClass(DestinationClass.class), null, source);

        assertNotNull(result);
        assertEquals("", result.name);
    }

    @Test
    void testConstructor() throws NoSuchFieldException {
        IField sourceField = wrapClass(SourceClass.class).getDeclaredField("name");
        IField destinationField = wrapClass(DestinationClass.class).getDeclaredField("name");

        SimpleFieldMappingExecutor newExecutor = new SimpleFieldMappingExecutor(reflection, sourceField, destinationField);

        assertNotNull(newExecutor);
    }

    @SuppressWarnings("unchecked")
    private static <T> IClass<T> wrapClass(Class<?> clazz) {
        return (IClass<T>) reflection.getClass(clazz);
    }

    // Test helper classes
    @SuppressWarnings("unused")
    private static class SourceClass {
        private String name;
        private int age;
        private boolean active;
        private double score;
    }

    @SuppressWarnings("unused")
    private static class DestinationClass {
        private String name;
        private int age;
        private boolean active;
        private double score;

        public DestinationClass() {
            // Default constructor required
        }
    }
}
