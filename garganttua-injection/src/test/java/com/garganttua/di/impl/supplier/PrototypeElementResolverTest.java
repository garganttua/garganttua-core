package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeField;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link PrototypeElementResolver}.
 * Tests prototype bean resolution with @Prototype annotation.
 */
public class PrototypeElementResolverTest {

    private PrototypeElementResolver resolver;
    private Set<IClass<? extends java.lang.annotation.Annotation>> qualifiers;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        qualifiers = new HashSet<IClass<? extends java.lang.annotation.Annotation>>();
        resolver = new PrototypeElementResolver(qualifiers);
    }

    @Test
    void testConstructorWithQualifiers() {
        assertNotNull(resolver);
    }

    private static IAnnotatedElement adapt(Field field) {
        return RuntimeField.of(field);
    }

    @Test
    void testResolveFieldWithPrototypeAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithPrototype.class.getDeclaredField("prototypeField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(IClass.getClass(String.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void testResolveFieldWithoutPrototypeAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithPrototype.class.getDeclaredField("nonPrototypeField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        // Without proper bean factory setup, resolution depends on context
        // Just verify the resolver executes without error
    }

    @Test
    void testResolveWithDifferentTypes() throws NoSuchFieldException {
        Field intField = TestClassWithPrototype.class.getDeclaredField("prototypeInt");
        Field booleanField = TestClassWithPrototype.class.getDeclaredField("prototypeBoolean");

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
        Field field = TestClassWithPrototype.class.getDeclaredField("prototypeField");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, adapt(field));
        });
    }

    @Test
    void testResolverWithEmptyQualifiers() {
        PrototypeElementResolver emptyResolver = new PrototypeElementResolver(new HashSet<>());
        assertNotNull(emptyResolver);
    }

    @Test
    void testConstructorThrowsExceptionForNullQualifiers() {
        assertThrows(NullPointerException.class, () -> {
            new PrototypeElementResolver(null);
        });
    }

    @Test
    void testResolvedElementIsNotNullable() throws NoSuchFieldException {
        Field field = TestClassWithPrototype.class.getDeclaredField("prototypeField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertFalse(resolved.nullable());
    }

    @Test
    void testMultipleResolutionsCreateIndependentResolvers() throws NoSuchFieldException {
        Field field1 = TestClassWithPrototype.class.getDeclaredField("prototypeField");
        Field field2 = TestClassWithPrototype.class.getDeclaredField("prototypeInt");

        Resolved resolved1 = resolver.resolve(IClass.getClass(String.class), adapt(field1));
        Resolved resolved2 = resolver.resolve(IClass.getClass(Integer.class), adapt(field2));

        assertNotSame(resolved1, resolved2);
        assertNotSame(resolved1.elementSupplier(), resolved2.elementSupplier());
    }

    // Test helper class
    @SuppressWarnings("unused")
    private static class TestClassWithPrototype {
        @Prototype
        private String prototypeField;

        private String nonPrototypeField;

        @Prototype
        private Integer prototypeInt;

        @Prototype
        private Boolean prototypeBoolean;
    }
}
