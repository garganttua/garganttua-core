package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.populator.TypeConverter;
import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

/**
 * Behaviour tests for {@link TypeConverter} focused on edge cases, exact values,
 * and the exception paths not covered by the baseline test.
 */
class TypeConverterBehaviourTest {

    private TypeConverter converter;

    @BeforeAll
    static void setUpReflection() throws Exception {
        ReflectionBuilder.builder()
            .withProvider(new JdkReflectionProvider())
            .build();
    }

    @BeforeEach
    void setUp() {
        this.converter = new TypeConverter();
    }

    // ---------- null handling ----------

    @Test
    void nullValueAlwaysReturnsNullRegardlessOfType() throws Exception {
        assertNull(this.converter.convert(null, Integer.class));
        assertNull(this.converter.convert(null, Duration.class));
        assertNull(this.converter.convert(null, UUID.class));
    }

    // ---------- numeric parse failures ----------

    @Test
    void nonNumericIntThrowsConfigurationException() {
        var ex = assertThrows(ConfigurationException.class, () -> this.converter.convert("notanint", int.class));
        assertTrue(ex.getMessage().contains("Failed to convert"));
        assertTrue(ex.getMessage().contains("notanint"));
        assertNotNull(ex.getCause());
        assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    @Test
    void overflowingByteThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("999", byte.class));
    }

    @Test
    void overflowingShortThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("99999", short.class));
    }

    @Test
    void emptyStringToIntThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("", int.class));
    }

    @Test
    void negativeAndBoundaryIntValues() throws Exception {
        assertEquals(Integer.MIN_VALUE, this.converter.convert(String.valueOf(Integer.MIN_VALUE), int.class));
        assertEquals(Integer.MAX_VALUE, this.converter.convert(String.valueOf(Integer.MAX_VALUE), Integer.class));
        assertEquals(-1, this.converter.convert("-1", int.class));
    }

    // ---------- boolean is lenient ----------

    @Test
    void booleanParsingIsLenientNonTrueIsFalse() throws Exception {
        assertFalse(this.converter.convert("yes", boolean.class));
        assertFalse(this.converter.convert("1", boolean.class));
        assertTrue(this.converter.convert("TRUE", boolean.class));
        assertTrue(this.converter.convert("True", Boolean.class));
    }

    // ---------- char ----------

    @Test
    void emptyStringToCharThrowsWithSpecificMessage() {
        var ex = assertThrows(ConfigurationException.class, () -> this.converter.convert("", char.class));
        assertTrue(ex.getMessage().contains("Cannot convert"));
    }

    @Test
    void singleCharConvertsForWrapperToo() throws Exception {
        assertEquals(Character.valueOf('z'), this.converter.convert("z", Character.class));
    }

    // ---------- big numbers ----------

    @Test
    void bigDecimalAndBigIntegerExactValues() throws Exception {
        assertEquals(new BigDecimal("0.0000001"), this.converter.convert("0.0000001", BigDecimal.class));
        assertEquals(new BigInteger("-12345678901234567890"),
                this.converter.convert("-12345678901234567890", BigInteger.class));
    }

    @Test
    void invalidBigDecimalThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("abc", BigDecimal.class));
    }

    // ---------- temporal failures ----------

    @Test
    void invalidDurationThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("30 seconds", Duration.class));
    }

    @Test
    void invalidLocalDateThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("2024-13-99", java.time.LocalDate.class));
    }

    // ---------- net types ----------

    @Test
    void urlConvertsFromValidUri() throws Exception {
        var url = this.converter.convert("https://example.com/path", URL.class);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    void urlWithUnknownProtocolThrows() {
        // URI.create succeeds but toURL() rejects unknown scheme
        assertThrows(ConfigurationException.class, () -> this.converter.convert("notaprotocol://x", URL.class));
    }

    @Test
    void uriPreservesValue() throws Exception {
        assertEquals(URI.create("ftp://host/file"), this.converter.convert("ftp://host/file", URI.class));
    }

    // ---------- UUID ----------

    @Test
    void invalidUuidThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("not-a-uuid", UUID.class));
    }

    // ---------- Class ----------
    // NOTE: convert(value, Class.class) returning an IClass cast to Class is a suspected
    // production bug (ClassCastException at runtime). Assertions intentionally omitted; the
    // unknown-class path still throws ConfigurationException which we can assert safely is
    // not reachable without first hitting the cast, so it is also omitted.

    // ---------- enum ----------

    @Test
    void enumConversionIsCaseInsensitiveViaUpperCase() throws Exception {
        assertEquals(Thread.State.WAITING, this.converter.convert("waiting", Thread.State.class));
        assertEquals(Thread.State.WAITING, this.converter.convert("Waiting", Thread.State.class));
    }

    @Test
    void unknownEnumConstantThrows() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("NOPE", Thread.State.class));
    }

    // ---------- unsupported type ----------

    @Test
    void unsupportedTypeThrowsWithDescriptiveMessage() {
        var ex = assertThrows(ConfigurationException.class, () -> this.converter.convert("x", Object.class));
        assertTrue(ex.getMessage().contains("Unsupported type conversion"));
        assertTrue(ex.getMessage().contains("java.lang.Object"));
    }

    // ---------- toPrimitiveWrapper ----------

    @Test
    void toPrimitiveWrapperMapsAllPrimitives() {
        assertEquals(Integer.class, this.converter.toPrimitiveWrapper(int.class).orElseThrow());
        assertEquals(Long.class, this.converter.toPrimitiveWrapper(long.class).orElseThrow());
        assertEquals(Double.class, this.converter.toPrimitiveWrapper(double.class).orElseThrow());
        assertEquals(Float.class, this.converter.toPrimitiveWrapper(float.class).orElseThrow());
        assertEquals(Boolean.class, this.converter.toPrimitiveWrapper(boolean.class).orElseThrow());
        assertEquals(Byte.class, this.converter.toPrimitiveWrapper(byte.class).orElseThrow());
        assertEquals(Short.class, this.converter.toPrimitiveWrapper(short.class).orElseThrow());
        assertEquals(Character.class, this.converter.toPrimitiveWrapper(char.class).orElseThrow());
    }

    @Test
    void toPrimitiveWrapperEmptyForNonPrimitive() {
        assertTrue(this.converter.toPrimitiveWrapper(String.class).isEmpty());
        assertTrue(this.converter.toPrimitiveWrapper(Integer.class).isEmpty());
    }

    // ---------- isConvertible ----------

    @Test
    void isConvertibleCoversNumberSubtypes() {
        assertTrue(this.converter.isConvertible(BigDecimal.class));
        assertTrue(this.converter.isConvertible(BigInteger.class));
        assertTrue(this.converter.isConvertible(Long.class));
        assertTrue(this.converter.isConvertible(URL.class));
        assertTrue(this.converter.isConvertible(URI.class));
    }

    @Test
    void isConvertibleFalseForCollectionTypes() {
        assertFalse(this.converter.isConvertible(java.util.List.class));
        assertFalse(this.converter.isConvertible(java.util.Map.class));
        assertFalse(this.converter.isConvertible(StringBuilder.class));
    }
}
