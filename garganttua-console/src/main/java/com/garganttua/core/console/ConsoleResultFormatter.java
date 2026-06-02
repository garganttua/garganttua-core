package com.garganttua.core.console;

import static com.garganttua.core.console.ConsolePalette.BRIGHT_GREEN;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_MAGENTA;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_YELLOW;
import static com.garganttua.core.console.ConsolePalette.DIM;
import static com.garganttua.core.console.ConsolePalette.ITALIC;
import static com.garganttua.core.console.ConsolePalette.WHITE;

/**
 * Formats expression result values for colored console display and validates
 * session variable names. Extracted from {@link ScriptConsole}.
 */
final class ConsoleResultFormatter {

    private final ConsolePalette palette;

    ConsoleResultFormatter(ConsolePalette palette) {
        this.palette = palette;
    }

    /** Whether a result should be considered "void" and not displayed. */
    boolean isVoidResult(Object value) {
        if (value == null) {
            return true;
        }
        // Skip empty strings (typically from print operations that return "")
        if (value instanceof String s && s.isEmpty()) {
            return true;
        }
        // Skip "Exiting..." message from exit() command
        if (value instanceof String s && s.equals("Exiting...")) {
            return true;
        }
        return false;
    }

    boolean isValidVariableName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    String formatValueColored(Object value) {
        if (value == null) {
            return palette.color("null", DIM, ITALIC);
        }
        if (value instanceof String) {
            return palette.color("\"" + value + "\"", BRIGHT_GREEN);
        }
        if (value instanceof Character) {
            return palette.color("'" + value + "'", BRIGHT_GREEN);
        }
        if (value instanceof Number) {
            return palette.color(value.toString(), BRIGHT_MAGENTA);
        }
        if (value instanceof Boolean) {
            return palette.color(value.toString(), BRIGHT_YELLOW);
        }
        return palette.color(value.toString(), WHITE);
    }
}
