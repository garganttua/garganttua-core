package com.garganttua.core.runtime;

import static com.garganttua.core.runtime.RuntimeContext.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;

class RuntimeBuilderTest {

    @BeforeAll
    public static void setup() {
        ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleRuntimeBuilderTest() {

        assertDoesNotThrow(() -> {
            DummyRuntimeProcessStep step = new DummyRuntimeProcessStep();
            IRuntimesBuilder t = RuntimesBuilder.builder();
            IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true).withPackage("com.garganttua");
            contextBuilder.build().onInit().onStart();

            Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder)
                    .runtime("runtime-1", String.class, String.class)
                    .stage("stage-1")
                    .step("step-1", FixedObjectSupplierBuilder.of(step), String.class)
                    .condition(custom(of(10), i -> 1 > 0))
                    .method()
                    .output(true)
                    .variable("method-returned")
                    .method("method")
                    .withParam(input(String.class))
                    .withParam(FixedObjectSupplierBuilder.of("input-parameter"))
                    .withParam(variable("variable", String.class))
                    .withParam(context()).up()
                    .katch(DiException.class).code(401).fallback(true).abort(true).up()
                    .fallBack()
                    .output(true)
                    .variable("fallback-returned")
                    .method("fallbackMethod")
                    .withParam(FixedObjectSupplierBuilder.of("input-parameter"))
                    .withParam(exception(DiException.class))
                    .withParam(code())
                    .withParam(exceptionMessage())
                    .withParam(context()).up()
                    .up().up().up()
                    .build();

            assertNotNull(runtimes);
            assertEquals(1, runtimes.size());

            IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
            IRuntimeResult<String, String> result = runtime.execute("input");
            assertEquals("", result.getOutput());
        });

    }

    @Test
    public void testAutoDetectionBuild() {
        IRuntimesBuilder t = RuntimesBuilder.builder();

        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder).autoDetect(true).build();

    }

    /* @Test */
    public void diContextNotBuildShoudlPreventRuntimesBuilding() {
        /*
         * DslException exception = assertThrows(DslException.class, () -> {
         * DummyRuntimeProcessStep step = new DummyRuntimeProcessStep();
         * IRuntimesBuilder t = RuntimesBuilder.builder();
         * IDiContextBuilder contextBuilder =
         * DiContext.builder().autoDetect(true).withPackage("com.garganttua");
         * 
         * Map<String, IRuntime<?,?>> runtimes =
         * t.context(contextBuilder).runtime("runtime-1", String.class, String.class)
         * .stage("stage-1")
         * .step("step-1")
         * .object(FixedObjectSupplierBuilder.of(step), String.class)
         * .method("method").withParam("input-parameter").storeReturn(
         * "stage-1-step-1-returned").up().up().up()
         * .up().build();
         * });
         * assertEquals("Build is not yet authorized", exception.getMessage());
         */
    }

    /*
     * @Test
     * public void exceptionThrownIfNoContext() throws CoreException {
     * 
     * NullPointerException exception = assertThrows(NullPointerException.class, ()
     * -> {
     * new RuntimesBuilder().runtime("test");
     * });
     * 
     * assertEquals("Context cannot be null", exception.getMessage());
     * }
     */
}
