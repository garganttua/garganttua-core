package com.garganttua.core.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Options for filtering which stages are executed in a workflow.
 *
 * <p>
 * By default, all stages are executed. Use the {@link Builder} to specify
 * a starting stage, stopping stage, or stages to skip.
 * </p>
 *
 * @param startFrom  the first stage to execute (inclusive), or empty for the first stage
 * @param stopAfter  the last stage to execute (inclusive), or empty for the last stage
 * @param skipStages stage names to exclude from execution
 * @since 2.0.0-ALPHA01
 */
public record WorkflowExecutionOptions(
    Optional<String> startFrom,
    Optional<String> stopAfter,
    Set<String> skipStages
) {
    private static final WorkflowExecutionOptions NONE = new WorkflowExecutionOptions(
            Optional.empty(), Optional.empty(), Collections.emptySet());

    public WorkflowExecutionOptions {
        skipStages = skipStages != null ? Collections.unmodifiableSet(new HashSet<>(skipStages)) : Collections.emptySet();
        startFrom = startFrom != null ? startFrom : Optional.empty();
        stopAfter = stopAfter != null ? stopAfter : Optional.empty();
    }

    /**
     * Returns options with no filtering (all stages executed).
     */
    public static WorkflowExecutionOptions none() {
        return NONE;
    }

    /**
     * Returns true if any filtering is active.
     */
    public boolean hasFiltering() {
        return startFrom.isPresent() || stopAfter.isPresent() || !skipStages.isEmpty();
    }

    /**
     * Creates a new builder for execution options.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link WorkflowExecutionOptions}.
     */
    public static class Builder {
        private String startFrom;
        private String stopAfter;
        private final Set<String> skipStages = new HashSet<>();

        private Builder() {}

        /**
         * Sets the first stage to execute (inclusive).
         */
        public Builder startFrom(String stageName) {
            this.startFrom = stageName;
            return this;
        }

        /**
         * Sets the last stage to execute (inclusive).
         */
        public Builder stopAfter(String stageName) {
            this.stopAfter = stageName;
            return this;
        }

        /**
         * Adds a stage to skip during execution.
         */
        public Builder skipStage(String stageName) {
            this.skipStages.add(stageName);
            return this;
        }

        /**
         * Adds multiple stages to skip during execution.
         */
        public Builder skipStages(Set<String> stageNames) {
            this.skipStages.addAll(stageNames);
            return this;
        }

        /**
         * Builds the execution options.
         */
        public WorkflowExecutionOptions build() {
            return new WorkflowExecutionOptions(
                    Optional.ofNullable(startFrom),
                    Optional.ofNullable(stopAfter),
                    skipStages);
        }
    }
}
