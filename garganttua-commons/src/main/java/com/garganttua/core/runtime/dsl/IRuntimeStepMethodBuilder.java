package com.garganttua.core.runtime.dsl;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;

/**
 * Builder for configuring a step's main method execution.
 *
 * <p>
 * IRuntimeStepMethodBuilder provides a fluent DSL for configuring how a step's main method
 * is invoked and how its result is handled. It extends {@link IMethodBinderBuilder} to provide
 * method binding capabilities and adds runtime-specific configuration for output, variables,
 * exception handling, and result codes.
 * </p>
 *
 * <p>
 * This builder is the DSL equivalent of configuring {@code @Operation} methods with annotations
 * like {@code @Output}, {@code @Variable}, {@code @Catch}, and {@code @Code}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * stepBuilder
 *     .method()
 *         .name("processOrder")
 *         .parameter(Input.class)
 *         .parameter(Context.class)
 *         .output(true)
 *         .code(200)
 *         .katch(IllegalArgumentException.class).code(400).end()
 *         .katch(IOException.class).code(500).end()
 *         .abortOnUncatchedException(false)
 *         .nullable(false)
 *         .end();
 * }</pre>
 *
 * @param <ExecutionReturn> the return type of the method
 * @param <StepObjectType> the type of object containing the method
 * @param <InputType> the runtime input type
 * @param <OutputType> the runtime output type
 * @since 2.0.0-ALPHA01
 * @see IRuntimeStepBuilder
 * @see IRuntimeStepCatchBuilder
 * @see com.garganttua.core.runtime.annotations.Operation
 */
public interface IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                IMethodBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>,
                IContextBuilderObserver {

        /**
         * Checks if the method throws a specific exception type.
         *
         * @param exception the exception class to check
         * @return true if the method declares this exception, false otherwise
         */
        boolean isThrown(Class<? extends Throwable> exception);

        /**
         * Configures the method's return value to be stored as a variable.
         *
         * <p>
         * Equivalent to {@code @Variable(name = "variableName")} on a method.
         * </p>
         *
         * @param variableName the name for storing the return value
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Variable
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(String variableName);

        /**
         * Configures whether the method's return value should be the runtime output.
         *
         * <p>
         * Equivalent to {@code @Output} annotation on a method.
         * </p>
         *
         * @param output true to set return value as output, false otherwise
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Output
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output);

        /**
         * Sets the result code to be set when the method executes successfully.
         *
         * <p>
         * Equivalent to {@code @Code(value)} annotation on a method.
         * </p>
         *
         * @param code the result code to set
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Code
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(Integer code);

        /**
         * Sets a condition that determines whether this method should execute.
         *
         * <p>
         * The condition is evaluated before the method executes. If it returns false,
         * the method is skipped.
         * </p>
         *
         * @param conditionBuilder the condition builder
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Condition
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
                        IConditionBuilder conditionBuilder);

        /**
         * Begins configuration of exception catching for a specific exception type.
         *
         * <p>
         * Equivalent to {@code @Catch(exception = ...)} annotation.
         * </p>
         *
         * @param exception the exception class to catch
         * @return a builder for configuring the catch behavior
         * @throws DslException if the catch configuration is invalid
         * @see IRuntimeStepCatchBuilder
         * @see com.garganttua.core.runtime.annotations.Catch
         */
        IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(
                        Class<? extends Throwable> exception) throws DslException;

        /**
         * Configures whether to abort runtime execution on uncaught exceptions.
         *
         * <p>
         * Equivalent to {@code @Operation(abortOnUncatchedException = ...)} attribute.
         * </p>
         *
         * @param abort true to abort on uncaught exceptions, false to continue
         * @return this builder for method chaining
         * @see com.garganttua.core.runtime.annotations.Operation#abortOnUncatchedException()
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abortOnUncatchedException(
                        boolean abort);

        /**
         * Configures whether null return values are acceptable.
         *
         * @param nullable true to allow null returns, false otherwise
         * @return this builder for method chaining
         */
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
                        boolean nullable);
}
