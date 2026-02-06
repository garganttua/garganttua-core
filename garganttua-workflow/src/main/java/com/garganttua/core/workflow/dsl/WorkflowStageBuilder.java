package com.garganttua.core.workflow.dsl;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowScript.ScriptSource;
import com.garganttua.core.workflow.WorkflowStage;

import lombok.Setter;

public class WorkflowStageBuilder implements IWorkflowStageBuilder {

    @Setter
    private IWorkflowBuilder up;

    private final String name;
    private final List<WorkflowScript> scripts = new ArrayList<>();
    private String wrapExpression;
    private String catchExpression;
    private String catchDownstreamExpression;

    public WorkflowStageBuilder(String name) {
        this.name = name;
    }

    @Override
    public IWorkflowScriptBuilder script(String content) {
        return createScriptBuilder(ScriptSource.of(content));
    }

    @Override
    public IWorkflowScriptBuilder script(File file) {
        return createScriptBuilder(ScriptSource.of(file));
    }

    @Override
    public IWorkflowScriptBuilder script(Path path) {
        return createScriptBuilder(ScriptSource.of(path));
    }

    @Override
    public IWorkflowScriptBuilder script(InputStream inputStream) {
        return createScriptBuilder(ScriptSource.of(inputStream));
    }

    @Override
    public IWorkflowScriptBuilder script(Reader reader) {
        return createScriptBuilder(ScriptSource.of(reader));
    }

    private IWorkflowScriptBuilder createScriptBuilder(ScriptSource source) {
        WorkflowScriptBuilder builder = new WorkflowScriptBuilder(source);
        builder.setUp(this);
        return builder;
    }

    void addScript(WorkflowScript script) {
        this.scripts.add(script);
    }

    @Override
    public IWorkflowStageBuilder wrap(String expression) {
        this.wrapExpression = expression;
        return this;
    }

    @Override
    public IWorkflowStageBuilder catch_(String expression) {
        this.catchExpression = expression;
        return this;
    }

    @Override
    public IWorkflowStageBuilder catchDownstream(String expression) {
        this.catchDownstreamExpression = expression;
        return this;
    }

    @Override
    public IWorkflowBuilder up() {
        if (up instanceof WorkflowBuilder workflowBuilder) {
            workflowBuilder.addStage(build());
        }
        return up;
    }

    @Override
    public WorkflowStage build() throws DslException {
        return new WorkflowStage(name, new ArrayList<>(scripts), wrapExpression, catchExpression, catchDownstreamExpression);
    }
}
