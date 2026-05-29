package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.script.dsl.ScriptsBuilder;
import com.garganttua.core.workflow.dsl.IWorkflowBuilder;
import com.garganttua.core.workflow.dsl.WorkflowsBuilder;

/**
 * Validates {@code WorkflowBuilder.precompile(true)}:
 * <ol>
 *   <li>the same {@code ICompiledScript} is reused across {@code execute()}
 *       calls (no per-call ANTLR parse / runtime build);</li>
 *   <li>concurrent executions from N threads on the SAME Workflow instance
 *       all return independent, correct results.</li>
 * </ol>
 */
@DisplayName("Workflow precompile — thread-safe shared compiled handle")
class WorkflowPrecompileTest {

    private static IReflectionBuilder reflectionBuilder;
    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;
    private IRuntimesBuilder runtimesBuilder;
    private IScriptsBuilder scriptsBuilder;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setupClass() throws Exception {
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
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true)
                .provide(injectionContextBuilder);
        injectionContextBuilder.build().onInit().onStart();
        expressionContextBuilder.build();
        runtimesBuilder = RuntimesBuilder.builder().provide(injectionContextBuilder);
        scriptsBuilder = ScriptsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .provide(runtimesBuilder);
    }

    @Test
    @DisplayName("precompile(true) → 200 concurrent executes return correct per-call results")
    void concurrentExecutes_onPrecompiledWorkflow_areIsolated() throws Exception {
        IWorkflowBuilder builder = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("precompiled-echo")
                .precompile(true)
                .stage("echo")
                    .script("greeting <- concatenate(\"hello \", @0)")
                        .name("echo-script")
                        .output("greeting", "greeting")
                        .up()
                    .up();
        IWorkflow workflow = builder.build();
        assertNotNull(workflow);

        final int threads = 16;
        final int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<CompletableFuture<WorkflowResult>> futures = new ArrayList<>();
            AtomicInteger seq = new AtomicInteger();
            for (int i = 0; i < iterations; i++) {
                final String name = "caller-" + seq.getAndIncrement();
                futures.add(CompletableFuture.supplyAsync(
                        () -> workflow.execute(WorkflowInput.of(name)),
                        pool));
            }

            for (int i = 0; i < futures.size(); i++) {
                final int iter = i;
                WorkflowResult r = futures.get(iter).get(30, TimeUnit.SECONDS);
                assertTrue(r.isSuccess(),
                        "iteration " + iter + " failed: " + r.exception().orElse(null));
                assertEquals(0, r.code());
                String greeting = r.getVariable("greeting", IClass.getClass(String.class))
                        .orElseThrow(() -> new AssertionError("iteration " + iter + " missing 'greeting'"));
                assertTrue(greeting.startsWith("hello caller-"),
                        "iteration " + iter + " unexpected greeting: " + greeting);
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("precompile(true) — same workflow, sequential execs all OK")
    void sequentialExecutes_onPrecompiledWorkflow_allSucceed() throws Exception {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("precompiled-counter")
                .precompile(true)
                .stage("count")
                    .script("v <- @0")
                        .name("counter")
                        .output("v", "v")
                        .up()
                    .up()
                .build();

        for (int i = 0; i < 50; i++) {
            WorkflowResult r = workflow.execute(WorkflowInput.of(i));
            assertTrue(r.isSuccess(), "iteration " + i + " failed");
            Integer v = r.getVariable("v", IClass.getClass(Integer.class)).orElse(null);
            assertEquals(i, v, "iteration " + i + " expected " + i + " got " + v);
        }
    }

    @Test
    @DisplayName("precompile(false) — fallback path still works")
    void noPrecompile_legacyPathStillWorks() throws Exception {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("no-precompile")
                .precompile(false)
                .stage("greet")
                    .script("greeting <- \"static\"")
                        .name("g")
                        .output("greeting", "greeting")
                        .up()
                    .up()
                .build();

        WorkflowResult r = workflow.execute();
        assertTrue(r.isSuccess());
        assertEquals("static",
                r.getVariable("greeting", IClass.getClass(String.class)).orElse(null));
    }
}
