package com.garganttua.core.workflow.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.chaining.CodeAction;

/**
 * Renders a human-readable textual representation of a workflow structure.
 *
 * <p>
 * This renderer produces an ANSI-colored box-drawing diagram showing stages,
 * scripts, data flows, bypass arrows, conditions, error handling, and code actions.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class WorkflowRenderer {

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

    /**
     * Renders the workflow as a formatted ANSI string.
     *
     * @param name            the workflow name
     * @param stages          the workflow stages
     * @param presetVariables the preset variables
     * @param inlineAll       whether all scripts are forced inline
     * @return the rendered workflow string
     */
    public String render(String name, List<WorkflowStage> stages,
                         Map<String, Object> presetVariables, boolean inlineAll) {
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
            for (var script : stage.scripts()) {
                for (var entry : script.getInputs().entrySet()) {
                    String inputExpr = entry.getValue();
                    if (inputExpr.startsWith("@")) {
                        String varName = inputExpr.substring(1);
                        stageInputs.add(varName);
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

            // Stage condition
            if (stage.condition() != null && !stage.condition().isEmpty()) {
                sb.append(stageColor).append("  │").append(RESET);
                String condLine = "  " + YELLOW + "⚡ when: " + RESET + CYAN + truncate(stage.condition(), 55) + RESET;
                sb.append(condLine).append(padRight("", boxWidth - stripAnsi(condLine).length()));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
            }

            // Stage wrap
            if (stage.hasWrap()) {
                sb.append(stageColor).append("  │").append(RESET);
                String wrapLine = "  " + MAGENTA + "⟳ wrap: " + RESET + CYAN + truncate(stage.wrapExpression(), 55) + RESET;
                sb.append(wrapLine).append(padRight("", boxWidth - stripAnsi(wrapLine).length()));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
            }

            // Stage catch
            if (stage.catchExpression() != null && !stage.catchExpression().isEmpty()) {
                sb.append(stageColor).append("  │").append(RESET);
                String catchLine = "  " + RED + "! catch: " + RESET + YELLOW + truncate(stage.catchExpression(), 55) + RESET;
                sb.append(catchLine).append(padRight("", boxWidth - stripAnsi(catchLine).length()));
                sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
            }

            // Stage catchDownstream
            if (stage.catchDownstreamExpression() != null && !stage.catchDownstreamExpression().isEmpty()) {
                sb.append(stageColor).append("  │").append(RESET);
                String catchDsLine = "  " + RED + "* catchDownstream: " + RESET + YELLOW + truncate(stage.catchDownstreamExpression(), 45) + RESET;
                sb.append(catchDsLine).append(padRight("", boxWidth - stripAnsi(catchDsLine).length()));
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

                // Script condition
                if (script.getCondition() != null && !script.getCondition().isEmpty()) {
                    sb.append(stageColor).append("  │").append(RESET);
                    String condLine = "    " + YELLOW + "⚡ when: " + RESET + CYAN + truncate(script.getCondition(), 55) + RESET;
                    sb.append(condLine).append(padRight("", boxWidth - stripAnsi(condLine).length()));
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

                // Error handling - catch
                if (script.getCatchExpression() != null) {
                    sb.append(stageColor).append("  │").append(RESET);
                    String catchLine = "    " + RED + "! " + RESET + YELLOW + truncate(script.getCatchExpression(), 60) + RESET;
                    sb.append(catchLine).append(padRight("", boxWidth - stripAnsi(catchLine).length()));
                    sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                }

                // Error handling - catchDownstream
                if (script.getCatchDownstreamExpression() != null) {
                    sb.append(stageColor).append("  │").append(RESET);
                    String catchDsLine = "    " + RED + "* " + RESET + YELLOW + truncate(script.getCatchDownstreamExpression(), 60) + RESET;
                    sb.append(catchDsLine).append(padRight("", boxWidth - stripAnsi(catchDsLine).length()));
                    sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                }

                // Code actions
                if (!script.getCodeActions().isEmpty()) {
                    for (var codeEntry : script.getCodeActions().entrySet()) {
                        if (codeEntry.getValue() != CodeAction.CONTINUE) {
                            sb.append(stageColor).append("  │").append(RESET);
                            String codeLine = "    " + MAGENTA + "↩ onCode(" + codeEntry.getKey() + ") → "
                                    + RESET + YELLOW + codeEntry.getValue().name() + RESET;
                            sb.append(codeLine).append(padRight("", boxWidth - stripAnsi(codeLine).length()));
                            sb.append(stageColor).append("│").append(RESET).append(bypassCol).append("\n");
                        }
                    }
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
}
