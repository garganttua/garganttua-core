package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.context.resolver.NullElementResolver;
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
        resolver = new NullElementResolver();
    }

    @Test
    void testResolveFieldWithNullAnnotation() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        Resolved resolved = resolver.resolve(String.class, field);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(String.class, resolved.elementType());
        assertNotNull(resolved.elementSupplier());
        assertFalse(resolved.nullable());
    }

    @Test
    void testResolveFieldWithNullAndNullable() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableWithNullableAnnotation");

        Resolved resolved = resolver.resolve(String.class, field);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(String.class, resolved.elementType());
        assertTrue(resolved.nullable());
    }

    @Test
    void testResolveSuppliesNull() throws NoSuchFieldException, SupplyException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        Resolved resolved = resolver.resolve(String.class, field);
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);

        assertNull(value);
    }

    @Test
    void testResolveParameterWithNullAnnotation() throws NoSuchMethodException {
        Parameter parameter = TestClassWithNull.class.getConstructor(String.class)
                .getParameters()[0];

        Resolved resolved = resolver.resolve(String.class, parameter);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(String.class, resolved.elementType());
        assertNotNull(resolved.elementSupplier());
    }

    @Test
    void testResolveWithDifferentTypes() throws NoSuchFieldException {
        Field intField = TestClassWithNull.class.getDeclaredField("nullableInt");
        Field booleanField = TestClassWithNull.class.getDeclaredField("nullableBoolean");
        Field objectField = TestClassWithNull.class.getDeclaredField("nullableObject");

        Resolved intResolved = resolver.resolve(Integer.class, intField);
        Resolved booleanResolved = resolver.resolve(Boolean.class, booleanField);
        Resolved objectResolved = resolver.resolve(Object.class, objectField);

        assertTrue(intResolved.resolved());
        assertEquals(Integer.class, intResolved.elementType());

        assertTrue(booleanResolved.resolved());
        assertEquals(Boolean.class, booleanResolved.elementType());

        assertTrue(objectResolved.resolved());
        assertEquals(Object.class, objectResolved.elementType());
    }

    @Test
    void testResolveThrowsExceptionForNullElement() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(String.class, null);
        });
    }

    @Test
    void testResolveThrowsExceptionForNullElementType() throws NoSuchFieldException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableField");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, field);
        });
    }

    @Test
    void testResolvedSupplierBuildsCorrectly() throws NoSuchFieldException, SupplyException {
        Field field = TestClassWithNull.class.getDeclaredField("nullableInt");

        Resolved resolved = resolver.resolve(Integer.class, field);
        ISupplier<?> supplier = resolved.elementSupplier().build();

        assertNotNull(supplier);
        assertNull(supplier.supply().orElse(null));
    }

    @Test
    void testMultipleResolutionsAreIndependent() throws NoSuchFieldException {
        Field field1 = TestClassWithNull.class.getDeclaredField("nullableField");
        Field field2 = TestClassWithNull.class.getDeclaredField("nullableObject");

        Resolved resolved1 = resolver.resolve(String.class, field1);
        Resolved resolved2 = resolver.resolve(Object.class, field2);

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
