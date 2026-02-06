package com.garganttua.core.workflow.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.chaining.CodeAction;

class ScriptGeneratorTest {

    private ScriptGenerator generator;

    @BeforeEach
    void setup() {
        generator = new ScriptGenerator();
    }

    @Test
    void testPositionalVariableReplacement() throws WorkflowException {
        // Script with positional variables
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("data", "@inputData");
        inputs.put("config", "@configValue");

        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of("@result = process(@0, @1)"))
                .inputs(inputs)
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Verify positional variables are replaced with named variables
        assertTrue(generated.contains("@result = process(@data, @config)"),
                "Positional variables should be replaced with named variables");
        assertFalse(generated.contains("@0") || generated.contains("@1"),
                "No positional variables should remain");
    }

    @Test
    void testPositionalVariableReplacementWithPartialUsage() throws WorkflowException {
        // Script that only uses @0 and @2 (skips @1)
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("x", "@a");
        inputs.put("y", "@b");
        inputs.put("z", "@c");

        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of("@result = @0 + @2"))
                .inputs(inputs)
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        assertTrue(generated.contains("@result = @x + @z"),
                "Only used positional variables should be replaced");
    }

    @Test
    void testNamedVariablesUnchanged() throws WorkflowException {
        // Script that uses named variables (not positional)
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("data", "@inputData");

        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of("@result = @name + @other"))
                .inputs(inputs)
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        assertTrue(generated.contains("@result = @name + @other"),
                "Named variables should remain unchanged");
    }

    @Test
    void testNoInputsNoReplacement() throws WorkflowException {
        // Script with positional variables but no inputs
        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of("@result = @0 + @1"))
                .inputs(Map.of())
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        assertTrue(generated.contains("@result = @0 + @1"),
                "With no inputs, positional variables should remain unchanged");
    }

    @Test
    void testWordBoundaryRespected() throws WorkflowException {
        // Ensure @0 doesn't get replaced in @01 or @0abc
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("first", "@x");

        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of("@result = @0 + @01 + @0abc"))
                .inputs(inputs)
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // @0 should be replaced, but @01 and @0abc should NOT be replaced
        assertTrue(generated.contains("@first + @01 + @0abc"),
                "Word boundary should be respected - @0 replaced but not @01 or @0abc");
    }

    @Test
    void testIncludeCaseGeneratesExecuteScriptPattern() throws WorkflowException {
        // File-based script with inputs and outputs (non-inline)
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("data", "@inputData");
        inputs.put("config", "@configValue");

        Map<String, String> outputs = new LinkedHashMap<>();
        outputs.put("result", "processedResult");
        outputs.put("status", "processingStatus");

        File scriptFile = new File("/tmp/test-script.gs");

        WorkflowScript script = WorkflowScript.builder()
                .name("processor")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .inputs(inputs)
                .outputs(outputs)
                .catchExpression("handleError(@exception)")
                .codeActions(Map.of(1, CodeAction.ABORT))
                .build();

        WorkflowStage stage = new WorkflowStage("process", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Should generate include()
        assertTrue(generated.contains("_process_processor_ref <- include("),
                "Should have include() with ref variable");

        // Should generate execute_script() with positional args
        assertTrue(generated.contains("execute_script(@_process_processor_ref, @data, @config)"),
                "Should have execute_script() with input args: " + generated);

        // Should have catch clause on execute_script
        assertTrue(generated.contains("! => handleError(@exception)"),
                "Should have catch clause");

        // Should have code action as pipe clause with equals()
        assertTrue(generated.contains("equals(@_process_processor_code, 1) => abort()"),
                "Should have code action as pipe clause with equals(): " + generated);

        // Should have output mappings using script_variable()
        assertTrue(generated.contains("result <- script_variable(@_process_processor_ref, \"processedResult\")"),
                "Should use script_variable() for output mappings: " + generated);
        assertTrue(generated.contains("status <- script_variable(@_process_processor_ref, \"processingStatus\")"),
                "Should use script_variable() for all output mappings: " + generated);

        // Should NOT contain old-style output mapping syntax
        assertFalse(generated.contains(".processedResult"),
                "Should not have old-style field access syntax");
    }

    @Test
    void testIncludeCaseNoInputs() throws WorkflowException {
        // File-based script with no inputs
        File scriptFile = new File("/tmp/simple.gs");

        WorkflowScript script = WorkflowScript.builder()
                .name("simple")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .inputs(Map.of())
                .outputs(Map.of("out", "result"))
                .build();

        WorkflowStage stage = new WorkflowStage("run", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // execute_script with just the ref (no extra args)
        assertTrue(generated.contains("execute_script(@_run_simple_ref)"),
                "Should have execute_script() with just ref when no inputs: " + generated);
    }

    @Test
    void testMultilineScriptReplacement() throws WorkflowException {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("url", "@apiUrl");
        inputs.put("timeout", "@configTimeout");

        String scriptContent = """
                @response = fetch(@0)
                @data = parse(@response)
                @result = process(@data, @1)
                """;

        WorkflowScript script = WorkflowScript.builder()
                .name("test-script")
                .source(WorkflowScript.ScriptSource.of(scriptContent))
                .inputs(inputs)
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        assertTrue(generated.contains("fetch(@url)"),
                "@0 should be replaced with @url");
        assertTrue(generated.contains("process(@data, @timeout)"),
                "@1 should be replaced with @timeout");
    }
}
