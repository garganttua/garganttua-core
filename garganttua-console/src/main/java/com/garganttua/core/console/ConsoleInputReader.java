package com.garganttua.core.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import org.jline.reader.LineReader;

import static com.garganttua.core.console.ConsolePalette.BOLD;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_BLACK;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_GREEN;
import static com.garganttua.core.console.ConsolePalette.RESET;

/**
 * Reads a complete (possibly multi-line) statement from JLine or a fallback
 * reader, handling explicit continuation markers and unclosed brackets.
 * Extracted from {@link ScriptConsole}.
 */
final class ConsoleInputReader {

    private static final String PROMPT = BOLD + BRIGHT_GREEN + "gs" + RESET + BRIGHT_GREEN + "> " + RESET;
    private static final String CONTINUATION_PROMPT = BRIGHT_BLACK + "... " + RESET;

    private final boolean useJLine;
    private final LineReader lineReader;
    private final BufferedReader fallbackReader;
    private final PrintStream out;
    private final boolean colorsEnabled;

    ConsoleInputReader(boolean useJLine, LineReader lineReader, BufferedReader fallbackReader,
            PrintStream out, boolean colorsEnabled) {
        this.useJLine = useJLine;
        this.lineReader = lineReader;
        this.fallbackReader = fallbackReader;
        this.out = out;
        this.colorsEnabled = colorsEnabled;
    }

    /**
     * Reads a complete statement, handling multi-line input via trailing
     * {@code ..} / {@code \\} markers or unclosed brackets/parentheses.
     */
    String readStatement() throws IOException {
        StringBuilder statement = new StringBuilder();
        boolean firstLine = true;
        boolean previousWasContinuation = false;

        while (true) {
            String line = readLine(firstLine);
            if (line == null) {
                return statement.length() > 0 ? statement.toString() : null;
            }

            if (firstLine && line.trim().isEmpty()) {
                return "";
            }

            // Check if statement continues on next line
            String trimmed = line.trim();

            // Check for explicit continuation markers:
            // - ".." at end: multi-line continuation (won't be interpreted by terminal)
            // - "\" at end: traditional backslash continuation (may not work in all
            // terminals)
            boolean explicitContinuation = trimmed.endsWith("..") || trimmed.endsWith("\\");
            String continuationMarker = trimmed.endsWith("..") ? ".." : (trimmed.endsWith("\\") ? "\\" : null);

            // Add newline before this line if previous line was a continuation
            if (previousWasContinuation) {
                statement.append("\n");
            }

            // Strip trailing continuation marker (and whitespace before it)
            if (continuationMarker != null) {
                String withoutMarker = trimmed.substring(0, trimmed.length() - continuationMarker.length())
                        .stripTrailing();
                line = withoutMarker;
            }

            statement.append(line);

            // Check if we need to continue reading
            boolean continues = explicitContinuation || needsContinuation(trimmed, statement.toString());

            if (continues) {
                previousWasContinuation = true;
                firstLine = false;
            } else {
                break;
            }
        }

        return statement.toString();
    }

    private String readLine(boolean firstLine) throws IOException {
        String promptStr = firstLine ? prompt() : continuationPrompt();
        String plainPrompt = firstLine ? "gs> " : "... ";

        if (useJLine && lineReader != null) {
            // Use JLine for interactive input with history support
            return lineReader.readLine(colorsEnabled ? promptStr : plainPrompt);
        } else {
            // Fallback to BufferedReader for testing
            out.print(promptStr);
            out.flush();
            return fallbackReader.readLine();
        }
    }

    private boolean needsContinuation(String currentLine, String fullStatement) {
        // Check for unclosed brackets/parentheses
        int parens = 0;
        int brackets = 0;
        int braces = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < fullStatement.length(); i++) {
            char c = fullStatement.charAt(i);

            if (inString) {
                if (c == stringChar && (i == 0 || fullStatement.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            } else {
                switch (c) {
                    case '"':
                    case '\'':
                        inString = true;
                        stringChar = c;
                        break;
                    case '(':
                        parens++;
                        break;
                    case ')':
                        parens--;
                        break;
                    case '[':
                        brackets++;
                        break;
                    case ']':
                        brackets--;
                        break;
                    case '{':
                        braces++;
                        break;
                    case '}':
                        braces--;
                        break;
                }
            }
        }

        return parens > 0 || brackets > 0 || braces > 0 || inString;
    }

    private String prompt() {
        return colorsEnabled ? PROMPT : "gs> ";
    }

    private String continuationPrompt() {
        return colorsEnabled ? CONTINUATION_PROMPT : "... ";
    }
}
