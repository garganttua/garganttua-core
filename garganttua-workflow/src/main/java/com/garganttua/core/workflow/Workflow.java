package com.garganttua.core.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableContextHolder;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.workflow.dsl.WorkflowDescriptor;
import com.garganttua.core.workflow.generator.ScriptGenerationOptions;
import com.garganttua.core.workflow.generator.ScriptGenerator;
import com.garganttua.core.workflow.renderer.WorkflowRenderer;

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
public class Workflow implements IWorkflow, IObservable {
    private static final Logger log = Logger.getLogger(Workflow.class);

    private final String name;
    private final String generatedScript;
    private final List<WorkflowStage> stages;
    private final Map<String, Object> presetVariables;
    private final com.garganttua.core.script.IScriptingEnvironment scriptingEnvironment;
    /** Non-null when WorkflowBuilder.precompile(true) was set AND the
     *  workflow uses no per-call filtering. Thread-safe — reused across
     *  concurrent execute() calls. */
    private final com.garganttua.core.script.ICompiledScript precompiled;
    private final boolean inlineAll;
    private final WorkflowTimingConfig timingConfig;
    private final ObservableRegistry observers = new ObservableRegistry();
    private final ScriptGenerator scriptGenerator = new ScriptGenerator();
    private final WorkflowRenderer renderer = new WorkflowRenderer();

    /**
     * Creates a new Workflow.
     *
     * <p>The {@link com.garganttua.core.script.IScriptingEnvironment} is the
     * single point of entry into the script layer — every stage execution
     * spawns a fresh {@code IScript} via {@code env.newScript()}, which
     * encapsulates the underlying expression context, runtimes-builder
     * factory, and class-loader manager. Workflow no longer depends directly
     * on the Expression or Runtime layers; the dependency graph is now
     * {@code Workflow → Script → {Expression, Runtimes, ClassLoader}}.
     */
    public Workflow(String name, String generatedScript, List<WorkflowStage> stages,
            Map<String, Object> presetVariables,
            com.garganttua.core.script.IScriptingEnvironment scriptingEnvironment,
            com.garganttua.core.script.ICompiledScript precompiled,
            boolean inlineAll, WorkflowTimingConfig timingConfig) {
        this.name = name;
        this.generatedScript = generatedScript;
        this.stages = List.copyOf(stages);
        this.presetVariables = Map.copyOf(presetVariables);
        this.scriptingEnvironment = scriptingEnvironment;
        this.precompiled = precompiled;
        this.inlineAll = inlineAll;
        this.timingConfig = timingConfig != null ? timingConfig : WorkflowTimingConfig.disabled();
    }

    @Override
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.observers.addObserver(observer);
    }

    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.observers.removeObserver(observer);
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
    public boolean isPrecompiled() {
        return this.precompiled != null;
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
        // Reuse a caller-pinned execution id (e.g. the api's EXECUTION_UUID) so
        // stage:*/script:* observability events correlate with the caller's
        // api:operation:* events. Falls back to a fresh id when the workflow is
        // driven directly, outside any correlating caller.
        UUID uuid = options.executionId().orElseGet(UUID::randomUUID);
        Instant start = Instant.now();

        try {
            List<WorkflowStage> effectiveStages;
            String scriptSource;

            if (options.hasFiltering()) {
                effectiveStages = filterStages(stages, options);
                scriptSource = scriptGenerator.generate(name, effectiveStages, presetVariables, inlineAll,
                        ScriptGenerationOptions.withTiming(timingConfig));
                log.debug("Executing workflow '{}' with filtered stages {} and script:\n{}",
                        name, effectiveStages.stream().map(WorkflowStage::name).toList(), scriptSource);
            } else {
                effectiveStages = stages;
                scriptSource = generatedScript;
                log.debug("Executing workflow '{}' with script:\n{}", name, scriptSource);
            }

            // Cached path: only when precompile is on AND no per-call filtering
            // has rewritten the source. Both conditions must hold for the
            // pre-built ICompiledScript to match the script we want to run.
            boolean useCached = this.precompiled != null && !options.hasFiltering();
            if (useCached) {
                return executePrecompiled(uuid, start, input, effectiveStages);
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

    /**
     * Thread-safe execution path: reuses {@link #precompiled} across calls. No
     * per-call ANTLR parse, no per-call IRuntime build. Multiple threads can
     * call this method concurrently on the same Workflow instance.
     */
    private WorkflowResult executePrecompiled(UUID uuid, Instant start,
            WorkflowInput input, List<WorkflowStage> stagesToCollect) throws ScriptException {
        List<Object> args = new ArrayList<>();
        if (input.payload() != null) {
            args.add(input.payload());
        }
        for (var param : input.parameters().values()) {
            args.add(param);
        }

        com.garganttua.core.script.IScriptExecutionResult res;
        ObservableContextHolder.Session previous = ObservableContextHolder.push(observers, uuid);
        try {
            res = args.isEmpty()
                    ? this.precompiled.execute()
                    : this.precompiled.execute(args.toArray());
        } finally {
            ObservableContextHolder.pop(previous);
        }

        if (res.hasAborted()) {
            Instant stop = Instant.now();
            Throwable exception = res.exception().orElse(null);
            String message = exception != null ? exception.getMessage() : "Script execution aborted";
            log.error("Workflow '{}' (precompiled) aborted: {}", name, message, exception);
            return WorkflowResult.failure(uuid, start, stop,
                    new WorkflowException(message, exception));
        }

        Instant stop = Instant.now();
        Map<String, Object> variables = filterToCollectedVariables(res.variables(), stagesToCollect);
        Map<String, Object> stageOutputs = collectStageOutputsFromMap(res.variables(), stagesToCollect);
        return WorkflowResult.success(
                uuid,
                res.output().orElse(null),
                res.code(),
                variables,
                stageOutputs,
                start,
                stop);
    }

    private WorkflowResult executeScript(UUID uuid, Instant start, String scriptSource,
            WorkflowInput input, List<WorkflowStage> stagesToCollect) throws ScriptException {
        // 1. Spawn a fresh IScript via the scripting environment — wires
        // expression context + fresh-per-call IRuntimesBuilder + class-loader
        // manager internally.
        IScript script = this.scriptingEnvironment.newScript();
        script.load(scriptSource);

        // 2. Inject preset variables (named)
        for (var preset : presetVariables.entrySet()) {
            script.setVariable(preset.getKey(), preset.getValue());
        }

        // 3. Build positional arguments: payload + parameters
        List<Object> args = new ArrayList<>();
        if (input.payload() != null) {
            args.add(input.payload());
        }
        for (var param : input.parameters().values()) {
            args.add(param);
        }

        // 4. Compile and execute (bind the observer session for this thread so
        // script-side `:observe(...)` calls dispatch to this workflow's registry)
        script.compile();
        int code;
        ObservableContextHolder.Session previous = ObservableContextHolder.push(observers, uuid);
        try {
            code = args.isEmpty()
                    ? script.execute()
                    : script.execute(args.toArray());
        } finally {
            ObservableContextHolder.pop(previous);
        }

        // 4. Check for execution errors
        if (script.hasAborted()) {
            Instant stop = Instant.now();
            Throwable exception = script.getLastException().orElse(null);
            String message = script.getLastExceptionMessage().orElse("Script execution aborted");
            log.error("Workflow '{}' aborted: {}", name, message, exception);
            logErrorDump(script, scriptSource, stagesToCollect, message, exception);
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

    @Override
    public String describeWorkflow() {
        return renderer.render(name, stages, presetVariables, inlineAll);
    }

    @Override
    public WorkflowDescriptor getDescriptor() {
        List<WorkflowDescriptor.StageDescriptor> stageDescriptors = stages.stream()
                .map(stage -> new WorkflowDescriptor.StageDescriptor(
                        stage.name(),
                        stage.wrapExpression(),
                        stage.catchExpression(),
                        stage.catchDownstreamExpression(),
                        stage.condition(),
                        stage.scripts().stream()
                                .map(script -> new WorkflowDescriptor.ScriptDescriptor(
                                        script.getName(),
                                        script.getDescription(),
                                        script.getSource().type().name(),
                                        script.getPath(),
                                        script.isInline(),
                                        new HashMap<>(script.getInputs()),
                                        new HashMap<>(script.getOutputs()),
                                        script.getCatchExpression(),
                                        script.getCatchDownstreamExpression(),
                                        script.getCodeActions().entrySet().stream()
                                                .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        e -> e.getValue().name())),
                                        script.getCondition()))
                                .toList()))
                .toList();

        return new WorkflowDescriptor(
                name,
                inlineAll,
                new LinkedHashMap<>(presetVariables),
                stageDescriptors);
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

                script.getVariable(resultVarName, IClass.getClass(Object.class))
                        .ifPresent(v -> variables.put(resultVarName, v));
                script.getVariable(codeVarName, IClass.getClass(Integer.class))
                        .ifPresent(v -> variables.put(codeVarName, v));
                script.getVariable(refVarName, IClass.getClass(Object.class))
                        .ifPresent(v -> variables.put(refVarName, v));

                // Collect output mappings
                for (String outputVar : ws.getOutputs().keySet()) {
                    script.getVariable(outputVar, IClass.getClass(Object.class))
                            .ifPresent(v -> variables.put(outputVar, v));
                }
            }
        }

        // Collect special variables
        script.getVariable("output", IClass.getClass(Object.class))
                .ifPresent(v -> variables.put("output", v));
        script.getVariable("code", IClass.getClass(Integer.class))
                .ifPresent(v -> variables.put("code", v));

        return variables;
    }

    private static String sanitizeIdentifier(String name) {
        if (name == null) {
            return "script";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private void logErrorDump(IScript script, String scriptSource,
                              List<WorkflowStage> stagesToCollect, String message, Throwable exception) {
        StringBuilder dump = new StringBuilder();
        dump.append("\n╔══════════════════════════════════════════════════════════════════════╗\n");
        dump.append("║  WORKFLOW ERROR DUMP                                                ║\n");
        dump.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        dump.append("║  Workflow: ").append(name).append("\n");
        dump.append("║  Error: ").append(message).append("\n");

        // Exception chain
        if (exception != null) {
            dump.append("║\n║  Exception chain:\n");
            Throwable t = exception;
            int depth = 0;
            while (t != null && depth < 10) {
                dump.append("║    ").append("  ".repeat(depth))
                    .append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append("\n");
                t = t.getCause();
                depth++;
            }
        }

        // Error context variables from script
        dump.append("║\n║  Script error context:\n");
        script.getVariable("_scriptErrorLine", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Line: ").append(v).append("\n"));
        script.getVariable("_scriptErrorSource", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Source: ").append(v).append("\n"));
        script.getVariable("_scriptErrorStep", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Step: ").append(v).append("\n"));
        script.getVariable("_scriptErrorType", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Type: ").append(v).append("\n"));

        // Collected variables at failure point
        dump.append("║\n║  Variables at failure point:\n");
        Map<String, Object> vars = collectVariables(script, stagesToCollect);
        if (vars.isEmpty()) {
            dump.append("║    (none)\n");
        } else {
            for (var entry : vars.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (val.length() > 120) val = val.substring(0, 120) + "...";
                dump.append("║    ").append(entry.getKey()).append(" = ").append(val).append("\n");
            }
        }

        // Stages
        dump.append("║\n║  Stages: ");
        dump.append(stagesToCollect.stream().map(WorkflowStage::name).collect(Collectors.joining(" → ")));
        dump.append("\n");

        // Generated script (truncated)
        dump.append("║\n║  Generated script:\n");
        for (String line : scriptSource.split("\n")) {
            dump.append("║    ").append(line).append("\n");
        }

        dump.append("╚══════════════════════════════════════════════════════════════════════╝");
        log.error("{}", dump);
    }

    private Map<String, Object> collectStageOutputs(IScript script, List<WorkflowStage> stagesToCollect) {
        Map<String, Object> stageOutputs = new HashMap<>();
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                for (var output : ws.getOutputs().entrySet()) {
                    String key = stage.name() + "." + output.getKey();
                    Optional<Object> value = script.getVariable(output.getKey(), IClass.getClass(Object.class));
                    value.ifPresent(v -> stageOutputs.put(key, v));
                }
            }
        }
        return stageOutputs;
    }

    /**
     * Same as {@link #collectVariables(IScript, List)} but reads from a
     * variables map (as returned by {@link com.garganttua.core.script.IScriptExecutionResult#variables()}).
     * Used by the precompiled execution path.
     */
    private Map<String, Object> filterToCollectedVariables(Map<String, Object> source,
            List<WorkflowStage> stagesToCollect) {
        Map<String, Object> variables = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return variables;
        }
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");
                String resultVarName = "_" + stage.name() + "_" + scriptName + "_result";
                String codeVarName = "_" + stage.name() + "_" + scriptName + "_code";
                String refVarName = "_" + stage.name() + "_" + scriptName + "_ref";
                copyIfPresent(source, variables, resultVarName);
                copyIfPresent(source, variables, codeVarName);
                copyIfPresent(source, variables, refVarName);
                for (String outputVar : ws.getOutputs().keySet()) {
                    copyIfPresent(source, variables, outputVar);
                }
            }
        }
        copyIfPresent(source, variables, "output");
        copyIfPresent(source, variables, "code");
        return variables;
    }

    /** Mirror of {@link #collectStageOutputs(IScript, List)} for the precompiled path. */
    private Map<String, Object> collectStageOutputsFromMap(Map<String, Object> source,
            List<WorkflowStage> stagesToCollect) {
        Map<String, Object> stageOutputs = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return stageOutputs;
        }
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                for (var output : ws.getOutputs().entrySet()) {
                    String key = stage.name() + "." + output.getKey();
                    Object v = source.get(output.getKey());
                    if (v != null) {
                        stageOutputs.put(key, v);
                    }
                }
            }
        }
        return stageOutputs;
    }

    private static void copyIfPresent(Map<String, Object> src, Map<String, Object> dst, String key) {
        Object v = src.get(key);
        if (v != null) {
            dst.put(key, v);
        }
    }
}
