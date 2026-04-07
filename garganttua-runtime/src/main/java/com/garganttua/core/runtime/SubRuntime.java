package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.execution.ExecutorChain;
import com.garganttua.core.execution.IExecutorChain;

import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight runtime that executes steps within an existing {@link IRuntimeContext}.
 *
 * <p>
 * Unlike {@link Runtime}, a {@code SubRuntime} does not create its own context.
 * It executes its steps in the caller's context, sharing variables, output, and
 * exception state. This is used for statement groups {@code (...)} in scripts,
 * where statements execute sequentially in the enclosing scope.
 * </p>
 *
 * @param <InputType>  the input type of the enclosing runtime
 * @param <OutputType> the output type of the enclosing runtime
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class SubRuntime<InputType, OutputType> {

    private final String name;
    private final Map<String, IRuntimeStep<?, InputType, OutputType>> steps;

    public SubRuntime(String name, Map<String, IRuntimeStep<?, InputType, OutputType>> steps) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.steps = Collections.unmodifiableMap(
                new java.util.LinkedHashMap<>(Objects.requireNonNull(steps, "Steps cannot be null")));
    }

    /**
     * Executes all steps sequentially in the given context.
     *
     * @param context the parent runtime context (shared, not copied)
     * @throws RuntimeException if execution fails
     */
    public void execute(IRuntimeContext<InputType, OutputType> context) {
        log.atDebug().log("[SubRuntime {}] Executing {} steps in parent context", name, steps.size());

        IExecutorChain<IRuntimeContext<InputType, OutputType>> chain = new ExecutorChain<>(true);
        steps.values().forEach(step -> step.defineExecutionStep(chain));
        chain.execute(context);

        log.atDebug().log("[SubRuntime {}] Execution complete", name);
    }
}
