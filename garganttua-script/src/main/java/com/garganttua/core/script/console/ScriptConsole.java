package com.garganttua.core.script.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.garganttua.core.annotation.processor.IndexedAnnotationScanner;
import com.garganttua.core.bootstrap.banner.BootstrapSummary;
import com.garganttua.core.bootstrap.banner.GarganttuaBanner;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.bootstrap.dsl.IBoostrap;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.context.MutexContext;
import com.garganttua.core.mutex.dsl.IMutexManagerBuilder;
import com.garganttua.core.mutex.dsl.MutexManagerBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.console.ConsoleExecutionContext.ConsoleContext;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Interactive console (REPL) for Garganttua Script.
 *
 * <p>
 * Provides an interactive command-line interface where users can
 * enter script statements and see results immediately. Variables
 * persist across statements within a session.
 * </p>
 *
 * <h2>Console Functions</h2>
 * <ul>
 * <li>{@code help()} - Show help message</li>
 * <li>{@code vars()} - List all variables</li>
 * <li>{@code clear()} - Clear all variables</li>
 * <li>{@code load("file")} - Load and execute a script file</li>
 * <li>{@code man()} - List all expression functions</li>
 * <li>{@code man("name")} or {@code man(index)} - Show function
 * documentation</li>
 * <li>{@code syntax()} - Show syntax reference</li>
 * <li>{@code exit()} or {@code quit()} - Exit the console</li>
 * </ul>
 */
public class ScriptConsole {

    private static final String VERSION = "2.0.0-ALPHA01";

    // ANSI Color Codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String ITALIC = "\u001B[3m";
    private static final String UNDERLINE = "\u001B[4m";

    // Foreground colors
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Bright foreground colors
    private static final String BRIGHT_BLACK = "\u001B[90m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";

    // Styled prompts
    private static final String PROMPT = BOLD + BRIGHT_GREEN + "gs" + RESET + BRIGHT_GREEN + "> " + RESET;
    private static final String CONTINUATION_PROMPT = BRIGHT_BLACK + "... " + RESET;

    // History file location
    private static final String HISTORY_FILE = ".garganttua_script_history";

    // JLine terminal and reader (for interactive mode)
    private Terminal terminal;
    private LineReader lineReader;
    private History history;

    // Fallback reader for testing
    private final BufferedReader fallbackReader;
    private final PrintStream out;
    private final PrintStream err;
    private final boolean colorsEnabled;
    private final boolean useJLine;

    private IExpressionContext expressionContext;
    private IInjectionContext injectionContext;
    private IBoostrap bootstrap;

    private final Map<String, Object> sessionVariables = new LinkedHashMap<>();
    private int statementCount = 0;
    private boolean running = true;

    /**
     * Creates a new console with standard I/O using JLine for history support.
     */
    public ScriptConsole() {
        this.fallbackReader = null;
        this.out = System.out;
        this.err = System.err;
        this.colorsEnabled = detectColorSupport();
        this.useJLine = true;
        initializeJLine();
    }

    /**
     * Creates a new console with custom I/O streams.
     * Uses BufferedReader fallback for testing (no JLine history).
     *
     * @param reader input reader
     * @param out    standard output
     * @param err    error output
     */
    public ScriptConsole(BufferedReader reader, PrintStream out, PrintStream err) {
        this(reader, out, err, false);
    }

    /**
     * Creates a new console with custom I/O streams and color setting.
     * Uses BufferedReader fallback for testing (no JLine history).
     *
     * @param reader        input reader
     * @param out           standard output
     * @param err           error output
     * @param colorsEnabled whether to use ANSI colors
     */
    public ScriptConsole(BufferedReader reader, PrintStream out, PrintStream err, boolean colorsEnabled) {
        this.fallbackReader = reader;
        this.out = out;
        this.err = err;
        this.colorsEnabled = colorsEnabled;
        this.useJLine = false;
        // Don't initialize JLine in test mode
    }

    /**
     * Initializes JLine terminal and line reader with history support.
     */
    private void initializeJLine() {
        try {
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Set up history with file persistence
            Path historyPath = getHistoryPath();
            this.history = new DefaultHistory();

            this.lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(history)
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

    /**
     * Gets the history file path.
     */
    private Path getHistoryPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, HISTORY_FILE);
    }

    /**
     * Detects if the terminal supports ANSI colors.
     */
    private static boolean detectColorSupport() {
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
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    // Color helper methods
    private String color(String text, String... codes) {
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

    private String prompt() {
        return colorsEnabled ? PROMPT : "gs> ";
    }

    private String continuationPrompt() {
        return colorsEnabled ? CONTINUATION_PROMPT : "... ";
    }

    /**
     * Starts the interactive console.
     */
    public void start() {
        initializeContext();

        try {
            while (running) {
                try {
                    String input = readStatement();
                    if (input == null) {
                        // EOF reached
                        break;
                    }

                    input = input.trim();
                    if (input.isEmpty()) {
                        continue;
                    }

                    executeStatement(input);

                    // Check if exit was requested via exit()/quit() functions
                    ConsoleContext ctx = ConsoleExecutionContext.get();
                    if (ctx != null && ctx.isExitRequested()) {
                        running = false;
                    }
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed - just show new prompt
                    out.println();
                } catch (EndOfFileException e) {
                    // Ctrl+D pressed - exit
                    break;
                } catch (IOException e) {
                    err.println("Error reading input: " + e.getMessage());
                }
            }
        } finally {
            // Save history before exit
            saveHistory();
            closeTerminal();
        }

        out.println(color("Goodbye!", BRIGHT_CYAN) + " " + color("👋", RESET));
    }

    /**
     * Saves the command history to file.
     */
    private void saveHistory() {
        if (useJLine && history != null) {
            try {
                history.save();
            } catch (IOException e) {
                // Ignore history save errors
            }
        }
    }

    /**
     * Closes the JLine terminal.
     */
    private void closeTerminal() {
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    private void initializeContext() {
        // Print banner
        printBanner();

        Instant startTime = Instant.now();

        // Use indexed scanner for fast compile-time annotation lookup
        ObjectReflectionHelper.setAnnotationScanner(new IndexedAnnotationScanner());

        out.print(color("  Initializing contexts", DIM) + color("...", DIM, BRIGHT_BLACK));
        out.flush();

        // Create injection context builder
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        // Create mutex manager builder
        IMutexManagerBuilder mutexManagerBuilder = MutexManagerBuilder.builder()
                .withPackage("com.garganttua.core.mutex")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Create expression context builder with specific packages for faster scanning
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua.core.expression.functions")
                .withPackage("com.garganttua.core.script.console")
                .withPackage("com.garganttua.core.script.functions")
                .withPackage("com.garganttua.core.condition")
                .withPackage("com.garganttua.core.injection.functions")
                .withPackage("com.garganttua.core.mutex.functions")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Build contexts manually to ensure proper lifecycle
        this.injectionContext = injectionContextBuilder.build();
        this.injectionContext.onInit().onStart();

        // Build mutex manager and set in thread-local context
        IMutexManager mutexManager = mutexManagerBuilder.build();
        MutexContext.set(mutexManager);

        this.expressionContext = expressionContextBuilder.build();

        out.println(color(" Done!", BRIGHT_GREEN, BOLD));

        // Set up the console execution context for console functions
        ConsoleContext consoleCtx = new ConsoleContext(sessionVariables, expressionContext, out, err,
                terminal, lineReader, colorsEnabled);
        ConsoleExecutionContext.set(consoleCtx);

        Duration startupTime = Duration.between(startTime, Instant.now());

        // Print summary with contributor information
        printStartupSummary(startupTime);

        out.println();
        out.println("Type " + color("help()", BRIGHT_YELLOW) + " for commands, " + color("exit()", BRIGHT_YELLOW)
                + " to quit.");
        out.println();
    }

    private void printBanner() {
        GarganttuaBanner banner = new GarganttuaBanner(VERSION, colorsEnabled);
        banner.print(out);
    }

    private void printStartupSummary(Duration startupTime) {
        BootstrapSummary summary = new BootstrapSummary(colorsEnabled)
                .applicationName("Garganttua Script Console")
                .applicationVersion(VERSION)
                .startupTime(startupTime)
                .buildersCount(2) // injection + expression
                .builtObjectsCount(2); // contexts built

        // Add information from InjectionContext if it implements
        // IBootstrapSummaryContributor
        if (injectionContext instanceof IBootstrapSummaryContributor contributor) {
            String category = contributor.getSummaryCategory();
            Map<String, String> items = contributor.getSummaryItems();
            for (Map.Entry<String, String> entry : items.entrySet()) {
                summary.addItem(category, entry.getKey(), entry.getValue());
            }
        }

        // Add information from ExpressionContext if it implements
        // IBootstrapSummaryContributor
        if (expressionContext instanceof IBootstrapSummaryContributor contributor) {
            String category = contributor.getSummaryCategory();
            Map<String, String> items = contributor.getSummaryItems();
            for (Map.Entry<String, String> entry : items.entrySet()) {
                summary.addItem(category, entry.getKey(), entry.getValue());
            }
        }

        summary.print(out);
    }

    /**
     * Reads a complete statement, handling multi-line input.
     * Multi-line statements are detected by:
     * - Lines ending with backslash (\) for explicit continuation
     * - Open brackets/parentheses
     */
    private String readStatement() throws IOException {
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

    /**
     * Reads a single line using JLine or fallback reader.
     *
     * @param firstLine true if this is the first line (uses main prompt), false for
     *                  continuation
     * @return the line read, or null on EOF
     */
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

    /**
     * Determines if the current input needs continuation based on unclosed
     * brackets.
     * Note: Backslash continuation is handled separately in readStatement().
     */
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

    /**
     * Executes a script statement and displays the result.
     */
    private void executeStatement(String statement) {
        statementCount++;

        try {
            ScriptContext script = new ScriptContext(expressionContext, injectionContext, bootstrap);

            // Inject session variables from previous statements
            for (Map.Entry<String, Object> entry : sessionVariables.entrySet()) {
                script.setVariable(entry.getKey(), entry.getValue());
            }

            script.load(statement);
            script.compile();

            int exitCode = script.execute();

            // Collect any new or updated variables
            collectVariables(script, statement);

            // Display the expression result (stored in special variable "_")
            displayExpressionResult(script, statement);

            // Show result if there's a meaningful exit code
            if (exitCode != 0) {
                out.println(color("→ ", BRIGHT_BLACK) + color(String.valueOf(exitCode), BRIGHT_YELLOW));
            }

        } catch (ScriptException e) {
            err.println(color("✗ Error: ", BRIGHT_RED, BOLD) + color(e.getMessage(), RED));
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                err.println(color("  ↳ ", BRIGHT_BLACK) + color(e.getCause().getMessage(), DIM));
            }
        } catch (Exception e) {
            err.println(color("✗ Unexpected error: ", BRIGHT_RED, BOLD) + color(e.getMessage(), RED));
        }
    }

    /**
     * Displays the result of an expression if it's non-null and not a void
     * operation.
     * Results are stored in the special variable "_" by the script runtime.
     */
    private void displayExpressionResult(IScript script, String statement) {
        // Don't display result for variable assignments (they are shown separately)
        String trimmed = statement.trim();
        if (trimmed.contains("<-") || (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("->"))) {
            return;
        }

        // Get the last result from the "_" special variable
        Optional<?> result = script.getVariable("_", Object.class);
        if (result.isPresent()) {
            Object value = result.get();
            // Skip if the result is a trivial value or already printed by the expression
            // itself
            if (value != null && !isVoidResult(value)) {
                out.println(color("⇒ ", BRIGHT_CYAN) + formatValueColored(value));
            }
        }
    }

    /**
     * Checks if a result should be considered "void" and not displayed.
     */
    private boolean isVoidResult(Object value) {
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

    /**
     * Collects variables from executed script into session variables.
     */
    private void collectVariables(IScript script, String statement) {
        // Extract variable names from the statement
        // Look for patterns like "varName <-" or "varName ="
        String[] lines = statement.split("\n");
        for (String line : lines) {
            line = line.trim();

            // Check for <- assignment
            int arrowIndex = line.indexOf("<-");
            if (arrowIndex > 0) {
                String varName = line.substring(0, arrowIndex).trim();
                if (isValidVariableName(varName)) {
                    Optional<?> value = script.getVariable(varName, Object.class);
                    if (value.isPresent()) {
                        sessionVariables.put(varName, value.get());
                        out.println(color(varName, BRIGHT_CYAN) + color(" = ", BRIGHT_BLACK)
                                + formatValueColored(value.get()));
                    }
                }
            }

            // Check for = assignment (not ==)
            int eqIndex = line.indexOf("=");
            if (eqIndex > 0 && arrowIndex < 0) {
                // Make sure it's not part of -> or ==
                if (eqIndex > 0 && line.charAt(eqIndex - 1) != '-' && line.charAt(eqIndex - 1) != '=' &&
                        (eqIndex + 1 >= line.length() || line.charAt(eqIndex + 1) != '=')) {
                    String varName = line.substring(0, eqIndex).trim();
                    if (isValidVariableName(varName)) {
                        Optional<?> value = script.getVariable(varName, Object.class);
                        if (value.isPresent()) {
                            sessionVariables.put(varName, value.get());
                            out.println(color(varName, BRIGHT_CYAN) + color(" = ", BRIGHT_BLACK)
                                    + color("<expression>", DIM, ITALIC));
                        }
                    }
                }
            }
        }
    }

    private boolean isValidVariableName(String name) {
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

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Character) {
            return "'" + value + "'";
        }
        return value.toString();
    }

    private String formatValueColored(Object value) {
        if (value == null) {
            return color("null", DIM, ITALIC);
        }
        if (value instanceof String) {
            return color("\"" + value + "\"", BRIGHT_GREEN);
        }
        if (value instanceof Character) {
            return color("'" + value + "'", BRIGHT_GREEN);
        }
        if (value instanceof Number) {
            return color(value.toString(), BRIGHT_MAGENTA);
        }
        if (value instanceof Boolean) {
            return color(value.toString(), BRIGHT_YELLOW);
        }
        return color(value.toString(), WHITE);
    }

}
