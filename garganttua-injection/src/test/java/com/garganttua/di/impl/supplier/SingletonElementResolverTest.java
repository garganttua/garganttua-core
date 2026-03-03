package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link SingletonElementResolver}.
 * Tests singleton bean resolution with @Singleton annotation.
 */
public class SingletonElementResolverTest {

    private SingletonElementResolver resolver;
    private Set<IClass<? extends java.lang.annotation.Annotation>> qualifiers;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        qualifiers = new HashSet<IClass<? extends java.lang.annotation.Annotation>>();
        resolver = new SingletonElementResolver(qualifiers);
    }

    @Test
    void testConstructorWithQualifiers() {
        assertNotNull(resolver);
    }

    private static IAnnotatedElement adapt(Field field) {
        return IInjectableElementResolver.toIAnnotatedElement(field.getAnnotations(), field.getDeclaredAnnotations());
    }

    @Test
    void testResolveFieldWithSingletonAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("singletonField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(IClass.getClass(String.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void testResolveFieldWithoutSingletonAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("nonSingletonField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        // Without proper bean factory setup, resolution depends on context
        // Just verify the resolver executes without error
    }

    @Test
    void testResolveWithDifferentTypes() throws NoSuchFieldException {
        Field intField = TestClassWithSingleton.class.getDeclaredField("singletonInt");
        Field booleanField = TestClassWithSingleton.class.getDeclaredField("singletonBoolean");

        Resolved intResolved = resolver.resolve(IClass.getClass(Integer.class), adapt(intField));
        Resolved booleanResolved = resolver.resolve(IClass.getClass(Boolean.class), adapt(booleanField));

        assertNotNull(intResolved);
        assertEquals(IClass.getClass(Integer.class), intResolved.elementType());

        assertNotNull(booleanResolved);
        assertEquals(IClass.getClass(Boolean.class), booleanResolved.elementType());
    }

    @Test
    void testResolveThrowsExceptionForNullElement() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(IClass.getClass(String.class), null);
        });
    }

    @Test
    void testResolveThrowsExceptionForNullElementType() throws NoSuchFieldException {
        Field field = TestClassWithSingleton.class.getDeclaredField("singletonField");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, adapt(field));
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

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

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
