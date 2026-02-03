package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.context.ScriptContext;

class ScriptComplexTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    private IScript createScript(String source) {
        IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        ExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        IInjectionContext injectionContext = injectionContextBuilder.build();
        injectionContext.onInit().onStart();

        IExpressionContext expressionContext = expressionContextBuilder.build();

        ScriptContext ctx = new ScriptContext(expressionContext, injectionContext);
        ctx.load(source);
        ctx.compile();
        return ctx;
    }

    // =====================================================================
    // MULTI-STEP FLOW
    // =====================================================================

    @Test
    void testThreeStepsLastCodeWins() {
        IScript s = createScript("""
                "step0" -> 100
                "step1" -> 200
                "step2" -> 300
                """);
        assertEquals(300, s.execute());
    }

    @Test
    void testVariableResultAssignment() {
        IScript s = createScript("""
                x <- string("hello") -> 100;
                y <- string("world") -> 200;
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testExpressionAssignmentAcrossSteps() {
        IScript s = createScript("""
                x = string("hello") -> 100;
                y = string("world") -> 200;
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testMixedAssignments() {
        IScript s = createScript("""
                a <- string("hello") -> 100;
                b = string("world") -> 200;
                "done" -> 300
                """);
        assertEquals(300, s.execute());
    }

    @Test
    void testFiveStepsSequential() {
        IScript s = createScript("""
                "a" -> 10
                "b" -> 20
                "c" -> 30
                "d" -> 40
                "e" -> 50
                """);
        assertEquals(50, s.execute());
    }

    // =====================================================================
    // LOCAL CATCH CLAUSES (!) - SCRIPT STOPS AFTER CATCH
    // =====================================================================

    @Test
    void testCatchAllStopsExecution() {
        // class("nonexistent.Foo") throws ExpressionException
        // Catch-all catches it; subsequent step should NOT run
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => "caught" -> 400
                "should-not-run" -> 200
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testTypedCatchMatchesBySimpleName() {
        // The cause chain is ScriptException → SupplyException → ExpressionException
        // CatchClause.matches receives the direct cause (SupplyException)
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! SupplyException.Class => "caught" -> 401
                """);
        assertEquals(401, s.execute());
    }

    @Test
    void testTypedCatchMatchesByFqcn() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! com.garganttua.core.supply.SupplyException.Class => "caught-fqcn" -> 402
                """);
        assertEquals(402, s.execute());
    }

    @Test
    void testTypedCatchDoesNotMatchFallsToNext() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! IllegalStateException.Class => "wrong" -> 401
                ! => "caught-all" -> 500
                """);
        assertEquals(500, s.execute());
    }

    @Test
    void testMultipleCatchClausesFirstMatchWins() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! SupplyException.Class => "specific" -> 401
                ! => "generic" -> 500
                """);
        assertEquals(401, s.execute());
    }

    @Test
    void testNoCatchUnhandledExceptionReturnsErrorCode() {
        // No catch clause → handleException records code 50 (GENERIC_RUNTIME_ERROR_CODE)
        // ExecutorChain rethrow=false → runtime returns normally with code 50
        IScript s = createScript("""
                class("nonexistent.Foo")
                """);
        assertEquals(50, s.execute());
    }

    @Test
    void testCatchHandlerSetsVariableAndCode() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => err <- "error-value" -> 400
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testCatchStopsChainSubsequentStepsNotExecuted() {
        // Step-0 throws and is caught → code 400
        // Step-1 and Step-2 must NOT execute
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => "caught" -> 400
                "step1" -> 200
                "step2" -> 300
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testCatchNotTriggeredOnSuccess() {
        // Expression succeeds → catch clause is ignored, next step runs
        IScript s = createScript("""
                string("hello") -> 100;
                ! => "should-not-catch" -> 400
                "step1" -> 200
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testCatchWithExpressionAssignHandler() {
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => errExpr = "lazy-error" -> 403
                """);
        assertEquals(403, s.execute());
    }

    @Test
    void testUnhandledExceptionStopsSubsequentSteps() {
        // Step-0 throws unhandled → code 50, chain stops
        // Step-1 should NOT execute
        IScript s = createScript("""
                class("nonexistent.Foo")
                "step1" -> 200
                """);
        assertEquals(50, s.execute());
    }

    // =====================================================================
    // DOWNSTREAM CATCH CLAUSES (*) - CATCHES EXCEPTIONS FROM LATER STEPS
    // =====================================================================

    @Test
    void testDownstreamCatchAll() {
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream-caught" -> 600
                class("nonexistent.Foo")
                """);
        assertEquals(600, s.execute());
    }

    @Test
    void testMultipleDownstreamCatchesSpecificMatch() {
        // Downstream sees ExpressionException (unwrapped by RuntimeStepExecutionTools.findExceptionForReport)
        IScript s = createScript("""
                "step0" -> 100
                * ExpressionException.Class => "expr-caught" -> 601
                * => "generic-caught" -> 602
                class("nonexistent.Foo")
                """);
        assertEquals(601, s.execute());
    }

    @Test
    void testMultipleDownstreamCatchesFallToGeneric() {
        IScript s = createScript("""
                "step0" -> 100
                * IllegalStateException.Class => "state-caught" -> 601
                * => "generic-caught" -> 602
                class("nonexistent.Foo")
                """);
        assertEquals(602, s.execute());
    }

    @Test
    void testDownstreamCatchWithVariableAssignment() {
        IScript s = createScript("""
                "step0" -> 100
                * => errMsg <- "downstream-error" -> 500
                class("nonexistent.Foo")
                """);
        assertEquals(500, s.execute());
    }

    @Test
    void testDownstreamCatchNotTriggeredWhenLocalCatchHandles() {
        // Step-0 has downstream catch
        // Step-1 has local catch that handles → no exception propagates
        // Downstream catch should NOT trigger
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream" -> 600
                class("nonexistent.Foo")
                ! => "local-caught" -> 400
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testDownstreamCatchFromEarlierStepCatchesLaterStepException() {
        // step0 has downstream catch, step1 OK, step2 throws
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream-caught" -> 700
                "step1" -> 200
                class("nonexistent.Foo")
                """);
        assertEquals(700, s.execute());
    }

    @Test
    void testDownstreamCatchNotTriggeredWhenAllSucceed() {
        // All steps succeed → downstream catch never fires
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream" -> 600
                "step1" -> 200
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testDownstreamCatchWithExpressionAssignHandler() {
        IScript s = createScript("""
                "step0" -> 100
                * => errExpr = "lazy-downstream" -> 700
                class("nonexistent.Foo")
                """);
        assertEquals(700, s.execute());
    }

    @Test
    void testMultipleDownstreamCatchesWithFqcn() {
        IScript s = createScript("""
                "step0" -> 100
                * java.lang.IllegalStateException.Class => "state" -> 601
                * com.garganttua.core.expression.ExpressionException.Class => "expr" -> 602
                * => "generic" -> 603
                class("nonexistent.Foo")
                """);
        assertEquals(602, s.execute());
    }

    // =====================================================================
    // PIPE CLAUSES (|)
    // =====================================================================

    @Test
    void testDefaultPipeClause() {
        IScript s = createScript("""
                string("hello")
                | => "piped" -> 300
                """);
        assertEquals(300, s.execute());
    }

    @Test
    void testConditionalPipeTrueMatchesFirst() {
        IScript s = createScript("""
                string("hello")
                | true => "matched" -> 301
                | => "default" -> 302
                """);
        assertEquals(301, s.execute());
    }

    @Test
    void testConditionalPipeFalseSkipsToDefault() {
        IScript s = createScript("""
                string("hello")
                | false => "skipped" -> 301
                | => "default" -> 302
                """);
        assertEquals(302, s.execute());
    }

    @Test
    void testPipeSetsVariableAndCode() {
        IScript s = createScript("""
                string("hello")
                | => result <- "piped-value" -> 303
                """);
        assertEquals(303, s.execute());
    }

    @Test
    void testMultiplePipesFirstTrueWins() {
        IScript s = createScript("""
                string("hello")
                | false => "first" -> 301
                | true => "second" -> 302
                | => "default" -> 303
                """);
        assertEquals(302, s.execute());
    }

    @Test
    void testPipeNotEvaluatedOnException() {
        // Expression throws, catch handles it → pipe is never evaluated
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => "caught" -> 400
                | => "should-not-pipe" -> 300
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testPipeOnlyOnSuccess() {
        IScript s = createScript("""
                string("hello") -> 100;
                | true => "piped" -> 200
                """);
        assertEquals(200, s.execute());
    }

    @Test
    void testNoPipeMatches() {
        // All conditions false, no default → pipe does nothing, original code preserved
        IScript s = createScript("""
                string("hello") -> 100;
                | false => "nope" -> 200
                """);
        assertEquals(100, s.execute());
    }

    @Test
    void testPipeAfterStepThenNextStep() {
        // Step-0 with pipe, step-1 runs after
        IScript s = createScript("""
                string("hello") -> 100;
                | true => "piped" -> 200
                "step1" -> 300
                """);
        assertEquals(300, s.execute());
    }

    // =====================================================================
    // COMBINED SCENARIOS
    // =====================================================================

    @Test
    void testCatchSetsCodeOverridesDefault() {
        IScript s = createScript("""
                class("nonexistent.Foo") -> 200;
                ! => "error" -> 400
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testSuccessfulStepWithDownstreamThenFailingStep() {
        IScript s = createScript("""
                a <- string("first") -> 100;
                * => "downstream" -> 999
                class("nonexistent.Foo")
                """);
        assertEquals(999, s.execute());
    }

    @Test
    void testMultipleStepsOnlyLastFailsNoDownstream() {
        IScript s = createScript("""
                "step0" -> 100
                "step1" -> 200
                class("nonexistent.Foo")
                """);
        // No catch, no downstream → unhandled → code 50
        assertEquals(50, s.execute());
    }

    @Test
    void testLocalCatchPreventsDownstreamFromFiring() {
        // Step-0: success, has downstream catch
        // Step-1: success
        // Step-2: fails, local catch handles → stops chain
        // Downstream from step-0 should NOT trigger
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream" -> 600
                "step1" -> 200
                class("nonexistent.Foo")
                ! => "local" -> 400
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testDownstreamTriggeredWhenNoLocalCatch() {
        // Step-0: success, has downstream catch
        // Step-1: success
        // Step-2: fails, NO local catch → downstream triggered
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream" -> 600
                "step1" -> 200
                class("nonexistent.Foo")
                """);
        assertEquals(600, s.execute());
    }

    @Test
    void testPipeAndCatchOnDifferentSteps() {
        // Step-0: success with pipe
        // Step-1: fails with catch
        IScript s = createScript("""
                string("hello")
                | => "piped" -> 300
                class("nonexistent.Foo")
                ! => "caught" -> 400
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testPipeThenDownstreamCatchOnSameStep() {
        // Step-0: success, has downstream catch AND pipe (pipe before downstream in grammar: catch* downstream* pipe*)
        // Wait - grammar order is catch, downstream, pipe. But here we need downstream + pipe.
        // Grammar: statement: expression ';' catchClause* downstreamCatchClause* pipeClause*
        // So downstream comes BEFORE pipe.
        IScript s = createScript("""
                string("ok") -> 100;
                * => "downstream" -> 600
                | true => "piped" -> 300
                class("nonexistent.Foo")
                """);
        // Step-0: expression succeeds (code 100), downstream registered, pipe matches (code 300)
        // Step-1: throws, unhandled → handleException(code 50), fallback fires → code 600
        assertEquals(600, s.execute());
    }

    @Test
    void testLocalCatchAndDownstreamOnSameStatement() {
        // Statement has both ! and * clauses
        // Expression throws → local catch handles, downstream is registered but never triggered
        IScript s = createScript("""
                class("nonexistent.Foo")
                ! => "local" -> 400
                * => "downstream-never" -> 600
                "step1" -> 200
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testChainOfFourStepsMiddleThrowsWithDownstream() {
        IScript s = createScript("""
                "step0" -> 100
                * ExpressionException.Class => "caught-from-0" -> 700
                "step1" -> 200
                class("nonexistent.Foo")
                "step3" -> 300
                """);
        // step2 throws, step3 never runs, downstream from step0 catches it
        assertEquals(700, s.execute());
    }

    @Test
    void testTwoUnhandledExceptionsOnlyFirstRecorded() {
        // Only one step can throw per execution (chain stops after first unhandled)
        IScript s = createScript("""
                class("nonexistent.Foo1")
                class("nonexistent.Foo2")
                """);
        // step0 throws unhandled → code 50, chain stops
        assertEquals(50, s.execute());
    }

    @Test
    void testComplexScenarioSuccessPath() {
        // Full success scenario with variables, assignments, pipes, and dormant catches
        IScript s = createScript("""
                a <- string("hello") -> 100;
                ! SupplyException.Class => "err" -> 500
                b <- string("world") -> 200;
                * => "downstream" -> 600
                string("end") -> 300;
                | true => d <- "piped" -> 400
                """);
        // All succeed: step0 code 100, step1 code 200, step2 code 300, pipe overrides to 400
        assertEquals(400, s.execute());
    }

    @Test
    void testComplexScenarioWithFailureInMiddle() {
        // Step-0 OK, Step-1 throws caught locally, Step-2 never runs
        IScript s = createScript("""
                a <- string("hello") -> 100;
                class("nonexistent.Foo")
                ! => err <- "handled" -> 400
                "step2-never" -> 200
                """);
        assertEquals(400, s.execute());
    }

    @Test
    void testDownstreamCatchAfterMultipleSuccessfulSteps() {
        // Step-0 has downstream, steps 1-3 succeed, step-4 throws
        IScript s = createScript("""
                "step0" -> 100
                * => "downstream" -> 900
                "step1" -> 200
                "step2" -> 300
                "step3" -> 400
                class("nonexistent.Foo")
                """);
        assertEquals(900, s.execute());
    }
}
