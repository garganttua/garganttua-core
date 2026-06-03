package com.garganttua.core.console;

/**
 * ANSI color palette and color-application helper for the console, plus terminal
 * color-support detection. Extracted from {@link ScriptConsole}; the color constants
 * are statically imported there so existing references resolve unqualified.
 */
final class ConsolePalette {

    // Reset and styles
    static final String RESET = "\u001B[0m";
    static final String BOLD = "\u001B[1m";
    static final String DIM = "\u001B[2m";
    static final String ITALIC = "\u001B[3m";
    static final String UNDERLINE = "\u001B[4m";

    // Foreground colors
    static final String BLACK = "\u001B[30m";
    static final String RED = "\u001B[31m";
    static final String GREEN = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String BLUE = "\u001B[34m";
    static final String MAGENTA = "\u001B[35m";
    static final String CYAN = "\u001B[36m";
    static final String WHITE = "\u001B[37m";

    // Bright foreground colors
    static final String BRIGHT_BLACK = "\u001B[90m";
    static final String BRIGHT_RED = "\u001B[91m";
    static final String BRIGHT_GREEN = "\u001B[92m";
    static final String BRIGHT_YELLOW = "\u001B[93m";
    static final String BRIGHT_BLUE = "\u001B[94m";
    static final String BRIGHT_MAGENTA = "\u001B[95m";
    static final String BRIGHT_CYAN = "\u001B[96m";
    static final String BRIGHT_WHITE = "\u001B[97m";

    private final boolean colorsEnabled;

    ConsolePalette(boolean colorsEnabled) {
        this.colorsEnabled = colorsEnabled;
    }

    /** Wraps {@code text} in the given ANSI codes (followed by RESET) when colors are enabled. */
    String color(String text, String... codes) {
        if (!colorsEnabled) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            sb.append(code);
        }
        sb.append(text).append(RESET);
        return sb.toString();
    }

    /** Detects whether the current terminal supports ANSI colors. */
    static boolean detectColorSupport() {
        // Check for common environment variables that indicate color support
        String term = System.getenv("TERM");
        String colorterm = System.getenv("COLORTERM");
        String forceColor = System.getenv("FORCE_COLOR");

        // Force color if explicitly requested
        if (forceColor != null && !forceColor.equals("0")) {
            return true;
        }

        // Check if stdout is a terminal (System.console() returns non-null)
        if (System.console() == null) {
            return false;
        }

        // Check TERM variable
        if (term != null) {
            return term.contains("color") || term.contains("xterm") ||
                    term.contains("screen") || term.contains("tmux") ||
                    term.contains("vt100") || term.contains("ansi") ||
                    term.contains("linux") || term.contains("cygwin");
        }

        // Check COLORTERM
        if (colorterm != null) {
            return true;
        }

        // Default to true on Unix-like systems
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }
}
