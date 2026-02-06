package com.garganttua.core.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.context.ScriptContext;
import com.garganttua.core.workflow.generator.ScriptGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * Workflow implementation that executes a pre-generated script.
 *
 * <p>
 * The workflow receives a generated script from its builder and executes it
 * using a {@link ScriptContext}. The builder is responsible for generating
 * the script from the workflow stages and configuration.
 * </p>
 *
 * <p>
 * When executed with {@link WorkflowExecutionOptions}, the workflow can filter
 * stages at runtime by regenerating the script with only the selected stages.
 * The default execution path (no options) uses the cached pre-generated script.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class Workflow implements IWorkflow {

    private final String name;
    private final String generatedScript;
    private final List<WorkflowStage> stages;
    private final Map<String, Object> presetVariables;
    private final IExpressionContext expressionContext;
    private final IInjectionContext injectionContext;
    private final boolean inlineAll;
    private final ScriptGenerator scriptGenerator = new ScriptGenerator();

    /**
     * Creates a new Workflow with all required components.
     *
     * @param name              the workflow name
     * @param generatedScript   the pre-generated script to execute
     * @param stages            the workflow stages (for result collection)
     * @param presetVariables   preset variables for the workflow
     * @param expressionContext the expression context for script evaluation
     * @param injectionContext  the injection context for bean resolution
     * @param inlineAll         whether all file-based scripts should be inlined
     */
    public Workflow(String name, String generatedScript, List<WorkflowStage> stages,
            Map<String, Object> presetVariables, IExpressionContext expressionContext,
            IInjectionContext injectionContext, boolean inlineAll) {
        this.name = name;
        this.generatedScript = generatedScript;
        this.stages = List.copyOf(stages);
        this.presetVariables = Map.copyOf(presetVariables);
        this.expressionContext = expressionContext;
        this.injectionContext = injectionContext;
        this.inlineAll = inlineAll;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGeneratedScript() {
        return generatedScript;
    }

    @Override
    public WorkflowResult execute() {
        return execute(WorkflowInput.empty(), WorkflowExecutionOptions.none());
    }

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        return execute(input, WorkflowExecutionOptions.none());
    }

    @Override
    public WorkflowResult execute(WorkflowInput input, WorkflowExecutionOptions options) {
        UUID uuid = UUID.randomUUID();
        Instant start = Instant.now();

        try {
            List<WorkflowStage> effectiveStages;
            String scriptSource;

            if (options.hasFiltering()) {
                effectiveStages = filterStages(stages, options);
                scriptSource = scriptGenerator.generate(name, effectiveStages, presetVariables, inlineAll);
                log.debug("Executing workflow '{}' with filtered stages {} and script:\n{}",
                        name, effectiveStages.stream().map(WorkflowStage::name).toList(), scriptSource);
            } else {
                effectiveStages = stages;
                scriptSource = generatedScript;
                log.debug("Executing workflow '{}' with script:\n{}", name, scriptSource);
            }

            return executeScript(uuid, start, scriptSource, input, effectiveStages);

        } catch (WorkflowException e) {
            Instant stop = Instant.now();
            log.error("Workflow '{}' stage filtering failed: {}", name, e.getMessage(), e);
            return WorkflowResult.failure(uuid, start, stop, e);
        } catch (ScriptException e) {
            Instant stop = Instant.now();
            log.error("Script execution failed: {}", e.getMessage(), e);
            return WorkflowResult.failure(uuid, start, stop,
                    new WorkflowException("Script execution failed", e));
        }
    }

    private WorkflowResult executeScript(UUID uuid, Instant start, String scriptSource,
            WorkflowInput input, List<WorkflowStage> stagesToCollect) throws ScriptException {
        // 1. Create and configure the ScriptContext
        IScript script = new ScriptContext(expressionContext, injectionContext);
        script.load(scriptSource);

        // 2. Inject initial variables
        if (input.payload() != null) {
            script.setVariable("input", input.payload());
        }
        for (var param : input.parameters().entrySet()) {
            script.setVariable(param.getKey(), param.getValue());
        }
        for (var preset : presetVariables.entrySet()) {
            script.setVariable(preset.getKey(), preset.getValue());
        }

        // 3. Compile and execute
        script.compile();
        int code = script.execute();

        // 4. Check for execution errors
        if (script.hasAborted()) {
            Instant stop = Instant.now();
            Throwable exception = script.getLastException().orElse(null);
            String message = script.getLastExceptionMessage().orElse("Script execution aborted");
            log.error("Workflow '{}' aborted: {}", name, message, exception);
            return WorkflowResult.failure(uuid, start, stop,
                    new WorkflowException(message, exception));
        }

        // 5. Collect results
        Instant stop = Instant.now();
        Map<String, Object> variables = collectVariables(script, stagesToCollect);
        Map<String, Object> stageOutputs = collectStageOutputs(script, stagesToCollect);

        return WorkflowResult.success(
                uuid,
                script.getOutput().orElse(null),
                code,
                variables,
                stageOutputs,
                start,
                stop);
    }

    /**
     * Filters the stages list according to the execution options.
     *
     * @param allStages the complete list of workflow stages
     * @param options   the execution options specifying filtering criteria
     * @return the filtered list of stages to execute
     * @throws WorkflowException if validation fails (unknown stage names, invalid range, empty result)
     */
    private List<WorkflowStage> filterStages(List<WorkflowStage> allStages,
            WorkflowExecutionOptions options) throws WorkflowException {
        Set<String> allNames = allStages.stream()
                .map(WorkflowStage::name)
                .collect(Collectors.toSet());

        // Validate stage names
        if (options.startFrom().isPresent() && !allNames.contains(options.startFrom().get())) {
            throw new WorkflowException("Unknown stage name in startFrom: " + options.startFrom().get());
        }
        if (options.stopAfter().isPresent() && !allNames.contains(options.stopAfter().get())) {
            throw new WorkflowException("Unknown stage name in stopAfter: " + options.stopAfter().get());
        }
        for (String skip : options.skipStages()) {
            if (!allNames.contains(skip)) {
                throw new WorkflowException("Unknown stage name in skipStages: " + skip);
            }
        }

        // Find start and stop indices
        int startIdx = 0;
        int stopIdx = allStages.size() - 1;

        if (options.startFrom().isPresent()) {
            for (int i = 0; i < allStages.size(); i++) {
                if (allStages.get(i).name().equals(options.startFrom().get())) {
                    startIdx = i;
                    break;
                }
            }
        }

        if (options.stopAfter().isPresent()) {
            for (int i = 0; i < allStages.size(); i++) {
                if (allStages.get(i).name().equals(options.stopAfter().get())) {
                    stopIdx = i;
                    break;
                }
            }
        }

        // Validate ordering
        if (startIdx > stopIdx) {
            throw new WorkflowException("startFrom stage '" + options.startFrom().orElse("") +
                    "' comes after stopAfter stage '" + options.stopAfter().orElse("") + "'");
        }

        // Build filtered list
        List<WorkflowStage> filtered = new ArrayList<>();
        for (int i = startIdx; i <= stopIdx; i++) {
            WorkflowStage stage = allStages.get(i);
            if (!options.skipStages().contains(stage.name())) {
                filtered.add(stage);
            }
        }

        if (filtered.isEmpty()) {
            throw new WorkflowException("All stages were filtered out by execution options");
        }

        return filtered;
    }

    private Map<String, Object> collectVariables(IScript script, List<WorkflowStage> stagesToCollect) {
        Map<String, Object> variables = new HashMap<>();
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");
                String resultVarName = "_" + stage.name() + "_" + scriptName + "_result";
                String codeVarName = "_" + stage.name() + "_" + scriptName + "_code";
                String refVarName = "_" + stage.name() + "_" + scriptName + "_ref";

                script.getVariable(resultVarName, Object.class)
                        .ifPresent(v -> variables.put(resultVarName, v));
                script.getVariable(codeVarName, Integer.class)
                        .ifPresent(v -> variables.put(codeVarName, v));
                script.getVariable(refVarName, Object.class)
                        .ifPresent(v -> variables.put(refVarName, v));

                // Collect output mappings
                for (String outputVar : ws.getOutputs().keySet()) {
                    script.getVariable(outputVar, Object.class)
                            .ifPresent(v -> variables.put(outputVar, v));
                }
            }
        }

        // Collect special variables
        script.getVariable("output", Object.class)
                .ifPresent(v -> variables.put("output", v));
        script.getVariable("code", Integer.class)
                .ifPresent(v -> variables.put("code", v));

        return variables;
    }

    private static String sanitizeIdentifier(String name) {
        if (name == null) {
            return "script";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private Map<String, Object> collectStageOutputs(IScript script, List<WorkflowStage> stagesToCollect) {
        Map<String, Object> stageOutputs = new HashMap<>();
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                for (var output : ws.getOutputs().entrySet()) {
                    String key = stage.name() + "." + output.getKey();
                    Optional<Object> value = script.getVariable(output.getKey(), Object.class);
                    value.ifPresent(v -> stageOutputs.put(key, v));
                }
            }
        }
        return stageOutputs;
    }
}
