package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.populator.PopulationContext;

/**
 * Behaviour tests for {@link MethodMappingStrategy#fromString} and {@link PopulationContext}
 * path/warning/error bookkeeping.
 */
class MethodMappingBehaviourTest {

    // ---------- MethodMappingStrategy.fromString ----------

    @Test
    void fromStringNullDefaultsToSmart() {
        assertEquals(MethodMappingStrategy.SMART, MethodMappingStrategy.fromString(null));
    }

    @Test
    void fromStringEmptyDefaultsToSmart() {
        assertEquals(MethodMappingStrategy.SMART, MethodMappingStrategy.fromString(""));
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(MethodMappingStrategy.DIRECT, MethodMappingStrategy.fromString("direct"));
        assertEquals(MethodMappingStrategy.CAMEL_CASE, MethodMappingStrategy.fromString("camel_case"));
        assertEquals(MethodMappingStrategy.KEBAB_CASE, MethodMappingStrategy.fromString("Kebab_Case"));
    }

    @Test
    void fromStringUnknownThrows() {
        assertThrows(IllegalArgumentException.class, () -> MethodMappingStrategy.fromString("bogus"));
    }

    // ---------- PopulationContext ----------

    @Test
    void contextRootPathIsEmpty() {
        assertEquals("", new PopulationContext(false).getCurrentPath());
    }

    @Test
    void pushAndPopBuildDotPath() {
        var ctx = new PopulationContext(false);
        ctx.pushPath("server");
        assertEquals("server", ctx.getCurrentPath());
        ctx.pushPath("host");
        assertEquals("server.host", ctx.getCurrentPath());
        ctx.popPath();
        assertEquals("server", ctx.getCurrentPath());
        ctx.popPath();
        assertEquals("", ctx.getCurrentPath());
    }

    @Test
    void warningsArePrefixedWithCurrentPath() {
        var ctx = new PopulationContext(false);
        ctx.pushPath("a");
        ctx.addWarning("oops");
        assertEquals(java.util.List.of("a: oops"), ctx.getWarnings());
        assertFalse(ctx.hasErrors());
    }

    @Test
    void errorsArePrefixedAndFlagged() {
        var ctx = new PopulationContext(true);
        ctx.pushPath("x");
        ctx.pushPath("y");
        ctx.addError("bad");
        assertTrue(ctx.hasErrors());
        assertEquals(java.util.List.of("x.y: bad"), ctx.getErrors());
    }

    @Test
    void warningsAndErrorsSnapshotsAreImmutable() {
        var ctx = new PopulationContext(false);
        ctx.addWarning("w");
        var warnings = ctx.getWarnings();
        assertThrows(UnsupportedOperationException.class, () -> warnings.add("nope"));
    }

    @Test
    void emptyContextHasNoErrorsOrWarnings() {
        var ctx = new PopulationContext(true);
        assertFalse(ctx.hasErrors());
        assertTrue(ctx.getWarnings().isEmpty());
        assertTrue(ctx.getErrors().isEmpty());
    }
}
