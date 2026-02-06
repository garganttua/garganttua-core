package com.garganttua.core.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    public boolean isSuccess() {
        return code != null && code == 0 && exception.isEmpty();
    }

    public boolean hasAborted() {
        return exception.isPresent();
    }

    public Duration duration() {
        return Duration.between(start, stop);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getStageOutput(String stage, String name, Class<T> type) {
        String key = stage + "." + name;
        Object value = stageOutputs.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String name, Class<T> type) {
        Object value = variables.get(name);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public static WorkflowResult success(UUID uuid, Object output, int code, Map<String, Object> variables,
            Map<String, Object> stageOutputs, Instant start, Instant stop) {
        return new WorkflowResult(uuid, output, code, variables, stageOutputs, start, stop,
                Optional.empty(), Optional.empty());
    }

    public static WorkflowResult failure(UUID uuid, Instant start, Instant stop, Throwable exception) {
        return new WorkflowResult(uuid, null, -1, Collections.emptyMap(), Collections.emptyMap(),
                start, stop, Optional.of(exception), Optional.ofNullable(exception.getMessage()));
    }
}
