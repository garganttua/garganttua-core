package com.garganttua.core.workflow.dsl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowScript.ScriptSource;
import com.garganttua.core.workflow.chaining.CodeAction;
import com.garganttua.core.workflow.header.ScriptHeader;
import com.garganttua.core.workflow.header.ScriptHeader.HeaderInput;
import com.garganttua.core.workflow.header.ScriptHeader.HeaderOutput;
import com.garganttua.core.workflow.header.ScriptHeaderParser;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowScriptBuilder implements IWorkflowScriptBuilder {

    private static final ScriptHeaderParser HEADER_PARSER = new ScriptHeaderParser();

    @Setter
    private IWorkflowStageBuilder up;

    private final ScriptSource source;
    private String name;
    private String description;
    private String condition;
    private boolean inline = false;
    private String catchExpression;
    private String catchDownstreamExpression;
    private final Map<String, String> inputs = new LinkedHashMap<>();
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
    public IWorkflowScriptBuilder when(String expression) {
        this.condition = expression;
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
        // Merge header metadata from script content before building
        Map<String, String> mergedInputs = new LinkedHashMap<>(inputs);
        Map<String, String> mergedOutputs = new HashMap<>(outputs);
        String mergedDescription = description;
        String mergedCatch = catchExpression;
        String mergedCatchDownstream = catchDownstreamExpression;
        ScriptSource effectiveSource = source;

        try {
            // Build a temporary script to load content
            WorkflowScript temp = WorkflowScript.builder().source(source).build();
            String content = temp.loadContent();
            if (content != null) {
                HEADER_PARSER.parse(content).ifPresent(header -> {
                    log.atDebug().log("Parsed #@workflow header from script '{}': {} inputs, {} outputs",
                            name, header.inputs().size(), header.outputs().size());
                    mergeHeader(header, mergedInputs, mergedOutputs);
                });
            }
        } catch (WorkflowException e) {
            log.atDebug().log("Could not load script content for header parsing: {}", e.getMessage());
        }

        return WorkflowScript.builder()
                .name(name)
                .description(mergedDescription)
                .source(effectiveSource)
                .inline(inline)
                .condition(condition)
                .catchExpression(mergedCatch)
                .catchDownstreamExpression(mergedCatchDownstream)
                .inputs(mergedInputs)
                .outputs(mergedOutputs)
                .codeActions(new HashMap<>(codeActions))
                .build();
    }

    private void mergeHeader(ScriptHeader header,
                             Map<String, String> mergedInputs,
                             Map<String, String> mergedOutputs) {
        // Merge inputs: header provides defaults, explicit .input() calls take precedence
        for (int i = 0; i < header.inputs().size(); i++) {
            HeaderInput hi = header.inputs().get(i);
            if (!mergedInputs.containsKey(hi.name())) {
                mergedInputs.put(hi.name(), "@" + hi.name());
            }
        }
        // Merge outputs: header provides defaults, explicit .output() calls take precedence
        for (HeaderOutput ho : header.outputs()) {
            if (!mergedOutputs.containsKey(ho.name())) {
                mergedOutputs.put(ho.name(), ho.variable());
            }
        }
    }
}
