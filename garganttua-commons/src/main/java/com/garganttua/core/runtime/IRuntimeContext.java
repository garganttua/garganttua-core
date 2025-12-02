package com.garganttua.core.runtime;

import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.injection.IDiContext;

/**
 * Runtime execution context that maintains state and provides access to shared resources during workflow execution.
 *
 * <p>
 * IRuntimeContext extends {@link IDiContext} to provide dependency injection capabilities while adding
 * runtime-specific features such as variable storage, exception tracking, input/output management,
 * and result code handling. The context is accessible to all stages and steps during runtime execution.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Variable Management</b> - Store and retrieve named variables for inter-step communication</li>
 *   <li><b>Input Access</b> - Access the original input object from any step</li>
 *   <li><b>Output Management</b> - Set the final output to be returned in the runtime result</li>
 *   <li><b>Exception Tracking</b> - Record and query exceptions that occurred during execution</li>
 *   <li><b>Result Codes</b> - Set custom result codes to indicate success/failure states</li>
 *   <li><b>Dependency Injection</b> - Access injected beans through the inherited IDiContext methods</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Operation
 * public void processOrder(@Input Order order, @Context IRuntimeContext<Order, OrderResult> ctx) {
 *     // Access input
 *     Optional<Order> input = ctx.getInput();
 *
 *     // Store intermediate results in variables
 *     ctx.setVariable("processedAt", Instant.now());
 *     ctx.setVariable("orderId", order.getId());
 *
 *     // Set custom result code
 *     ctx.setCode(0); // Success
 *
 *     // Set output
 *     ctx.setOutput(new OrderResult(order.getId(), "PROCESSED"));
 * }
 *
 * @FallBack
 * public void handleError(@Exception Throwable e, @Context IRuntimeContext<Order, OrderResult> ctx) {
 *     // Access exception information
 *     Optional<RuntimeException> runtimeEx = ctx.getException(RuntimeException.class);
 *     Optional<Integer> code = ctx.getCode();
 *
 *     // Set error output
 *     ctx.setOutput(new OrderResult(null, "ERROR: " + e.getMessage()));
 *     ctx.setCode(50); // Error code
 * }
 * }</pre>
 *
 * @param <InputType> the input type for the runtime execution
 * @param <OutputType> the output type for the runtime execution
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see IRuntimeResult
 * @see com.garganttua.core.injection.IDiContext
 * @see com.garganttua.core.runtime.annotations.Context
 */
public interface IRuntimeContext<InputType, OutputType> extends IDiContext {

    /**
     * Returns the current runtime result being constructed during execution.
     *
     * <p>
     * The result contains the accumulated output, timing information, exception records,
     * and result codes up to this point in the execution.
     * </p>
     *
     * @return the current runtime result
     * @see IRuntimeResult
     */
    IRuntimeResult<InputType, OutputType> getResult();

    /**
     * Stores a named variable in the context for inter-step communication.
     *
     * <p>
     * Variables are useful for passing data between steps without coupling them directly.
     * They are stored in the context and can be retrieved by any subsequent step.
     * </p>
     *
     * @param <VariableType> the type of the variable being stored
     * @param variableName the name of the variable
     * @param variable the variable value to store
     * @see #getVariable(String, Class)
     * @see com.garganttua.core.runtime.annotations.Variable
     */
    <VariableType> void setVariable(String variableName, VariableType variable);

    /**
     * Retrieves a named variable from the context.
     *
     * <p>
     * Returns an Optional containing the variable if it exists and matches the requested type,
     * or empty if the variable does not exist or has a different type.
     * </p>
     *
     * @param <VariableType> the expected type of the variable
     * @param variableName the name of the variable to retrieve
     * @param variableType the class of the expected variable type
     * @return an Optional containing the variable if found and of the correct type, otherwise empty
     * @see #setVariable(String, Object)
     */
    <VariableType> Optional<VariableType> getVariable(String variableName, Class<VariableType> variableType);

    /**
     * Returns the original input object provided to the runtime execution.
     *
     * @return an Optional containing the input if available, otherwise empty
     * @see com.garganttua.core.runtime.annotations.Input
     */
    Optional<InputType> getInput();

    /**
     * Retrieves an exception of a specific type that was recorded during execution.
     *
     * <p>
     * This method searches the exception records for an exception matching the requested type.
     * It supports polymorphic matching, so requesting a superclass will match subclass exceptions.
     * </p>
     *
     * @param <ExceptionType> the type of exception to retrieve
     * @param exceptionType the class of the exception to find
     * @return an Optional containing the exception if found, otherwise empty
     * @see #recordException(RuntimeExceptionRecord)
     */
    <ExceptionType> Optional<ExceptionType> getException(Class<ExceptionType> exceptionType);

    /**
     * Returns the current result code set during execution.
     *
     * <p>
     * Result codes are integers that indicate the execution status.
     * By convention, 0 indicates success and non-zero values indicate errors.
     * </p>
     *
     * @return an Optional containing the result code if set, otherwise empty
     * @see #setCode(int)
     * @see IRuntime#GENERIC_RUNTIME_SUCCESS_CODE
     * @see IRuntime#GENERIC_RUNTIME_ERROR_CODE
     */
    Optional<Integer> getCode();

    /**
     * Returns the message of the most recent exception recorded.
     *
     * @return an Optional containing the exception message if available, otherwise empty
     */
    Optional<String> getExceptionMessage();

    /**
     * Sets the output to be returned in the final runtime result.
     *
     * <p>
     * This method is typically called by steps annotated with {@code @Output} or methods
     * configured as output methods in the DSL. The output can be set multiple times during
     * execution; the last value set will be the final output.
     * </p>
     *
     * @param output the output value to set
     * @see com.garganttua.core.runtime.annotations.Output
     */
    void setOutput(OutputType output);

    /**
     * Checks if the provided class is compatible with the runtime's output type.
     *
     * @param class1 the class to check for compatibility
     * @return true if the class is assignable to the output type, false otherwise
     * @see #getOutputType()
     */
    boolean isOfOutputType(Class<?> class1);

    /**
     * Returns the output type class for this runtime context.
     *
     * @return the class representing the output type
     * @see #isOfOutputType(Class)
     */
    Class<?> getOutputType();

    /**
     * Sets a custom result code for the runtime execution.
     *
     * <p>
     * Result codes are used to indicate the status of execution beyond simple success/failure.
     * Custom codes can be used to represent different business outcomes or error conditions.
     * </p>
     *
     * @param i the result code to set
     * @see #getCode()
     * @see com.garganttua.core.runtime.annotations.Code
     */
    void setCode(int i);

    /**
     * Records an exception that occurred during runtime execution.
     *
     * <p>
     * Exception records contain detailed information about where and why an exception occurred,
     * including the runtime name, stage, step, exception type, and whether it caused execution
     * to abort.
     * </p>
     *
     * @param runtimeExceptionRecord the exception record to store
     * @see RuntimeExceptionRecord
     * @see #findException(RuntimeExceptionRecord)
     */
    void recordException(RuntimeExceptionRecord runtimeExceptionRecord);

    /**
     * Searches for an exception record matching the provided pattern.
     *
     * <p>
     * The pattern can specify criteria such as runtime name, stage, step, exception type,
     * and abort status. Only non-null fields in the pattern are used for matching.
     * </p>
     *
     * @param pattern the pattern to match against recorded exceptions
     * @return an Optional containing the matching exception record if found, otherwise empty
     * @see RuntimeExceptionRecord#matches(RuntimeExceptionRecord)
     */
    Optional<RuntimeExceptionRecord> findException(RuntimeExceptionRecord pattern);

    /**
     * Finds the exception record that caused the runtime execution to abort, if any.
     *
     * @return an Optional containing the aborting exception record if found, otherwise empty
     * @see RuntimeExceptionRecord
     */
    Optional<RuntimeExceptionRecord> findAbortingExceptionReport();

    /**
     * Returns the unique identifier for this runtime execution instance.
     *
     * <p>
     * The UUID can be used for tracking, logging, and correlating this execution
     * with external systems or distributed traces.
     * </p>
     *
     * @return the UUID for this execution
     * @see IRuntime#execute(UUID, Object)
     */
    UUID uuid();

}
