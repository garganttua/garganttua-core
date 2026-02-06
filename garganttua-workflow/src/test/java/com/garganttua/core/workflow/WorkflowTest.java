package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.*;

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

class WorkflowTest {

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

        // Build contexts to initialize them
        injectionContextBuilder.build().onInit().onStart();
        expressionContextBuilder.build();
    }

    @Test
    void testSimpleWorkflow() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("test-workflow")
                .stage("stage1")
                    .script("result <- \"hello world\"")
                        .name("hello")
                        .output("greeting", "result")
                        .up()
                    .up()
                .build();

        // Debug: print generated script
        System.out.println("Generated script:\n" + workflow.getGeneratedScript());

        WorkflowResult result = workflow.execute();

        // Debug: print result info
        if (!result.isSuccess()) {
            System.out.println("Workflow failed. Code: " + result.code());
            result.exception().ifPresent(e -> e.printStackTrace());
            result.exceptionMessage().ifPresent(m -> System.out.println("Message: " + m));
        }

        assertTrue(result.isSuccess());
        assertEquals(0, result.code());
        assertFalse(result.hasAborted());
    }

    @Test
    void testWorkflowWithInputPayload() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("input-workflow")
                .stage("process")
                    .script("result <- @input")
                        .name("passthrough")
                        .output("output", "result")
                        .up()
                    .up()
                .build();

        WorkflowInput input = WorkflowInput.of("test-data");
        WorkflowResult result = workflow.execute(input);

        assertTrue(result.isSuccess());
    }

    @Test
    void testWorkflowWithParameters() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("param-workflow")
                .variable("prefix", "Hello")
                .stage("greet")
                    .script("result <- @prefix")
                        .name("greeter")
                        .output("message", "result")
                        .up()
                    .up()
                .build();

        WorkflowResult result = workflow.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    void testWorkflowMultipleStages() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("multi-stage-workflow")
                .stage("stage1")
                    .script("value <- 10")
                        .name("init")
                        .output("val", "value")
                        .up()
                    .up()
                .stage("stage2")
                    .script("doubled <- 20")
                        .name("double")
                        .output("result", "doubled")
                        .up()
                    .up()
                .build();

        WorkflowResult result = workflow.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    void testGetGeneratedScript() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("generated-script-test")
                .stage("test")
                    .script("x <- 42")
                        .name("answer")
                        .up()
                    .up()
                .build();

        String script = workflow.getGeneratedScript();

        assertNotNull(script);
        assertTrue(script.contains("Workflow: generated-script-test"));
        assertTrue(script.contains("Stage: test"));
    }

    @Test
    void testWorkflowWithFileScript() throws Exception {
        // Create a temporary script file
        Path scriptPath = tempDir.resolve("test-script.gs");
        Files.writeString(scriptPath, "result <- \"from file\"");

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("file-workflow")
                .stage("file-stage")
                    .script(scriptPath)
                        .name("file-script")
                        .inline()  // Force inline to embed content directly
                        .output("data", "result")
                        .up()
                    .up()
                .build();

        WorkflowResult result = workflow.execute();

        assertTrue(result.isSuccess());
    }

    @Test
    void testWorkflowResult() {
        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("result-test")
                .stage("compute")
                    .script("output <- \"final result\"")
                        .name("final")
                        .up()
                    .up()
                .build();

        WorkflowResult result = workflow.execute();

        assertNotNull(result.uuid());
        assertNotNull(result.start());
        assertNotNull(result.stop());
        assertNotNull(result.duration());
        assertTrue(result.duration().toMillis() >= 0);
    }

    @Test
    void testWorkflowWithoutContextsFails() {
        // Build without providing contexts should fail
        assertThrows(Exception.class, () -> {
            WorkflowBuilder.create()
                    .name("no-context")
                    .stage("test")
                        .script("x <- 1")
                            .up()
                        .up()
                    .build();
        });
    }

    @Test
    void testEmptyWorkflowBuildFails() {
        assertThrows(Exception.class, () -> {
            WorkflowBuilder.create()
                    .provide(injectionContextBuilder)
                    .provide(expressionContextBuilder)
                    .name("empty")
                    .build();
        });
    }

    @Test
    void testComplexWorkflowWithThreeScripts() throws Exception {
        // Script 1: Validation - validates input data and sets validation status
        String validationScript = """
            validationStatus <- "pending"
            inputData <- @input
            isValid <- true
            validationStatus <- "completed"
            validatedData <- @inputData
            """;

        // Script 2: Transformation - transforms the validated data
        String transformScript = """
            transformedData <- @validatedData
            processingStep <- "transform"
            outputFormat <- "json"
            result <- @transformedData
            """;

        // Script 3: Finalization - prepares final output
        String finalizationScript = """
            finalResult <- @result
            status <- "success"
            output <- @finalResult
            """;

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("data-processing-pipeline")
                .variable("pipelineVersion", "1.0.0")
                .variable("environment", "test")
                .stage("validation")
                    .script(validationScript)
                        .name("data-validator")
                        .output("validatedData", "validatedData")
                        .output("validationStatus", "validationStatus")
                        .up()
                    .up()
                .stage("transformation")
                    .script(transformScript)
                        .name("data-transformer")
                        .input("validatedData", "@validatedData")
                        .output("result", "result")
                        .output("outputFormat", "outputFormat")
                        .up()
                    .up()
                .stage("finalization")
                    .script(finalizationScript)
                        .name("finalizer")
                        .input("result", "@result")
                        .output("finalResult", "finalResult")
                        .output("status", "status")
                        .up()
                    .up()
                .build();

        // Print the generated script
        String generatedScript = workflow.getGeneratedScript();
        System.out.println("=".repeat(80));
        System.out.println("GENERATED WORKFLOW SCRIPT");
        System.out.println("=".repeat(80));
        System.out.println(generatedScript);
        System.out.println("=".repeat(80));

        // Execute the workflow
        WorkflowInput input = WorkflowInput.of("test-input-data");
        WorkflowResult result = workflow.execute(input);

        // Print execution result
        System.out.println("EXECUTION RESULT");
        System.out.println("=".repeat(80));
        System.out.println("UUID: " + result.uuid());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Code: " + result.code());
        System.out.println("Duration: " + result.duration().toMillis() + "ms");
        System.out.println("Variables: " + result.variables());
        System.out.println("Stage Outputs: " + result.stageOutputs());
        result.exception().ifPresent(e -> System.out.println("Exception: " + e.getMessage()));
        System.out.println("=".repeat(80));

        // Assertions
        assertTrue(result.isSuccess());
        assertEquals(0, result.code());
    }

    @Test
    void testComplexWorkflowWithInclude() throws Exception {
        // Script 1: Validation - uses @0 for positional arg from input mapping
        Path validationScript = tempDir.resolve("validation.gs");
        Files.writeString(validationScript, """
            validationStatus <- "completed"
            validatedData <- @0
            """);

        // Script 2: Transformation - uses @0 for validated data
        Path transformScript = tempDir.resolve("transform.gs");
        Files.writeString(transformScript, """
            outputFormat <- "json"
            result <- @0
            """);

        // Script 3: Finalization - uses @0 for result
        Path finalizationScript = tempDir.resolve("finalization.gs");
        Files.writeString(finalizationScript, """
            status <- "success"
            finalResult <- @0
            """);

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("data-processing-pipeline-with-include")
                .variable("pipelineVersion", "1.0.0")
                .variable("environment", "test")
                .stage("validation")
                    .script(validationScript)
                        .name("data-validator")
                        .input("inputData", "@input")
                        .output("validatedData", "validatedData")
                        .output("validationStatus", "validationStatus")
                        .up()
                    .up()
                .stage("transformation")
                    .script(transformScript)
                        .name("data-transformer")
                        .input("validatedData", "@validatedData")
                        .output("result", "result")
                        .output("outputFormat", "outputFormat")
                        .up()
                    .up()
                .stage("finalization")
                    .script(finalizationScript)
                        .name("finalizer")
                        .input("result", "@result")
                        .output("finalResult", "finalResult")
                        .output("status", "status")
                        .up()
                    .up()
                .build();

        // Print the generated script
        String generatedScript = workflow.getGeneratedScript();
        System.out.println("=".repeat(80));
        System.out.println("GENERATED WORKFLOW SCRIPT (WITH INCLUDE)");
        System.out.println("=".repeat(80));
        System.out.println(generatedScript);
        System.out.println("=".repeat(80));

        // Verify that include() and execute_script() are used
        assertTrue(generatedScript.contains("include("), "Script should contain include() calls");
        assertTrue(generatedScript.contains("execute_script("), "Script should contain execute_script() calls");
        assertTrue(generatedScript.contains("script_variable("), "Script should contain script_variable() calls");
        assertTrue(generatedScript.contains("validation.gs"), "Script should reference validation.gs");
        assertTrue(generatedScript.contains("transform.gs"), "Script should reference transform.gs");
        assertTrue(generatedScript.contains("finalization.gs"), "Script should reference finalization.gs");

        // Execute the workflow
        WorkflowInput input = WorkflowInput.of("test-input-data");
        WorkflowResult result = workflow.execute(input);

        // Print execution result
        System.out.println("EXECUTION RESULT");
        System.out.println("=".repeat(80));
        System.out.println("UUID: " + result.uuid());
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Code: " + result.code());
        System.out.println("Duration: " + result.duration().toMillis() + "ms");
        System.out.println("Variables: " + result.variables());
        System.out.println("Stage Outputs: " + result.stageOutputs());
        result.exception().ifPresent(e -> {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        });
        System.out.println("=".repeat(80));

        assertTrue(result.isSuccess(), "Workflow should succeed");
        assertEquals(0, result.code());
    }

    @Test
    void testScriptHeaderFormat() throws Exception {
        // Demonstrate the header format in script files
        System.out.println("=".repeat(80));
        System.out.println("SCRIPT HEADER FORMAT DEMONSTRATION");
        System.out.println("=".repeat(80));

        // Script with full header
        String scriptWithHeader = """
            #@workflow
            #  inputs:
            #    - name: data type: Map
            #    - name: config type: String
            #  outputs:
            #    - name: result variable: processedResult type: Object
            #    - name: status variable: processingStatus type: String
            #  returnCodes:
            #    0: SUCCESS
            #    1: VALIDATION_ERROR
            #    2: PROCESSING_ERROR
            #    99: UNKNOWN_ERROR
            #@end

            # Actual script code starts here
            processedResult <- @data
            processingStatus <- "completed"
            """;

        System.out.println("SCRIPT WITH HEADER:");
        System.out.println("-".repeat(40));
        System.out.println(scriptWithHeader);
        System.out.println("-".repeat(40));

        // Parse the header
        var parser = new com.garganttua.core.workflow.header.ScriptHeaderParser();
        var headerOpt = parser.parse(scriptWithHeader);

        assertTrue(headerOpt.isPresent(), "Header should be parsed");

        var header = headerOpt.get();
        System.out.println("\nPARSED HEADER:");
        System.out.println("-".repeat(40));
        System.out.println("Inputs:");
        for (var input : header.inputs()) {
            System.out.println("  - " + input.name() + " (type: " + input.type() + ")");
        }
        System.out.println("Outputs:");
        for (var output : header.outputs()) {
            System.out.println("  - " + output.name() + " -> @" + output.variable() + " (type: " + output.type() + ")");
        }
        System.out.println("Return Codes:");
        for (var code : header.returnCodes().entrySet()) {
            System.out.println("  - " + code.getKey() + ": " + code.getValue());
        }

        // Strip header to get clean script
        String cleanScript = parser.stripHeader(scriptWithHeader);
        System.out.println("\nSCRIPT WITHOUT HEADER:");
        System.out.println("-".repeat(40));
        System.out.println(cleanScript);
        System.out.println("=".repeat(80));

        // Assertions
        assertEquals(2, header.inputs().size());
        assertEquals(2, header.outputs().size());
        assertEquals(4, header.returnCodes().size());
        assertEquals("data", header.inputs().get(0).name());
        assertEquals("Map", header.inputs().get(0).type());
        assertEquals("SUCCESS", header.returnCodes().get(0));
        assertEquals("VALIDATION_ERROR", header.returnCodes().get(1));
    }

    @Test
    void testProgrammaticChainingConfiguration() throws Exception {
        // Demonstrate programmatic chaining configuration via DSL
        System.out.println("=".repeat(80));
        System.out.println("PROGRAMMATIC CHAINING CONFIGURATION");
        System.out.println("=".repeat(80));

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("chaining-demo")

                // Preset variables available to all scripts
                .variable("apiUrl", "https://api.example.com")
                .variable("timeout", 30000)

                .stage("fetch")
                    .script("response <- \"fetched data\"\nstatusCode <- 200")
                        .name("api-fetcher")
                        // INPUT MAPPINGS: inject variables into script context
                        .input("url", "@apiUrl")           // url <- @apiUrl
                        .input("timeoutMs", "@timeout")    // timeoutMs <- @timeout
                        // OUTPUT MAPPINGS: extract variables from script
                        .output("fetchedData", "response")       // fetchedData <- @response
                        .output("httpStatus", "statusCode")      // httpStatus <- @statusCode
                        // CATCH CLAUSE: handle exceptions
                        .catch_("handleError(@exception)")
                        .up()
                    .up()

                .stage("validate")
                    .script("isValid <- true\nvalidatedData <- @inputData")
                        .name("validator")
                        // Chain from previous stage
                        .input("inputData", "@fetchedData")
                        .output("validated", "validatedData")
                        .output("validationResult", "isValid")
                        .up()
                    .up()

                .stage("transform")
                    .script("output <- @data")
                        .name("transformer")
                        .input("data", "@validated")
                        .output("finalOutput", "output")
                        .up()
                    .up()
                .build();

        String script = workflow.getGeneratedScript();
        System.out.println("GENERATED SCRIPT WITH CHAINING:");
        System.out.println("-".repeat(40));
        System.out.println(script);
        System.out.println("-".repeat(40));

        System.out.println("\nCHAINING CONFIGURATION SUMMARY:");
        System.out.println("-".repeat(40));
        System.out.println("""
            DSL Methods:

            .input(scriptVar, expression)
                Maps workflow expression to script variable
                Example: .input("url", "@apiUrl")
                Generated: url <- @apiUrl

            .output(workflowVar, scriptVar)
                Extracts script variable to workflow
                Example: .output("fetchedData", "response")
                Generated: fetchedData <- @response (inline)
                       or: fetchedData <- @_result.response (include)

            .onCode(code, action)  [TODO: pipe clause syntax]
                React to specific return codes
                Actions: CONTINUE, ABORT, SKIP_STAGE, RETRY

            .catch_(expression)
                Handle exceptions during script execution
                Example: .catch_("handleError(@exception)")
                Generated: ... ! handleError(@exception)
            """);
        System.out.println("=".repeat(80));

        // Verify script structure
        assertTrue(script.contains("# Preset variables"));
        assertTrue(script.contains("apiUrl <- \"https://api.example.com\""));
        assertTrue(script.contains("# Stage: fetch"));
        assertTrue(script.contains("fetchedData <- @response"));  // Output mapping
        assertTrue(script.contains("# Stage: validate"));
        assertTrue(script.contains("inputData <- @fetchedData")); // Input mapping (chaining)
        assertTrue(script.contains("# Stage: transform"));
    }

    @Test
    void testWorkflowDescriptor() throws Exception {
        // Build workflow with various configurations
        var workflowBuilder = (com.garganttua.core.workflow.dsl.WorkflowBuilder) WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("descriptor-test")
                .variable("config", "production")
                .variable("maxRetries", 3)
                .stage("fetch")
                    .script("response <- fetch(@url)")
                        .name("api-fetcher")
                        .input("url", "@apiUrl")
                        .output("data", "response")
                        .catch_("handleError(@exception)")
                        .up()
                    .up()
                .stage("process")
                    .script("result <- process(@data)")
                        .name("processor")
                        .input("data", "@data")
                        .output("processed", "result")
                        .catchDownstream("handleDownstream(@exception)")
                        .up()
                    .up();

        // Get textual description
        System.out.println("=".repeat(80));
        System.out.println("WORKFLOW CARTOGRAPHY (TEXTUAL)");
        System.out.println("=".repeat(80));
        System.out.println(workflowBuilder.describeWorkflow());

        // Get structured descriptor
        var descriptor = workflowBuilder.getDescriptor();
        System.out.println("WORKFLOW CARTOGRAPHY (DATA STRUCTURE)");
        System.out.println("=".repeat(80));
        System.out.println("Name: " + descriptor.name());
        System.out.println("Inline All: " + descriptor.inlineAll());
        System.out.println("Preset Variables: " + descriptor.presetVariables());
        System.out.println("Stages:");
        for (var stage : descriptor.stages()) {
            System.out.println("  - " + stage.name());
            for (var script : stage.scripts()) {
                System.out.println("    - Script: " + script.name());
                System.out.println("      Source Type: " + script.sourceType());
                System.out.println("      Inline: " + script.inline());
                System.out.println("      Inputs: " + script.inputMappings());
                System.out.println("      Outputs: " + script.outputMappings());
                if (script.catchExpression() != null) {
                    System.out.println("      Catch: " + script.catchExpression());
                }
                if (script.catchDownstreamExpression() != null) {
                    System.out.println("      CatchDownstream: " + script.catchDownstreamExpression());
                }
            }
        }
        System.out.println("=".repeat(80));

        // Assertions on descriptor
        assertEquals("descriptor-test", descriptor.name());
        assertFalse(descriptor.inlineAll());
        assertEquals(2, descriptor.presetVariables().size());
        assertEquals("production", descriptor.presetVariables().get("config"));
        assertEquals(2, descriptor.stages().size());
        assertEquals("fetch", descriptor.stages().get(0).name());
        assertEquals("process", descriptor.stages().get(1).name());
        assertEquals("api-fetcher", descriptor.stages().get(0).scripts().get(0).name());
        assertEquals("handleError(@exception)", descriptor.stages().get(0).scripts().get(0).catchExpression());
    }

    @Test
    void testInlineAll() throws Exception {
        // Create a temporary script file
        Path scriptPath = tempDir.resolve("inline-test.gs");
        Files.writeString(scriptPath, "fileResult <- \"from file\"");

        IWorkflow workflow = WorkflowBuilder.create()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .name("inline-all-test")
                .inlineAll()  // Force all scripts to be inlined
                .stage("test")
                    .script(scriptPath)
                        .name("file-script")
                        // Note: no .inline() call, but inlineAll should force it
                        .up()
                    .up()
                .build();

        // Verify generated script contains inlined content, not include()
        String script = workflow.getGeneratedScript();
        assertFalse(script.contains("include("), "Script should not contain include() when inlineAll is set");
        assertTrue(script.contains("fileResult <- \"from file\""), "Script should contain inlined content");

        // Execute and verify success
        WorkflowResult result = workflow.execute();
        assertTrue(result.isSuccess());
    }
}
