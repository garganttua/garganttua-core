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

        ScriptConsole console = createConsole(":exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Garganttua Script Console"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":help\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Console Commands:"));
        assertTrue(output.contains(":vars"));
        assertTrue(output.contains(":clear"));
        assertTrue(output.contains(":load"));
        assertTrue(output.contains(":exit"));
    }

    @Test
    void testVarsCommandEmpty() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":vars\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("No variables defined"));
    }

    @Test
    void testSyntaxCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":syntax\n:exit\n", out, err);
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
        ScriptConsole console = createConsole("\"hello\"\n:exit\n", out, err);
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

        ScriptConsole console = createConsole("x <- \"test\"\n:vars\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("x = \"test\"") || output.contains("x :"));
    }

    @Test
    void testClearCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("x <- \"test\"\n:clear\n:vars\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Variables cleared"));
        assertTrue(output.contains("No variables defined"));
    }

    @Test
    void testUnknownCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":unknown\n:exit\n", out, err);
        console.start();

        String errOutput = err.toString();
        assertTrue(errOutput.contains("Unknown command"));
    }

    @Test
    void testExitCodeDisplay() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole("\"hello\" -> 42\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("-> 42"));
    }

    @Test
    void testQuitAlias() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":quit\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void testShortQuitAlias() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":q\n", out, err);
        console.start();

        String output = out.toString();
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void testErrorHandling() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Invalid syntax should produce error but not crash
        ScriptConsole console = createConsole("invalid!!syntax\n:exit\n", out, err);
        console.start();

        String errOutput = err.toString();
        assertTrue(errOutput.contains("Error") || errOutput.length() > 0);

        String output = out.toString();
        assertTrue(output.contains("Goodbye!")); // Console should still exit gracefully
    }

    @Test
    void testManCommand() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        ScriptConsole console = createConsole(":man\n:exit\n", out, err);
        console.start();

        String output = out.toString();
        // Should list functions
        assertTrue(output.contains("print") || output.length() > 500);
    }
}
