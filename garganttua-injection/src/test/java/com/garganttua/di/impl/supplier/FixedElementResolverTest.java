package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.context.resolver.FixedElementResolver;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Test class for {@link FixedElementResolver}.
 * Tests fixed value resolution with @Fixed annotation.
 */
public class FixedElementResolverTest {

    private FixedElementResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new FixedElementResolver();
    }

    @Test
    void testResolveFixedInt() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedInt");

        Resolved resolved = resolver.resolve(int.class, field);

        assertNotNull(resolved);
        assertTrue(resolved.resolved());
        assertEquals(int.class, resolved.elementType());
        assertNotNull(resolved.elementSupplier());

        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(42, value);
    }

    @Test
    void testResolveFixedInteger() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedInteger");

        Resolved resolved = resolver.resolve(Integer.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(100, value);
    }

    @Test
    void testResolveFixedString() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedString");

        Resolved resolved = resolver.resolve(String.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals("test-value", value);
    }

    @Test
    void testResolveFixedDouble() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedDouble");

        Resolved resolved = resolver.resolve(double.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(3.14159, (Double) value, 0.00001);
    }

    @Test
    void testResolveFixedLong() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedLong");

        Resolved resolved = resolver.resolve(long.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(123456789L, value);
    }

    @Test
    void testResolveFixedFloat() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedFloat");

        Resolved resolved = resolver.resolve(float.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(2.718f, (Float) value, 0.001f);
    }

    @Test
    void testResolveFixedBoolean() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedBoolean");

        Resolved resolved = resolver.resolve(boolean.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals(true, value);
    }

    @Test
    void testResolveFixedByte() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedByte");

        Resolved resolved = resolver.resolve(byte.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals((byte) 127, value);
    }

    @Test
    void testResolveFixedShort() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedShort");

        Resolved resolved = resolver.resolve(short.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals((short) 32000, value);
    }

    @Test
    void testResolveFixedChar() throws NoSuchFieldException, DiException, SupplyException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedChar");

        Resolved resolved = resolver.resolve(char.class, field);

        assertTrue(resolved.resolved());
        ISupplier<?> supplier = resolved.elementSupplier().build();
        Object value = supplier.supply().orElse(null);
        assertEquals('A', value);
    }

    @Test
    void testResolveNonPrimitiveReturnsNotResolved() throws NoSuchFieldException, DiException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedObject");

        Resolved resolved = resolver.resolve(Object.class, field);

        assertFalse(resolved.resolved());
        assertNull(resolved.elementSupplier());
    }

    @Test
    void testResolveThrowsExceptionForNullElement() {
        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(int.class, null);
        });
    }

    @Test
    void testResolveThrowsExceptionForNullElementType() throws NoSuchFieldException {
        Field field = TestClassWithFixed.class.getDeclaredField("fixedInt");

        assertThrows(NullPointerException.class, () -> {
            resolver.resolve(null, field);
        });
    }

    @Test
    void testGetFixedValueForAllTypes() throws DiException, NoSuchFieldException {
        Fixed annotation = TestClassWithFixed.class.getDeclaredField("fixedInt")
                .getAnnotation(Fixed.class);

        assertEquals(42, FixedElementResolver.getFixedValue(annotation, int.class));
        assertEquals(42, FixedElementResolver.getFixedValue(annotation, Integer.class));
    }

    @Test
    void testGetFixedValueWithNullAnnotationReturnsNull() throws DiException {
        assertNull(FixedElementResolver.getFixedValue(null, int.class));
    }

    @Test
    void testGetFixedValueWithNullTypeReturnsNull() throws DiException, NoSuchFieldException {
        Fixed annotation = TestClassWithFixed.class.getDeclaredField("fixedInt")
                .getAnnotation(Fixed.class);

        assertNull(FixedElementResolver.getFixedValue(annotation, null));
    }

    @Test
    void testGetFixedValueThrowsExceptionForUnsupportedType() throws NoSuchFieldException {
        Fixed annotation = TestClassWithFixed.class.getDeclaredField("fixedInt")
                .getAnnotation(Fixed.class);

        assertThrows(DiException.class, () -> {
            FixedElementResolver.getFixedValue(annotation, Object.class);
        });
    }

    // Test helper class
    @SuppressWarnings("unused")
    private static class TestClassWithFixed {
        @Fixed(valueInt = 42)
        private int fixedInt;

        @Fixed(valueInt = 100)
        private Integer fixedInteger;

        @Fixed(valueString = "test-value")
        private String fixedString;

        @Fixed(valueDouble = 3.14159)
        private double fixedDouble;

        @Fixed(valueLong = 123456789L)
        private long fixedLong;

        @Fixed(valueFloat = 2.718f)
        private float fixedFloat;

        @Fixed(valueBoolean = true)
        private boolean fixedBoolean;

        @Fixed(valueByte = 127)
        private byte fixedByte;

        @Fixed(valueShort = 32000)
        private short fixedShort;

        @Fixed(valueChar = 'A')
        private char fixedChar;

        @Fixed(valueInt = 999)
        private Object fixedObject;
    }
}
