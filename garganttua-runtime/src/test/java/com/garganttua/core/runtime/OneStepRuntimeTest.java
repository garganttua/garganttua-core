package com.garganttua.core.runtime;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.runtime.RuntimeContext.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimeStepBuilder;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.runtime.runtimes.onestep.DummyRuntimeProcessOutputStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class OneStepRuntimeTest {

    @BeforeEach
    void logTestStart(TestInfo testInfo) {
        log.atInfo().log("Executing test method: {}", testInfo.getTestMethod().get().getName());
    }

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    private IInjectionContextBuilder contextBuilder() {
        return InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime.resolver")
                .withPackage("com.garganttua.core.runtime");
    }

    private IRuntimesBuilder builder() {
        IInjectionContextBuilder ctx = contextBuilder();
        ctx.build().onInit().onStart();
        return RuntimesBuilder.builder().provide(ctx);
    }

    private IRuntime<String, String> get(IRuntimesBuilder b) {
        Map<String, IRuntime<?, ?>> runtimes = b.build();
        //assertEquals(1, runtimes.size());
        return cast(runtimes.get("runtime-1"));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object o) {
        return (T) o;
    }

    private IRuntimeStepBuilder<String, DummyRuntimeProcessOutputStep, String, String> baseRuntime(IRuntimesBuilder b,
            DummyRuntimeProcessOutputStep step) {
        return b.runtime("runtime-1", String.class, String.class)
                .stage("stage-1")
                .step("step-1", of(step), String.class)
                .method()
                .condition(custom(of(10), i -> true))
                .output(true)
                .variable("method-returned")
                .method("method")
                .code(201)
                .katch(DiException.class).code(401).up()
                .withParam(input(String.class))
                .withParam(of("fixed-value-in-method"))
                .withParam(variable("variable", String.class))
                .withParam(context()).up();
    }

    private IRuntimeStepBuilder<String, DummyRuntimeProcessOutputStep, String, String> baseFallback(
            IRuntimeStepBuilder<String, DummyRuntimeProcessOutputStep, String, String> b) {
        return b.fallBack()
                .onException(DiException.class).up()
                .output(true)
                .variable("fallback-returned")
                .method("fallbackMethod")
                .withParam(input(String.class))
                .withParam(of("fixed-value-in-method"))
                .withParam(exception(DiException.class))
                .withParam(code())
                .withParam(exceptionMessage())
                .withParam(context())
                .up();
    }

    // -------------------------------------------------------------------------
    // TESTS
    // -------------------------------------------------------------------------

    @Test
    void builtAutoDetectedRuntimesShouldContain() {
        IRuntimesBuilder t = builder();
        Map<String, IRuntime<?, ?>> runtimes = t.autoDetect(true).build();

        assertEquals(3, runtimes.size());
        assertTrue(runtimes.containsKey("runtime-1"));
    }

    @Test
    void simpleRuntimeBuilderTest() {
        DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

        IRuntimesBuilder b = baseFallback(
                baseRuntime(builder(), step)).up().up().variable("variable", of("preset-variable")).up();

        IRuntime<String, String> runtime = get(b);

        assertDoesNotThrow(() -> {
            var result = runtime.execute("input").orElseThrow();
            assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());
        });
    }

    @Test
    void uncatchedException_abort_true() {
        DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

        IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step).method()
                .abortOnUncatchedException(true).up()).up().up().variable("variable", of("custom-exception")).up();

        IRuntime<String, String> runtime = get(b);

        var r = runtime.execute("input").orElseThrow();

        assertEquals(1, r.getExceptions().size());
        assertTrue(r.hasAborted());
        assertTrue(r.getAbortingException().isPresent());
        assertEquals(IRuntime.GENERIC_RUNTIME_ERROR_CODE, r.code());
    }

    @Test
    void uncatchedException_abort_false_stepNotNullable() {
        DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

        IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step).method()
                .abortOnUncatchedException(false).up()).up().up().variable("variable", of("custom-exception"))
                .up();

        IRuntime<String, String> runtime = get(b);

        var r = runtime.execute("input").orElseThrow();

        assertEquals(2, r.getExceptions().size());
        assertTrue(r.hasAborted());
    }

    @Test
    void uncatchedException_abort_false_stepNullable() {
        DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

        IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step).method()
                .nullable(true).abortOnUncatchedException(false).up()).up().up().variable("variable", of("custom-exception"))
                .up();

        IRuntime<String, String> runtime = get(b);

        var r = runtime.execute("input").orElseThrow();

        assertEquals(1, r.getExceptions().size());
        assertFalse(r.hasAborted());
        assertEquals(201, r.code());
    }

    @Test
    void catchedExceptionHandledByFallback() {
        DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

        IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step)).up().up().variable("variable", of("di-exception")).up();

        IRuntime<String, String> runtime = get(b);

        var r = runtime.execute("input").orElseThrow();

        assertEquals(1, r.getExceptions().size());
        assertTrue(r.hasAborted());
        assertTrue(r.getAbortingException().isPresent());
    }

    @Test
    void autodetectionBuild() throws Exception {
        IRuntimesBuilder t = builder();
        Map<String, IRuntime<?, ?>> runtimes = t.autoDetect(true).build();

        IRuntime<String, String> runtime = cast(runtimes.get("runtime-1"));
        var r = runtime.execute("input").orElseThrow();

        assertEquals("input-processed-fixed-value-in-method-preset-variable", r.output());
        assertEquals(201, r.code());
    }
}