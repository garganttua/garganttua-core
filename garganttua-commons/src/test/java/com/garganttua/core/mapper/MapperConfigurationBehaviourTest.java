package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MapperConfigurationBehaviourTest {

    @Test
    void defaultsMatchDocumentedValues() {
        MapperConfiguration c = new MapperConfiguration();
        assertTrue(c.failOnError());
        assertTrue(c.doValidation());
        assertTrue(c.failOnCycle());
        assertTrue(c.autoConventionMapping());
        assertFalse(c.strictMode());
    }

    @Test
    void configureOverwritesAndConvenienceAccessorReflectsIt() {
        MapperConfiguration c = new MapperConfiguration();
        c.configure(MapperConfigurationItem.STRICT_MODE, true);
        c.configure(MapperConfigurationItem.FAIL_ON_ERROR, false);
        assertTrue(c.strictMode());
        assertFalse(c.failOnError());
        // untouched items keep defaults
        assertTrue(c.doValidation());
    }

    @Test
    void getConfigurationReturnsRawValueAndNullWhenUnset() {
        MapperConfiguration c = new MapperConfiguration();
        assertEquals(Boolean.TRUE, c.getConfiguration(MapperConfigurationItem.DO_VALIDATION));
        // a non-boolean value can be stored and read back raw
        c.configure(MapperConfigurationItem.DO_VALIDATION, "anything");
        assertEquals("anything", c.getConfiguration(MapperConfigurationItem.DO_VALIDATION));
    }

    @Test
    void lastWriteWins() {
        MapperConfiguration c = new MapperConfiguration();
        c.configure(MapperConfigurationItem.FAIL_ON_CYCLE, false);
        c.configure(MapperConfigurationItem.FAIL_ON_CYCLE, true);
        assertTrue(c.failOnCycle());
    }
}
