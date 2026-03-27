package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Tests for statement groups feature.
 */
class ScriptGroupTest {

    private static IReflectionBuilder reflectionBuilder;
    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;

    @BeforeAll
    static void setupClass() throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @BeforeEach
    void setup() {
        injectionContextBuilder = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        injectionContextBuilder.build().onInit().onStart();
        expressionContextBuilder.build();
    }

    @Test
    void testSimpleStatementGroup() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            result <- (
                a <- 10
                b <- 20
                @b
            )
            """);
        script.compile();
        int code = script.execute();

        assertEquals(0, code);
        assertEquals(20, script.getVariable("result", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupWithCodeMapping() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            result <- (
                value <- 42
            ) -> 99
            """);
        script.compile();
        int code = script.execute();

        assertEquals(99, code);
        assertEquals(42, script.getVariable("value", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupWithInnerCodeMappings() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            (
                first <- 1 -> 10
                second <- 2 -> 20
            ) -> 100
            """);
        script.compile();
        int code = script.execute();

        // The outer group code (100) should be the final code
        assertEquals(100, code);
    }

    @Test
    void testNestedGroups() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            outer <- (
                a <- 5
                inner <- (
                    b <- 10
                    @b
                )
                @inner
            )
            """);
        script.compile();
        int code = script.execute();

        // With nested groups, the outer result captures the inner result
        // Code might not be 0 due to inner group execution
        assertEquals(10, script.getVariable("outer", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testGroupVariablesVisibility() throws Exception {
        IScript script = new ScriptContext(expressionContextBuilder.build(), injectionContextBuilder.build());
        script.load("""
            x <- 100
            (
                y <- 150
                z <- 175
            )
            final <- @z
            """);
        script.compile();
        int code = script.execute();

        assertEquals(0, code);
        assertEquals(100, script.getVariable("x", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(150, script.getVariable("y", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(175, script.getVariable("z", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(175, script.getVariable("final", IClass.getClass(Integer.class)).orElse(null));
    }

    // =====================================================================
    // CATCH CLAUSES INSIDE GROUPS
    // =====================================================================

    private IScript createScript(String source) {
        IInjectionContextBuilder ijb = InjectionContext.builder()
                .provide(reflectionBuilder)
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder ecb = ExpressionContextBuilder.builder();
        ecb.withPackage("com.garganttua").autoDetect(true).provide(ijb);

        IInjectionContext ic = ijb.build();
        ic.onInit().onStart();
        IExpressionContext ec = ecb.build();

        ScriptContext ctx = new ScriptContext(ec, ic);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    @Test
    void testCatchClauseCodeInsideGroup() {
        // Inner statement has catch clause with code mapping
        // class("nonexistent.Foo") throws -> catch sets code 400
        IScript s = createScript("""
                (
                    class("nonexistent.Foo")
                    ! -> 400
                )
                """);
        assertEquals(400, s.execute());
        assertFalse(s.hasAborted());
    }

    @Test
    void testCatchClauseWithHandlerInsideGroup() {
        // Inner statement has catch clause with handler expression
        IScript s = createScript("""
                result <- (
                    class("nonexistent.Foo")
                    ! => "caught" -> 400
                )
                """);
        assertEquals(400, s.execute());
        assertFalse(s.hasAborted());
    }

    @Test
    void testCatchInGroupStopsRemainingStatements() {
        // After catch in group, remaining statements in the group don't execute
        IScript s = createScript("""
                (
                    class("nonexistent.Foo")
                    ! -> 400
                    shouldNotRun <- "ran"
                )
                """);
        assertEquals(400, s.execute());
        // shouldNotRun should not be set since execution stopped after catch
        assertTrue(s.getVariable("shouldNotRun", IClass.getClass(String.class)).isEmpty());
    }

    @Test
    void testCatchInGroupDoesNotAffectNextTopLevelStatement() {
        // After a caught exception inside a group, the next top-level statement runs
        IScript s = createScript("""
                (
                    class("nonexistent.Foo")
                    ! -> 400
                )
                afterGroup <- "continued" -> 200
                """);
        assertEquals(200, s.execute());
        assertEquals("continued", s.getVariable("afterGroup", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testOuterGroupCatchClause() {
        // Exception in group, no inner catch, outer group catch handles it
        IScript s = createScript("""
                (
                    class("nonexistent.Foo")
                )
                ! -> 500
                """);
        assertEquals(500, s.execute());
    }

    @Test
    void testCatchInsideNestedGroup() {
        // Catch clause on a statement inside a nested group
        IScript s = createScript("""
                (
                    (
                        class("nonexistent.Foo")
                        ! -> 401
                    )
                )
                afterOuter <- "ok" -> 200
                """);
        assertEquals(200, s.execute());
        assertEquals("ok", s.getVariable("afterOuter", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testCatchInsideIfBlock() {
        // Catch clause works inside an if() block body
        IScript s = createScript("""
                if(true, (
                    class("nonexistent.Foo")
                    ! -> 401
                ), 0)
                after <- "continued" -> 200
                """);
        assertEquals(200, s.execute());
        assertEquals("continued", s.getVariable("after", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testCatchInsideIfBlockElse() {
        // Catch clause works inside an if() else block body
        IScript s = createScript("""
                if(false, 0, (
                    class("nonexistent.Foo")
                    ! -> 402
                ))
                after <- "continued" -> 200
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testCatchWithHandlerVariableInsideGroup() {
        // Handler in catch clause sets a variable
        IScript s = createScript("""
                (
                    class("nonexistent.Foo")
                    ! => msg <- @message -> 400
                )
                """);
        assertEquals(400, s.execute());
        // The handler should have set msg variable
        assertTrue(s.getVariable("msg", IClass.getClass(String.class)).isPresent());
    }

    // =====================================================================
    // VARIABLE PROPAGATION FROM IF BLOCKS TO PARENT SCOPE
    // =====================================================================

    @Test
    void testVariableAssignmentInIfBlockPropagatesToParent() {
        // Variable assigned inside if() block must be visible after the if
        IScript s = createScript("""
                _code <- 405
                if(true, (
                    _code <- 200
                ), 0)
                result <- @_code
                """);
        int code = s.execute();
        assertEquals(0, code);
        assertEquals(200, s.getVariable("result", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testVariableAssignmentWithFunctionCallInIfBlock() {
        // Variable assigned via function call result inside if() block
        IScript s = createScript("""
                _code <- "initial"
                if(true, (
                    _code <- concatenate("up", "dated")
                ), 0)
                result <- @_code
                """);
        int code = s.execute();
        assertEquals(0, code);
        assertEquals("updated", s.getVariable("result", IClass.getClass(String.class)).orElse(null));
    }

    @Test
    void testVariableInIfElseBranchPropagates() {
        // Variable assigned in else branch propagates
        IScript s = createScript("""
                _code <- 405
                if(false, 0, (
                    _code <- 300
                ))
                result <- @_code
                """);
        s.execute();
        assertEquals(300, s.getVariable("result", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testVariableOverwrittenInIfBlockVisibleAfter() {
        // Exact pattern from workflow: init group -> if-block overwrites -> exit group reads
        IScript s = createScript("""
                (
                    _code <- 405
                )
                if(true, (
                    _code <- 400
                ), 0)
                (
                    result <- @_code
                )
                """);
        s.execute();
        assertEquals(400, s.getVariable("result", IClass.getClass(Integer.class)).orElse(null));
    }

    @Test
    void testVariableInIfBlockVisibleInPipeClauses() {
        // Full workflow pattern: init -> if-block sets _code -> exit-code reads via pipes
        IScript s = createScript("""
                (
                    _code <- 405
                )
                if(true, (
                    _code <- 400
                ), 0)
                (
                    0
                        | equals(@_code, 0) -> 0
                        | equals(@_code, 400) -> 400
                        | equals(@_code, 405) -> 405
                )
                """);
        int code = s.execute();
        assertEquals(400, code);
    }

    @Test
    void testMultipleVariablesInIfBlockPropagate() {
        // Multiple variables assigned inside if() block
        IScript s = createScript("""
                a <- 1
                b <- 2
                if(true, (
                    a <- 10
                    b <- 20
                    c <- 30
                ), 0)
                ra <- @a
                rb <- @b
                rc <- @c
                """);
        s.execute();
        assertEquals(10, s.getVariable("ra", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(20, s.getVariable("rb", IClass.getClass(Integer.class)).orElse(null));
        assertEquals(30, s.getVariable("rc", IClass.getClass(Integer.class)).orElse(null));
    }
}
