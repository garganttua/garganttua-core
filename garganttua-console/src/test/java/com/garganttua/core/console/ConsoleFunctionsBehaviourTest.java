package com.garganttua.core.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.console.ConsoleExecutionContext.ConsoleContext;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.context.IExpressionNodeFactory;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;

/**
 * Behaviour tests for {@link ConsoleFunctions}. Exercises the no-context error
 * paths, the pure-string help/syntax builders, the session-variable functions
 * (vars/clear), load() validation/error paths, exit/quit signalling, and the
 * man() delegation to the expression context via a recording stub.
 */
class ConsoleFunctionsBehaviourTest {

    @AfterEach
    void tearDown() {
        ConsoleExecutionContext.clear();
    }

    private RecordingExpressionContext ec;

    private ConsoleContext newContext(Map<String, Object> vars, ByteArrayOutputStream out) {
        ec = new RecordingExpressionContext();
        return new ConsoleContext(vars, ec, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    private void bind(ConsoleContext ctx) {
        ConsoleExecutionContext.set(ctx);
    }

    // ---- no-context error paths ----

    @Test
    void vars_withoutContext_throwsExpressionException() {
        ConsoleExecutionContext.clear();
        ExpressionException ex = assertThrows(ExpressionException.class, ConsoleFunctions::vars);
        assertTrue(ex.getMessage().contains("not in console context"));
    }

    @Test
    void clear_withoutContext_throwsExpressionException() {
        ConsoleExecutionContext.clear();
        assertThrows(ExpressionException.class, ConsoleFunctions::clear);
    }

    @Test
    void exit_withoutContext_throwsExpressionException() {
        ConsoleExecutionContext.clear();
        assertThrows(ExpressionException.class, ConsoleFunctions::exit);
    }

    @Test
    void manList_withoutContext_throwsExpressionException() {
        ConsoleExecutionContext.clear();
        assertThrows(ExpressionException.class, ConsoleFunctions::manList);
    }

    // ---- help / syntax pure-string builders (no context needed) ----

    @Test
    void help_withoutContext_returnsEmptyButPrintsCommandList() {
        ConsoleExecutionContext.clear();
        // Capture System.out since help() falls back to getOut()=System.out without a context.
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            String returned = ConsoleFunctions.help();
            assertEquals("", returned);
        } finally {
            System.setOut(original);
        }
        String printed = buf.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Console Commands:"));
        assertTrue(printed.contains("vars()"));
        assertTrue(printed.contains("Multi-line Input:"));
    }

    @Test
    void syntax_withoutContext_returnsEmptyButPrintsReference() {
        ConsoleExecutionContext.clear();
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            assertEquals("", ConsoleFunctions.syntax());
        } finally {
            System.setOut(original);
        }
        String printed = buf.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Quick Syntax Reference:"));
        assertTrue(printed.contains("Exception Handling:"));
    }

    // ---- vars ----

    @Test
    void vars_emptyMap_printsNoVariablesDefined() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        assertEquals("", ConsoleFunctions.vars());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("No variables defined."));
    }

    @Test
    void vars_withEntries_printsNameTypeAndFormattedValue() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("greeting", "hi");
        vars.put("count", 7);
        vars.put("flag", Boolean.TRUE);
        vars.put("missing", null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(vars, out));

        assertEquals("", ConsoleFunctions.vars());
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Session Variables:"));
        // String value is quoted and typed as String.
        assertTrue(printed.contains("greeting : String = \"hi\""), printed);
        // Number value typed as Integer, printed raw.
        assertTrue(printed.contains("count : Integer = 7"), printed);
        assertTrue(printed.contains("flag : Boolean = true"), printed);
        // Null value typed as "null".
        assertTrue(printed.contains("missing : null = null"), printed);
    }

    // ---- clear ----

    @Test
    void clear_emptiesSessionVariables_andConfirms() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("x", "v");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(vars, out));

        assertEquals("", ConsoleFunctions.clear());
        assertTrue(vars.isEmpty());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Variables cleared."));
    }

    // ---- load ----

    @Test
    void load_nullFilename_throws() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ExpressionException ex = assertThrows(ExpressionException.class, () -> ConsoleFunctions.load(null));
        assertTrue(ex.getMessage().contains("cannot be null or blank"));
    }

    @Test
    void load_blankFilename_throws() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        assertThrows(ExpressionException.class, () -> ConsoleFunctions.load("   "));
    }

    @Test
    void load_withoutContext_throws() {
        ConsoleExecutionContext.clear();
        assertThrows(ExpressionException.class, () -> ConsoleFunctions.load("anything.gs"));
    }

    @Test
    void load_nonexistentFile_throwsFileNotFound() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ConsoleFunctions.load("/no/such/path/does-not-exist.gs"));
        assertTrue(ex.getMessage().contains("file not found"));
    }

    @Test
    void load_existingFile_currentlyThrowsNotYetImplemented(@TempDir Path tmp) throws Exception {
        Path script = tmp.resolve("ok.gs");
        Files.writeString(script, "print(\"hi\")\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));

        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ConsoleFunctions.load(script.toAbsolutePath().toString()));
        assertTrue(ex.getMessage().contains("not yet implemented"), ex.getMessage());
        // It prints the loading banner before failing.
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Loading:"));
    }

    // ---- exit / quit ----

    @Test
    void exit_setsExitRequested_andReturnsExitingSentinel() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleContext ctx = newContext(new LinkedHashMap<>(), out);
        bind(ctx);
        assertFalse(ctx.isExitRequested());
        assertEquals("Exiting...", ConsoleFunctions.exit());
        assertTrue(ctx.isExitRequested());
    }

    @Test
    void quit_isAliasForExit_andSignalsExit() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleContext ctx = newContext(new LinkedHashMap<>(), out);
        bind(ctx);
        assertEquals("Exiting...", ConsoleFunctions.quit());
        assertTrue(ctx.isExitRequested());
    }

    // ---- man delegation ----

    @Test
    void manByName_blankName_delegatesToManList() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ec.manAllResult = "ALL-FUNCTIONS";
        assertEquals("", ConsoleFunctions.manByName("  "));
        // Blank name routes to manList(), which calls man() (no-arg) and prints it.
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("ALL-FUNCTIONS"));
        assertTrue(ec.manNoArgCalled);
    }

    @Test
    void manByName_known_printsResolvedDoc() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ec.manByKey.put("print", "PRINT-DOC");
        assertEquals("", ConsoleFunctions.manByName("print"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("PRINT-DOC"));
    }

    @Test
    void manByName_unknown_throwsNoDocumentation() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        // RecordingExpressionContext returns null for unknown keys.
        ExpressionException ex = assertThrows(ExpressionException.class, () -> ConsoleFunctions.manByName("ghost"));
        assertTrue(ex.getMessage().contains("no documentation found"));
    }

    @Test
    void manByIndex_unknown_throwsNoDocumentationForIndex() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ExpressionException ex = assertThrows(ExpressionException.class, () -> ConsoleFunctions.manByIndex(999));
        assertTrue(ex.getMessage().contains("no documentation found for index"));
    }

    @Test
    void manByIndex_known_printsResolvedDoc() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bind(newContext(new LinkedHashMap<>(), out));
        ec.manByIndex.put(2, "INDEX-2-DOC");
        assertEquals("", ConsoleFunctions.manByIndex(2));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("INDEX-2-DOC"));
    }

    /** Minimal recording stub of {@link IExpressionContext} for man() delegation tests. */
    private static final class RecordingExpressionContext implements IExpressionContext {
        String manAllResult = "DEFAULT-MAN";
        boolean manNoArgCalled = false;
        final Map<String, String> manByKey = new LinkedHashMap<>();
        final Map<Integer, String> manByIndex = new LinkedHashMap<>();

        @Override
        public void register(String key, IExpressionNodeFactory<?, ? extends ISupplier<?>> factory) {
        }

        @Override
        public IExpression<?, ? extends ISupplier<?>> expression(String expression) {
            return null;
        }

        @Override
        public String man(String key) {
            return manByKey.get(key);
        }

        @Override
        public String man() {
            manNoArgCalled = true;
            return manAllResult;
        }

        @Override
        public String man(int index) {
            return manByIndex.get(index);
        }

        @Override
        public void registerVariableType(String name, IClass<?> type) {
        }

        @Override
        public Set<String> getFactoryKeys() {
            return Set.of();
        }

        @Override
        public void enableDynamicFunctions() {
        }
    }
}
