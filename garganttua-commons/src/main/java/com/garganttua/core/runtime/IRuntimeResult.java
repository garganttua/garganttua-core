package com.garganttua.core.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates the result of a runtime workflow execution, including output, timing metrics, and exception information.
 *
 * <p>
 * IRuntimeResult is returned by {@link IRuntime#execute(Object)} after the runtime completes execution.
 * It provides access to the final output produced, execution timing information, result codes,
 * and any exceptions that occurred during the workflow.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><b>Output</b> - The final output produced by the runtime execution</li>
 *   <li><b>Timing Metrics</b> - Duration of execution in various formats (millis, nanos, pretty-printed)</li>
 *   <li><b>Result Code</b> - Integer status code indicating success or specific error conditions</li>
 *   <li><b>Exception Records</b> - Detailed records of all exceptions that occurred</li>
 *   <li><b>Abort Status</b> - Whether execution was aborted due to an uncaught exception</li>
 *   <li><b>Execution UUID</b> - Unique identifier for tracking this execution instance</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IRuntime<Order, OrderResult> runtime = runtimesBuilder.build().get("orderProcessing");
 * Optional<IRuntimeResult<Order, OrderResult>> resultOpt = runtime.execute(order);
 *
 * if (resultOpt.isPresent()) {
 *     IRuntimeResult<Order, OrderResult> result = resultOpt.get();
 *
 *     // Check execution status
 *     if (!result.hasAborted() && result.code() == 0) {
 *         // Success - get the output
 *         OrderResult output = result.output();
 *         System.out.println("Order processed: " + output.getStatus());
 *
 *         // Log timing information
 *         System.out.println("Execution took: " + result.prettyDuration());
 *     } else {
 *         // Error occurred
 *         System.err.println("Execution failed with code: " + result.code());
 *
 *         // Get exception details
 *         Optional<RuntimeExceptionRecord> abortingEx = result.getAbortingException();
 *         abortingEx.ifPresent(ex ->
 *             System.err.println("Aborting exception: " + ex.exceptionMessage())
 *         );
 *
 *         // Get all exceptions
 *         Set<RuntimeExceptionRecord> exceptions = result.getExceptions();
 *         exceptions.forEach(ex ->
 *             System.err.println("Exception in " + ex.stageName() + "." + ex.stepName())
 *         );
 *     }
 *
 *     // Execution tracking
 *     System.out.println("Execution UUID: " + result.uuid());
 * }
 * }</pre>
 *
 * @param <InputType> the input type that was processed
 * @param <OutputType> the output type produced by the runtime
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see IRuntimeContext
 * @see RuntimeExceptionRecord
 */
public interface IRuntimeResult<InputType, OutputType> {

    /**
     * Returns the final output produced by the runtime execution.
     *
     * <p>
     * The output is set during execution by steps annotated with {@code @Output} or
     * methods configured to produce output in the DSL. If no output was explicitly set,
     * this may return null depending on the runtime implementation.
     * </p>
     *
     * @return the output produced by the runtime, or null if no output was set
     * @see com.garganttua.core.runtime.annotations.Output
     * @see IRuntimeContext#setOutput(Object)
     */
    OutputType output();

    /**
     * Returns the total execution duration.
     *
     * @return the execution duration as a Duration object
     * @see #prettyDuration()
     */
    Duration duration();

    /**
     * Returns a human-readable representation of the execution duration.
     *
     * <p>
     * The format is optimized for readability, showing appropriate units
     * (e.g., "125ms", "2.5s", "1m 30s").
     * </p>
     *
     * @return a formatted string representing the duration
     * @see #duration()
     */
    String prettyDuration();

    /**
     * Returns the execution duration in milliseconds as a Duration object.
     *
     * @return the duration in milliseconds
     * @see #durationMillis()
     */
    Duration durationInMillis();

    /**
     * Returns the execution duration in milliseconds as a primitive long.
     *
     * @return the duration in milliseconds
     * @see #durationInMillis()
     */
    long durationMillis();

    /**
     * Returns the execution duration in nanoseconds.
     *
     * @return the duration in nanoseconds
     * @see #prettyDurationInNanos()
     */
    long durationInNanos();

    /**
     * Returns a human-readable representation of the execution duration in nanoseconds.
     *
     * <p>
     * This is useful for high-precision timing measurements where millisecond
     * precision is insufficient.
     * </p>
     *
     * @return a formatted string representing the duration in nanoseconds
     * @see #durationInNanos()
     */
    String prettyDurationInNanos();

    /**
     * Returns the result code indicating the execution status.
     *
     * <p>
     * By convention, 0 indicates success ({@link IRuntime#GENERIC_RUNTIME_SUCCESS_CODE})
     * and 50 indicates a generic error ({@link IRuntime#GENERIC_RUNTIME_ERROR_CODE}).
     * Custom codes can be set using {@link IRuntimeContext#setCode(int)} or
     * the {@code @Code} annotation.
     * </p>
     *
     * @return the result code, or null if no code was set
     * @see IRuntime#GENERIC_RUNTIME_SUCCESS_CODE
     * @see IRuntime#GENERIC_RUNTIME_ERROR_CODE
     * @see com.garganttua.core.runtime.annotations.Code
     */
    Integer code();

    /**
     * Returns all exception records that were recorded during execution.
     *
     * <p>
     * This includes both caught exceptions (handled by {@code @Catch} or fallback methods)
     * and uncaught exceptions that may have caused execution to abort.
     * </p>
     *
     * @return a set of all exception records, or an empty set if no exceptions occurred
     * @see RuntimeExceptionRecord
     * @see #getAbortingException()
     */
    Set<RuntimeExceptionRecord> getExceptions();

    /**
     * Returns the exception record that caused the runtime execution to abort, if any.
     *
     * <p>
     * An aborting exception is one that was not caught and caused the execution to
     * terminate prematurely. This typically occurs when a step throws an exception
     * and is configured with {@code abortOnUncatchedException = true}.
     * </p>
     *
     * @return an Optional containing the aborting exception record if present, otherwise empty
     * @see #hasAborted()
     * @see com.garganttua.core.runtime.annotations.Operation#abortOnUncatchedException()
     */
    Optional<RuntimeExceptionRecord> getAbortingException();

    /**
     * Indicates whether the runtime execution was aborted due to an uncaught exception.
     *
     * <p>
     * When true, the workflow did not complete normally and may have incomplete output.
     * Use {@link #getAbortingException()} to get details about the exception that caused
     * the abort.
     * </p>
     *
     * @return true if execution was aborted, false if it completed normally
     * @see #getAbortingException()
     */
    boolean hasAborted();

    /**
     * Returns the unique identifier for this runtime execution instance.
     *
     * <p>
     * The UUID is either automatically generated or explicitly provided when calling
     * {@link IRuntime#execute(UUID, Object)}. It can be used for tracking, logging,
     * and correlating this execution with external systems.
     * </p>
     *
     * @return the execution UUID
     * @see IRuntime#execute(UUID, Object)
     * @see IRuntimeContext#uuid()
     */
    Object uuid();

}
