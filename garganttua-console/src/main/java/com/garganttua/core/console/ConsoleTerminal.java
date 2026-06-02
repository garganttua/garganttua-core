package com.garganttua.core.console;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Owns the JLine terminal, line reader, history and completer for the interactive
 * console, plus their initialization, history persistence and teardown. Extracted
 * from {@link ScriptConsole}; left uninitialized in test mode (fallback reader).
 */
final class ConsoleTerminal {

    private static final String HISTORY_FILE = ".garganttua_script_history";

    private final PrintStream err;

    private Terminal terminal;
    private LineReader lineReader;
    private History history;
    private ScriptCompleter completer;

    ConsoleTerminal(PrintStream err) {
        this.err = err;
    }

    Terminal terminal() {
        return terminal;
    }

    LineReader lineReader() {
        return lineReader;
    }

    ScriptCompleter completer() {
        return completer;
    }

    /** Initializes the JLine terminal with history persistence; falls back silently on failure. */
    void initialize() {
        try {
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up history with file persistence
            Path historyPath = getHistoryPath();
            this.history = new DefaultHistory();
            this.completer = new ScriptCompleter();

            this.lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(history)
                    .completer(completer)
                    .variable(LineReader.HISTORY_FILE, historyPath)
                    .option(LineReader.Option.HISTORY_BEEP, false)
                    .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
                    .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                    .build();

            // Load history from file if it exists
            if (Files.exists(historyPath)) {
                try {
                    history.load();
                } catch (IOException e) {
                    // Ignore history load errors
                }
            }
        } catch (IOException e) {
            // Fall back to non-JLine mode if terminal creation fails
            err.println("Warning: Could not initialize terminal, history support disabled.");
        }
    }

    private Path getHistoryPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, HISTORY_FILE);
    }

    /** Saves the command history to file (no-op when not initialized). */
    void saveHistory() {
        if (history != null) {
            try {
                history.save();
            } catch (IOException e) {
                // Ignore history save errors
            }
        }
    }

    /** Closes the JLine terminal (no-op when not initialized). */
    void close() {
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }
}
