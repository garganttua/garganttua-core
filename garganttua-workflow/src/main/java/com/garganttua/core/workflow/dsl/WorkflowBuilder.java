package com.garganttua.core.workflow.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.Workflow;
import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.generator.ScriptGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing {@link IWorkflow} instances with fluent API.
 */
@Slf4j
public class WorkflowBuilder extends AbstractDependentBuilder<IWorkflowBuilder, IWorkflow>
        implements IWorkflowBuilder {

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD),
            DependencySpec.require(IExpressionContextBuilder.class, DependencyPhase.BUILD));

    private final ScriptGenerator scriptGenerator = new ScriptGenerator();

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

    // ANSI Color codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String ITALIC = "\u001B[3m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BG_BLUE = "\u001B[44m";

    private record BypassFlow(String variable, int sourceStage, int targetStage) {
        boolean isActive(int currentStage) {
            return currentStage > sourceStage && currentStage <= targetStage;
        }
        boolean startsAt(int stageIdx) { return stageIdx == sourceStage; }
        boolean endsAt(int stageIdx) { return stageIdx == targetStage; }
    }

    @Override
    public String describeWorkflow() {
        StringBuilder sb = new StringBuilder();
        int boxWidth = 70;

        sb.append("\n");

        // Header
        sb.append(CYAN).append("  ╔").append("═".repeat(boxWidth)).append("╗\n");
        sb.append("  ║").append(RESET).append(BOLD).append(BG_BLUE).append(WHITE);
        sb.append(centerText("WORKFLOW: " + name, boxWidth));
        sb.append(RESET).append(CYAN).append("║\n");
        sb.append("  ╚").append("═".repeat(boxWidth)).append("╝").append(RESET).append("\n\n");

        // Collect data flow information
        Map<String, Integer> outputToStageIndex = new HashMap<>();
        Map<String, List<String>> stageOutputVars = new HashMap<>();
        for (int i = 0; i < stages.size(); i++) {
            var stage = stages.get(i);
            List<String> outputs = new ArrayList<>();
            for (var script : stage.scripts()) {
                for (String outVar : script.getOutputs().keySet()) {
                    outputs.add(outVar);
                    outputToStageIndex.put(outVar, i);
                }
            }
            stageOutputVars.put(stage.name(), outputs);
        }

        // Collect config var usage
        Map<String, List<String>> configVarToStages = new HashMap<>();
        for (var configVar : presetVariables.keySet()) {
            List<String> usingStages = new ArrayList<>();
            for (var stage : stages) {
                for (var script : stage.scripts()) {
                    for (var inputExpr : script.getInputs().values()) {
                        if (inputExpr.equals("@" + configVar) && !usingStages.contains(stage.name())) {
                            usingStages.add(stage.name());
                        }
                    }
                }
            }
            if (!usingStages.isEmpty()) {
                configVarToStages.put(configVar, usingStages);
            }
        }

        // Configuration section
        if (!presetVariables.isEmpty() || inlineAll) {
            sb.append(DIM).append("  ┌─ ").append(RESET).append(YELLOW).append(BOLD)
              .append("Configuration").append(RESET).append(DIM).append(" ─").append("─".repeat(50)).append("┐\n");

            if (inlineAll) {
                sb.append("  │  ").append(MAGENTA).append("Mode: ").append(RESET)
                  .append(YELLOW).append("INLINE ALL").append(RESET)
                  .append(padRight("", 49)).append(DIM).append("│\n").append(RESET);
            }

            if (!presetVariables.isEmpty()) {
                sb.append("  │  ").append(CYAN).append("Variables:").append(RESET)
                  .append(padRight("", 52)).append(DIM).append("│\n").append(RESET);
                for (var entry : presetVariables.entrySet()) {
                    String varName = entry.getKey();
                    List<String> usingStages = configVarToStages.get(varName);
                    String usageInfo = usingStages != null ? DIM + " → " + CYAN + String.join(", ", usingStages) + RESET : "";
                    String varLine = "     " + GREEN + "@" + varName + RESET + " = " +
                                    YELLOW + formatValue(entry.getValue()) + RESET + usageInfo;
                    sb.append("  │  ").append(varLine)
                      .append(padRight("", boxWidth - stripAnsi(varLine).length() - 4))
                      .append(DIM).append("│\n").append(RESET);
                }
            }
            sb.append(DIM).append("  └").append("─".repeat(boxWidth)).append("┘").append(RESET).append("\n\n");
        }

        // Detect bypass flows
        List<BypassFlow> bypassFlows = new ArrayList<>();
        for (int targetIdx = 0; targetIdx < stages.size(); targetIdx++) {
            for (var script : stages.get(targetIdx).scripts()) {
                for (var inputExpr : script.getInputs().values()) {
                    if (inputExpr.startsWith("@")) {
                        String varName = inputExpr.substring(1);
                        Integer sourceIdx = outputToStageIndex.get(varName);
                        if (sourceIdx != null && targetIdx - sourceIdx > 1) {
                            bypassFlows.add(new BypassFlow(varName, sourceIdx, targetIdx));
                        }
                    }
                }
            }
        }

        // Assign lanes to bypasses
        Map<BypassFlow, Integer> bypassLanes = new HashMap<>();
        for (var bypass : bypassFlows) {
            int lane = 0;
            while (true) {
                final int testLane = lane;
                boolean conflict = bypassLanes.entrySet().stream()
                    .anyMatch(e -> e.getValue() == testLane && rangesOverlap(e.getKey(), bypass));
                if (!conflict) {
                    bypassLanes.put(bypass, lane);
                    break;
                }
                lane++;
            }
        }
        int maxLane = bypassLanes.values().stream().mapToInt(i -> i).max().orElse(-1);

        // Stages
        for (int stageIdx = 0; stageIdx < stages.size(); stageIdx++) {
            final int currentStageIdx = stageIdx;
            var stage = stages.get(stageIdx);
            boolean isFirst = stageIdx == 0;
            boolean isLast = stageIdx == stages.size() - 1;

            // Collect inputs
            List<String> stageInputs = new ArrayList<>();
            Map<String, String> inputToProvider = new HashMap<>();
            for (var script : stage.scripts()) {
                for (var entry : script.getInputs().entrySet()) {
                    String inputExpr = entry.getValue();
                    if (inputExpr.startsWith("@")) {
                        String varName = inputExpr.substring(1);
                        stageInputs.add(varName);
                        String providerStage = findProviderStage(varName, stageIdx);
                        if (providerStage != null) {
                            inputToProvider.put(varName, providerStage);
                        }
                    }
                }
            }

            String bypassCol = buildBypassColumn(stageIdx, bypassFlows, bypassLanes, maxLane);

            // Input arrows
            if (!isFirst) {
                List<String> directInputs = stageInputs.stream()
                    .filter(v -> {
                        Integer src = outputToStageIndex.get(v);
                        return src != null && currentStageIdx - src == 1;
                    })
                    .toList();

                if (!directInputs.isEmpty()) {
                    sb.append(GREEN).append("                         │").append(RESET).append(bypassCol).append("\n");
                    for (String input : directInputs) {
                        sb.append(GREEN).append("                         ├─ ").append(RESET)
                          .append(DIM).append("@").append(input).append(RESET).append(bypassCol).append("\n");
                    }
                    sb.append(GREEN).append("                         ▼").append(RESET).append(bypassCol).append("\n");
                }
            }

            // Bypass arrivals
            List<BypassFlow> endingBypasses = bypassFlows.stream()
                .filter(b -> b.endsAt(currentStageIdx))
                .toList();

            for (var bypass : endingBypasses) {
                int lane = bypassLanes.get(bypass);
                sb.append(YELLOW).append("  ╰").append("─".repeat(3 + lane * 4)).append("▶ ")
                  .append(DIM).append("@").append(bypass.variable()).append(" (bypass from ")
                  .append(stages.get(bypass.sourceStage()).name()).append(")").append(RESET).append("\n");
            }

            // Stage box
            String stageColor = getStageColor(stageIdx);
            sb.append(stageColor).append("  ┌").append("─".repeat(boxWidth)).append("┐")
              .append(RESET).append(bypassCol).append("\n");
            sb.append(stageColor).append("  │").append(RESET).append(BOLD).append(stageColor);
            String stageTitle = "  STAGE " + (stageIdx + 1) + ": " + stage.name().toUpperCase() + "  ";
            sb.append(stageTitle).append(RESET).append(stageColor);
            sb.append(padRight("", boxWidth - stageTitle.length())).append("│")
              .append(RESET).append(bypassCol).append("\n");

            // Stage wrap/catch
            if (stage.hasWrap()) {
                sb.append(stageColor).append("  │").append(RESET);
                String wrapLine = "  " + MAGENTA + "⟳ wrap: " + RESET + CYAN + truncate(stage.wrapExpression(), 55) + RESET;
                sb.append(wrapLine).append(padRight("", boxWidth - stripAnsi(wrapLine).length()));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
            }
            if (stage.hasCatch() && stage.catchExpression() != null) {
                sb.append(stageColor).append("  │").append(RESET);
                String catchLine = "  " + RED + "! catch: " + RESET + YELLOW + truncate(stage.catchExpression(), 55) + RESET;
                sb.append(catchLine).append(padRight("", boxWidth - stripAnsi(catchLine).length()));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
            }

            sb.append(stageColor).append("  ├").append("─".repeat(boxWidth)).append("┤")
              .append(RESET).append(bypassCol).append("\n");

            // Scripts
            for (int scriptIdx = 0; scriptIdx < stage.scripts().size(); scriptIdx++) {
                var script = stage.scripts().get(scriptIdx);
                String scriptName = script.getName() != null ? script.getName() : "script-" + scriptIdx;
                String sourceTag = script.isFile() ? (script.isInline() || inlineAll ? "inline" : "include") : "inline";

                sb.append(stageColor).append("  │").append(RESET);
                sb.append("  ").append(WHITE).append(BOLD).append("◆ ").append(RESET)
                  .append(WHITE).append(scriptName).append(RESET)
                  .append(DIM).append(" [").append(sourceTag).append("]").append(RESET);
                sb.append(padRight("", boxWidth - 6 - scriptName.length() - sourceTag.length() - 3));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");

                if (script.getDescription() != null && !script.getDescription().isEmpty()) {
                    sb.append(stageColor).append("  │").append(RESET);
                    String descLine = "    " + DIM + ITALIC + truncate(script.getDescription(), boxWidth - 8) + RESET;
                    sb.append(descLine).append(padRight("", boxWidth - stripAnsi(descLine).length()));
                    sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                }

                // Inputs
                if (!script.getInputs().isEmpty()) {
                    sb.append(stageColor).append("  │").append(RESET).append(DIM)
                      .append("    Inputs:").append(RESET)
                      .append(padRight("", boxWidth - 11))
                      .append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");

                    int inputIndex = 0;
                    for (var entry : script.getInputs().entrySet()) {
                        sb.append(stageColor).append("  │").append(RESET);
                        String inputLine = "      " + CYAN + "@" + inputIndex + RESET + " " +
                                          GREEN + entry.getKey() + RESET + DIM + " ← " + RESET +
                                          YELLOW + entry.getValue() + RESET;
                        sb.append(inputLine).append(padRight("", boxWidth - stripAnsi(inputLine).length()));
                        sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                        inputIndex++;
                    }
                }

                // Outputs
                if (!script.getOutputs().isEmpty()) {
                    sb.append(stageColor).append("  │").append(RESET).append(DIM)
                      .append("    Outputs:").append(RESET)
                      .append(padRight("", boxWidth - 12))
                      .append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");

                    int outputIndex = 0;
                    for (var entry : script.getOutputs().entrySet()) {
                        sb.append(stageColor).append("  │").append(RESET);
                        String outputLine = "      " + MAGENTA + "[" + outputIndex + "]" + RESET + " " +
                                           GREEN + entry.getKey() + RESET + DIM + " ← @" + RESET +
                                           CYAN + entry.getValue() + RESET;
                        sb.append(outputLine).append(padRight("", boxWidth - stripAnsi(outputLine).length()));
                        sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                        outputIndex++;
                    }
                }

                // Error handling
                if (script.getCatchExpression() != null) {
                    sb.append(stageColor).append("  │").append(RESET);
                    String catchLine = "    " + RED + "! " + RESET + YELLOW + truncate(script.getCatchExpression(), 60) + RESET;
                    sb.append(catchLine).append(padRight("", boxWidth - stripAnsi(catchLine).length()));
                    sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                }

                if (scriptIdx < stage.scripts().size() - 1) {
                    sb.append(stageColor).append("  │").append(RESET).append(DIM)
                      .append("  ").append("·".repeat(boxWidth - 4)).append(RESET)
                      .append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                }
            }

            sb.append(stageColor).append("  └").append("─".repeat(boxWidth)).append("┘")
              .append(RESET).append(bypassCol).append("\n");

            // Output arrows
            if (!isLast) {
                List<String> outputs = stageOutputVars.getOrDefault(stage.name(), List.of());
                List<BypassFlow> startingBypasses = bypassFlows.stream()
                    .filter(b -> b.startsAt(currentStageIdx))
                    .toList();

                if (!outputs.isEmpty()) {
                    String bypassColPipe = buildBypassColumn(stageIdx + 1, bypassFlows, bypassLanes, maxLane);
                    sb.append(GREEN).append("                         │").append(RESET).append(bypassColPipe).append("\n");

                    for (String output : outputs) {
                        BypassFlow startingBypass = startingBypasses.stream()
                            .filter(b -> b.variable().equals(output))
                            .findFirst()
                            .orElse(null);

                        if (startingBypass != null) {
                            int lane = bypassLanes.get(startingBypass);
                            String targetName = stages.get(startingBypass.targetStage()).name();
                            sb.append(GREEN).append("                         ├──▶ ").append(RESET)
                              .append(DIM).append("@").append(output).append(RESET)
                              .append(YELLOW).append(" ─".repeat(lane + 1)).append("╮")
                              .append(DIM).append(" (to ").append(targetName).append(")").append(RESET).append("\n");
                        } else {
                            sb.append(GREEN).append("                         ├──▶ ").append(RESET)
                              .append(DIM).append("@").append(output).append(RESET).append(bypassColPipe).append("\n");
                        }
                    }
                }
            }
        }

        // Final outputs
        if (!stages.isEmpty()) {
            var lastStage = stages.get(stages.size() - 1);
            List<String> finalOutputs = stageOutputVars.getOrDefault(lastStage.name(), List.of());
            if (!finalOutputs.isEmpty()) {
                sb.append("\n");
                sb.append(GREEN).append(BOLD).append("  ┌─ Final Outputs ").append("─".repeat(51)).append("┐\n");
                for (String output : finalOutputs) {
                    sb.append("  │  ").append(RESET).append(MAGENTA).append("◀══ ").append(RESET)
                      .append(GREEN).append(BOLD).append("@").append(output).append(RESET);
                    sb.append(padRight("", boxWidth - 7 - output.length()));
                    sb.append(GREEN).append(BOLD).append("│\n");
                }
                sb.append("  └").append("─".repeat(boxWidth)).append("┘").append(RESET).append("\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    private boolean rangesOverlap(BypassFlow a, BypassFlow b) {
        return !(a.targetStage() <= b.sourceStage() || b.targetStage() <= a.sourceStage());
    }

    private String buildBypassColumn(int stageIdx, List<BypassFlow> bypasses,
                                      Map<BypassFlow, Integer> lanes, int maxLane) {
        if (maxLane < 0) return "";
        StringBuilder col = new StringBuilder("  ");
        for (int lane = 0; lane <= maxLane; lane++) {
            final int currentLane = lane;
            BypassFlow activeBypass = bypasses.stream()
                .filter(b -> lanes.getOrDefault(b, -1) == currentLane && b.isActive(stageIdx))
                .findFirst()
                .orElse(null);
            if (activeBypass != null) {
                col.append(YELLOW).append("│").append(RESET).append("   ");
            } else {
                col.append("    ");
            }
        }
        return col.toString();
    }

    private String getStageColor(int index) {
        String[] colors = {BLUE, MAGENTA, CYAN, GREEN, YELLOW};
        return colors[index % colors.length];
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, width - padding - text.length()));
    }

    private String padRight(String s, int length) {
        if (length <= 0) return "";
        return " ".repeat(length);
    }

    private String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private String findProviderStage(String variable, int currentStageIndex) {
        for (int i = 0; i < currentStageIndex; i++) {
            var stage = stages.get(i);
            for (var script : stage.scripts()) {
                if (script.getOutputs().containsKey(variable)) {
                    return stage.name();
                }
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + value + "\"";
        return value.toString();
    }

    @Override
    public WorkflowDescriptor getDescriptor() {
        List<WorkflowDescriptor.StageDescriptor> stageDescriptors = stages.stream()
                .map(stage -> new WorkflowDescriptor.StageDescriptor(
                        stage.name(),
                        stage.wrapExpression(),
                        stage.catchExpression(),
                        stage.catchDownstreamExpression(),
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
                                                .collect(java.util.stream.Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        e -> e.getValue().name()))))
                                .toList()))
                .toList();

        return new WorkflowDescriptor(
                name,
                inlineAll,
                new LinkedHashMap<>(presetVariables),
                stageDescriptors);
    }
}
