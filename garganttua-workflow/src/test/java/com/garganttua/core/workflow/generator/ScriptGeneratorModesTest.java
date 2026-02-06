package com.garganttua.core.workflow.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.WorkflowInput;
import com.garganttua.core.workflow.WorkflowResult;
import com.garganttua.core.workflow.dsl.WorkflowBuilder;

/**
 * Tests demonstrating the generated scripts in both inline and include modes,
 * using ALL resource scripts from src/test/resources/scripts/.
 *
 * <p>
 * <b>Include mode</b> (default for file-based scripts): generates
 * {@code include()} + {@code execute_script()} + {@code script_variable()} pattern.
 * The child script is loaded/compiled at runtime and executed with positional arguments.
 * </p>
 *
 * <p>
 * <b>Inline mode</b> (via {@code .inline()} or {@code inlineAll}): reads the file
 * content and embeds it directly in the generated script. Positional variable references
 * ({@code @0}, {@code @1}) are replaced with named input variables.
 * </p>
 *
 * <p>
 * Pipeline: fetch-api -> validate-data -> transform-data -> calculate-stats -> finalize-output
 * </p>
 */
class ScriptGeneratorModesTest {

    private static final Path GENERATED_DIR = Path.of("src/test/resources/scripts/generated");

    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupClass() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    @BeforeEach
    void setup() {
        injectionContextBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage("com.garganttua.core.runtime");

        expressionContextBuilder = ExpressionContextBuilder.builder();
        expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

        injectionContextBuilder.build().onInit().onStart();
        expressionContextBuilder.build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Full pipeline with ALL 5 scripts — INCLUDE MODE
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void testAllScripts_IncludeMode() throws Exception {
        Path fetchPath = copyResourceToTemp("scripts/fetch-api.gs");
        Path validatePath = copyResourceToTemp("scripts/validate-data.gs");
        Path transformPath = copyResourceToTemp("scripts/transform-data.gs");
        Path statsPath = copyResourceToTemp("scripts/calculate-stats.gs");
        Path finalizePath = copyResourceToTemp("scripts/finalize-output.gs");

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("full-pipeline")
                .variable("apiUrl", "https://api.example.com/data")
                .variable("requestTimeout", 30000)
                .variable("targetFormat", "json")

                // Stage 1: Fetch API
                .stage("fetch")
                    .script(fetchPath)
                        .name("api-fetcher")
                        .input("url", "@apiUrl")
                        .input("timeout", "@requestTimeout")
                        .output("rawData", "apiResponse")
                        .output("httpCode", "httpStatus")
                        .up()
                    .up()

                // Stage 2: Validate Data
                .stage("validation")
                    .script(validatePath)
                        .name("data-validator")
                        .input("data", "@rawData")
                        .input("strict", "true")
                        .output("validated", "validatedData")
                        .output("validationStatus", "validationStatus")
                        .up()
                    .up()

                // Stage 3: Transform Data
                .stage("transform")
                    .script(transformPath)
                        .name("data-transformer")
                        .input("inputData", "@validated")
                        .input("format", "@targetFormat")
                        .output("transformed", "transformedData")
                        .up()
                    .up()

                // Stage 4: Calculate Statistics
                .stage("statistics")
                    .script(statsPath)
                        .name("stats-calculator")
                        .input("values", "@transformed")
                        .output("sum", "total")
                        .output("count", "itemCount")
                        .up()
                    .up()

                // Stage 5: Finalize Output
                .stage("finalize")
                    .script(finalizePath)
                        .name("finalizer")
                        .input("data", "@transformed")
                        .input("metadata", "\"pipeline-v1\"")
                        .output("result", "finalOutput")
                        .output("finalStatus", "finalStatus")
                        .up()
                    .up()

                .build();

        // Get generated script and write to file
        String generated = workflow.getGeneratedScript();
        Files.createDirectories(GENERATED_DIR);
        Path outputFile = GENERATED_DIR.resolve("full-pipeline-include.gs");
        Files.writeString(outputFile, generated);
        System.out.println("Generated script written to: " + outputFile.toAbsolutePath());

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  ALL 5 SCRIPTS — INCLUDE MODE");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(generated);
        System.out.println("═══════════════════════════════════════════════════════════════");

        // ── Preset variables ──
        assertTrue(generated.contains("apiUrl <- \"https://api.example.com/data\""),
                "Should have preset apiUrl");
        assertTrue(generated.contains("requestTimeout <- 30000"),
                "Should have preset requestTimeout");
        assertTrue(generated.contains("targetFormat <- \"json\""),
                "Should have preset targetFormat");

        // ── Stage 1: fetch (fetch-api.gs) ──
        assertTrue(generated.contains("# Stage: fetch"),
                "Should have fetch stage header");
        assertTrue(generated.contains("url <- @apiUrl"),
                "Fetch: input mapping url");
        assertTrue(generated.contains("timeout <- @requestTimeout"),
                "Fetch: input mapping timeout");
        assertTrue(generated.contains("include("),
                "Fetch: should include() script");
        assertTrue(generated.contains("execute_script("),
                "Fetch: should execute_script()");
        assertTrue(generated.contains("rawData <- script_variable("),
                "Fetch: output rawData via script_variable");
        assertTrue(generated.contains("httpCode <- script_variable("),
                "Fetch: output httpCode via script_variable");

        // ── Stage 2: validation (validate-data.gs) ──
        assertTrue(generated.contains("# Stage: validation"),
                "Should have validation stage header");
        assertTrue(generated.contains("data <- @rawData"),
                "Validation: input mapping data from upstream rawData");
        assertTrue(generated.contains("strict <- true"),
                "Validation: input mapping strict");
        assertTrue(generated.contains("validated <- script_variable("),
                "Validation: output validated via script_variable");

        // ── Stage 3: transform (transform-data.gs) ──
        assertTrue(generated.contains("# Stage: transform"),
                "Should have transform stage header");
        assertTrue(generated.contains("inputData <- @validated"),
                "Transform: input mapping from upstream");
        assertTrue(generated.contains("format <- @targetFormat"),
                "Transform: input mapping format from preset");
        assertTrue(generated.contains("transformed <- script_variable("),
                "Transform: output transformed via script_variable");

        // ── Stage 4: statistics (calculate-stats.gs) ──
        assertTrue(generated.contains("# Stage: statistics"),
                "Should have statistics stage header");
        assertTrue(generated.contains("values <- @transformed"),
                "Statistics: input mapping from upstream");
        assertTrue(generated.contains("sum <- script_variable("),
                "Statistics: output sum via script_variable");
        assertTrue(generated.contains("count <- script_variable("),
                "Statistics: output count via script_variable");

        // ── Stage 5: finalize (finalize-output.gs) ──
        assertTrue(generated.contains("# Stage: finalize"),
                "Should have finalize stage header");
        assertTrue(generated.contains("data <- @transformed"),
                "Finalize: input mapping data");
        assertTrue(generated.contains("metadata <- \"pipeline-v1\""),
                "Finalize: input mapping metadata literal");
        assertTrue(generated.contains("result <- script_variable("),
                "Finalize: output result via script_variable");
        assertTrue(generated.contains("finalStatus <- script_variable("),
                "Finalize: output finalStatus via script_variable");

        // ── Global: no embedded script bodies ──
        assertFalse(generated.contains("validationStatus <- \"pending\""),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("fetchComplete <- true"),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("processingStep <- "),
                "Include mode should not embed any script body");

        // ── Execute workflow ──
        WorkflowResult result = workflow.execute(WorkflowInput.of("test-data"));

        System.out.println("EXECUTION RESULT: success=" + result.isSuccess() + " code=" + result.code());
        System.out.println("Variables: " + result.variables());
        result.exception().ifPresent(e -> {
            System.out.println("Exception: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
        });

        assertTrue(result.isSuccess(), "Workflow should succeed: "
                + result.exception().map(Throwable::getMessage).orElse("code=" + result.code()));
        assertEquals(0, result.code());
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Full pipeline with ALL 5 scripts — INLINE MODE
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void testAllScripts_InlineMode() throws Exception {
        Path fetchPath = copyResourceToTemp("scripts/fetch-api.gs");
        Path validatePath = copyResourceToTemp("scripts/validate-data.gs");
        Path transformPath = copyResourceToTemp("scripts/transform-data.gs");
        Path statsPath = copyResourceToTemp("scripts/calculate-stats.gs");
        Path finalizePath = copyResourceToTemp("scripts/finalize-output.gs");

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("full-pipeline")
                .variable("apiUrl", "https://api.example.com/data")
                .variable("requestTimeout", 30000)
                .variable("targetFormat", "json")

                // Stage 1: Fetch API (inlined)
                .stage("fetch")
                    .script(fetchPath)
                        .name("api-fetcher")
                        .inline()
                        .input("url", "@apiUrl")
                        .input("timeout", "@requestTimeout")
                        .output("rawData", "apiResponse")
                        .output("httpCode", "httpStatus")
                        .up()
                    .up()

                // Stage 2: Validate Data (inlined)
                .stage("validation")
                    .script(validatePath)
                        .name("data-validator")
                        .inline()
                        .input("data", "@rawData")
                        .input("strict", "true")
                        .output("validated", "validatedData")
                        .output("validationStatus", "validationStatus")
                        .up()
                    .up()

                // Stage 3: Transform Data (inlined)
                .stage("transform")
                    .script(transformPath)
                        .name("data-transformer")
                        .inline()
                        .input("inputData", "@validated")
                        .input("format", "@targetFormat")
                        .output("transformed", "transformedData")
                        .up()
                    .up()

                // Stage 4: Calculate Statistics (inlined)
                .stage("statistics")
                    .script(statsPath)
                        .name("stats-calculator")
                        .inline()
                        .input("values", "@transformed")
                        .output("sum", "total")
                        .output("count", "itemCount")
                        .up()
                    .up()

                // Stage 5: Finalize Output (inlined)
                .stage("finalize")
                    .script(finalizePath)
                        .name("finalizer")
                        .inline()
                        .input("data", "@transformed")
                        .input("metadata", "\"pipeline-v1\"")
                        .output("result", "finalOutput")
                        .output("finalStatus", "finalStatus")
                        .up()
                    .up()

                .build();

        // Get generated script and write to file
        String generated = workflow.getGeneratedScript();
        Files.createDirectories(GENERATED_DIR);
        Path outputFile = GENERATED_DIR.resolve("full-pipeline-inline.gs");
        Files.writeString(outputFile, generated);
        System.out.println("Generated script written to: " + outputFile.toAbsolutePath());

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  ALL 5 SCRIPTS — INLINE MODE");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(generated);
        System.out.println("═══════════════════════════════════════════════════════════════");

        // ── Preset variables ──
        assertTrue(generated.contains("apiUrl <- \"https://api.example.com/data\""),
                "Should have preset apiUrl");
        assertTrue(generated.contains("requestTimeout <- 30000"),
                "Should have preset requestTimeout");
        assertTrue(generated.contains("targetFormat <- \"json\""),
                "Should have preset targetFormat");

        // ── Stage 1: fetch (inlined, @0/@1 replaced) ──
        assertTrue(generated.contains("# Stage: fetch"),
                "Should have fetch stage header");
        assertTrue(generated.contains("url <- @apiUrl"),
                "Fetch: input mapping url");
        assertTrue(generated.contains("timeout <- @requestTimeout"),
                "Fetch: input mapping timeout");
        assertTrue(generated.contains("apiResponse <- \"fetched data from \" + @url"),
                "Fetch: @0 in body replaced with @url");
        assertTrue(generated.contains("httpStatus <- 200"),
                "Fetch: embedded script body");
        assertTrue(generated.contains("rawData <- @apiResponse"),
                "Fetch: inline output mapping rawData");
        assertTrue(generated.contains("httpCode <- @httpStatus"),
                "Fetch: inline output mapping httpCode");

        // ── Stage 2: validation (inlined, @0/@1 replaced) ──
        assertTrue(generated.contains("# Stage: validation"),
                "Should have validation stage header");
        assertTrue(generated.contains("data <- @rawData"),
                "Validation: input mapping data from upstream rawData");
        assertTrue(generated.contains("strict <- true"),
                "Validation: input mapping strict");
        assertTrue(generated.contains("validatedData <- @data"),
                "Validation: @0 in body replaced with @data");
        assertTrue(generated.contains("validated <- @validatedData"),
                "Validation: inline output mapping validated");

        // ── Stage 3: transform (inlined, @0/@1 replaced) ──
        assertTrue(generated.contains("# Stage: transform"),
                "Should have transform stage header");
        assertTrue(generated.contains("inputData <- @validated"),
                "Transform: input mapping from upstream");
        assertTrue(generated.contains("format <- @targetFormat"),
                "Transform: input mapping format from preset");
        assertTrue(generated.contains("transformedData <- @inputData"),
                "Transform: @0 in body replaced with @inputData");
        assertTrue(generated.contains("outputFormat <- @format"),
                "Transform: @1 in body replaced with @format");
        assertTrue(generated.contains("transformed <- @transformedData"),
                "Transform: inline output mapping transformed");

        // ── Stage 4: statistics (inlined, @0 replaced) ──
        assertTrue(generated.contains("# Stage: statistics"),
                "Should have statistics stage header");
        assertTrue(generated.contains("values <- @transformed"),
                "Statistics: input mapping from upstream");
        assertTrue(generated.contains("result <- @values"),
                "Statistics: @0 in body replaced with @values");
        assertTrue(generated.contains("total <- 0"),
                "Statistics: embedded script body");
        assertTrue(generated.contains("itemCount <- 0"),
                "Statistics: embedded script body");
        assertTrue(generated.contains("sum <- @total"),
                "Statistics: inline output mapping sum");
        assertTrue(generated.contains("count <- @itemCount"),
                "Statistics: inline output mapping count");

        // ── Stage 5: finalize (inlined, @0/@1 replaced) ──
        assertTrue(generated.contains("# Stage: finalize"),
                "Should have finalize stage header");
        assertTrue(generated.contains("data <- @transformed"),
                "Finalize: input mapping data");
        assertTrue(generated.contains("metadata <- \"pipeline-v1\""),
                "Finalize: input mapping metadata literal");
        assertTrue(generated.contains("finalOutput <- @data"),
                "Finalize: @0 in body replaced with @data");
        assertTrue(generated.contains("finalStatus <- \"success\""),
                "Finalize: embedded script body");
        assertTrue(generated.contains("result <- @finalOutput"),
                "Finalize: inline output mapping result");

        // ── Global: no @0/@1 remaining, no include pattern ──
        assertFalse(generated.contains("@0"),
                "No @0 positional references should remain after replacement");
        assertFalse(generated.contains("include("),
                "Inline mode should not use include()");
        assertFalse(generated.contains("execute_script("),
                "Inline mode should not use execute_script()");
        assertFalse(generated.contains("script_variable("),
                "Inline mode should not use script_variable()");

        // ── Execute workflow ──
        WorkflowResult result = workflow.execute(WorkflowInput.of("test-data"));

        System.out.println("EXECUTION RESULT: success=" + result.isSuccess() + " code=" + result.code());
        System.out.println("Variables: " + result.variables());
        result.exception().ifPresent(e -> {
            System.out.println("Exception: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
        });

        assertTrue(result.isSuccess(), "Workflow should succeed: "
                + result.exception().map(Throwable::getMessage).orElse("code=" + result.code()));
        assertEquals(0, result.code());
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Helper
    // ──────────────────────────────────────────────────────────────────────

    private Path copyResourceToTemp(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String fileName = Path.of(resourcePath).getFileName().toString();
            Path targetPath = tempDir.resolve(fileName);
            Files.writeString(targetPath, content);
            return targetPath;
        }
    }
}
