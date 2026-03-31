package com.garganttua.core.workflow.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.Workflow;
import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.generator.ScriptGenerator;
import com.garganttua.core.workflow.renderer.WorkflowRenderer;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing {@link IWorkflow} instances with fluent API.
 */
@Slf4j
public class WorkflowBuilder extends AbstractDependentBuilder<IWorkflowBuilder, IWorkflow>
        implements IWorkflowBuilder {

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            DependencySpec.require(IClass.getClass(IInjectionContextBuilder.class), DependencyPhase.BUILD),
            DependencySpec.require(IClass.getClass(IExpressionContextBuilder.class), DependencyPhase.BUILD));

    private final ScriptGenerator scriptGenerator = new ScriptGenerator();
    private final WorkflowRenderer renderer = new WorkflowRenderer();

    private String name = "unnamed-workflow";
    private final Map<String, Object> presetVariables = new LinkedHashMap<>();
    private final List<WorkflowStage> stages = new ArrayList<>();
    private IExpressionContext expressionContext;
    private IInjectionContext injectionContext;
    private boolean inlineAll = false;

    private WorkflowBuilder() {
        super(DEPENDENCIES);
        log.atTrace().log("WorkflowBuilder created");
    }

    public static IWorkflowBuilder create() {
        return new WorkflowBuilder();
    }

    @Override
    public IWorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public IWorkflowBuilder variable(String name, Object value) {
        this.presetVariables.put(name, value);
        return this;
    }

    @Override
    public IWorkflowBuilder inlineAll() {
        this.inlineAll = true;
        return this;
    }

    @Override
    public IWorkflowStageBuilder stage(String name) {
        WorkflowStageBuilder stageBuilder = new WorkflowStageBuilder(name);
        stageBuilder.setUp(this);
        return stageBuilder;
    }

    void addStage(WorkflowStage stage) {
        this.stages.add(stage);
    }

    @Override
    protected IWorkflow doBuild() throws DslException {
        log.atTrace().log("Building workflow '{}'", name);

        if (stages.isEmpty()) {
            throw new DslException("Workflow must have at least one stage");
        }

        if (injectionContext == null) {
            throw new DslException("InjectionContext is required");
        }

        if (expressionContext == null) {
            throw new DslException("ExpressionContext is required");
        }

        String generatedScript;
        try {
            generatedScript = scriptGenerator.generate(name, stages, presetVariables, inlineAll);
            log.atDebug().log("Generated workflow script for '{}':\n{}", name, generatedScript);
        } catch (WorkflowException e) {
            throw new DslException("Failed to generate workflow script", e);
        }

        Workflow workflow = new Workflow(
                name,
                generatedScript,
                new ArrayList<>(stages),
                new LinkedHashMap<>(presetVariables),
                expressionContext,
                injectionContext,
                inlineAll);

        log.atDebug().log("Workflow '{}' built with {} stages", name, stages.size());
        return workflow;
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext ctx) {
            this.injectionContext = ctx;
        } else if (dependency instanceof IExpressionContext ctx) {
            this.expressionContext = ctx;
        }
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build processing needed
    }

    @Override
    public IWorkflowBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        return super.provide(dependency);
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
}
