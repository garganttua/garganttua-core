package com.garganttua.core.workflow.header;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of a script's {@code #@workflow ... #@end} metadata block.
 *
 * <p>Captures the declared description, input/output mappings, named return codes,
 * and optional catch expressions. All collection components are defensively made
 * unmodifiable and {@code null} values are normalised to empties by the canonical
 * constructor.
 *
 * @param description               free-text description, or {@code null}
 * @param inputs                    declared input parameters
 * @param outputs                   declared output mappings
 * @param returnCodes               exit-code-to-label mappings
 * @param catchExpression           immediate catch expression, or {@code null}
 * @param catchDownstreamExpression downstream catch expression, or {@code null}
 */
public record ScriptHeader(
    String description,
    List<HeaderInput> inputs,
    List<HeaderOutput> outputs,
    Map<Integer, String> returnCodes,
    String catchExpression,
    String catchDownstreamExpression
) {
    public ScriptHeader {
        description = description != null ? description.trim() : null;
        inputs = inputs != null ? Collections.unmodifiableList(inputs) : Collections.emptyList();
        outputs = outputs != null ? Collections.unmodifiableList(outputs) : Collections.emptyList();
        returnCodes = returnCodes != null ? Collections.unmodifiableMap(returnCodes) : Collections.emptyMap();
        catchExpression = catchExpression != null ? catchExpression.trim() : null;
        catchDownstreamExpression = catchDownstreamExpression != null ? catchDownstreamExpression.trim() : null;
    }

    /** Convenience constructor without catch expressions. */
    public ScriptHeader(String description, List<HeaderInput> inputs, List<HeaderOutput> outputs,
            Map<Integer, String> returnCodes) {
        this(description, inputs, outputs, returnCodes, null, null);
    }

    /** {@return {@code true} if this header declares a non-empty description} */
    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    /** {@return {@code true} if this header declares a non-empty catch expression} */
    public boolean hasCatch() {
        return catchExpression != null && !catchExpression.isEmpty();
    }

    /** {@return {@code true} if this header declares a non-empty catchDownstream expression} */
    public boolean hasCatchDownstream() {
        return catchDownstreamExpression != null && !catchDownstreamExpression.isEmpty();
    }

    /**
     * Describes a script input parameter.
     *
     * @param name        the input variable name (accessible as @name inside the script)
     * @param position    the positional index (accessible as @0, @1, etc.)
     * @param type        the expected type
     * @param description optional description
     */
    public record HeaderInput(String name, Integer position, String type, String description) {
        /** Convenience constructor without an explicit position or description. */
        public HeaderInput(String name, String type) {
            this(name, null, type, null);
        }

        /** Convenience constructor without a description. */
        public HeaderInput(String name, int position, String type) {
            this(name, position, type, null);
        }

        /**
         * Resolves the effective positional index for this input.
         *
         * @param listIndex the fallback index (this input's order in the header)
         * @return the explicit {@code position} if set, otherwise {@code listIndex}
         */
        public int effectivePosition(int listIndex) {
            return position != null ? position : listIndex;
        }
    }

    /**
     * Describes a script output mapping.
     *
     * @param name        the workflow-side output name
     * @param variable    the script-side variable to read the value from
     * @param type        the declared type, or {@code null}
     * @param description optional description, or {@code null}
     */
    public record HeaderOutput(String name, String variable, String type, String description) {
        /** Convenience constructor without a type or description. */
        public HeaderOutput(String name, String variable) {
            this(name, variable, null, null);
        }
    }

    /** {@return an empty header with no description, inputs, outputs, return codes, or catch clauses} */
    public static ScriptHeader empty() {
        return new ScriptHeader(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), null, null);
    }
}
