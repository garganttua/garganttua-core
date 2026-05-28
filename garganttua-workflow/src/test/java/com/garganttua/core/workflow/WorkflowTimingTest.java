package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.workflow.dsl.WorkflowsBuilder;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.script.dsl.ScriptsBuilder;

class WorkflowTimingTest {

    private static IReflectionBuilder reflectionBuilder;
    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;
    private IRuntimesBuilder runtimesBuilder;
    private IScriptsBuilder scriptsBuilder;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setupClass() throws Exception {
        Class<? extends IReflectionProvider> providerClass = (Class<? extends IReflectionProvider>) Class
                .forName("com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
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

        runtimesBuilder = RuntimesBuilder.builder().provide(injectionContextBuilder);
        scriptsBuilder = ScriptsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .provide(runtimesBuilder);
    }

    @Test
    void timingDisabled_byDefault_producesScriptWithoutObserveCalls() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("untimed-workflow")
                .stage("step")
                    .script("result <- \"x\"").name("doit").output("greeting", "result").up()
                    .up()
                .build();

        assertFalse(workflow.getGeneratedScript().contains("observe("),
                "default config must produce a script byte-identical to the historical output (no observe() calls)");
    }

    @Test
    void timingEnabled_emitsStartAndEndForEachStage() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("timed-workflow")
                .stage("decode")
                    .script("result <- \"ok\"").name("d1").output("decoded", "result").up()
                    .up()
                .stage("verify")
                    .script("result <- \"ok\"").name("v1").output("verified", "result").up()
                    .up()
                .timing(WorkflowTimingConfig.of())
                .build();

        String script = workflow.getGeneratedScript();
        assertTrue(script.contains("observe(\"start\", \"stage:decode\")"), script);
        assertTrue(script.contains("observe(\"end\", \"stage:decode\")"), script);
        assertTrue(script.contains("observe(\"start\", \"stage:verify\")"), script);
        assertTrue(script.contains("observe(\"end\", \"stage:verify\")"), script);
        assertTrue(script.contains("observe(\"start\", \"script:decode.d1\")"), script);
        assertTrue(script.contains("observe(\"end\", \"script:decode.d1\""), script);
    }

    @Test
    void timingStagesOnly_omitsScriptMarkers() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("stages-only")
                .stage("only")
                    .script("result <- \"ok\"").name("inner").output("o", "result").up()
                    .up()
                .timing(WorkflowTimingConfig.of().scripts(false))
                .build();

        String script = workflow.getGeneratedScript();
        assertTrue(script.contains("observe(\"start\", \"stage:only\")"));
        assertFalse(script.contains("script:only.inner"),
                "scripts(false) must suppress script-level markers");
    }

    @Test
    void timingDisableStage_excludesNamedStage() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("partial")
                .stage("hot")
                    .script("result <- \"ok\"").name("h").output("o", "result").up()
                    .up()
                .stage("cold")
                    .script("result <- \"ok\"").name("c").output("o2", "result").up()
                    .up()
                .timing(WorkflowTimingConfig.of().disableStage("cold"))
                .build();

        String script = workflow.getGeneratedScript();
        assertTrue(script.contains("stage:hot"));
        assertFalse(script.contains("\"stage:cold\""),
                "disableStage must exclude the named stage from markers");
    }

    @Test
    void timingEnabled_observerReceivesStartAndEndEvents() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("live-events")
                .stage("alpha")
                    .script("result <- \"ok\"").name("step").output("out", "result").up()
                    .up()
                .timing(WorkflowTimingConfig.of())
                .build();

        List<ObservableEvent> received = new CopyOnWriteArrayList<>();
        ((Workflow) workflow).addObserver(received::add);

        WorkflowResult result = workflow.execute();
        assertTrue(result.isSuccess(), () -> "workflow failed: " + result.exceptionMessage().orElse("?"));

        long stageStarts = received.stream()
                .filter(e -> e instanceof StartEvent && e.source().startsWith("stage:"))
                .count();
        long stageEnds = received.stream()
                .filter(e -> e instanceof EndEvent && e.source().startsWith("stage:"))
                .count();
        long scriptStarts = received.stream()
                .filter(e -> e instanceof StartEvent && e.source().startsWith("script:"))
                .count();
        long scriptEnds = received.stream()
                .filter(e -> e instanceof EndEvent && e.source().startsWith("script:"))
                .count();
        assertEquals(1, stageStarts, "expected 1 stage StartEvent, got: " + received);
        assertEquals(1, stageEnds, "expected 1 stage EndEvent, got: " + received);
        assertEquals(1, scriptStarts, "expected 1 script StartEvent, got: " + received);
        assertEquals(1, scriptEnds, "expected 1 script EndEvent, got: " + received);

        EndEvent stageEnd = received.stream()
                .filter(e -> e instanceof EndEvent && e.source().equals("stage:alpha"))
                .map(e -> (EndEvent) e)
                .findFirst().orElse(null);
        assertNotNull(stageEnd, "stage end event not received");
        assertTrue(!stageEnd.duration().isNegative(), "duration must be non-negative");
    }

    @Test
    void timingDisabled_observerReceivesNoStageOrScriptEvents() {
        IWorkflow workflow = WorkflowsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(scriptsBuilder)
                .workflow("silent")
                .stage("stage")
                    .script("result <- \"ok\"").name("s").output("o", "result").up()
                    .up()
                .build();

        List<ObservableEvent> received = new CopyOnWriteArrayList<>();
        ((Workflow) workflow).addObserver(received::add);

        WorkflowResult result = workflow.execute();
        assertTrue(result.isSuccess());

        long stageOrScript = received.stream()
                .filter(e -> e.source().startsWith("stage:") || e.source().startsWith("script:"))
                .count();
        assertEquals(0, stageOrScript,
                "no stage/script events should fire when timing is disabled (Runtime events still propagate)");
    }
}
