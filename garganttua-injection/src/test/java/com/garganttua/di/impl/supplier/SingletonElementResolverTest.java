package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;

/**
 * Test class for {@link SingletonElementResolver}.
 * Tests singleton bean resolution with @Singleton annotation.
 */
public class SingletonElementResolverTest {

    private SingletonElementResolver resolver;
    private Set<Class<? extends java.lang.annotation.Annotation>> qualifiers;

    @BeforeEach
    void setUp() {
        qualifiers = new HashSet<>();
        resolver = new SingletonElementResolver(qualifiers);
    }

    @Test
    void testConstructorWithQualifiers() {
        assertNotNull(resolver);
    }

    @Test
    void testResolveFieldWithSingletonAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("singletonField");

        Resolved resolved = resolver.resolve(String.class, field);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(String.class, resolved.elementType());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void testResolveFieldWithoutSingletonAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("nonSingletonField");

        Resolved resolved = resolver.resolve(String.class, field);

        assertNotNull(resolved);
        // Without proper bean factory setup, resolution depends on context
        // Just verify the resolver executes without error
    }

    @Test
    void testResolveWithDifferentTypes() throws NoSuchFieldException {
        Field intField = TestClassWithSingleton.class.getDeclaredField("singletonInt");
        Field booleanField = TestClassWithSingleton.class.getDeclaredField("singletonBoolean");

        Resolved intResolved = resolver.resolve(Integer.class, intField);
        Resolved booleanResolved = resolver.resolve(Boolean.class, booleanField);

        assertNotNull(intResolved);
        assertEquals(Integer.class, intResolved.elementType());

        assertNotNull(booleanResolved);
        assertEquals(Boolean.class, booleanResolved.elementType());
    }

    @Test
    void testResolveThrowsExceptionForNullElement() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(String.class, null);
        });
    }

    @Test
    void testResolveThrowsExceptionForNullElementType() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("singletonField");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, field);
        });
    }

    @Test
    void testResolverWithEmptyQualifiers() {
        SingletonElementResolver emptyResolver = new SingletonElementResolver(new HashSet<>());
        assertNotNull(emptyResolver);
    }

    @Test
    void testConstructorThrowsExceptionForNullQualifiers() {
        assertThrows(NullPointerException.class, () -> {
            new SingletonElementResolver(null);
        });
    }

    @Test
    void testResolvedElementIsNotNullable() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("singletonField");

        Resolved resolved = resolver.resolve(String.class, field);

        assertFalse(resolved.nullable());
    }

    // Test helper class
    @SuppressWarnings("unused")
    private static class TestClassWithSingleton {
        @Singleton
        private String singletonField;

        private String nonSingletonField;

        @Singleton
        private Integer singletonInt;

        @Singleton
        private Boolean singletonBoolean;
    }
}
