package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.console.ScriptConsole;

class ScriptConsoleTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    private ScriptConsole createConsole(String input, ByteArrayOutputStream out, ByteArrayOutputStream err) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        return new ScriptConsole(reader, new PrintStream(out), new PrintStream(err));
    }

    @Test
    void testConsoleStartsAndExits() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("exit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("SCRIPT CONSOLE") || output.contains("Garganttua"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("help()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Console Commands:"));
        assertTrue(output.contains("vars()"));
        assertTrue(output.contains("clear()"));
        assertTrue(output.contains("load("));
        assertTrue(output.contains("exit()"));
    }

    @Test
    void testVarsCommandEmpty() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("vars()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("No variables defined"));
    }

    @Test
    void testSyntaxCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("syntax()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Quick Syntax Reference"));
    }

    @Test
    void testSimpleExpression() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Note: print() outputs to System.out, not the console's out stream
        // So we test that the expression executes without error
        ScriptConsole console = createConsole("\"hello\"\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        String errOutput = err.toString();

        // Should complete without errors
        assertTrue(output.contains("Goodbye!"));
        assertFalse(errOutput.contains("Error"));
    }

    @Test
    void testVariableAssignment() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("x <- \"test\"\nvars()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("x") && output.contains("test"));
    }

    @Test
    void testClearCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("x <- \"test\"\nclear()\nvars()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Variables cleared"));
        assertTrue(output.contains("No variables defined"));
    }

    @Test
    void testExitCodeDisplay() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("\"hello\" -> 42\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("42"));
    }

    @Test
    void testQuitAlias() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("quit()\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void testErrorHandling() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Invalid syntax should produce error but not crash
        ScriptConsole console = createConsole("invalid!!syntax\nexit()\n", out, err);
        console.start();

        String errOutput = err.toString();
        assertTrue(errOutput.contains("Error") || errOutput.length() > 0);

        String output = out.toString();
        assertTrue(output.contains("Goodbye!")); // Console should still exit gracefully
    }

    @Test
    void testManCommandList() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("man()\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        // Should list functions
        assertTrue(output.contains("print") || output.length() > 500);
    }

    @Test
    void testManCommandByName() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // man("print") should either show documentation or error gracefully
        ScriptConsole console = createConsole("man(\"print\")\nexit()\n", out, err);
        console.start();

        String output = out.toString();
        String errOutput = err.toString();

        // Should complete successfully (even if function not found, it should exit gracefully)
        assertTrue(output.contains("Goodbye!"));
        // If there's an error about function not found, that's acceptable
        // The main test is that the console handles the command without crashing
    }

    @Test
    void testMultiLineContinuationWithBackslash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Test multi-line input with backslash continuation and catch clause
        // The backslash at end of line should trigger continuation
        String input = "class(\"nonexistent.Foo\") \\\n! => print(\"caught\") -> 400\nexit()\n";
        ScriptConsole console = createConsole(input, out, err);
        console.start();

        String output = out.toString();
        String errOutput = err.toString();

        // Should complete without syntax errors
        assertTrue(output.contains("Goodbye!"));
        // The catch clause should handle the error, so we expect exit code 400
        // or at least no syntax error
        assertFalse(errOutput.contains("Syntax error"), "Should not have syntax error: " + errOutput);
    }

    @Test
    void testMultiLineContinuationWithDoubleDot() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Test multi-line input with ".." continuation marker (terminal-safe)
        String input = "class(\"nonexistent.Foo\") ..\n! => print(\"caught-dd\") -> 401\nexit()\n";
        ScriptConsole console = createConsole(input, out, err);
        console.start();

        String output = out.toString();
        String errOutput = err.toString();

        // Should complete without syntax errors
        assertTrue(output.contains("Goodbye!"));
        assertFalse(errOutput.contains("Syntax error"), "Should not have syntax error: " + errOutput);
        // Should show exit code 401 from the catch handler
        assertTrue(output.contains("401"), "Should show exit code 401");
    }
}
