package com.garganttua.core.workflow.header;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ScriptHeader(
    String description,
    List<HeaderInput> inputs,
    List<HeaderOutput> outputs,
    Map<Integer, String> returnCodes
) {
    public ScriptHeader {
        description = description != null ? description.trim() : null;
        inputs = inputs != null ? Collections.unmodifiableList(inputs) : Collections.emptyList();
        outputs = outputs != null ? Collections.unmodifiableList(outputs) : Collections.emptyList();
        returnCodes = returnCodes != null ? Collections.unmodifiableMap(returnCodes) : Collections.emptyMap();
    }

    /**
     * Returns true if this header has a description.
     */
    public boolean hasDescription() {
        return description != null && !description.isEmpty();
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
        public HeaderInput(String name, String type) {
            this(name, null, type, null);
        }

        public HeaderInput(String name, int position, String type) {
            this(name, position, type, null);
        }

        /**
         * Returns the effective position (explicit or derived from list index).
         */
        public int effectivePosition(int listIndex) {
            return position != null ? position : listIndex;
        }
    }

    public record HeaderOutput(String name, String variable, String type, String description) {
        public HeaderOutput(String name, String variable) {
            this(name, variable, null, null);
        }
    }

    public static ScriptHeader empty() {
        return new ScriptHeader(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
    }
}
