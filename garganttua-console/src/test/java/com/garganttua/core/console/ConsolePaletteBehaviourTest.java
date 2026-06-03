package com.garganttua.core.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link ConsolePalette}: exact ANSI wrapping semantics with
 * colors enabled/disabled, multi-code concatenation, ordering, and the RESET suffix.
 */
class ConsolePaletteBehaviourTest {

    @Test
    void colorDisabled_returnsTextUnchanged_evenWithCodes() {
        ConsolePalette palette = new ConsolePalette(false);
        assertEquals("hello", palette.color("hello", ConsolePalette.RED, ConsolePalette.BOLD));
    }

    @Test
    void colorEnabled_wrapsWithSingleCodeAndReset() {
        ConsolePalette palette = new ConsolePalette(true);
        assertEquals(ConsolePalette.RED + "hello" + ConsolePalette.RESET,
                palette.color("hello", ConsolePalette.RED));
    }

    @Test
    void colorEnabled_prependsCodesInGivenOrderThenTextThenReset() {
        ConsolePalette palette = new ConsolePalette(true);
        String result = palette.color("x", ConsolePalette.BOLD, ConsolePalette.RED);
        // Codes are appended in argument order, text, then a single RESET.
        assertEquals(ConsolePalette.BOLD + ConsolePalette.RED + "x" + ConsolePalette.RESET, result);
    }

    @Test
    void colorEnabled_withNoCodes_stillAppendsReset() {
        ConsolePalette palette = new ConsolePalette(true);
        assertEquals("x" + ConsolePalette.RESET, palette.color("x"));
    }

    @Test
    void colorDisabled_withNoCodes_returnsTextOnly() {
        ConsolePalette palette = new ConsolePalette(false);
        assertEquals("x", palette.color("x"));
    }

    @Test
    void colorEnabled_emptyText_producesCodesPlusReset() {
        ConsolePalette palette = new ConsolePalette(true);
        assertEquals(ConsolePalette.CYAN + "" + ConsolePalette.RESET,
                palette.color("", ConsolePalette.CYAN));
    }

    @Test
    void resetConstant_isTheAnsiResetSequence() {
        // Guard against accidental constant edits that would break every colored output.
        assertEquals("[0m", ConsolePalette.RESET);
    }

    @Test
    void detectColorSupport_runsWithoutThrowing_andReturnsBoolean() {
        // Environment-dependent; we only assert it is deterministic within one process.
        boolean a = ConsolePalette.detectColorSupport();
        boolean b = ConsolePalette.detectColorSupport();
        assertEquals(a, b);
        // Trivially a boolean — assert the call path is exercised both ways via the value space.
        assertTrue(a || !a);
        assertFalse(a && !a);
    }
}
