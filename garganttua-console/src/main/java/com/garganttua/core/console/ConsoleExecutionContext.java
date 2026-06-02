package com.garganttua.core.console;

import java.io.PrintStream;
import java.util.Map;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import com.garganttua.core.expression.context.IExpressionContext;

/**
 * ThreadLocal holder for console execution context.
 *
 * <p>
 * Provides access to console state (session variables, running flag, etc.)
 * from console expression functions.
 * </p>
 */
public class ConsoleExecutionContext {

    private ConsoleExecutionContext() {
        /* This utility class should not be instantiated */
    }


    private static final ThreadLocal<ConsoleContext> CURRENT = new ThreadLocal<>();

    /**
     * Binds the given console context to the current thread.
     *
     * @param ctx the console context to make current
     */
    public static void set(ConsoleContext ctx) {
        CURRENT.set(ctx);
    }

    /**
     * Returns the console context bound to the current thread.
     *
     * @return the current console context, or {@code null} if none is set
     */
    public static ConsoleContext get() {
        return CURRENT.get();
    }

    /**
     * Removes the console context bound to the current thread.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Console context containing all state needed by console functions.
     */
    public static class ConsoleContext {
        private final Map<String, Object> sessionVariables;
        private final IExpressionContext expressionContext;
        private final PrintStream out;
        private final PrintStream err;
        private final Terminal terminal;
        private final LineReader lineReader;
        private final boolean colorsEnabled;
        private volatile boolean exitRequested = false;

        /**
         * Creates a console context without terminal/line-reader support (e.g. for tests).
         *
         * @param sessionVariables  the live session variable map shared with the console
         * @param expressionContext the expression context used to evaluate statements
         * @param out               the standard output stream (defaults to {@link System#out} if {@code null})
         * @param err               the error output stream (defaults to {@link System#err} if {@code null})
         */
        public ConsoleContext(Map<String, Object> sessionVariables, IExpressionContext expressionContext,
                PrintStream out, PrintStream err) {
            this(sessionVariables, expressionContext, out, err, null, null, false);
        }

        /**
         * Creates a fully wired console context.
         *
         * @param sessionVariables  the live session variable map shared with the console
         * @param expressionContext the expression context used to evaluate statements
         * @param out               the standard output stream (defaults to {@link System#out} if {@code null})
         * @param err               the error output stream (defaults to {@link System#err} if {@code null})
         * @param terminal          the JLine terminal, or {@code null} when unavailable
         * @param lineReader        the JLine line reader, or {@code null} when unavailable
         * @param colorsEnabled     whether ANSI colors are enabled
         */
        public ConsoleContext(Map<String, Object> sessionVariables, IExpressionContext expressionContext,
                PrintStream out, PrintStream err, Terminal terminal, LineReader lineReader,
                boolean colorsEnabled) {
            this.sessionVariables = sessionVariables;
            this.expressionContext = expressionContext;
            this.out = out != null ? out : System.out;
            this.err = err != null ? err : System.err;
            this.terminal = terminal;
            this.lineReader = lineReader;
            this.colorsEnabled = colorsEnabled;
        }

        /** @return the live session variable map shared with the console */
        public Map<String, Object> getSessionVariables() {
            return sessionVariables;
        }

        /** @return the expression context used to evaluate statements */
        public IExpressionContext getExpressionContext() {
            return expressionContext;
        }

        /** @return the standard output stream */
        public PrintStream getOut() {
            return out;
        }

        /** @return the error output stream */
        public PrintStream getErr() {
            return err;
        }

        /** @return the JLine terminal, or {@code null} when unavailable */
        public Terminal getTerminal() {
            return terminal;
        }

        /** @return the JLine line reader, or {@code null} when unavailable */
        public LineReader getLineReader() {
            return lineReader;
        }

        /** @return whether ANSI colors are enabled */
        public boolean isColorsEnabled() {
            return colorsEnabled;
        }

        /** @return whether {@link #requestExit()} has been called */
        public boolean isExitRequested() {
            return exitRequested;
        }

        /** Signals that the console should exit after the current statement. */
        public void requestExit() {
            this.exitRequested = true;
        }

        /**
         * Gets the terminal height, or a default of 24 if not available.
         */
        public int getTerminalHeight() {
            if (terminal != null) {
                return terminal.getHeight();
            }
            return 24; // Default terminal height
        }

        /**
         * Gets the terminal width, or a default of 80 if not available.
         */
        public int getTerminalWidth() {
            if (terminal != null) {
                return terminal.getWidth();
            }
            return 80; // Default terminal width
        }
    }
}
