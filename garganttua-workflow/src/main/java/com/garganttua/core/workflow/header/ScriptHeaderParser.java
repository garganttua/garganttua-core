package com.garganttua.core.workflow.header;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.header.ScriptHeader.HeaderInput;
import com.garganttua.core.workflow.header.ScriptHeader.HeaderOutput;

public class ScriptHeaderParser {

    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "#@workflow\\s*(.*?)#@end",
        Pattern.DOTALL | Pattern.MULTILINE
    );

    // Matches: - name: varName position: N type: Type
    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "-\\s*name:\\s*(\\w+)(?:\\s*position:\\s*(\\d+))?(?:\\s*type:\\s*(\\w+))?",
        Pattern.MULTILINE
    );

    private static final Pattern OUTPUT_PATTERN = Pattern.compile(
        "-\\s*name:\\s*(\\w+)\\s*variable:\\s*(\\w+)(?:\\s*type:\\s*(\\w+))?",
        Pattern.MULTILINE
    );

    private static final Pattern RETURN_CODE_PATTERN = Pattern.compile(
        "(\\d+):\\s*(.+)",
        Pattern.MULTILINE
    );

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
        "description:\\s*(.+?)(?=\\n#|inputs:|outputs:|returnCodes:|$)",
        Pattern.DOTALL | Pattern.MULTILINE
    );

    public Optional<ScriptHeader> parse(String scriptContent) throws WorkflowException {
        Matcher headerMatcher = HEADER_PATTERN.matcher(scriptContent);
        if (!headerMatcher.find()) {
            return Optional.empty();
        }

        String headerContent = headerMatcher.group(1);
        String description = parseDescription(headerContent);
        List<HeaderInput> inputs = parseInputs(headerContent);
        List<HeaderOutput> outputs = parseOutputs(headerContent);
        Map<Integer, String> returnCodes = parseReturnCodes(headerContent);

        return Optional.of(new ScriptHeader(description, inputs, outputs, returnCodes));
    }

    public String stripHeader(String scriptContent) {
        return HEADER_PATTERN.matcher(scriptContent).replaceFirst("").trim();
    }

    private String parseDescription(String headerContent) {
        Matcher matcher = DESCRIPTION_PATTERN.matcher(headerContent);
        if (matcher.find()) {
            String desc = matcher.group(1).trim();
            // Clean up the description: remove comment markers and normalize whitespace
            desc = desc.replaceAll("#\\s*", " ").replaceAll("\\s+", " ").trim();
            return desc.isEmpty() ? null : desc;
        }
        return null;
    }

    private List<HeaderInput> parseInputs(String headerContent) {
        List<HeaderInput> inputs = new ArrayList<>();
        int inputsStart = headerContent.indexOf("inputs:");
        if (inputsStart == -1) {
            return inputs;
        }

        int outputsStart = headerContent.indexOf("outputs:");
        int returnCodesStart = headerContent.indexOf("returnCodes:");
        int endIndex = headerContent.length();
        if (outputsStart != -1 && outputsStart > inputsStart) {
            endIndex = Math.min(endIndex, outputsStart);
        }
        if (returnCodesStart != -1 && returnCodesStart > inputsStart) {
            endIndex = Math.min(endIndex, returnCodesStart);
        }

        String inputsSection = headerContent.substring(inputsStart, endIndex);
        Matcher matcher = INPUT_PATTERN.matcher(inputsSection);
        int index = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            String positionStr = matcher.group(2);
            String type = matcher.group(3);
            Integer position = positionStr != null ? Integer.parseInt(positionStr) : index;
            inputs.add(new HeaderInput(name, position, type, null));
            index++;
        }
        return inputs;
    }

    private List<HeaderOutput> parseOutputs(String headerContent) {
        List<HeaderOutput> outputs = new ArrayList<>();
        int outputsStart = headerContent.indexOf("outputs:");
        if (outputsStart == -1) {
            return outputs;
        }

        int returnCodesStart = headerContent.indexOf("returnCodes:");
        int endIndex = headerContent.length();
        if (returnCodesStart != -1 && returnCodesStart > outputsStart) {
            endIndex = returnCodesStart;
        }

        String outputsSection = headerContent.substring(outputsStart, endIndex);
        Matcher matcher = OUTPUT_PATTERN.matcher(outputsSection);
        while (matcher.find()) {
            String name = matcher.group(1);
            String variable = matcher.group(2);
            String type = matcher.group(3);
            outputs.add(new HeaderOutput(name, variable, type, null));
        }
        return outputs;
    }

    private Map<Integer, String> parseReturnCodes(String headerContent) {
        Map<Integer, String> returnCodes = new HashMap<>();
        int returnCodesStart = headerContent.indexOf("returnCodes:");
        if (returnCodesStart == -1) {
            return returnCodes;
        }

        String returnCodesSection = headerContent.substring(returnCodesStart);
        Matcher matcher = RETURN_CODE_PATTERN.matcher(returnCodesSection);
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2).trim();
            returnCodes.put(code, description);
        }
        return returnCodes;
    }
}
