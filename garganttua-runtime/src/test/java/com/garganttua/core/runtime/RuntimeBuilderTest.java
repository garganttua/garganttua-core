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
                    .variable("variable", of("preset-variable"))
                    .stage("stage-1")
                    .step("step-1", FixedObjectSupplierBuilder.of(step), String.class)
                    .condition(custom(of(10), i -> 1 > 0))
                    .method()
                    .output(true)
                    .variable("method-returned")
                    .method("method")
                    .withParam(input(String.class))
                    .withParam(FixedObjectSupplierBuilder.of("fixed-value-in-method"))
                    .withParam(variable("variable", String.class))
                    .withParam(context()).up()
                    .katch(DiException.class).code(401).fallback(true).abort(true).up()
                    .fallBack()
                    .output(true)
                    .variable("fallback-returned")
                    .method("fallbackMethod")
                    .withParam(input(String.class))
                    .withParam(FixedObjectSupplierBuilder.of("fixed-value-in-method"))
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
            assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());
        });

    }

    @SuppressWarnings("unchecked")
    //@Test
    public void testAutoDetectionBuild() {
        IRuntimesBuilder t = RuntimesBuilder.builder();

        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder).autoDetect(true).build();
        IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
        IRuntimeResult<String, String> result = runtime.execute("input");
        assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());

        System.out.println(result.getPrettyDuration());

    }

}
