package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.context.resolver.NullElementResolver;
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import jakarta.annotation.Nullable;

/**
 * Test class for {@link NullElementResolver}.
 * Tests null value resolution with @Null annotation.
 */
public class NullElementResolverTest {

    private NullElementResolver resolver;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        resolver = new NullElementResolver();
    }

    private static IAnnotatedElement adapt(Field field) {
        return IInjectableElementResolver.toIAnnotatedElement(field.getAnnotations(), field.getDeclaredAnnotations());
    }

    private static IAnnotatedElement adapt(Parameter parameter) {
        return IInjectableElementResolver.toIAnnotatedElement(parameter.getAnnotations(), parameter.getDeclaredAnnotations());
    }

    @Test
    void testResolveFieldWithNullAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(IClass.getClass(String.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
        assertFalse(resolved.nullable());
    }

    @Test
    void testResolveFieldWithNullAndNullable() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableWithNullableAnnotation");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(IClass.getClass(String.class), resolved.elementType());
        assertTrue(resolved.nullable());
    }

    @Test
    void testResolveSuppliesNull() throws NoSuchFieldException, SupplyException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(field));
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);

        assertNull(value);
    }

    @Test
    void testResolveParameterWithNullAnnotation() throws NoSuchMethodException {
        Parameter parameter = TestClassWithNull.class.getConstructor(String.class)
                .getParameters()[0];

        Resolved resolved = resolver.resolve(IClass.getClass(String.class), adapt(parameter));

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(IClass.getClass(String.class), resolved.elementType());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void testResolveWithDifferentTypes() throws NoSuchFieldException {
        Field intField = TestClassWithNull.class.getDeclaredField("nullableInt");
        Field booleanField = TestClassWithNull.class.getDeclaredField("nullableBoolean");
        Field objectField = TestClassWithNull.class.getDeclaredField("nullableObject");

        Resolved intResolved = resolver.resolve(IClass.getClass(Integer.class), adapt(intField));
        Resolved booleanResolved = resolver.resolve(IClass.getClass(Boolean.class), adapt(booleanField));
        Resolved objectResolved = resolver.resolve(IClass.getClass(Object.class), adapt(objectField));

        assertTrue(intResolved.resolved());
        assertEquals(IClass.getClass(Integer.class), intResolved.elementType());

        assertTrue(booleanResolved.resolved());
        assertEquals(IClass.getClass(Boolean.class), booleanResolved.elementType());

        assertTrue(objectResolved.resolved());
        assertEquals(IClass.getClass(Object.class), objectResolved.elementType());
    }

    @Test
    void testResolveThrowsExceptionForNullElement() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(IClass.getClass(String.class), null);
        });
    }

    @Test
    void testResolveThrowsExceptionForNullElementType() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, adapt(field));
        });
    }

    @Test
    void testResolvedSupplierBuildsCorrectly() throws NoSuchFieldException, SupplyException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableInt");

        Resolved resolved = resolver.resolve(IClass.getClass(Integer.class), adapt(field));
        ISupplier<?> supplier = resolved.elementSupplier().build();

        assertNotNull(supplier);
        assertNull(supplier.supply().orElse(null));
    }

    @Test
    void testMultipleResolutionsAreIndependent() throws NoSuchFieldException {
        Field field1 = TestClassWithNull.class.getDeclaredField("nullableField");
        Field field2 = TestClassWithNull.class.getDeclaredField("nullableObject");

        Resolved resolved1 = resolver.resolve(IClass.getClass(String.class), adapt(field1));
        Resolved resolved2 = resolver.resolve(IClass.getClass(Object.class), adapt(field2));

        assertNotSame(resolved1, resolved2);
        assertNotSame(resolved1.elementSupplier(), resolved2.elementSupplier());
    }

    // Test helper class
    @SuppressWarnings("unused")
    private static class TestClassWithNull {
        @Null
        private String nullableField;

        @Null
        @Nullable
        private String nullableWithNullableAnnotation;

        @Null
        private Integer nullableInt;

        @Null
        private Boolean nullableBoolean;

        @Null
        private Object nullableObject;

        public TestClassWithNull(@Null String param) {
            this.nullableField = param;
        }
    }
}
