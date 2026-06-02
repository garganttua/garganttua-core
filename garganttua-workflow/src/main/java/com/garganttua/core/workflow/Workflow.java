package com.garganttua.core.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservableContextHolder;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
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

    /** Registers an observer to receive this workflow's observability events. */
    @Override
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.observers.addObserver(observer);
    }

    /** Detaches a previously registered observer. */
    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.observers.removeObserver(observer);
    }

    /** {@return the workflow name} */
    @Override
    public String getName() {
        return name;
    }

    /** {@return the Garganttua Script source generated from this workflow's stages} */
    @Override
    public String getGeneratedScript() {
        return generatedScript;
    }

    /** {@return {@code true} when this workflow holds a pre-compiled script reused across executions} */
    @Override
    public boolean isPrecompiled() {
        return this.precompiled != null;
    }

    /** Executes the workflow with empty input and no execution options. */
    @Override
    public WorkflowResult execute() {
        return execute(WorkflowInput.empty(), WorkflowExecutionOptions.none());
    }

    /** Executes the workflow with the given input and no execution options. */
    @Override
    public WorkflowResult execute(WorkflowInput input) {
        return execute(input, WorkflowExecutionOptions.none());
    }

    /**
     * Executes the workflow with the given input and options.
     *
     * <p>When {@code options} request stage filtering the script is regenerated for
     * the selected stages; otherwise the cached pre-generated (or pre-compiled)
     * script is run. Execution never throws — failures are returned as a
     * {@link WorkflowResult#failure} carrying a {@link WorkflowException}.
     *
     * @param input   the workflow input (payload and named parameters)
     * @param options execution options (stage filtering, caller-pinned execution id)
     * @return the workflow result, success or failure
     */
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
        Map<String, Object> variables = WorkflowVariableCollector.filterToCollectedVariables(res.variables(), stagesToCollect);
        Map<String, Object> stageOutputs = WorkflowVariableCollector.collectStageOutputsFromMap(res.variables(), stagesToCollect);
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
            WorkflowVariableCollector.logErrorDump(this.name, script, scriptSource, stagesToCollect, message, exception);
            return WorkflowResult.failure(uuid, start, stop,
                    new WorkflowException(message, exception));
        }

        // 5. Collect results
        Instant stop = Instant.now();
        Map<String, Object> variables = WorkflowVariableCollector.collectVariables(script, stagesToCollect);
        Map<String, Object> stageOutputs = WorkflowVariableCollector.collectStageOutputs(script, stagesToCollect);

        return WorkflowResult.success(
                uuid,
                script.getOutput().orElse(null),
                code,
                variables,
                stageOutputs,
                start,
                stop);
    }

    /** {@return a human-readable ANSI-rendered diagram of this workflow's structure} */
    @Override
    public String describeWorkflow() {
        return renderer.render(name, stages, presetVariables, inlineAll);
    }

    /** {@return an immutable {@link WorkflowDescriptor} snapshot of this workflow's stages and scripts} */
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

}
