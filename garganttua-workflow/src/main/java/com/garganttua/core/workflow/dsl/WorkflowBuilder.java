package com.garganttua.core.workflow.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.script.IScriptingEnvironment;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.Workflow;
import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.WorkflowTimingConfig;
import com.garganttua.core.workflow.generator.ScriptGenerationOptions;
import com.garganttua.core.workflow.generator.ScriptGenerator;
import com.garganttua.core.workflow.renderer.WorkflowRenderer;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for a single {@link IWorkflow}. Internal building block of
 * {@link WorkflowsBuilder} — only reachable via
 * {@link IWorkflowsBuilder#workflow(String)}. No longer Bootstrap-
 * discoverable in its own right: the plural builder owns the SPI surface.
 */
@Reflected
public class WorkflowBuilder extends AbstractDependentBuilder<IWorkflowBuilder, IWorkflow>
        implements IWorkflowBuilder {
    private static final Logger log = Logger.getLogger(WorkflowBuilder.class);

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            DependencySpec.require(IClass.getClass(IInjectionContextBuilder.class)),
            DependencySpec.use(IClass.getClass(IObservabilityBuilder.class)));

    private final ScriptGenerator scriptGenerator = new ScriptGenerator();
    private final WorkflowRenderer renderer = new WorkflowRenderer();

    private final IWorkflowsBuilder parent;
    private String name = "unnamed-workflow";
    private final Map<String, Object> presetVariables = new LinkedHashMap<>();
    private final List<WorkflowStage> stages = new ArrayList<>();
    private IInjectionContextBuilder injectionContextBuilder;
    private IObservabilityBuilder observabilityBuilder;
    private IScriptingEnvironment scriptingEnvironment;
    private boolean inlineAll = false;
    private boolean precompile = false;
    private WorkflowTimingConfig timingConfig = WorkflowTimingConfig.disabled();

    WorkflowBuilder(IWorkflowsBuilder parent) {
        super(DEPENDENCIES);
        this.parent = parent;
        log.trace("WorkflowBuilder created (parent={})", parent != null ? "set" : "null");
    }

    static WorkflowBuilder createChild(IWorkflowsBuilder parent, String name) {
        WorkflowBuilder b = new WorkflowBuilder(parent);
        b.name = name;
        return b;
    }

    /**
     * Returns the parent {@link IWorkflowsBuilder} that opened this builder.
     *
     * @return the owning plural builder
     * @throws IllegalStateException if this builder was created without a parent
     *         (i.e. outside of {@code WorkflowsBuilder.workflow()})
     */
    @Override
    public IWorkflowsBuilder up() {
        if (this.parent == null) {
            throw new IllegalStateException(
                    "WorkflowBuilder has no parent — opened outside of WorkflowsBuilder.workflow()");
        }
        return this.parent;
    }

    /**
     * Receive an already-built scripting environment. Used by
     * {@link WorkflowsBuilder} to propagate the parent-resolved
     * {@link IScriptingEnvironment} to children — bypasses the
     * IObservableBuilder/provide chain for a non-builder value.
     */
    void acceptScriptingEnvironment(IScriptingEnvironment env) {
        this.scriptingEnvironment = env;
    }

    /**
     * Sets the workflow name.
     *
     * @param name the workflow name
     * @return this builder for chaining
     */
    @Override
    public IWorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Registers a preset variable made available to every stage script.
     *
     * @param name  the variable name
     * @param value the variable value
     * @return this builder for chaining
     */
    @Override
    public IWorkflowBuilder variable(String name, Object value) {
        this.presetVariables.put(name, value);
        return this;
    }

    /**
     * Forces every file-backed script to be inlined into the generated script
     * rather than included by reference.
     *
     * @return this builder for chaining
     */
    @Override
    public IWorkflowBuilder inlineAll() {
        this.inlineAll = true;
        return this;
    }

    /**
     * Enables or disables pre-compilation of the generated script at build time.
     * When enabled, a fresh runtime is spawned once during {@link #doBuild()}.
     *
     * @param enabled {@code true} to pre-compile at build time
     * @return this builder for chaining
     */
    @Override
    public IWorkflowBuilder precompile(boolean enabled) {
        this.precompile = enabled;
        return this;
    }

    /**
     * Configures stage/script timing instrumentation.
     *
     * @param config the timing configuration; use
     *        {@link WorkflowTimingConfig#disabled()} to turn it off
     * @return this builder for chaining
     * @throws IllegalArgumentException if {@code config} is {@code null}
     */
    @Override
    public IWorkflowBuilder timing(WorkflowTimingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("timing config cannot be null; use WorkflowTimingConfig.disabled()");
        }
        this.timingConfig = config;
        return this;
    }

    /**
     * Opens a builder for a new stage appended to this workflow.
     *
     * @param name the stage name
     * @return a stage builder whose {@code up()} returns to this workflow builder
     */
    @Override
    public IWorkflowStageBuilder stage(String name) {
        WorkflowStageBuilder stageBuilder = new WorkflowStageBuilder(name);
        stageBuilder.setUp(this);
        return stageBuilder;
    }

    void addStage(WorkflowStage stage) {
        this.stages.add(stage);
    }

    /**
     * Generates the workflow script from the configured stages, optionally
     * pre-compiles it, and assembles the {@link Workflow} instance, attaching it
     * to the observability binding when one was provided.
     *
     * @return the built workflow
     * @throws DslException if no stage was declared, the required
     *         {@link IInjectionContextBuilder} or {@link IScriptingEnvironment}
     *         is missing, or script generation/pre-compilation fails
     */
    @Override
    protected IWorkflow doBuild() throws DslException {
        log.trace("Building workflow '{}'", name);

        if (stages.isEmpty()) {
            throw new DslException("Workflow must have at least one stage");
        }

        if (injectionContextBuilder == null) {
            throw new DslException("InjectionContextBuilder is required");
        }

        if (scriptingEnvironment == null) {
            // Direct-DSL caller path: ask the parent to materialise the env
            // on demand. Bootstrap-driven flows already delivered it via
            // doPreBuildWithDependency() so this branch is the fallback for
            // workflowsBuilder.workflow("x").stage(…).build() chains.
            if (this.parent instanceof WorkflowsBuilder wsb) {
                IScriptingEnvironment env = wsb.requireScriptingEnvironment();
                if (env != null) {
                    this.scriptingEnvironment = env;
                }
            }
            if (scriptingEnvironment == null) {
                throw new DslException("IScriptingEnvironment is required — workflow can't compile its script");
            }
        }

        String generatedScript;
        ScriptGenerationOptions generationOptions = ScriptGenerationOptions.withTiming(this.timingConfig);
        try {
            generatedScript = scriptGenerator.generate(name, stages, presetVariables, inlineAll, generationOptions);
            log.debug("Generated workflow script for '{}':\n{}", name, generatedScript);
        } catch (WorkflowException e) {
            throw new DslException("Failed to generate workflow script", e);
        }

        com.garganttua.core.script.ICompiledScript compiled = null;
        if (this.precompile) {
            try {
                compiled = this.scriptingEnvironment.precompile(generatedScript, presetVariables);
                log.debug("Workflow '{}' pre-compiled — fresh runtime spawned once at build", name);
            } catch (com.garganttua.core.script.ScriptException e) {
                throw new DslException("Failed to pre-compile workflow '" + name + "'", e);
            }
        }

        Workflow workflow = new Workflow(
                name,
                generatedScript,
                new ArrayList<>(stages),
                new LinkedHashMap<>(presetVariables),
                this.scriptingEnvironment,
                compiled,
                inlineAll,
                this.timingConfig);

        log.debug("Workflow '{}' built with {} stages", name, stages.size());

        if (this.observabilityBuilder != null && workflow instanceof IObservable obs) {
            ObservabilityBinding binding = this.observabilityBuilder.getBinding();
            if (binding != null) {
                binding.attachSource(obs);
                log.trace("Workflow '{}' attached to ObservabilityBinding", name);
            } else {
                log.warn("Workflow '{}' has an IObservabilityBuilder dependency but its binding is null — observability builder not built yet?", name);
            }
        }
        return workflow;
    }

    /**
     * Pre-build dependency hook. No-op: the {@link IScriptingEnvironment} is
     * delivered via {@link #acceptScriptingEnvironment(IScriptingEnvironment)}
     * by the parent {@link WorkflowsBuilder}.
     *
     * @param dependency the resolved dependency (unused)
     */
    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // IScriptingEnvironment is delivered via acceptScriptingEnvironment()
        // by the parent WorkflowsBuilder; nothing pre-build dependency-side.
    }

    /**
     * Post-build dependency hook. No-op.
     *
     * @param dependency the resolved dependency (unused)
     */
    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build processing needed
    }

    /**
     * Accepts a provided dependency builder, capturing the
     * {@link IInjectionContextBuilder} and {@link IObservabilityBuilder} when
     * recognised before delegating to the superclass.
     *
     * @param dependency the dependency builder being provided
     * @return this builder for chaining
     * @throws DslException if the superclass rejects the dependency
     */
    @Override
    public IWorkflowBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder builder) {
            this.injectionContextBuilder = builder;
        }
        if (dependency instanceof IObservabilityBuilder obs) {
            this.observabilityBuilder = obs;
        }
        return super.provide(dependency);
    }

    /**
     * Renders a human-readable description of the workflow as currently
     * configured, without building it.
     *
     * @return the rendered workflow diagram
     */
    @Override
    public String describeWorkflow() {
        return renderer.render(name, stages, presetVariables, inlineAll);
    }

    /**
     * Builds an immutable {@link WorkflowDescriptor} snapshot of the workflow's
     * stages and scripts as currently configured.
     *
     * @return the descriptor snapshot
     */
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
}
