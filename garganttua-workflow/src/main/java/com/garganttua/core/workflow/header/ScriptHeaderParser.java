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

    // Matches: @in name: Type  or  @in name: Type [position]
    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "@in\\s+(\\w+):\\s*(\\w+)(?:\\s*\\[(\\d+)\\])?",
        Pattern.MULTILINE
    );

    // Matches: @out name -> variable: Type
    private static final Pattern OUTPUT_PATTERN = Pattern.compile(
        "@out\\s+(\\w+)\\s*->\\s*(\\w+)(?::\\s*(\\w+))?",
        Pattern.MULTILINE
    );

    // Matches: @return code: LABEL
    private static final Pattern RETURN_CODE_PATTERN = Pattern.compile(
        "@return\\s+(\\d+):\\s*(.+)",
        Pattern.MULTILINE
    );

    // Matches: @catch expression
    private static final Pattern CATCH_PATTERN = Pattern.compile(
        "@catch\\s+(.+)",
        Pattern.MULTILINE
    );

    // Matches: @catchDownstream expression
    private static final Pattern CATCH_DOWNSTREAM_PATTERN = Pattern.compile(
        "@catchDownstream\\s+(.+)",
        Pattern.MULTILINE
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
        String catchExpression = parseCatch(headerContent);
        String catchDownstreamExpression = parseCatchDownstream(headerContent);

        return Optional.of(new ScriptHeader(description, inputs, outputs, returnCodes,
                catchExpression, catchDownstreamExpression));
    }

    public String stripHeader(String scriptContent) {
        return HEADER_PATTERN.matcher(scriptContent).replaceFirst("").trim();
    }

    private String parseDescription(String headerContent) {
        // Description is all text before the first @in, @out, or @return tag
        String[] lines = headerContent.split("\\n");
        StringBuilder desc = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.replaceAll("^#\\s*", "").trim();
            if (trimmed.startsWith("@in ") || trimmed.startsWith("@out ") || trimmed.startsWith("@return ")
                    || trimmed.startsWith("@catch ") || trimmed.startsWith("@catchDownstream ")) {
                break;
            }
            if (!trimmed.isEmpty()) {
                if (desc.length() > 0) {
                    desc.append(" ");
                }
                desc.append(trimmed);
            }
        }
        String result = desc.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private List<HeaderInput> parseInputs(String headerContent) {
        List<HeaderInput> inputs = new ArrayList<>();
        Matcher matcher = INPUT_PATTERN.matcher(headerContent);
        int index = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            String positionStr = matcher.group(3);
            Integer position = positionStr != null ? Integer.parseInt(positionStr) : index;
            inputs.add(new HeaderInput(name, position, type, null));
            index++;
        }
        return inputs;
    }

    private List<HeaderOutput> parseOutputs(String headerContent) {
        List<HeaderOutput> outputs = new ArrayList<>();
        Matcher matcher = OUTPUT_PATTERN.matcher(headerContent);
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
        Matcher matcher = RETURN_CODE_PATTERN.matcher(headerContent);
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2).trim();
            returnCodes.put(code, description);
        }
        return returnCodes;
    }

    private String parseCatch(String headerContent) {
        Matcher matcher = CATCH_PATTERN.matcher(headerContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String parseCatchDownstream(String headerContent) {
        Matcher matcher = CATCH_DOWNSTREAM_PATTERN.matcher(headerContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}