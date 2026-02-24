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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("process", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("run", List.of(script), null, null, null, null);

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

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        assertTrue(generated.contains("fetch(@url)"),
                "@0 should be replaced with @url");
        assertTrue(generated.contains("process(@data, @timeout)"),
                "@1 should be replaced with @timeout");
    }

    @Test
    void testConditionalFileScript() throws WorkflowException {
        // File-based script with a condition
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("env", "@environment");

        Map<String, String> outputs = new LinkedHashMap<>();
        outputs.put("deployResult", "result");

        File scriptFile = new File("/tmp/deploy.gs");

        WorkflowScript script = WorkflowScript.builder()
                .name("deploy")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .condition("equals(@env, \"prod\")")
                .inputs(inputs)
                .outputs(outputs)
                .codeActions(Map.of(1, CodeAction.ABORT))
                .build();

        WorkflowStage stage = new WorkflowStage("run", List.of(script), null, null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Should have condition variable
        assertTrue(generated.contains("_run_deploy_cond <- equals(@env, \"prod\")"),
                "Should emit condition variable: " + generated);

        // Include should be unconditional
        assertTrue(generated.contains("_run_deploy_ref <- include("),
                "Include should be unconditional: " + generated);

        // Should use noop() -> 0 for default code
        assertTrue(generated.contains("_run_deploy_code <- noop() -> 0"),
                "Should have noop() -> 0 for default code: " + generated);

        // Should have conditional execute_script via pipe
        assertTrue(generated.contains("| @_run_deploy_cond => _run_deploy_code <- execute_script(@_run_deploy_ref, @env)"),
                "Should have conditional execute_script: " + generated);

        // Code actions should use combined condition
        assertTrue(generated.contains("| and(@_run_deploy_cond, equals(@_run_deploy_code, 1)) => abort()"),
                "Code actions should use combined condition: " + generated);

        // Output mappings should be conditional via noop + pipe
        assertTrue(generated.contains("noop()\n    | @_run_deploy_cond => deployResult <- script_variable(@_run_deploy_ref, \"result\")"),
                "Output mappings should be conditional: " + generated);

        // Should NOT have catch clauses (omitted when conditional)
        assertFalse(generated.contains("! =>"),
                "Catch clauses should be omitted when conditional: " + generated);
    }

    @Test
    void testConditionalInlineScript() throws WorkflowException {
        // Inline script with a condition
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("data", "@inputData");

        String scriptContent = "result <- process(@0)\noutput <- transform(@result)";

        WorkflowScript script = WorkflowScript.builder()
                .name("processor")
                .source(WorkflowScript.ScriptSource.of(scriptContent))
                .condition("equals(@mode, \"active\")")
                .inputs(inputs)
                .outputs(Map.of("workflowResult", "output"))
                .build();

        WorkflowStage stage = new WorkflowStage("test", List.of(script), null, null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Should have condition variable
        assertTrue(generated.contains("_test_processor_cond <- equals(@mode, \"active\")"),
                "Should emit condition variable: " + generated);

        // Each line should be guarded with noop + pipe
        assertTrue(generated.contains("noop()\n    | @_test_processor_cond => result <- process(@data)"),
                "First line should be guarded: " + generated);
        assertTrue(generated.contains("noop()\n    | @_test_processor_cond => output <- transform(@result)"),
                "Second line should be guarded: " + generated);

        // Output mappings should be conditional
        assertTrue(generated.contains("noop()\n    | @_test_processor_cond => workflowResult <- @output"),
                "Output mappings should be conditional: " + generated);
    }

    @Test
    void testConditionalStage() throws WorkflowException {
        // Stage with a condition gates all scripts
        File scriptFile = new File("/tmp/deploy.gs");

        WorkflowScript script1 = WorkflowScript.builder()
                .name("script1")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .inputs(Map.of())
                .outputs(Map.of())
                .build();

        WorkflowScript script2 = WorkflowScript.builder()
                .name("script2")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .inputs(Map.of())
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("deploy", List.of(script1, script2),
                null, null, null, "equals(@environment, \"prod\")");

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Stage condition variable should be emitted once
        assertTrue(generated.contains("_deploy_cond <- equals(@environment, \"prod\")"),
                "Should emit stage condition variable: " + generated);

        // Both scripts should reference the stage condition
        assertTrue(generated.contains("_deploy_script1_cond <- @_deploy_cond"),
                "Script1 should reference stage condition: " + generated);
        assertTrue(generated.contains("_deploy_script2_cond <- @_deploy_cond"),
                "Script2 should reference stage condition: " + generated);

        // Both scripts should use conditional execution
        assertTrue(generated.contains("| @_deploy_script1_cond => _deploy_script1_code <- execute_script("),
                "Script1 should have conditional execute_script: " + generated);
        assertTrue(generated.contains("| @_deploy_script2_cond => _deploy_script2_code <- execute_script("),
                "Script2 should have conditional execute_script: " + generated);
    }

    @Test
    void testConditionalStageAndScript() throws WorkflowException {
        // Stage condition AND script condition should be combined with and()
        File scriptFile = new File("/tmp/deploy.gs");

        WorkflowScript script = WorkflowScript.builder()
                .name("deploy")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .condition("equals(@region, \"us\")")
                .inputs(Map.of())
                .outputs(Map.of())
                .build();

        WorkflowStage stage = new WorkflowStage("prod", List.of(script),
                null, null, null, "equals(@environment, \"prod\")");

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Stage condition should be emitted
        assertTrue(generated.contains("_prod_cond <- equals(@environment, \"prod\")"),
                "Should emit stage condition: " + generated);

        // Script condition should combine stage + script with and()
        assertTrue(generated.contains("_prod_deploy_cond <- and(@_prod_cond, equals(@region, \"us\"))"),
                "Should combine conditions with and(): " + generated);
    }

    @Test
    void testNoConditionUnchanged() throws WorkflowException {
        // Verify that without conditions, the generated code is the same as before
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("data", "@inputData");

        File scriptFile = new File("/tmp/test.gs");

        WorkflowScript script = WorkflowScript.builder()
                .name("processor")
                .source(WorkflowScript.ScriptSource.of(scriptFile))
                .inputs(inputs)
                .outputs(Map.of("result", "output"))
                .catchExpression("handleError(@exception)")
                .build();

        WorkflowStage stage = new WorkflowStage("run", List.of(script), null, null, null, null);

        String generated = generator.generate("test-workflow", List.of(stage), null);

        // Should NOT have any condition variables
        assertFalse(generated.contains("_cond"),
                "Should not have condition variables without conditions: " + generated);

        // Should NOT have noop() for conditionals
        assertFalse(generated.contains("noop()"),
                "Should not have noop() without conditions: " + generated);

        // Should have normal execute_script
        assertTrue(generated.contains("_run_processor_code <- execute_script(@_run_processor_ref, @data)"),
                "Should have normal execute_script: " + generated);

        // Should have catch clause
        assertTrue(generated.contains("! => handleError(@exception)"),
                "Should have catch clause: " + generated);

        // Should have normal output mapping
        assertTrue(generated.contains("result <- script_variable(@_run_processor_ref, \"output\")"),
                "Should have normal output mapping: " + generated);
    }
}
