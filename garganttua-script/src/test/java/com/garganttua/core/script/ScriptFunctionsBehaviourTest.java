package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.functions.ScriptFunctions;

/**
 * Behaviour tests for the pure, context-free helpers in {@link ScriptFunctions}:
 * {@code print}, {@code format} and {@code cast}. The {@code include}/{@code call}/
 * {@code execute_script}/{@code script_variable} family requires an active
 * {@code ScriptExecutionContext} and is exercised through the script integration
 * tests instead.
 */
class ScriptFunctionsBehaviourTest {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupReflection() throws Exception {
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    // ---- print / println ----

    @Test
    void printReturnsStringRepresentation() {
        assertEquals("hello", ScriptFunctions.print("hello"));
        assertEquals("42", ScriptFunctions.print(42));
    }

    @Test
    void printNullReturnsLiteralNull() {
        assertEquals("null", ScriptFunctions.print((Object) null));
    }

    @Test
    void printStringAndIntConcatenates() {
        assertEquals("count=7", ScriptFunctions.print("count=", 7));
    }

    @Test
    void printStringAndIntWithNullStringUsesNull() {
        assertEquals("null9", ScriptFunctions.print((String) null, 9));
    }

    @Test
    void printlnDelegatesToPrint() {
        assertEquals("x", ScriptFunctions.println("x"));
        assertEquals("null", ScriptFunctions.println(null));
    }

    @Test
    void eprintReturnsStringRepresentation() {
        assertEquals("err", ScriptFunctions.eprint("err"));
        assertEquals("null", ScriptFunctions.eprint(null));
        assertEquals("err2", ScriptFunctions.eprintln("err2"));
    }

    // ---- format ----

    @Test
    void formatOneArg() {
        assertEquals("Hello world", ScriptFunctions.format("Hello %s", "world"));
    }

    @Test
    void formatOneArgWithIntegerSpecifier() {
        assertEquals("Count: 5", ScriptFunctions.format("Count: %d", 5));
    }

    @Test
    void formatTwoArgs() {
        assertEquals("a-b", ScriptFunctions.format("%s-%s", "a", "b"));
    }

    @Test
    void formatThreeArgs() {
        assertEquals("1/2/3", ScriptFunctions.format("%s/%s/%s", "1", "2", "3"));
    }

    @Test
    void formatNullPatternReturnsLiteralNull() {
        assertEquals("null", ScriptFunctions.format(null, "x"));
        assertEquals("null", ScriptFunctions.format(null, "x", "y"));
        assertEquals("null", ScriptFunctions.format(null, "x", "y", "z"));
    }

    @Test
    void formatNonStringPatternUsesToString() {
        // pattern.toString() of an Integer is "%d"-incompatible literal -> just the value
        assertEquals("100", ScriptFunctions.format(100, "ignored"));
    }

    // ---- cast ----

    @Test
    void castNullTypeThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.cast(null, "value"));
        assertTrue(ex.getMessage().contains("type cannot be null"));
    }

    @Test
    void castNullValueReturnsNull() {
        assertNull(ScriptFunctions.cast(IClass.getClass(String.class), null));
    }

    @Test
    void castAssignableValueReturnsValue() {
        Object value = "hello";
        String r = ScriptFunctions.cast(IClass.getClass(String.class), value);
        assertEquals("hello", r);
    }

    @Test
    void castWideningToObjectSucceeds() {
        Object r = ScriptFunctions.cast(IClass.getClass(Object.class), "x");
        assertEquals("x", r);
    }

    @Test
    void castIncompatibleTypeThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.cast(IClass.getClass(Integer.class), "not-an-int"));
        assertTrue(ex.getMessage().contains("cannot cast"));
        assertTrue(ex.getMessage().contains("Integer"));
    }

    // ---- include / call validation (no context active) ----

    @Test
    void includeBlankPathThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.include("   "));
        assertTrue(ex.getMessage().contains("path cannot be null or blank"));
    }

    @Test
    void includeNullPathThrows() {
        assertThrows(ExpressionException.class, () -> ScriptFunctions.include(null));
    }

    @Test
    void callBlankNameThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.call(""));
        assertTrue(ex.getMessage().contains("script name cannot be null or blank"));
    }

    @Test
    void executeScriptNullNameThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.executeScript(null));
        assertTrue(ex.getMessage().contains("script name cannot be null"));
    }

    @Test
    void scriptVariableNullScriptNameThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.scriptVariable(null, "v"));
        assertTrue(ex.getMessage().contains("script name cannot be null"));
    }

    @Test
    void scriptVariableBlankVarNameThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ScriptFunctions.scriptVariable("s", "  "));
        assertTrue(ex.getMessage().contains("variable name cannot be null or blank"));
    }
}
