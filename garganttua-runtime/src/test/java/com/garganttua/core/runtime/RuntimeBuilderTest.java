package com.garganttua.core.runtime;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.runtime.RuntimeContext.*;
import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @Test
    public void builtAutoDetectedRuntimesShouldContain(){
    IRuntimesBuilder t = RuntimesBuilder.builder();

        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        assertTrue(t.context(contextBuilder).autoDetect(true).build().containsKey("runtime-1"));
        assertTrue(t.context(contextBuilder).autoDetect(true).build().containsKey("RuntimeWithCatchedExceptionAndHandledByFallback"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleRuntimeBuilderTest() {

        DummyRuntimeProcessStep step = new DummyRuntimeProcessStep();
        IRuntimesBuilder t = RuntimesBuilder.builder();
        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder)
                .runtime("runtime-1", String.class, String.class)
                .variable("variable", of("preset-variable"))
                .stage("stage-1")
                .step("step-1", FixedObjectSupplierBuilder.of(step), String.class)
                .method()
                .condition(custom(of(10), i -> 1 > 0))
                .output(true)
                .variable("method-returned")
                .method("method")
                .katch(DiException.class).code(401).up()
                .withParam(input(String.class))
                .withParam(FixedObjectSupplierBuilder.of("fixed-value-in-method"))
                .withParam(variable("variable", String.class))
                .withParam(context()).up()
                .fallBack()
                .onException(DiException.class).up()
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

        assertDoesNotThrow(() -> {
            Optional<IRuntimeResult<String,String>> result = runtime.execute("input");
            assertEquals("input-processed-fixed-value-in-method-preset-variable", result.get().output());
    
            System.out.println(result.get().output());
            System.out.println(result.get().prettyDuration());
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCatchedExceptionThrownByStepAndHandledByFallback() {

        DummyRuntimeProcessStep step = new DummyRuntimeProcessStep();
        IRuntimesBuilder t = RuntimesBuilder.builder();
        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder)
                .runtime("runtime-1", String.class, String.class)
                .variable("variable", of("di-exception"))
                .stage("stage-1")
                .step("step-1", FixedObjectSupplierBuilder.of(step), String.class)
                .method()
                .condition(custom(of(10), i -> 1 > 0))
                .output(true)
                .variable("method-returned")
                .method("method")
                .katch(DiException.class).code(401).up()
                .withParam(input(String.class))
                .withParam(FixedObjectSupplierBuilder.of("fixed-value-in-method"))
                .withParam(variable("variable", String.class))
                .withParam(context()).up()
                .fallBack()
                .onException(DiException.class).up()
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

        assertDoesNotThrow(() -> {
            Optional<IRuntimeResult<String,String>> result = runtime.execute("input");
            assertEquals("input-fallback-fixed-value-in-method-401-input-processed-fixed-value-in-method-di-exception", result.get().output());
    
            System.out.println(result.get().output());
            System.out.println(result.get().prettyDuration());
        });

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAutoDetectionBuild() throws InterruptedException, ExecutionException {

        IRuntimesBuilder t = RuntimesBuilder.builder();

        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder).autoDetect(true).build();
        IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");

        Optional<IRuntimeResult<String,String>> result = runtime.execute("input");

        assertEquals("input-processed-fixed-value-in-method-preset-variable", result.get().output());
        assertEquals(201, result.get().code());

        System.out.println(result.get().output());
        System.out.println(result.get().prettyDuration());

    }

}
