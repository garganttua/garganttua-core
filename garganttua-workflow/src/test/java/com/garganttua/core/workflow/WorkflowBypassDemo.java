package com.garganttua.core.workflow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.annotation.processor.IndexedAnnotationScanner;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.workflow.dsl.WorkflowBuilder;

class WorkflowBypassDemo {

    private static IReflectionBuilder reflectionBuilder;

    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setupClass() throws Exception {
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new IndexedAnnotationScanner());
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
    void demonstrateBypassFlow() {
        var workflowBuilder = (com.garganttua.core.workflow.dsl.WorkflowBuilder) WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("Data Pipeline with Bypass")
                .variable("apiUrl", "https://api.example.com")

                // Stage 1: Fetch - produces rawData and metadata
                .stage("fetch")
                    .script("rawData <- \"fetched\"\nmetadata <- \"meta-info\"")
                        .name("api-fetcher")
                        .description("Fetches data from API and extracts metadata")
                        .input("url", "@apiUrl")
                        .output("rawData", "rawData")      // Used by stage 2
                        .output("metadata", "metadata")    // Used directly by stage 3 (bypass)
                        .up()
                    .up()

                // Stage 2: Transform - uses rawData, produces transformed
                .stage("transform")
                    .script("transformed <- @input")
                        .name("data-transformer")
                        .description("Transforms raw data to target format")
                        .input("input", "@rawData")        // From stage 1
                        .output("transformed", "transformed")
                        .up()
                    .up()

                // Stage 3: Finalize - uses transformed AND metadata (bypass from stage 1)
                .stage("finalize")
                    .script("result <- @data")
                        .name("finalizer")
                        .description("Finalizes output with metadata from stage 1")
                        .input("data", "@transformed")     // From stage 2
                        .input("meta", "@metadata")        // BYPASS: directly from stage 1!
                        .output("finalResult", "result")
                        .up()
                    .up();

        System.out.println(workflowBuilder.describeWorkflow());
    }
}
