package com.garganttua.core.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link ConsoleResultFormatter}: void-result detection,
 * variable-name validation edge cases, and colored value formatting per type.
 * Uses a colors-disabled palette so formatting assertions are on the plain text.
 */
class ConsoleResultFormatterBehaviourTest {

    private ConsoleResultFormatter plainFormatter() {
        return new ConsoleResultFormatter(new ConsolePalette(false));
    }

    private ConsoleResultFormatter coloredFormatter() {
        return new ConsoleResultFormatter(new ConsolePalette(true));
    }

    // ---- isVoidResult ----

    @Test
    void isVoidResult_nullIsVoid() {
        assertTrue(plainFormatter().isVoidResult(null));
    }

    @Test
    void isVoidResult_emptyStringIsVoid() {
        assertTrue(plainFormatter().isVoidResult(""));
    }

    @Test
    void isVoidResult_exitingSentinelIsVoid() {
        assertTrue(plainFormatter().isVoidResult("Exiting..."));
    }

    @Test
    void isVoidResult_nonEmptyStringIsNotVoid() {
        assertFalse(plainFormatter().isVoidResult("hello"));
    }

    @Test
    void isVoidResult_blankButNonEmptyStringIsNotVoid() {
        // A single space is non-empty and not the sentinel, so it is displayable.
        assertFalse(plainFormatter().isVoidResult(" "));
    }

    @Test
    void isVoidResult_numberZeroIsNotVoid() {
        assertFalse(plainFormatter().isVoidResult(0));
    }

    @Test
    void isVoidResult_booleanFalseIsNotVoid() {
        assertFalse(plainFormatter().isVoidResult(Boolean.FALSE));
    }

    @Test
    void isVoidResult_exitingWithTrailingSpaceIsNotVoid() {
        // Equality is exact; the sentinel must match precisely.
        assertFalse(plainFormatter().isVoidResult("Exiting... "));
    }

    // ---- isValidVariableName ----

    @Test
    void isValidVariableName_nullIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName(null));
    }

    @Test
    void isValidVariableName_emptyIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName(""));
    }

    @Test
    void isValidVariableName_simpleNameIsValid() {
        assertTrue(plainFormatter().isValidVariableName("foo"));
    }

    @Test
    void isValidVariableName_leadingUnderscoreIsValid() {
        assertTrue(plainFormatter().isValidVariableName("_foo123"));
    }

    @Test
    void isValidVariableName_dollarSignIsValidJavaIdentifier() {
        assertTrue(plainFormatter().isValidVariableName("$x"));
    }

    @Test
    void isValidVariableName_leadingDigitIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName("1foo"));
    }

    @Test
    void isValidVariableName_hyphenInsideIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName("foo-bar"));
    }

    @Test
    void isValidVariableName_spaceInsideIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName("foo bar"));
    }

    @Test
    void isValidVariableName_dotInsideIsInvalid() {
        assertFalse(plainFormatter().isValidVariableName("foo.bar"));
    }

    // ---- formatValueColored (plain palette: assert exact decorated text) ----

    @Test
    void formatValueColored_nullPlain_isLiteralNull() {
        assertEquals("null", plainFormatter().formatValueColored(null));
    }

    @Test
    void formatValueColored_stringPlain_isQuoted() {
        assertEquals("\"hi\"", plainFormatter().formatValueColored("hi"));
    }

    @Test
    void formatValueColored_characterPlain_isSingleQuoted() {
        assertEquals("'a'", plainFormatter().formatValueColored('a'));
    }

    @Test
    void formatValueColored_numberPlain_isToString() {
        assertEquals("42", plainFormatter().formatValueColored(42));
        assertEquals("3.5", plainFormatter().formatValueColored(3.5));
    }

    @Test
    void formatValueColored_booleanPlain_isToString() {
        assertEquals("true", plainFormatter().formatValueColored(Boolean.TRUE));
        assertEquals("false", plainFormatter().formatValueColored(Boolean.FALSE));
    }

    @Test
    void formatValueColored_otherObjectPlain_isToString() {
        Object o = new Object() {
            @Override
            public String toString() {
                return "custom-repr";
            }
        };
        assertEquals("custom-repr", plainFormatter().formatValueColored(o));
    }

    // ---- formatValueColored (colored palette: assert ANSI wrapping is applied) ----

    @Test
    void formatValueColored_stringColored_wrapsQuotedInGreen() {
        String result = coloredFormatter().formatValueColored("hi");
        assertEquals(ConsolePalette.BRIGHT_GREEN + "\"hi\"" + ConsolePalette.RESET, result);
    }

    @Test
    void formatValueColored_numberColored_wrapsInMagenta() {
        String result = coloredFormatter().formatValueColored(7);
        assertEquals(ConsolePalette.BRIGHT_MAGENTA + "7" + ConsolePalette.RESET, result);
    }

    @Test
    void formatValueColored_booleanColored_wrapsInYellow() {
        String result = coloredFormatter().formatValueColored(Boolean.TRUE);
        assertEquals(ConsolePalette.BRIGHT_YELLOW + "true" + ConsolePalette.RESET, result);
    }

    @Test
    void formatValueColored_nullColored_usesDimItalic() {
        String result = coloredFormatter().formatValueColored(null);
        assertEquals(ConsolePalette.DIM + ConsolePalette.ITALIC + "null" + ConsolePalette.RESET, result);
    }
}
