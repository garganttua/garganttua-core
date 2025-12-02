package com.garganttua.core.runtime;

/**
 * Immutable record containing detailed information about an exception that occurred during runtime execution.
 *
 * <p>
 * RuntimeExceptionRecord captures comprehensive metadata about exceptions, including where they occurred
 * (runtime, stage, step), what type of exception it was, whether it caused execution to abort, and any
 * associated error code. These records are stored in the runtime context and included in the final result.
 * </p>
 *
 * <h2>Key Information Captured</h2>
 * <ul>
 *   <li><b>Location</b> - Runtime name, stage name, and step name where the exception occurred</li>
 *   <li><b>Exception Details</b> - The exception type, instance, and message</li>
 *   <li><b>Error Code</b> - Custom integer code associated with the exception</li>
 *   <li><b>Abort Status</b> - Whether the exception caused execution to terminate</li>
 *   <li><b>Executable Reference</b> - Reference to the method or code that threw the exception</li>
 * </ul>
 *
 * <h2>Pattern Matching</h2>
 * <p>
 * The {@link #matches(RuntimeExceptionRecord)} method supports flexible pattern matching where
 * null fields in the pattern are treated as wildcards. This enables queries like "find any
 * IllegalArgumentException in the validation stage" or "find the exception that caused abort".
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an exception record
 * RuntimeExceptionRecord record = new RuntimeExceptionRecord(
 *     "orderProcessing",           // runtime name
 *     "validation",                // stage name
 *     "validateAmount",            // step name
 *     IllegalArgumentException.class, // exception type
 *     new IllegalArgumentException("Amount must be positive"),
 *     400,                         // error code
 *     true,                        // has aborted
 *     "OrderValidator.validate()"  // executable reference
 * );
 *
 * // Pattern matching - find any IllegalArgumentException
 * RuntimeExceptionRecord pattern = new RuntimeExceptionRecord(
 *     null, null, null,
 *     IllegalArgumentException.class,
 *     null, null, null, null
 * );
 * boolean matches = record.matches(pattern); // true
 *
 * // Find exception from specific step
 * RuntimeExceptionRecord stepPattern = new RuntimeExceptionRecord(
 *     null, "validation", "validateAmount",
 *     null, null, null, null, null
 * );
 * matches = record.matches(stepPattern); // true
 *
 * // Access exception details
 * String message = record.exceptionMessage();
 * Class<? extends Throwable> type = record.exceptionType();
 * boolean aborted = record.hasAborted();
 * }</pre>
 *
 * @param runtimeName the name of the runtime where the exception occurred
 * @param stageName the name of the stage where the exception occurred
 * @param stepName the name of the step where the exception occurred
 * @param exceptionType the class of the exception
 * @param exception the actual exception instance
 * @param code the error code associated with this exception
 * @param hasAborted whether this exception caused execution to abort
 * @param executableReference reference to the method or executable that threw the exception
 * @since 2.0.0-ALPHA01
 * @see IRuntimeContext#recordException(RuntimeExceptionRecord)
 * @see IRuntimeContext#findException(RuntimeExceptionRecord)
 * @see IRuntimeResult#getExceptions()
 */
public record RuntimeExceptionRecord(String runtimeName, String stageName, String stepName, Class<? extends Throwable> exceptionType, Throwable exception, Integer code, Boolean hasAborted, String executableReference) {

    /**
     * Checks if this exception record matches a given pattern.
     *
     * <p>
     * Pattern matching uses null fields as wildcards. A record matches the pattern if
     * all non-null fields in the pattern match the corresponding fields in this record.
     * For exception types, polymorphic matching is supported: the pattern matches if
     * the pattern's exception type is assignable from this record's exception type.
     * </p>
     *
     * <h2>Matching Rules</h2>
     * <ul>
     *   <li><b>null pattern</b> - Returns false</li>
     *   <li><b>null field in pattern</b> - Treated as wildcard, always matches</li>
     *   <li><b>runtimeName</b> - Must equal exactly</li>
     *   <li><b>stageName</b> - Must equal exactly</li>
     *   <li><b>stepName</b> - Must equal exactly</li>
     *   <li><b>exceptionType</b> - Must be assignable (supports polymorphism)</li>
     *   <li><b>code</b> - Must equal exactly</li>
     *   <li><b>hasAborted</b> - Must equal exactly</li>
     * </ul>
     *
     * @param pattern the pattern to match against, with null fields acting as wildcards
     * @return true if this record matches the pattern, false otherwise
     */
    public boolean matches(RuntimeExceptionRecord pattern) {
        if (pattern == null) return false;

        if (pattern.runtimeName != null && !pattern.runtimeName.equals(this.runtimeName))
            return false;

        if (pattern.stageName != null && !pattern.stageName.equals(this.stageName))
            return false;

        if (pattern.stepName != null && !pattern.stepName.equals(this.stepName))
            return false;

        if (pattern.exceptionType != null) {
            if (this.exceptionType == null) return false;

            Class<?> expected = pattern.exceptionType;
            Class<?> actual = this.exceptionType;

            if (!expected.isAssignableFrom(actual))
                return false;
        }

        if (pattern.code != null && !pattern.code.equals(this.code))
            return false;

        if (pattern.hasAborted != null && !pattern.hasAborted.equals(this.hasAborted))
            return false;

        return true;
    }

    /**
     * Checks if this exception record matches an OnException handler configuration.
     *
     * <p>
     * This method creates a pattern from the OnException configuration and checks if
     * this record matches it. The OnException configuration specifies criteria for
     * exception handlers to trigger.
     * </p>
     *
     * @param onException the OnException handler configuration to match against
     * @return true if this record matches the OnException criteria, false otherwise
     * @see IRuntimeStepOnException
     * @see com.garganttua.core.runtime.annotations.OnException
     */
    public boolean matches(IRuntimeStepOnException onException) {
        return this.matches(new RuntimeExceptionRecord(onException.runtimeName(), onException.fromStage(), onException.fromStep(), onException.exception(), null, null, true, null));
    }

    /**
     * Returns the message of the exception.
     *
     * <p>
     * This is a convenience method equivalent to calling {@code exception().getMessage()}.
     * </p>
     *
     * @return the exception message, or null if the exception is null or has no message
     */
    public String exceptionMessage() {
        return this.exception.getMessage();
    }

}
