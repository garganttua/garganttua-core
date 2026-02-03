package com.garganttua.core.script.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.bootstrap.dsl.IBoostrap;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Interactive console (REPL) for Garganttua Script.
 *
 * <p>Provides an interactive command-line interface where users can
 * enter script statements and see results immediately. Variables
 * persist across statements within a session.</p>
 *
 * <h2>Special Commands</h2>
 * <ul>
 *   <li>{@code :help} - Show help message</li>
 *   <li>{@code :vars} - List all variables</li>
 *   <li>{@code :clear} - Clear all variables</li>
 *   <li>{@code :load <file>} - Load and execute a script file</li>
 *   <li>{@code :exit} or {@code :quit} - Exit the console</li>
 * </ul>
 */
public class ScriptConsole {

    private static final String VERSION = "2.0.0-ALPHA01";
    private static final String PROMPT = "gs> ";
    private static final String CONTINUATION_PROMPT = "... ";

    private final BufferedReader reader;
    private final PrintStream out;
    private final PrintStream err;

    private IExpressionContext expressionContext;
    private IInjectionContext injectionContext;
    private IBoostrap bootstrap;

    private final Map<String, Object> sessionVariables = new LinkedHashMap<>();
    private int statementCount = 0;
    private boolean running = true;

    /**
     * Creates a new console with standard I/O.
     */
    public ScriptConsole() {
        this(new BufferedReader(new InputStreamReader(System.in)), System.out, System.err);
    }

    /**
     * Creates a new console with custom I/O streams.
     *
     * @param reader input reader
     * @param out standard output
     * @param err error output
     */
    public ScriptConsole(BufferedReader reader, PrintStream out, PrintStream err) {
        this.reader = reader;
        this.out = out;
        this.err = err;
    }

    /**
     * Starts the interactive console.
     */
    public void start() {
        printWelcome();
        initializeContext();

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

                if (input.startsWith(":")) {
                    handleCommand(input);
                } else {
                    executeStatement(input);
                }
            } catch (IOException e) {
                err.println("Error reading input: " + e.getMessage());
            }
        }

        out.println("Goodbye!");
    }

    private void printWelcome() {
        out.println("Garganttua Script Console " + VERSION);
        out.println("Type :help for available commands, :exit to quit.");
        out.println();
    }

    private void initializeContext() {
        out.print("Initializing...");
        out.flush();

        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());

        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        this.injectionContext = injectionContextBuilder.build();
        this.injectionContext.onInit().onStart();
        this.expressionContext = expressionContextBuilder.build();

        out.println(" Ready!");
        out.println();
    }

    /**
     * Reads a complete statement, handling multi-line input.
     * Multi-line statements are detected by:
     * - Lines ending with continuation characters (!, *, |)
     * - Open brackets/parentheses
     */
    private String readStatement() throws IOException {
        StringBuilder statement = new StringBuilder();
        boolean firstLine = true;

        while (true) {
            out.print(firstLine ? PROMPT : CONTINUATION_PROMPT);
            out.flush();

            String line = reader.readLine();
            if (line == null) {
                return statement.length() > 0 ? statement.toString() : null;
            }

            if (firstLine && line.trim().isEmpty()) {
                return "";
            }

            statement.append(line);

            // Check if statement continues on next line
            String trimmed = line.trim();
            if (needsContinuation(trimmed, statement.toString())) {
                statement.append("\n");
                firstLine = false;
            } else {
                break;
            }
        }

        return statement.toString();
    }

    /**
     * Determines if the current input needs continuation.
     */
    private boolean needsContinuation(String currentLine, String fullStatement) {
        // Lines ending with these characters expect a handler on the next line
        if (currentLine.endsWith("!") || currentLine.endsWith("*") || currentLine.endsWith("|")) {
            return true;
        }

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
     * Handles special console commands starting with ':'.
     */
    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case ":help":
            case ":h":
            case ":?":
                printHelp();
                break;

            case ":vars":
            case ":variables":
                printVariables();
                break;

            case ":clear":
                clearVariables();
                break;

            case ":load":
                if (arg == null || arg.isBlank()) {
                    err.println("Usage: :load <filename>");
                } else {
                    loadScript(arg);
                }
                break;

            case ":man":
                if (arg == null || arg.isBlank()) {
                    out.println(expressionContext.man());
                } else {
                    printManual(arg);
                }
                break;

            case ":syntax":
                printSyntax();
                break;

            case ":exit":
            case ":quit":
            case ":q":
                running = false;
                break;

            default:
                err.println("Unknown command: " + command);
                err.println("Type :help for available commands.");
        }
    }

    /**
     * Executes a script statement and displays the result.
     */
    private void executeStatement(String statement) {
        statementCount++;

        try {
            ScriptContext script = new ScriptContext(expressionContext, injectionContext, bootstrap);
            script.load(statement);
            script.compile();

            int exitCode = script.execute();

            // Collect any new variables
            collectVariables(script, statement);

            // Show result if there's a meaningful exit code
            if (exitCode != 0) {
                out.println("-> " + exitCode);
            }

        } catch (ScriptException e) {
            err.println("Error: " + e.getMessage());
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                err.println("  Caused by: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            err.println("Unexpected error: " + e.getMessage());
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
                if (isValidVariableName(varName)) {
                    Optional<?> value = script.getVariable(varName, Object.class);
                    if (value.isPresent()) {
                        sessionVariables.put(varName, value.get());
                        out.println(varName + " = " + formatValue(value.get()));
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
                            out.println(varName + " = <expression>");
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

    private void printHelp() {
        out.println("Console Commands:");
        out.println("  :help, :h, :?     Show this help message");
        out.println("  :vars             List all session variables");
        out.println("  :clear            Clear all session variables");
        out.println("  :load <file>      Load and execute a script file");
        out.println("  :man [function]   Show function documentation");
        out.println("  :syntax           Show script syntax reference");
        out.println("  :exit, :quit, :q  Exit the console");
        out.println();
        out.println("Script Statements:");
        out.println("  expression                Execute an expression");
        out.println("  varName <- expression     Store result in variable");
        out.println("  varName = expression      Store expression (lazy)");
        out.println("  expression -> exitCode    Set exit code");
        out.println();
        out.println("Multi-line Input:");
        out.println("  Lines ending with !, *, or | continue on the next line.");
        out.println();
    }

    private void printVariables() {
        if (sessionVariables.isEmpty()) {
            out.println("No variables defined.");
            return;
        }

        out.println("Session Variables:");
        for (Map.Entry<String, Object> entry : sessionVariables.entrySet()) {
            Object value = entry.getValue();
            String typeName = value != null ? value.getClass().getSimpleName() : "null";
            out.println("  " + entry.getKey() + " : " + typeName + " = " + formatValue(value));
        }
    }

    private void clearVariables() {
        sessionVariables.clear();
        statementCount = 0;
        out.println("Variables cleared.");
    }

    private void loadScript(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            err.println("File not found: " + filename);
            return;
        }

        try {
            String content = Files.readString(file.toPath());

            // Strip shebang if present
            if (content.startsWith("#!")) {
                int newlineIndex = content.indexOf('\n');
                if (newlineIndex >= 0) {
                    content = content.substring(newlineIndex + 1);
                } else {
                    content = "";
                }
            }

            out.println("Loading: " + filename);

            ScriptContext script = new ScriptContext(expressionContext, injectionContext, bootstrap);
            script.load(content);
            script.compile();

            int exitCode = script.execute();
            out.println("Script completed with exit code: " + exitCode);

        } catch (IOException e) {
            err.println("Error reading file: " + e.getMessage());
        } catch (ScriptException e) {
            err.println("Script error: " + e.getMessage());
            if (e.getCause() != null) {
                err.println("  Caused by: " + e.getCause().getMessage());
            }
        }
    }

    private void printManual(String functionNameOrIndex) {
        String manual = null;

        try {
            int index = Integer.parseInt(functionNameOrIndex);
            manual = expressionContext.man(index);
        } catch (NumberFormatException e) {
            manual = expressionContext.man(functionNameOrIndex);
        }

        if (manual == null) {
            err.println("No documentation found for: " + functionNameOrIndex);
            err.println("Use :man to list all available functions.");
        } else {
            out.println(manual);
        }
    }

    private void printSyntax() {
        out.println("Quick Syntax Reference:");
        out.println();
        out.println("  Statements:");
        out.println("    expression              Execute expression");
        out.println("    var <- expression       Store result");
        out.println("    var = expression        Store expression (lazy)");
        out.println("    expression -> code      Set exit code");
        out.println();
        out.println("  Expressions:");
        out.println("    \"string\", 123, true    Literals");
        out.println("    @varName                Variable reference");
        out.println("    function(args)          Function call");
        out.println("    :method(target, args)   Method call");
        out.println("    :(Class.class, args)    Constructor call");
        out.println();
        out.println("  Exception Handling:");
        out.println("    ! Type => handler       Catch locally");
        out.println("    * Type => handler       Catch downstream");
        out.println();
        out.println("Use --syntax from command line for full reference.");
    }
}
