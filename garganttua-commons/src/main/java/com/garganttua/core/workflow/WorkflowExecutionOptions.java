package com.garganttua.core.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Options for filtering which stages are executed in a workflow.
 *
 * <p>
 * By default, all stages are executed. Use the {@link Builder} to specify
 * a starting stage, stopping stage, or stages to skip.
 * </p>
 *
 * @param startFrom   the first stage to execute (inclusive), or empty for the first stage
 * @param stopAfter   the last stage to execute (inclusive), or empty for the last stage
 * @param skipStages  stage names to exclude from execution
 * @param executionId the execution id to pin for observability correlation, or
 *                    empty to let {@code Workflow.execute} generate a random one.
 *                    Deliberately excluded from {@link #hasFiltering()}: pinning
 *                    an id must not rewrite the script, so the precompiled cache
 *                    path stays active.
 * @since 2.0.0-ALPHA01
 */
public record WorkflowExecutionOptions(
    Optional<String> startFrom,
    Optional<String> stopAfter,
    Set<String> skipStages,
    Optional<UUID> executionId
) {
    private static final WorkflowExecutionOptions NONE = new WorkflowExecutionOptions(
            Optional.empty(), Optional.empty(), Collections.emptySet(), Optional.empty());

    public WorkflowExecutionOptions {
        skipStages = skipStages != null ? Collections.unmodifiableSet(new HashSet<>(skipStages)) : Collections.emptySet();
        startFrom = startFrom != null ? startFrom : Optional.empty();
        stopAfter = stopAfter != null ? stopAfter : Optional.empty();
        executionId = executionId != null ? executionId : Optional.empty();
    }

    /**
     * Returns shared options with no filtering (all stages executed).
     *
     * @return the singleton no-op options instance
     */
    public static WorkflowExecutionOptions none() {
        return NONE;
    }

    /**
     * Indicates whether any stage filtering is active.
     *
     * <p>
     * Note that {@link #executionId()} is intentionally excluded: pinning an id
     * must not be treated as filtering so the precompiled cache path stays active.
     * </p>
     *
     * @return {@code true} if a start, stop, or skip filter is set
     */
    public boolean hasFiltering() {
        return startFrom.isPresent() || stopAfter.isPresent() || !skipStages.isEmpty();
    }

    /**
     * Creates a new builder for execution options.
     *
     * @return a fresh {@link Builder}
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
        private UUID executionId;

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
         * Pins the execution id for observability correlation. When set,
         * {@code Workflow.execute} reuses it instead of generating a random
         * UUID, so {@code stage:*}/{@code script:*} events share the caller's
         * id (e.g. the api's EXECUTION_UUID behind {@code api:operation:*}).
         */
        public Builder executionId(UUID executionId) {
            this.executionId = executionId;
            return this;
        }

        /**
         * Builds the execution options from the configured values.
         *
         * @return a new immutable {@link WorkflowExecutionOptions}
         */
        public WorkflowExecutionOptions build() {
            return new WorkflowExecutionOptions(
                    Optional.ofNullable(startFrom),
                    Optional.ofNullable(stopAfter),
                    skipStages,
                    Optional.ofNullable(executionId));
        }
    }
}
