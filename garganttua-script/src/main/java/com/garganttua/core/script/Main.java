package com.garganttua.core.script;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

public class Main {

    private static final String VERSION = "2.0.0-ALPHA01";
    private static final String SHEBANG_PREFIX = "#!";

    public static void main(String[] args) {
        // No arguments: start interactive console
        if (args.length == 0) {
            startConsole();
            return;
        }

        String firstArg = args[0];

        if ("--help".equals(firstArg) || "-h".equals(firstArg)) {
            printUsage();
            System.exit(0);
        }

        if ("--version".equals(firstArg) || "-v".equals(firstArg)) {
            System.out.println("garganttua-script " + VERSION);
            System.exit(0);
        }

        if ("--console".equals(firstArg) || "-c".equals(firstArg)) {
            startConsole();
            return;
        }

        if ("--syntax".equals(firstArg) || "-s".equals(firstArg)) {
            printSyntax();
            System.exit(0);
        }

        if ("--man".equals(firstArg) || "-m".equals(firstArg)) {
            try {
                if (args.length > 1) {
                    // Specific function manual
                    printManual(args[1]);
                } else {
                    // List all functions
                    printManualList();
                }
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Error loading manual: " + e.getMessage());
                System.exit(1);
            }
        }

        // Check for --dump flag
        boolean dumpOnError = false;
        String[] filteredArgs = args;
        for (int i = 0; i < args.length; i++) {
            if ("--dump".equals(args[i]) || "-d".equals(args[i])) {
                dumpOnError = true;
                // Remove --dump from args
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 0, newArgs, 0, i);
                System.arraycopy(args, i + 1, newArgs, i, args.length - i - 1);
                filteredArgs = newArgs;
                break;
            }
        }

        if (filteredArgs.length == 0) {
            printUsage();
            System.exit(1);
        }

        File scriptFile = new File(filteredArgs[0]);
        if (!scriptFile.exists()) {
            System.err.println("Error: Script file not found: " + filteredArgs[0]);
            System.exit(1);
        }

        String[] scriptArgs = filteredArgs.length > 1
                ? Arrays.copyOfRange(filteredArgs, 1, filteredArgs.length)
                : new String[0];

        try {
            int exitCode = executeScript(scriptFile, scriptArgs, dumpOnError);
            System.exit(exitCode);
        } catch (ScriptException e) {
            System.err.println("Script error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static IReflectionProvider loadReflectionProvider() {
        try {
            Class<?> providerClass = Class.forName("com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
            return (IReflectionProvider) providerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RuntimeReflectionProvider. "
                    + "Ensure garganttua-runtime-reflection is on the classpath.", e);
        }
    }

    private static int executeScript(File scriptFile, String[] args, boolean dumpOnError)
            throws ScriptException, IOException {
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(loadReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());

        // Build injection context
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        // Build expression context with dependency on injection context
        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        // Build injection context first
        IInjectionContext injectionContext = injectionContextBuilder.build();

        // Initialize lifecycle BEFORE building expression context
        injectionContext.onInit().onStart();

        // Now build expression context
        IExpressionContext expressionContext = expressionContextBuilder.build();

        ScriptContext script = new ScriptContext(expressionContext, injectionContextBuilder, null);

        String scriptContent = readScriptFile(scriptFile);
        script.load(scriptContent);
        script.compile();

        int exitCode = script.execute((Object[]) args);

        if (script.hasAborted() && dumpOnError) {
            printErrorDump(System.err, script, scriptFile, scriptContent, args);
        }

        return exitCode;
    }

    private static void printErrorDump(PrintStream out, ScriptContext script,
                                        File scriptFile, String scriptContent, String[] args) {
        out.println();
        out.println("╔══════════════════════════════════════════════════════════════════════╗");
        out.println("║  SCRIPT ERROR DUMP                                                  ║");
        out.println("╠══════════════════════════════════════════════════════════════════════╣");
        out.println("║  File: " + scriptFile.getAbsolutePath());

        // Exception chain
        script.getLastException().ifPresent(ex -> {
            out.println("║");
            out.println("║  Exception chain:");
            Throwable t = ex;
            int depth = 0;
            while (t != null && depth < 10) {
                out.println("║    " + "  ".repeat(depth) + t.getClass().getSimpleName() + ": " + t.getMessage());
                t = t.getCause();
                depth++;
            }
        });

        // Error context variables
        out.println("║");
        out.println("║  Error context:");
        script.getVariable("_scriptErrorLine", com.garganttua.core.reflection.IClass.getClass(Object.class))
                .ifPresent(v -> out.println("║    Line: " + v));
        script.getVariable("_scriptErrorSource", com.garganttua.core.reflection.IClass.getClass(Object.class))
                .ifPresent(v -> out.println("║    Source: " + v));
        script.getVariable("_scriptErrorStep", com.garganttua.core.reflection.IClass.getClass(Object.class))
                .ifPresent(v -> out.println("║    Step: " + v));
        script.getVariable("_scriptErrorType", com.garganttua.core.reflection.IClass.getClass(Object.class))
                .ifPresent(v -> out.println("║    Type: " + v));
        script.getVariable("_scriptErrorMessage", com.garganttua.core.reflection.IClass.getClass(Object.class))
                .ifPresent(v -> out.println("║    Message: " + v));

        // Arguments
        if (args != null && args.length > 0) {
            out.println("║");
            out.println("║  Arguments:");
            for (int i = 0; i < args.length; i++) {
                out.println("║    @" + i + " = " + args[i]);
            }
        }

        // Variables at failure point
        out.println("║");
        out.println("║  Variables:");
        Map<String, Object> vars = script.getAllVariables();
        if (vars.isEmpty()) {
            out.println("║    (none)");
        } else {
            for (var entry : vars.entrySet()) {
                if (entry.getKey().startsWith("_scriptError")) continue;
                String val = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (val.length() > 120) val = val.substring(0, 120) + "...";
                out.println("║    " + entry.getKey() + " = " + val);
            }
        }

        // Script source with line numbers
        out.println("║");
        out.println("║  Script source:");
        String[] lines = scriptContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            out.printf("║    %3d │ %s%n", i + 1, lines[i]);
        }

        out.println("╚══════════════════════════════════════════════════════════════════════╝");
    }

    private static String readScriptFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        return stripShebang(content);
    }

    private static String stripShebang(String content) {
        if (content.startsWith(SHEBANG_PREFIX)) {
            int newlineIndex = content.indexOf('\n');
            if (newlineIndex >= 0) {
                return content.substring(newlineIndex + 1);
            }
            return "";
        }
        return content;
    }

    private static void startConsole() {
        try {
            Class<?> consoleClass = Class.forName("com.garganttua.core.console.ScriptConsole");
            Object console = consoleClass.getDeclaredConstructor().newInstance();
            consoleClass.getMethod("start").invoke(console);
        } catch (ClassNotFoundException e) {
            System.err.println("Console module not available.");
            System.err.println("Install garganttua-console or use: gs <script.gs>");
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error starting console: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Garganttua Script Engine " + VERSION);
        System.out.println();
        System.out.println("Usage: garganttua-script                            Start interactive console (requires garganttua-console)");
        System.out.println("       garganttua-script [--dump] <script.gs> [args] Execute a script file");
        System.out.println("       garganttua-script [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -c, --console      Start interactive console (REPL, requires garganttua-console)");
        System.out.println("  -d, --dump         Print error dump on crash (variables, context, source)");
        System.out.println("  -h, --help         Show this help message");
        System.out.println("  -v, --version      Show version information");
        System.out.println("  -m, --man          List all available expression functions");
        System.out.println("  -m, --man <name>   Show documentation for a specific function");
        System.out.println("  -s, --syntax       Show script syntax reference");
        System.out.println();
        System.out.println("Interactive Console:");
        System.out.println("  When started without arguments, the console allows you to");
        System.out.println("  enter script statements interactively. Type :help for commands.");
        System.out.println();
        System.out.println("Script Files:");
        System.out.println("  Scripts can start with a shebang line:");
        System.out.println("    #!/usr/bin/env garganttua-script");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  garganttua-script                     # Start console");
        System.out.println("  garganttua-script myscript.gs         # Run script");
        System.out.println("  garganttua-script script.gs arg1 arg2 # Run with arguments");
    }

    private static IExpressionContext buildExpressionContext() {
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(loadReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());

        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder
                .withPackage("com.garganttua")
                .autoDetect(true)
                .provide(injectionContextBuilder);

        IInjectionContext injectionContext = injectionContextBuilder.build();
        injectionContext.onInit().onStart();

        return expressionContextBuilder.build();
    }

    private static void printManualList() {
        IExpressionContext expressionContext = buildExpressionContext();
        System.out.println(expressionContext.man());
    }

    private static void printManual(String functionNameOrIndex) {
        IExpressionContext expressionContext = buildExpressionContext();

        String manual = null;

        // Try parsing as index first
        try {
            int index = Integer.parseInt(functionNameOrIndex);
            manual = expressionContext.man(index);
        } catch (NumberFormatException e) {
            // Not an index, try as function name
            manual = expressionContext.man(functionNameOrIndex);
        }

        if (manual == null) {
            System.err.println("No documentation found for: " + functionNameOrIndex);
            System.err.println();
            System.err.println("Use --man to list all available functions.");
            System.exit(1);
        }

        System.out.println(manual);
    }

    private static void printSyntax() {
        System.out.println("GARGANTTUA SCRIPT SYNTAX REFERENCE");
        System.out.println("===================================");
        System.out.println();
        System.out.println("1. BASIC STATEMENTS");
        System.out.println("-------------------");
        System.out.println();
        System.out.println("  Expression statement:");
        System.out.println("    expression");
        System.out.println();
        System.out.println("  Expression with exit code:");
        System.out.println("    expression -> exitCode");
        System.out.println();
        System.out.println("  Variable assignment (stores result):");
        System.out.println("    varName <- expression");
        System.out.println("    varName <- expression -> exitCode");
        System.out.println();
        System.out.println("  Expression assignment (stores the expression itself):");
        System.out.println("    varName = expression");
        System.out.println();
        System.out.println("2. EXPRESSIONS");
        System.out.println("--------------");
        System.out.println();
        System.out.println("  Literals:");
        System.out.println("    \"string\"          String literal");
        System.out.println("    'c'               Character literal");
        System.out.println("    123               Integer literal");
        System.out.println("    12.34             Float literal");
        System.out.println("    true, false       Boolean literals");
        System.out.println("    null              Null value");
        System.out.println();
        System.out.println("  Variable reference:");
        System.out.println("    @varName          Reference to a script variable");
        System.out.println();
        System.out.println("  Script arguments:");
        System.out.println("    @0, @1, @2...     Command-line arguments (0-indexed)");
        System.out.println();
        System.out.println("  Function call:");
        System.out.println("    functionName(arg1, arg2, ...)");
        System.out.println();
        System.out.println("  Method call:");
        System.out.println("    :methodName(target, arg1, arg2, ...)");
        System.out.println();
        System.out.println("  Constructor call:");
        System.out.println("    :(ClassName.class, arg1, arg2, ...)");
        System.out.println();
        System.out.println("  Types:");
        System.out.println("    int, long, double, boolean, char, byte, short, float");
        System.out.println("    String.class, java.util.List.class, etc.");
        System.out.println();
        System.out.println("3. EXCEPTION HANDLING");
        System.out.println("---------------------");
        System.out.println();
        System.out.println("  Catch clause (handles exceptions from current step):");
        System.out.println("    expression");
        System.out.println("    ! => handler");
        System.out.println("    ! ExceptionType.class => handler");
        System.out.println("    ! Type1.class, Type2.class => handler");
        System.out.println();
        System.out.println("  Downstream catch (handles exceptions from subsequent steps):");
        System.out.println("    expression");
        System.out.println("    * => handler");
        System.out.println("    * ExceptionType.class => handler");
        System.out.println();
        System.out.println("4. CONDITIONAL PIPES");
        System.out.println("--------------------");
        System.out.println();
        System.out.println("  Pipe clauses (must be on separate lines):");
        System.out.println("    expression -> 100");
        System.out.println("    | condition => handler -> 200");
        System.out.println("    | => defaultHandler -> 300");
        System.out.println();
        System.out.println("  Example with equals:");
        System.out.println("    print(@0) -> 100");
        System.out.println("    | equals(@0, \"yes\") => doSomething() -> 200");
        System.out.println("    | equals(@0, \"no\") => doOther() -> 201");
        System.out.println("    | => print(\"unknown\") -> 202");
        System.out.println();
        System.out.println("5. COMMENTS");
        System.out.println("-----------");
        System.out.println();
        System.out.println("  // Single-line comment");
        System.out.println("  # Hash comment (also used for shebang)");
        System.out.println("  /* Block comment */");
        System.out.println();
        System.out.println("6. COMPLETE EXAMPLE");
        System.out.println("-------------------");
        System.out.println();
        System.out.println("  #!/usr/bin/env garganttua-script");
        System.out.println("  ");
        System.out.println("  // Variable assignment");
        System.out.println("  greeting <- \"Hello\"");
        System.out.println("  ");
        System.out.println("  // Print with variable and arguments");
        System.out.println("  print(concatenate(@greeting, \" \", @0)) -> 100");
        System.out.println("  ");
        System.out.println("  // Conditional logic");
        System.out.println("  | equals(@0, \"world\") => print(\"Perfect!\") -> 200");
        System.out.println("  | => print(\"Try: ./script.gs world\") -> 201");
        System.out.println();
        System.out.println("Use --man to list all available expression functions.");
    }
}
