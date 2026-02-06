package com.garganttua.core.script.console;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Map;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.script.console.ConsoleExecutionContext.ConsoleContext;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Expression functions for console commands.
 *
 * <p>These functions replace the colon-prefixed commands (e.g., :help)
 * with regular expression calls (e.g., help()).</p>
 */
@Slf4j
public class ConsoleFunctions {

    private ConsoleFunctions() {
    }

    /**
     * Gets the output stream from the context, or System.out if not available.
     */
    private static PrintStream getOut() {
        ConsoleContext ctx = ConsoleExecutionContext.get();
        return ctx != null ? ctx.getOut() : System.out;
    }

    /**
     * Displays the help message with available commands.
     *
     * @return the help message
     */
    @Expression(name = "help", description = "Shows the console help message")
    public static String help() {
        log.atTrace().log("help()");

        StringBuilder sb = new StringBuilder();
        sb.append("Console Commands:\n");
        sb.append("  help()            Show this help message\n");
        sb.append("  vars()            List all session variables\n");
        sb.append("  clear()           Clear all session variables\n");
        sb.append("  load(\"file\")      Load and execute a script file\n");
        sb.append("  man()             List all expression functions\n");
        sb.append("  man(\"function\")   Show documentation for a function\n");
        sb.append("  man(index)        Show documentation by index\n");
        sb.append("  syntax()          Show script syntax reference\n");
        sb.append("  exit()            Exit the console\n");
        sb.append("  quit()            Exit the console (alias)\n");
        sb.append("\n");
        sb.append("Script Statements:\n");
        sb.append("  expression                Execute an expression\n");
        sb.append("  varName <- expression     Store result in variable\n");
        sb.append("  varName = expression      Store expression (lazy)\n");
        sb.append("  expression -> exitCode    Set exit code\n");
        sb.append("\n");
        sb.append("Reserved Variables:\n");
        sb.append("  @_                        Last expression result\n");
        sb.append("  @$0, @$1, @$2, ...        Script arguments\n");
        sb.append("  @code                     Current exit code (read/write)\n");
        sb.append("  @output                   Script output value (read/write)\n");
        sb.append("  @exception                Exception object (in catch handler)\n");
        sb.append("  @message                  Exception message (in catch handler)\n");
        sb.append("\n");
        sb.append("Multi-line Input:\n");
        sb.append("  Lines ending with .. continue on the next line.\n");
        sb.append("  Unclosed brackets/parentheses also trigger continuation.\n");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx != null) {
            displayPaginated(sb.toString(), ctx);
        } else {
            getOut().println(sb.toString());
        }
        return ""; // Already printed, don't display again
    }

    /**
     * Lists all session variables.
     *
     * @return a formatted string of all variables
     */
    @Expression(name = "vars", description = "Lists all session variables")
    public static String vars() {
        log.atTrace().log("vars()");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("vars: not in console context");
        }

        Map<String, Object> variables = ctx.getSessionVariables();
        if (variables.isEmpty()) {
            ctx.getOut().println("No variables defined.");
            return ""; // Already printed
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Session Variables:\n");
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            String typeName = value != null ? value.getClass().getSimpleName() : "null";
            String valueStr = formatValue(value);
            sb.append("  ").append(entry.getKey())
              .append(" : ").append(typeName)
              .append(" = ").append(valueStr).append("\n");
        }

        ctx.getOut().print(sb.toString());
        return ""; // Already printed
    }

    /**
     * Clears all session variables.
     *
     * @return confirmation message
     */
    @Expression(name = "clear", description = "Clears all session variables")
    public static String clear() {
        log.atTrace().log("clear()");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("clear: not in console context");
        }

        ctx.getSessionVariables().clear();
        ctx.getOut().println("Variables cleared.");
        return ""; // Already printed
    }

    /**
     * Loads and executes a script file.
     *
     * @param filename the script file to load
     * @return the exit code of the executed script
     */
    @Expression(name = "load", description = "Loads and executes a script file")
    public static int load(@Nullable String filename) {
        log.atDebug().log("load({})", filename);

        if (filename == null || filename.isBlank()) {
            throw new ExpressionException("load: filename cannot be null or blank");
        }

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("load: not in console context");
        }

        File file = new File(filename);
        if (!file.exists()) {
            throw new ExpressionException("load: file not found: " + filename);
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

            ctx.getOut().println("Loading: " + filename);

            // For now, we throw an error indicating this needs proper setup
            throw new ExpressionException("load: not yet implemented in expression mode. " +
                    "Use the script executor directly or include() for JAR files.");

        } catch (IOException e) {
            throw new ExpressionException("load: error reading file: " + e.getMessage());
        }
    }

    /**
     * Lists all available expression functions with paginated display.
     *
     * @return the manual listing
     */
    @Expression(name = "man", description = "Lists all expression functions (no args)")
    public static String manList() {
        log.atTrace().log("man()");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("man: not in console context");
        }

        IExpressionContext expressionContext = ctx.getExpressionContext();
        String content = expressionContext.man();
        displayPaginated(content, ctx);
        return ""; // Already printed
    }

    /**
     * Shows documentation for a specific function by name with paginated display.
     *
     * @param functionName the function name
     * @return the manual entry
     */
    @Expression(name = "man", description = "Shows documentation for a function by name")
    public static String manByName(@Nullable String functionName) {
        log.atDebug().log("man({})", functionName);

        if (functionName == null || functionName.isBlank()) {
            return manList();
        }

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("man: not in console context");
        }

        IExpressionContext expressionContext = ctx.getExpressionContext();
        String result = expressionContext.man(functionName);

        if (result == null) {
            throw new ExpressionException("man: no documentation found for: " + functionName);
        }

        displayPaginated(result, ctx);
        return ""; // Already printed
    }

    /**
     * Shows documentation for a specific function by index with paginated display.
     *
     * @param index the function index
     * @return the manual entry
     */
    @Expression(name = "man", description = "Shows documentation for a function by index")
    public static String manByIndex(int index) {
        log.atDebug().log("man({})", index);

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("man: not in console context");
        }

        IExpressionContext expressionContext = ctx.getExpressionContext();
        String result = expressionContext.man(index);

        if (result == null) {
            throw new ExpressionException("man: no documentation found for index: " + index);
        }

        displayPaginated(result, ctx);
        return ""; // Already printed
    }

    /**
     * Shows the script syntax reference.
     *
     * @return the syntax reference
     */
    @Expression(name = "syntax", description = "Shows the script syntax reference")
    public static String syntax() {
        log.atTrace().log("syntax()");

        StringBuilder sb = new StringBuilder();
        sb.append("Quick Syntax Reference:\n");
        sb.append("\n");
        sb.append("  Statements:\n");
        sb.append("    expression              Execute expression\n");
        sb.append("    var <- expression       Store result\n");
        sb.append("    var = expression        Store expression (lazy)\n");
        sb.append("    expression -> code      Set exit code\n");
        sb.append("\n");
        sb.append("  Expressions:\n");
        sb.append("    \"string\", 123, true    Literals\n");
        sb.append("    @varName                Variable reference (lazy)\n");
        sb.append("    .varName                Eager evaluation (eval stored expr)\n");
        sb.append("    function(args)          Function call\n");
        sb.append("    :method(target, args)   Method call\n");
        sb.append("    :(Class.class, args)    Constructor call\n");
        sb.append("\n");
        sb.append("  Reserved Variables:\n");
        sb.append("    @_                      Last expression result\n");
        sb.append("    @$0, @$1, @$2, ...      Script arguments (positional)\n");
        sb.append("    @code                   Current exit code\n");
        sb.append("    @output                 Script output value\n");
        sb.append("    @exception              Exception object (in catch)\n");
        sb.append("    @message                Exception message (in catch)\n");
        sb.append("\n");
        sb.append("  Exception Handling:\n");
        sb.append("    ! Type => handler       Catch locally\n");
        sb.append("    * Type => handler       Catch downstream\n");
        sb.append("\n");
        sb.append("  Pipes (conditional branching):\n");
        sb.append("    | condition => handler  Execute if condition true\n");
        sb.append("    | => handler            Default case (else)\n");
        sb.append("\n");
        sb.append("  Lazy vs Eager Evaluation:\n");
        sb.append("    expr = print(\"hello\")   Store expression (not executed)\n");
        sb.append("    .expr                   Force evaluation of stored expr\n");
        sb.append("    func(.expr)             Evaluate expr, pass result to func\n");
        sb.append("    func(@expr)             Pass expr as-is (func can eval it)\n");
        sb.append("    time(@expr)             Measure execution time of expr\n");
        sb.append("\n");
        sb.append("Use man() to list all available expression functions.\n");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx != null) {
            displayPaginated(sb.toString(), ctx);
        } else {
            getOut().println(sb.toString());
        }
        return ""; // Already printed
    }

    /**
     * Exits the console.
     *
     * @return exit message
     */
    @Expression(name = "exit", description = "Exits the console")
    public static String exit() {
        log.atTrace().log("exit()");

        ConsoleContext ctx = ConsoleExecutionContext.get();
        if (ctx == null) {
            throw new ExpressionException("exit: not in console context");
        }

        ctx.requestExit();
        return "Exiting...";
    }

    /**
     * Exits the console (alias for exit()).
     *
     * @return exit message
     */
    @Expression(name = "quit", description = "Exits the console (alias for exit)")
    public static String quit() {
        return exit();
    }

    /**
     * Formats a value for display.
     */
    private static String formatValue(Object value) {
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

    // ANSI Color Codes for pagination
    private static final String RESET = "\u001B[0m";
    private static final String DIM = "\u001B[2m";
    private static final String BRIGHT_BLACK = "\u001B[90m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";

    /**
     * Displays content with pagination support.
     *
     * <p>If the content exceeds the terminal height, it will be displayed
     * page by page, waiting for user input to continue.</p>
     *
     * @param content the content to display
     * @param ctx the console context
     */
    private static void displayPaginated(String content, ConsoleContext ctx) {
        PrintStream out = ctx.getOut();
        Terminal terminal = ctx.getTerminal();
        LineReader lineReader = ctx.getLineReader();

        String[] lines = content.split("\n", -1);

        // If no terminal or lineReader, just print everything
        if (terminal == null || lineReader == null) {
            out.println(content);
            return;
        }

        int terminalHeight = ctx.getTerminalHeight();
        // Reserve 2 lines for the prompt at the bottom
        int pageSize = Math.max(terminalHeight - 2, 10);

        // If content fits in one screen, just print it
        if (lines.length <= pageSize) {
            out.println(content);
            return;
        }

        int currentLine = 0;
        int totalLines = lines.length;
        boolean colorsEnabled = ctx.isColorsEnabled();

        while (currentLine < totalLines) {
            // Print a page of lines
            int endLine = Math.min(currentLine + pageSize, totalLines);
            for (int i = currentLine; i < endLine; i++) {
                out.println(lines[i]);
            }

            currentLine = endLine;

            // If there's more content, show pagination prompt
            if (currentLine < totalLines) {
                int linesRemaining = totalLines - currentLine;
                int percent = (currentLine * 100) / totalLines;

                String prompt;
                if (colorsEnabled) {
                    prompt = BRIGHT_BLACK + "── " + RESET +
                             BRIGHT_CYAN + percent + "%" + RESET +
                             BRIGHT_BLACK + " ── " + BRIGHT_YELLOW + linesRemaining + RESET +
                             BRIGHT_BLACK + " lines remaining ── [" + RESET +
                             "Enter" + BRIGHT_BLACK + ": next, " + RESET +
                             "q" + BRIGHT_BLACK + ": quit] ──" + RESET;
                } else {
                    prompt = "── " + percent + "% ── " + linesRemaining + " lines remaining ── [Enter: next, q: quit] ──";
                }

                out.print(prompt);
                out.flush();

                try {
                    // Read single character or line
                    String input = lineReader.readLine("");
                    if (input != null) {
                        input = input.trim().toLowerCase();
                        if (input.equals("q") || input.equals("quit") || input.equals("exit")) {
                            // Exit pagination
                            break;
                        }
                    }
                } catch (UserInterruptException e) {
                    // Ctrl+C pressed - exit pagination
                    out.println();
                    break;
                } catch (EndOfFileException e) {
                    // Ctrl+D pressed - exit pagination
                    out.println();
                    break;
                }
            }
        }
    }
}
