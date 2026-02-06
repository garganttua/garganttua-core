package com.garganttua.core.workflow.dsl;

import java.util.HashMap;
import java.util.Map;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowScript.ScriptSource;
import com.garganttua.core.workflow.chaining.CodeAction;

import lombok.Setter;

public class WorkflowScriptBuilder implements IWorkflowScriptBuilder {

    @Setter
    private IWorkflowStageBuilder up;

    private final ScriptSource source;
    private String name;
    private String description;
    private boolean inline = false;
    private String catchExpression;
    private String catchDownstreamExpression;
    private final Map<String, String> inputs = new HashMap<>();
    private final Map<String, String> outputs = new HashMap<>();
    private final Map<Integer, CodeAction> codeActions = new HashMap<>();

    public WorkflowScriptBuilder(ScriptSource source) {
        this.source = source;
    }

    @Override
    public IWorkflowScriptBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public IWorkflowScriptBuilder description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public IWorkflowScriptBuilder inline() {
        this.inline = true;
        return this;
    }

    @Override
    public IWorkflowScriptBuilder input(String scriptVar, String expression) {
        this.inputs.put(scriptVar, expression);
        return this;
    }

    @Override
    public IWorkflowScriptBuilder output(String workflowVar, String scriptVar) {
        this.outputs.put(workflowVar, scriptVar);
        return this;
    }

    @Override
    public IWorkflowScriptBuilder onCode(int code, CodeAction action) {
        this.codeActions.put(code, action);
        return this;
    }

    @Override
    public IWorkflowScriptBuilder catch_(String expression) {
        this.catchExpression = expression;
        return this;
    }

    @Override
    public IWorkflowScriptBuilder catchDownstream(String expression) {
        this.catchDownstreamExpression = expression;
        return this;
    }

    @Override
    public IWorkflowStageBuilder up() {
        if (up instanceof WorkflowStageBuilder stageBuilder) {
            stageBuilder.addScript(build());
        }
        return up;
    }

    @Override
    public WorkflowScript build() throws DslException {
        return WorkflowScript.builder()
                .name(name)
                .description(description)
                .source(source)
                .inline(inline)
                .catchExpression(catchExpression)
                .catchDownstreamExpression(catchDownstreamExpression)
                .inputs(new HashMap<>(inputs))
                .outputs(new HashMap<>(outputs))
                .codeActions(new HashMap<>(codeActions))
                .build();
    }
}
