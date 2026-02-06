package com.garganttua.core.script.console;

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

    public static void set(ConsoleContext ctx) {
        CURRENT.set(ctx);
    }

    public static ConsoleContext get() {
        return CURRENT.get();
    }

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

        public ConsoleContext(Map<String, Object> sessionVariables, IExpressionContext expressionContext,
                PrintStream out, PrintStream err) {
            this(sessionVariables, expressionContext, out, err, null, null, false);
        }

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

        public Map<String, Object> getSessionVariables() {
            return sessionVariables;
        }

        public IExpressionContext getExpressionContext() {
            return expressionContext;
        }

        public PrintStream getOut() {
            return out;
        }

        public PrintStream getErr() {
            return err;
        }

        public Terminal getTerminal() {
            return terminal;
        }

        public LineReader getLineReader() {
            return lineReader;
        }

        public boolean isColorsEnabled() {
            return colorsEnabled;
        }

        public boolean isExitRequested() {
            return exitRequested;
        }

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
