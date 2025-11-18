package com.garganttua.core.runtime;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.runtime.RuntimeContext.*;
import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

    @SuppressWarnings("unchecked")
    // @Test
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
    @Test
    public void testAutoDetectionBuild() throws InterruptedException, ExecutionException {
        IRuntimesBuilder t = RuntimesBuilder.builder();

        IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                .withPackage("com.garganttua.core.runtime");
        contextBuilder.build().onInit().onStart();

        Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder).autoDetect(true).build();
        IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");

        IRuntimeResult<String, String> result = runtime.execute("input");

        assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());

        System.out.println(result.output());
        System.out.println(result.getPrettyDuration());

        /*
         * 
         */

        int runs = 10;
        int threads = java.lang.Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Long>> tasks = new ArrayList<>();
        AtomicInteger inte = new AtomicInteger(0);

        for (int i = 0; i < runs; i++) {
            tasks.add(() -> {
                IRuntimeResult<String, String> r = runtime.execute("input-" + inte.getAndIncrement());
                System.out.println(r.output());
                System.out.println(r.getPrettyDurationInNanos());
                return r.getDurationInNanos();
            });
        }

        List<Future<Long>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // Collecte des durations
        long total = 0;
        long min = 10000000;
        long max = 0;

        for (Future<Long> f : futures) {
            long d = f.get(); // on récupère directement le long
            total = total + d; // addition au total

            if (d < min)
                min = d; // mise à jour du min
            if (d > max)
                max = d; // mise à jour du max
        }

        Long avg = total/runs;

        // Affichage pretty-color
        System.out.println("Moyenne : " + RuntimeResult.prettyNano(avg));
        System.out.println("Min     : " + RuntimeResult.prettyNano(min));
        System.out.println("Max     : " + RuntimeResult.prettyNano(max));

    }

}
