package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.populator.TypeConverter;

class TypeConverterTest {

    private TypeConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new TypeConverter();
    }

    @Test
    void testStringConversion() throws ConfigurationException {
        assertEquals("hello", this.converter.convert("hello", String.class));
    }

    @Test
    void testIntConversion() throws ConfigurationException {
        assertEquals(42, this.converter.convert("42", int.class));
        assertEquals(42, this.converter.convert("42", Integer.class));
    }

    @Test
    void testLongConversion() throws ConfigurationException {
        assertEquals(123456789L, this.converter.convert("123456789", long.class));
        assertEquals(123456789L, this.converter.convert("123456789", Long.class));
    }

    @Test
    void testDoubleConversion() throws ConfigurationException {
        assertEquals(3.14, this.converter.convert("3.14", double.class), 0.001);
        assertEquals(3.14, this.converter.convert("3.14", Double.class), 0.001);
    }

    @Test
    void testFloatConversion() throws ConfigurationException {
        assertEquals(1.5f, this.converter.convert("1.5", float.class), 0.001f);
    }

    @Test
    void testBooleanConversion() throws ConfigurationException {
        assertTrue(this.converter.convert("true", boolean.class));
        assertFalse(this.converter.convert("false", Boolean.class));
    }

    @Test
    void testByteConversion() throws ConfigurationException {
        assertEquals((byte) 127, this.converter.convert("127", byte.class));
    }

    @Test
    void testShortConversion() throws ConfigurationException {
        assertEquals((short) 1000, this.converter.convert("1000", short.class));
    }

    @Test
    void testCharConversion() throws ConfigurationException {
        assertEquals('A', this.converter.convert("A", char.class));
    }

    @Test
    void testCharConversionFailsForMultiChar() {
        assertThrows(ConfigurationException.class, () -> this.converter.convert("AB", char.class));
    }

    @Test
    void testBigDecimalConversion() throws ConfigurationException {
        assertEquals(new BigDecimal("123.456"), this.converter.convert("123.456", BigDecimal.class));
    }

    @Test
    void testBigIntegerConversion() throws ConfigurationException {
        assertEquals(new BigInteger("999999999999"), this.converter.convert("999999999999", BigInteger.class));
    }

    @Test
    void testDurationConversion() throws ConfigurationException {
        assertEquals(Duration.ofSeconds(30), this.converter.convert("PT30S", Duration.class));
    }

    @Test
    void testPeriodConversion() throws ConfigurationException {
        assertEquals(Period.ofDays(7), this.converter.convert("P7D", Period.class));
    }

    @Test
    void testInstantConversion() throws ConfigurationException {
        var instant = this.converter.convert("2024-01-01T00:00:00Z", Instant.class);
        assertNotNull(instant);
    }

    @Test
    void testLocalDateConversion() throws ConfigurationException {
        assertEquals(LocalDate.of(2024, 1, 15), this.converter.convert("2024-01-15", LocalDate.class));
    }

    @Test
    void testLocalTimeConversion() throws ConfigurationException {
        assertEquals(LocalTime.of(14, 30), this.converter.convert("14:30", LocalTime.class));
    }

    @Test
    void testLocalDateTimeConversion() throws ConfigurationException {
        var ldt = this.converter.convert("2024-01-15T14:30:00", LocalDateTime.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 14, 30, 0), ldt);
    }

    @Test
    void testPathConversion() throws ConfigurationException {
        assertEquals(Path.of("/tmp/test"), this.converter.convert("/tmp/test", Path.class));
    }

    @Test
    void testURIConversion() throws ConfigurationException {
        assertEquals(URI.create("https://example.com"), this.converter.convert("https://example.com", URI.class));
    }

    @Test
    void testUUIDConversion() throws ConfigurationException {
        var uuid = UUID.randomUUID();
        assertEquals(uuid, this.converter.convert(uuid.toString(), UUID.class));
    }

    @Test
    void testEnumConversion() throws ConfigurationException {
        assertEquals(Thread.State.RUNNABLE, this.converter.convert("RUNNABLE", Thread.State.class));
    }

    @Test
    void testNullConversion() throws ConfigurationException {
        assertNull(this.converter.convert(null, String.class));
    }

    @Test
    void testIsConvertible() {
        assertTrue(this.converter.isConvertible(String.class));
        assertTrue(this.converter.isConvertible(int.class));
        assertTrue(this.converter.isConvertible(Integer.class));
        assertTrue(this.converter.isConvertible(Duration.class));
        assertTrue(this.converter.isConvertible(Path.class));
        assertTrue(this.converter.isConvertible(Thread.State.class));
        assertFalse(this.converter.isConvertible(Object.class));
    }
}
