package com.garganttua.core.workflow.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.chaining.CodeAction;

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

    private ScriptGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        generator = new ScriptGenerator();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Full pipeline with ALL 5 scripts — INCLUDE MODE
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void testAllScripts_IncludeMode() throws Exception {
        List<WorkflowStage> stages = buildAllStages(false);

        Map<String, Object> presets = new LinkedHashMap<>();
        presets.put("apiUrl", "https://api.example.com/data");
        presets.put("requestTimeout", 30000);
        presets.put("targetFormat", "json");

        String generated = generator.generate("full-pipeline", stages, presets, false);

        // Store generated script to file
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
        assertTrue(generated.contains("_fetch_api_fetcher_ref <- include("),
                "Fetch: should include() script");
        assertTrue(generated.contains("execute_script(@_fetch_api_fetcher_ref, @url, @timeout)"),
                "Fetch: should execute_script() with inputs");
        assertTrue(generated.contains("! => handleFetchError(@exception)"),
                "Fetch: should have catch clause");
        assertTrue(generated.contains("equals(@_fetch_api_fetcher_code, 1) => abort()"),
                "Fetch: code 1 should abort");
        assertTrue(generated.contains("equals(@_fetch_api_fetcher_code, 2) => retry(3, @_current_script)"),
                "Fetch: code 2 should retry");
        assertTrue(generated.contains("rawData <- script_variable(@_fetch_api_fetcher_ref, \"apiResponse\")"),
                "Fetch: output rawData from apiResponse");
        assertTrue(generated.contains("httpCode <- script_variable(@_fetch_api_fetcher_ref, \"httpStatus\")"),
                "Fetch: output httpCode from httpStatus");

        // ── Stage 2: validation (validate-data.gs) ──
        assertTrue(generated.contains("# Stage: validation"),
                "Should have validation stage header");
        assertTrue(generated.contains("data <- @rawData"),
                "Validation: input mapping data from upstream rawData");
        assertTrue(generated.contains("strict <- true"),
                "Validation: input mapping strict");
        assertTrue(generated.contains("_validation_data_validator_ref <- include("),
                "Validation: should include() script");
        assertTrue(generated.contains("execute_script(@_validation_data_validator_ref, @data, @strict)"),
                "Validation: should execute_script() with inputs");
        assertTrue(generated.contains("equals(@_validation_data_validator_code, 1) => abort()"),
                "Validation: code 1 should abort");
        assertTrue(generated.contains("equals(@_validation_data_validator_code, 2) => abort()"),
                "Validation: code 2 should abort");
        assertTrue(generated.contains("validated <- script_variable(@_validation_data_validator_ref, \"validatedData\")"),
                "Validation: output validated from validatedData");
        assertTrue(generated.contains("validationStatus <- script_variable(@_validation_data_validator_ref, \"validationStatus\")"),
                "Validation: output validationStatus");

        // ── Stage 3: transform (transform-data.gs) ──
        assertTrue(generated.contains("# Stage: transform"),
                "Should have transform stage header");
        assertTrue(generated.contains("inputData <- @validated"),
                "Transform: input mapping from upstream validated");
        assertTrue(generated.contains("format <- @targetFormat"),
                "Transform: input mapping format from preset");
        assertTrue(generated.contains("_transform_data_transformer_ref <- include("),
                "Transform: should include() script");
        assertTrue(generated.contains("execute_script(@_transform_data_transformer_ref, @inputData, @format)"),
                "Transform: should execute_script() with inputs");
        assertTrue(generated.contains("transformed <- script_variable(@_transform_data_transformer_ref, \"transformedData\")"),
                "Transform: output transformed from transformedData");

        // ── Stage 4: statistics (calculate-stats.gs) ──
        assertTrue(generated.contains("# Stage: statistics"),
                "Should have statistics stage header");
        assertTrue(generated.contains("values <- @transformed"),
                "Statistics: input mapping from upstream transformed");
        assertTrue(generated.contains("_statistics_stats_calculator_ref <- include("),
                "Statistics: should include() script");
        assertTrue(generated.contains("execute_script(@_statistics_stats_calculator_ref, @values)"),
                "Statistics: should execute_script() with single input");
        assertTrue(generated.contains("sum <- script_variable(@_statistics_stats_calculator_ref, \"total\")"),
                "Statistics: output sum from total");
        assertTrue(generated.contains("count <- script_variable(@_statistics_stats_calculator_ref, \"itemCount\")"),
                "Statistics: output count from itemCount");

        // ── Stage 5: finalize (finalize-output.gs) ──
        assertTrue(generated.contains("# Stage: finalize"),
                "Should have finalize stage header");
        assertTrue(generated.contains("data <- @transformed"),
                "Finalize: input mapping data from upstream");
        assertTrue(generated.contains("metadata <- \"pipeline-v1\""),
                "Finalize: input mapping metadata literal");
        assertTrue(generated.contains("_finalize_finalizer_ref <- include("),
                "Finalize: should include() script");
        assertTrue(generated.contains("execute_script(@_finalize_finalizer_ref, @data, @metadata)"),
                "Finalize: should execute_script() with inputs");
        assertTrue(generated.contains("result <- script_variable(@_finalize_finalizer_ref, \"finalOutput\")"),
                "Finalize: output result from finalOutput");
        assertTrue(generated.contains("finalStatus <- script_variable(@_finalize_finalizer_ref, \"finalStatus\")"),
                "Finalize: output finalStatus");

        // ── Global: no embedded script bodies ──
        assertFalse(generated.contains("validationStatus <- \"pending\""),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("apiResponse <- "),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("processingStep <- "),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("total <- 0"),
                "Include mode should not embed any script body");
        assertFalse(generated.contains("finalOutput <- @"),
                "Include mode should not embed any script body");
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Full pipeline with ALL 5 scripts — INLINE MODE
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void testAllScripts_InlineMode() throws Exception {
        List<WorkflowStage> stages = buildAllStages(true);

        Map<String, Object> presets = new LinkedHashMap<>();
        presets.put("apiUrl", "https://api.example.com/data");
        presets.put("requestTimeout", 30000);
        presets.put("targetFormat", "json");

        String generated = generator.generate("full-pipeline", stages, presets, false);

        // Store generated script to file
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

        // ── Stage 1: fetch (fetch-api.gs inlined) ──
        assertTrue(generated.contains("# Stage: fetch"),
                "Should have fetch stage header");
        assertTrue(generated.contains("url <- @apiUrl"),
                "Fetch: input mapping url");
        assertTrue(generated.contains("timeout <- @requestTimeout"),
                "Fetch: input mapping timeout");
        // @0 replaced by @url in script body
        assertTrue(generated.contains("apiResponse <- \"fetched data from \" + @url"),
                "Fetch: @0 in body replaced with @url");
        assertTrue(generated.contains("httpStatus <- 200"),
                "Fetch: embedded script body");
        // Output mappings (workflowVar <- @scriptVar)
        assertTrue(generated.contains("rawData <- @apiResponse"),
                "Fetch: inline output mapping rawData");
        assertTrue(generated.contains("httpCode <- @httpStatus"),
                "Fetch: inline output mapping httpCode");

        // ── Stage 2: validation (validate-data.gs inlined) ──
        assertTrue(generated.contains("# Stage: validation"),
                "Should have validation stage header");
        assertTrue(generated.contains("data <- @rawData"),
                "Validation: input mapping data from upstream rawData");
        assertTrue(generated.contains("strict <- true"),
                "Validation: input mapping strict");
        // @0 replaced by @data in script body
        assertTrue(generated.contains("validatedData <- @data"),
                "Validation: @0 in body replaced with @data");
        assertTrue(generated.contains("@data == null |"),
                "Validation: @0 in condition replaced with @data");
        assertTrue(generated.contains("validationStatus <- \"pending\""),
                "Validation: embedded script body");
        // Output mappings
        assertTrue(generated.contains("validated <- @validatedData"),
                "Validation: inline output mapping validated");

        // ── Stage 3: transform (transform-data.gs inlined) ──
        assertTrue(generated.contains("# Stage: transform"),
                "Should have transform stage header");
        assertTrue(generated.contains("inputData <- @validated"),
                "Transform: input mapping from upstream");
        assertTrue(generated.contains("format <- @targetFormat"),
                "Transform: input mapping format from preset");
        // @0 replaced by @inputData, @1 replaced by @format
        assertTrue(generated.contains("transformedData <- @inputData"),
                "Transform: @0 in body replaced with @inputData");
        assertTrue(generated.contains("outputFormat <- @format"),
                "Transform: @1 in body replaced with @format");
        // Output mapping
        assertTrue(generated.contains("transformed <- @transformedData"),
                "Transform: inline output mapping transformed");

        // ── Stage 4: statistics (calculate-stats.gs inlined) ──
        assertTrue(generated.contains("# Stage: statistics"),
                "Should have statistics stage header");
        assertTrue(generated.contains("values <- @transformed"),
                "Statistics: input mapping from upstream");
        // @0 replaced by @values
        assertTrue(generated.contains("result <- @values"),
                "Statistics: @0 in body replaced with @values");
        assertTrue(generated.contains("total <- 0"),
                "Statistics: embedded script body");
        assertTrue(generated.contains("itemCount <- 0"),
                "Statistics: embedded script body");
        // Output mappings
        assertTrue(generated.contains("sum <- @total"),
                "Statistics: inline output mapping sum");
        assertTrue(generated.contains("count <- @itemCount"),
                "Statistics: inline output mapping count");

        // ── Stage 5: finalize (finalize-output.gs inlined) ──
        assertTrue(generated.contains("# Stage: finalize"),
                "Should have finalize stage header");
        assertTrue(generated.contains("data <- @transformed"),
                "Finalize: input mapping data");
        assertTrue(generated.contains("metadata <- \"pipeline-v1\""),
                "Finalize: input mapping metadata literal");
        // @0 replaced by @data
        assertTrue(generated.contains("finalOutput <- @data"),
                "Finalize: @0 in body replaced with @data");
        assertTrue(generated.contains("finalStatus <- \"success\""),
                "Finalize: embedded script body");
        // Output mappings
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
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Helper: builds the 5-stage pipeline
    // ──────────────────────────────────────────────────────────────────────

    private List<WorkflowStage> buildAllStages(boolean inline) throws Exception {
        Path fetchPath = copyResourceToTemp("scripts/fetch-api.gs");
        Path validatePath = copyResourceToTemp("scripts/validate-data.gs");
        Path transformPath = copyResourceToTemp("scripts/transform-data.gs");
        Path statsPath = copyResourceToTemp("scripts/calculate-stats.gs");
        Path finalizePath = copyResourceToTemp("scripts/finalize-output.gs");

        // Stage 1: Fetch API
        Map<String, String> fetchInputs = new LinkedHashMap<>();
        fetchInputs.put("url", "@apiUrl");
        fetchInputs.put("timeout", "@requestTimeout");

        Map<String, String> fetchOutputs = new LinkedHashMap<>();
        fetchOutputs.put("rawData", "apiResponse");
        fetchOutputs.put("httpCode", "httpStatus");

        WorkflowScript fetchScript = WorkflowScript.builder()
                .name("api-fetcher")
                .source(WorkflowScript.ScriptSource.of(fetchPath))
                .inline(inline)
                .inputs(fetchInputs)
                .outputs(fetchOutputs)
                .catchExpression("handleFetchError(@exception)")
                .codeActions(Map.of(1, CodeAction.ABORT, 2, CodeAction.RETRY))
                .build();

        // Stage 2: Validate Data
        Map<String, String> validateInputs = new LinkedHashMap<>();
        validateInputs.put("data", "@rawData");
        validateInputs.put("strict", "true");

        Map<String, String> validateOutputs = new LinkedHashMap<>();
        validateOutputs.put("validated", "validatedData");
        validateOutputs.put("validationStatus", "validationStatus");

        WorkflowScript validateScript = WorkflowScript.builder()
                .name("data-validator")
                .source(WorkflowScript.ScriptSource.of(validatePath))
                .inline(inline)
                .inputs(validateInputs)
                .outputs(validateOutputs)
                .codeActions(Map.of(1, CodeAction.ABORT, 2, CodeAction.ABORT))
                .build();

        // Stage 3: Transform Data
        Map<String, String> transformInputs = new LinkedHashMap<>();
        transformInputs.put("inputData", "@validated");
        transformInputs.put("format", "@targetFormat");

        Map<String, String> transformOutputs = new LinkedHashMap<>();
        transformOutputs.put("transformed", "transformedData");

        WorkflowScript transformScript = WorkflowScript.builder()
                .name("data-transformer")
                .source(WorkflowScript.ScriptSource.of(transformPath))
                .inline(inline)
                .inputs(transformInputs)
                .outputs(transformOutputs)
                .build();

        // Stage 4: Calculate Statistics
        Map<String, String> statsInputs = new LinkedHashMap<>();
        statsInputs.put("values", "@transformed");

        Map<String, String> statsOutputs = new LinkedHashMap<>();
        statsOutputs.put("sum", "total");
        statsOutputs.put("count", "itemCount");

        WorkflowScript statsScript = WorkflowScript.builder()
                .name("stats-calculator")
                .source(WorkflowScript.ScriptSource.of(statsPath))
                .inline(inline)
                .inputs(statsInputs)
                .outputs(statsOutputs)
                .build();

        // Stage 5: Finalize Output
        Map<String, String> finalizeInputs = new LinkedHashMap<>();
        finalizeInputs.put("data", "@transformed");
        finalizeInputs.put("metadata", "\"pipeline-v1\"");

        Map<String, String> finalizeOutputs = new LinkedHashMap<>();
        finalizeOutputs.put("result", "finalOutput");
        finalizeOutputs.put("finalStatus", "finalStatus");

        WorkflowScript finalizeScript = WorkflowScript.builder()
                .name("finalizer")
                .source(WorkflowScript.ScriptSource.of(finalizePath))
                .inline(inline)
                .inputs(finalizeInputs)
                .outputs(finalizeOutputs)
                .build();

        return List.of(
                new WorkflowStage("fetch", List.of(fetchScript), null, null, null),
                new WorkflowStage("validation", List.of(validateScript), null, null, null),
                new WorkflowStage("transform", List.of(transformScript), null, null, null),
                new WorkflowStage("statistics", List.of(statsScript), null, null, null),
                new WorkflowStage("finalize", List.of(finalizeScript), null, null, null));
    }

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
