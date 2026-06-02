package com.garganttua.core.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.reflection.IClass;

/**
 * Immutable outcome of a single workflow execution.
 *
 * @param uuid             correlation identifier of the execution
 * @param output           the workflow's final output value, may be {@code null}
 * @param code             the exit code ({@code 0} on success), may be {@code null}
 * @param variables        unmodifiable snapshot of the final workflow variables
 * @param stageOutputs     unmodifiable map of per-stage outputs keyed by {@code stage.name}
 * @param start            instant the execution started
 * @param stop             instant the execution stopped
 * @param exception        the failure cause, if the workflow aborted
 * @param exceptionMessage the failure message, if any
 */
public record WorkflowResult(
    UUID uuid,
    Object output,
    Integer code,
    Map<String, Object> variables,
    Map<String, Object> stageOutputs,
    Instant start,
    Instant stop,
    Optional<Throwable> exception,
    Optional<String> exceptionMessage
) {
    public WorkflowResult {
        variables = variables != null ? Collections.unmodifiableMap(variables) : Collections.emptyMap();
        stageOutputs = stageOutputs != null ? Collections.unmodifiableMap(stageOutputs) : Collections.emptyMap();
        exception = exception != null ? exception : Optional.empty();
        exceptionMessage = exceptionMessage != null ? exceptionMessage : Optional.empty();
    }

    /**
     * @return {@code true} if the workflow completed with exit code {@code 0} and no exception
     */
    public boolean isSuccess() {
        return code != null && code == 0 && exception.isEmpty();
    }

    /**
     * @return {@code true} if the workflow aborted with an exception
     */
    public boolean hasAborted() {
        return exception.isPresent();
    }

    /**
     * @return the elapsed time between {@link #start()} and {@link #stop()}
     */
    public Duration duration() {
        return Duration.between(start, stop);
    }

    /**
     * Looks up a stage output by stage name and output name, type-checked against {@code type}.
     *
     * @param stage the stage name
     * @param name  the output name within the stage
     * @param type  the expected type
     * @param <T>   the output type
     * @return the output value if present and assignable to {@code type}, otherwise empty
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getStageOutput(String stage, String name, IClass<T> type) {
        String key = stage + "." + name;
        Object value = stageOutputs.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Looks up a workflow variable by name, type-checked against {@code type}.
     *
     * @param name the variable name
     * @param type the expected type
     * @param <T>  the variable type
     * @return the variable value if present and assignable to {@code type}, otherwise empty
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String name, IClass<T> type) {
        Object value = variables.get(name);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Builds a successful result with no exception.
     *
     * @return a {@code WorkflowResult} with the given output, code and timings
     */
    public static WorkflowResult success(UUID uuid, Object output, int code, Map<String, Object> variables,
            Map<String, Object> stageOutputs, Instant start, Instant stop) {
        return new WorkflowResult(uuid, output, code, variables, stageOutputs, start, stop,
                Optional.empty(), Optional.empty());
    }

    /**
     * Builds a failed result carrying the exception and an exit code of {@code -1}.
     *
     * @param exception the failure cause
     * @return a {@code WorkflowResult} describing the abort
     */
    public static WorkflowResult failure(UUID uuid, Instant start, Instant stop, Throwable exception) {
        return new WorkflowResult(uuid, null, -1, Collections.emptyMap(), Collections.emptyMap(),
                start, stop, Optional.of(exception), Optional.ofNullable(exception.getMessage()));
    }
}
