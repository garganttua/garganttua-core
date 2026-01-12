package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TwoStepsRuntimeTest {

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
                .withPackage("com.garganttua.core.runtime.annotations")
                .withPackage("com.garganttua.core.runtime.runtimes.twosteps");
    }

    private IRuntimesBuilder builder() {
        IInjectionContextBuilder ctx = contextBuilder();
        ctx.build().onInit().onStart();
        return RuntimesBuilder.builder().provide(ctx).autoDetect(true);
    }

    private IRuntime<String, String> get(IRuntimesBuilder b) {
        Map<String, IRuntime<?, ?>> runtimes = b.build();
        assertEquals(1, runtimes.size());
        return cast(runtimes.get("two-steps-runtime"));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object o) {
        return (T) o;
    }

    @Test
    public void testNominal() {

        IRuntime<String, String> runtime = get(builder().runtime("two-steps-runtime", String.class, String.class)
                .variable("step-one-variable", "step-one-variable")
                .variable("output-step-variable", "output-step-variable").up());

        IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

        assertEquals(222, result.code());
        assertEquals("test-step-one-processed-step-one-variable-output-step-processed-output-step-variable",
                result.output());
    }

    @Test
    public void testCatchedExceptionInOutputStep() {

        IRuntime<String, String> runtime = get(builder().runtime("two-steps-runtime", String.class, String.class)
                .variable("step-one-variable", "step-one-variable").variable("output-step-variable", "di-exception")
                .up());

        IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

        assertEquals(444, result.code());
        assertEquals("test-step-one-processed-step-one-variable-output-step-fallback", result.output());
    }

    @Test
    public void testCatchedExceptionInStepOneStep() {

        IRuntime<String, String> runtime = get(builder().runtime("two-steps-runtime", String.class, String.class)
                .variable("step-one-variable", "di-excpetion").variable("output-step-variable", "di-exception").up());

        IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

        assertEquals(444, result.code());
        assertEquals("test-step-one-processed-di-excpetion-output-step-fallback", result.output());
    }

    @Test
    public void testUncatchedExceptionInStepOneStep_abortOnUncatchedException_false() {

        IRuntime<String, String> runtime = get(builder().runtime("two-steps-runtime", String.class, String.class)
                .variable("step-one-variable", "custom-excpetion")
                .variable("output-step-variable", "output-step-variable").up());

        IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

        assertEquals(222, result.code());
        assertEquals("test-step-one-processed-custom-excpetion-output-step-processed-output-step-variable",
                result.output());
    }

    @Test
    public void testUncatchedExceptionInOutputStep_abortOnUncatchedException_true() {

        IRuntime<String, String> runtime = get(builder().runtime("two-steps-runtime", String.class, String.class)
                .variable("step-one-variable", "step-one-variable").variable("output-step-variable", "custom-exception")
                .up());

        IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

        assertEquals(50, result.code());
        assertEquals(null, result.output());
    }
}
