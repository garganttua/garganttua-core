package com.garganttua.core.workflow;

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
import com.garganttua.core.workflow.dsl.WorkflowBuilder;
import com.garganttua.core.workflow.header.ScriptHeader;
import com.garganttua.core.workflow.header.ScriptHeaderParser;

/**
 * Tests for workflow scripts located in src/test/resources/scripts.
 */
class WorkflowScriptsTest {

    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;
    private ScriptHeaderParser headerParser;

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

        headerParser = new ScriptHeaderParser();
    }

    @Test
    void testValidateDataScriptHeader() throws Exception {
        String content = loadResource("scripts/validate-data.gs");

        var headerOpt = headerParser.parse(content);
        assertTrue(headerOpt.isPresent(), "Header should be present");

        ScriptHeader header = headerOpt.get();
        assertTrue(header.hasDescription(), "Header should have a description");
        assertTrue(header.description().contains("Validates input data"));

        assertEquals(2, header.inputs().size());
        assertEquals("data", header.inputs().get(0).name());
        assertEquals(Integer.valueOf(0), header.inputs().get(0).position());
        assertEquals("Object", header.inputs().get(0).type());
        assertEquals("strict", header.inputs().get(1).name());
        assertEquals(Integer.valueOf(1), header.inputs().get(1).position());

        assertEquals(2, header.outputs().size());
        assertEquals("validated", header.outputs().get(0).name());
        assertEquals("validatedData", header.outputs().get(0).variable());

        assertEquals(3, header.returnCodes().size());
        assertEquals("SUCCESS", header.returnCodes().get(0));
        assertEquals("VALIDATION_ERROR", header.returnCodes().get(1));
        assertEquals("NULL_DATA_ERROR", header.returnCodes().get(2));
    }

    @Test
    void testTransformDataScriptHeader() throws Exception {
        String content = loadResource("scripts/transform-data.gs");

        var headerOpt = headerParser.parse(content);
        assertTrue(headerOpt.isPresent());

        ScriptHeader header = headerOpt.get();
        assertTrue(header.hasDescription());
        assertTrue(header.description().contains("Transforms input data"));

        assertEquals(2, header.inputs().size());
        assertEquals("inputData", header.inputs().get(0).name());
        assertEquals("format", header.inputs().get(1).name());

        assertEquals(2, header.outputs().size());
        assertEquals(2, header.returnCodes().size());
    }

    @Test
    void testFetchApiScriptHeader() throws Exception {
        String content = loadResource("scripts/fetch-api.gs");

        var headerOpt = headerParser.parse(content);
        assertTrue(headerOpt.isPresent());

        ScriptHeader header = headerOpt.get();
        assertTrue(header.hasDescription());
        assertTrue(header.description().contains("Fetches data from a remote API"));

        assertEquals(2, header.inputs().size());
        assertEquals("url", header.inputs().get(0).name());
        assertEquals("String", header.inputs().get(0).type());
        assertEquals("timeout", header.inputs().get(1).name());
        assertEquals("Integer", header.inputs().get(1).type());

        assertEquals(4, header.returnCodes().size());
        assertEquals("SUCCESS", header.returnCodes().get(0));
        assertEquals("CONNECTION_ERROR", header.returnCodes().get(1));
        assertEquals("TIMEOUT_ERROR", header.returnCodes().get(2));
        assertEquals("HTTP_ERROR", header.returnCodes().get(3));
    }

    @Test
    void testFinalizeOutputScriptHeader() throws Exception {
        String content = loadResource("scripts/finalize-output.gs");

        var headerOpt = headerParser.parse(content);
        assertTrue(headerOpt.isPresent());

        ScriptHeader header = headerOpt.get();
        assertTrue(header.hasDescription());
        assertTrue(header.description().contains("Finalizes the output"));
    }

    @Test
    void testCalculateStatsScriptHeader() throws Exception {
        String content = loadResource("scripts/calculate-stats.gs");

        var headerOpt = headerParser.parse(content);
        assertTrue(headerOpt.isPresent());

        ScriptHeader header = headerOpt.get();
        assertTrue(header.hasDescription());
        assertTrue(header.description().contains("Calculates basic statistics"));

        assertEquals(1, header.inputs().size());
        assertEquals("values", header.inputs().get(0).name());
        assertEquals("List", header.inputs().get(0).type());

        assertEquals(2, header.outputs().size());
    }

    @Test
    void testStripHeaderRemovesMetadata() throws Exception {
        String content = loadResource("scripts/validate-data.gs");

        String cleanScript = headerParser.stripHeader(content);

        assertFalse(cleanScript.contains("#@workflow"));
        assertFalse(cleanScript.contains("#@end"));
        assertFalse(cleanScript.contains("description:"));
        assertTrue(cleanScript.contains("validationStatus <- \"pending\""));
    }

    @Test
    void testWorkflowWithResourceScripts() throws Exception {
        // Use simple inline scripts that avoid complex syntax
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("resource-scripts-workflow")
                .stage("validate")
                    .script("validatedData <- @data\nvalidationStatus <- \"completed\"")
                        .name("data-validator")
                        .description("Validates incoming data")
                        .input("data", "@input")
                        .output("validated", "validatedData")
                        .up()
                    .up()
                .stage("transform")
                    .script("transformedData <- @inputData\noutputFormat <- @format")
                        .name("data-transformer")
                        .description("Transforms data to target format")
                        .input("inputData", "@validated")
                        .input("format", "\"json\"")
                        .output("result", "transformedData")
                        .up()
                    .up()
                .stage("finalize")
                    .script("finalOutput <- @data\nfinalStatus <- \"success\"")
                        .name("finalizer")
                        .description("Prepares final output")
                        .input("data", "@result")
                        .output("final", "finalOutput")
                        .up()
                    .up()
                .build();

        WorkflowResult result = workflow.execute(WorkflowInput.of("test-data"));
        assertTrue(result.isSuccess(), "Workflow should succeed");
        assertEquals(0, result.code());
    }

    @Test
    void testWorkflowDescriptorWithDescriptions() throws Exception {
        Path validateScript = copyResourceToTemp("scripts/validate-data.gs");

        var workflowBuilder = (com.garganttua.core.workflow.dsl.WorkflowBuilder) WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("descriptor-with-descriptions")
                .stage("validate")
                    .script(validateScript)
                        .name("validator")
                        .description("Validates and sanitizes input data")
                        .inline()
                        .up()
                    .up();

        // Check textual description
        String textualDesc = workflowBuilder.describeWorkflow();
        System.out.println(textualDesc);
        assertTrue(textualDesc.contains("validator"));
        assertTrue(textualDesc.contains("Validates and sanitizes input data"));

        // Check data structure
        var descriptor = workflowBuilder.getDescriptor();
        assertEquals(1, descriptor.stages().size());
        assertEquals(1, descriptor.stages().get(0).scripts().size());

        var scriptDesc = descriptor.stages().get(0).scripts().get(0);
        assertEquals("validator", scriptDesc.name());
        assertEquals("Validates and sanitizes input data", scriptDesc.description());
    }

    @Test
    void testMultiStageWorkflowWithDescriptions() throws Exception {
        var workflowBuilder = (com.garganttua.core.workflow.dsl.WorkflowBuilder) WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("data-processing-pipeline")
                .variable("apiUrl", "https://api.example.com")
                .variable("timeout", 30000)
                .stage("fetch")
                    .script("response <- \"fetched data\"\nstatusCode <- 200")
                        .name("api-fetcher")
                        .description("Fetches data from remote API with retry logic")
                        .input("url", "@apiUrl")
                        .input("timeout", "@timeout")
                        .output("rawData", "response")
                        .output("status", "statusCode")
                        .catch_("handleFetchError(@exception)")
                        .up()
                    .up()
                .stage("validate")
                    .script("isValid <- true\nvalidatedData <- @data")
                        .name("data-validator")
                        .description("Validates data integrity and format")
                        .input("data", "@rawData")
                        .output("validated", "validatedData")
                        .output("validationResult", "isValid")
                        .catchDownstream("handleValidationError(@exception)")
                        .up()
                    .up()
                .stage("transform")
                    .script("transformed <- @input\nformat <- \"json\"")
                        .name("data-transformer")
                        .description("Transforms data to target schema")
                        .input("input", "@validated")
                        .output("result", "transformed")
                        .output("outputFormat", "format")
                        .up()
                    .up();

        // Print colored workflow description
        System.out.println("=".repeat(80));
        System.out.println("WORKFLOW WITH SCRIPT DESCRIPTIONS");
        System.out.println("=".repeat(80));
        System.out.println(workflowBuilder.describeWorkflow());
        System.out.println("=".repeat(80));

        // Verify descriptor
        var descriptor = workflowBuilder.getDescriptor();
        assertEquals(3, descriptor.stages().size());

        // Check each script has description
        assertEquals("Fetches data from remote API with retry logic",
                descriptor.stages().get(0).scripts().get(0).description());
        assertEquals("Validates data integrity and format",
                descriptor.stages().get(1).scripts().get(0).description());
        assertEquals("Transforms data to target schema",
                descriptor.stages().get(2).scripts().get(0).description());
    }

    private String loadResource(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path copyResourceToTemp(String resourcePath) throws Exception {
        String content = loadResource(resourcePath);
        String fileName = Path.of(resourcePath).getFileName().toString();
        Path targetPath = tempDir.resolve(fileName);
        Files.writeString(targetPath, content);
        return targetPath;
    }
}
