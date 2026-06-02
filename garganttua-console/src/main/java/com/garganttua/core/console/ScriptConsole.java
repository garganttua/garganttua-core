package com.garganttua.core.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.script.context.ScriptContext;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import com.garganttua.core.bootstrap.banner.BootstrapSummary;
import com.garganttua.core.bootstrap.banner.GarganttuaBanner;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.console.ConsoleExecutionContext.ConsoleContext;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.mutex.context.MutexContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;

import static com.garganttua.core.console.ConsolePalette.RESET;
import static com.garganttua.core.console.ConsolePalette.BOLD;
import static com.garganttua.core.console.ConsolePalette.DIM;
import static com.garganttua.core.console.ConsolePalette.ITALIC;
import static com.garganttua.core.console.ConsolePalette.RED;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_BLACK;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_RED;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_GREEN;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_YELLOW;
import static com.garganttua.core.console.ConsolePalette.BRIGHT_CYAN;

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

    private static final String VERSION = com.garganttua.core.bootstrap.GarganttuaVersion.getVersion();

    // JLine terminal lifecycle (terminal, line reader, history, completer)
    private final ConsoleTerminal jline;

    // Fallback reader for testing
    private final BufferedReader fallbackReader;
    private final PrintStream out;
    private final PrintStream err;
    private final boolean colorsEnabled;
    private final boolean useJLine;

    private IExpressionContext expressionContext;
    private IInjectionContext injectionContext;
    private IInjectionContextBuilder injectionContextBuilder;
    private com.garganttua.core.classloader.IClassLoaderManager classLoaderManager;

    private final boolean useAOT;

    private final Map<String, Object> sessionVariables = new LinkedHashMap<>();
    private int statementCount = 0;
    private boolean running = true;

    private final ConsolePalette palette;
    private final ConsoleResultFormatter formatter;

    private String color(String text, String... codes) {
        return palette.color(text, codes);
    }

    /**
     * Creates a new console with standard I/O using JLine for history support.
     */
    public ScriptConsole() {
        this(false);
    }

    /**
     * Creates a new console with standard I/O using JLine for history support.
     *
     * @param useAOT if true, use IndexedAnnotationScanner for faster startup
     */
    public ScriptConsole(boolean useAOT) {
        this.fallbackReader = null;
        this.out = System.out;
        this.err = System.err;
        this.colorsEnabled = ConsolePalette.detectColorSupport();
        this.palette = new ConsolePalette(this.colorsEnabled);
        this.formatter = new ConsoleResultFormatter(this.palette);
        this.useJLine = true;
        this.useAOT = useAOT;
        this.jline = new ConsoleTerminal(this.err);
        this.jline.initialize();
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
        this.palette = new ConsolePalette(this.colorsEnabled);
        this.formatter = new ConsoleResultFormatter(this.palette);
        this.useJLine = false;
        this.useAOT = false;
        // Don't initialize JLine in test mode
        this.jline = new ConsoleTerminal(this.err);
    }

    /**
     * Starts the interactive console.
     */
    public void start() {
        initializeContext();

        ConsoleInputReader inputReader =
                new ConsoleInputReader(useJLine, jline.lineReader(), fallbackReader, out, colorsEnabled);

        try {
            while (running) {
                try {
                    String input = inputReader.readStatement();
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
            jline.saveHistory();
            jline.close();
        }

        out.println(color("Goodbye!", BRIGHT_CYAN) + " " + color("\uD83D\uDC4B", RESET));
    }

    private void initializeContext() {
        // Print banner
        printBanner();

        Instant startTime = Instant.now();

        out.print(color("  Initializing contexts", DIM) + color("...", DIM, BRIGHT_BLACK));
        out.flush();
        if (useAOT) {
            out.println(color("  AOT annotation scanner enabled", DIM, BRIGHT_CYAN));
        }

        ConsoleContextFactory.BuiltContexts contexts = ConsoleContextFactory.build(useAOT);
        this.injectionContextBuilder = contexts.injectionContextBuilder();
        this.injectionContext = contexts.injectionContext();

        // Register the mutex manager in the thread-local context
        MutexContext.set(contexts.mutexManager());

        this.expressionContext = contexts.expressionContext();

        // Configure the completer now that contexts are ready
        if (jline.completer() != null) {
            jline.completer().setExpressionContext(this.expressionContext);
            jline.completer().setSessionVariables(this.sessionVariables);
        }

        out.println(color(" Done!", BRIGHT_GREEN, BOLD));

        // Set up the console execution context for console functions
        ConsoleContext consoleCtx = new ConsoleContext(sessionVariables, expressionContext, out, err,
                jline.terminal(), jline.lineReader(), colorsEnabled);
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
     * Executes a script statement and displays the result.
     */
    private void executeStatement(String statement) {
        statementCount++;

        try {
            ScriptContext script = new ScriptContext(expressionContext, () -> RuntimesBuilder.builder().provide(injectionContextBuilder), classLoaderManager);

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
                out.println(color("\u2192 ", BRIGHT_BLACK) + color(String.valueOf(exitCode), BRIGHT_YELLOW));
            }

        } catch (ScriptException e) {
            err.println(color("\u2717 Error: ", BRIGHT_RED, BOLD) + color(e.getMessage(), RED));
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                err.println(color("  \u21B3 ", BRIGHT_BLACK) + color(e.getCause().getMessage(), DIM));
            }
        } catch (Exception e) {
            err.println(color("\u2717 Unexpected error: ", BRIGHT_RED, BOLD) + color(e.getMessage(), RED));
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
        Optional<?> result = script.getVariable("_", IClass.getClass(Object.class));
        if (result.isPresent()) {
            Object value = result.get();
            // Skip if the result is a trivial value or already printed by the expression
            // itself
            if (value != null && !formatter.isVoidResult(value)) {
                out.println(color("\u21D2 ", BRIGHT_CYAN) + formatter.formatValueColored(value));
            }
        }
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
                if (formatter.isValidVariableName(varName)) {
                    Optional<?> value = script.getVariable(varName, IClass.getClass(Object.class));
                    if (value.isPresent()) {
                        sessionVariables.put(varName, value.get());
                        out.println(color(varName, BRIGHT_CYAN) + color(" = ", BRIGHT_BLACK)
                                + formatter.formatValueColored(value.get()));
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
                    if (formatter.isValidVariableName(varName)) {
                        Optional<?> value = script.getVariable(varName, IClass.getClass(Object.class));
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




}
