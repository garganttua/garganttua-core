package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.reflection.IClass;

/**
 * Static helpers shared by step binders: validating and storing a step's returned
 * value (as a variable or as the runtime output) and recording exceptions on the
 * context with the appropriate abort/code semantics.
 */
public class RuntimeStepExecutionTools {
    private static final Logger log = Logger.getLogger(RuntimeStepExecutionTools.class);

    /**
     * Validates a returned value against nullability and, if valid and non-null,
     * stores it in the named context variable. A {@code null} non-nullable value
     * triggers an aborting exception.
     *
     * @param runtimeName         the owning runtime name
     * @param stepName            the step name
     * @param variableName        the variable to store the value in
     * @param returned            the value returned by the step (may be {@code null})
     * @param context             the runtime context to mutate
     * @param nullable            whether a {@code null} return is permitted
     * @param logLineHeader       prefix used in log lines
     * @param executableReference reference to the executable, for diagnostics
     * @throws ExecutorException if a non-nullable variable received a {@code null} value
     */
    static public void validateAndStoreReturnedValueInVariable(String runtimeName, String stepName,
            String variableName,
            Object returned,
            IRuntimeContext<?, ?> context, boolean nullable, String logLineHeader, String executableReference)
            throws ExecutorException {

        log.trace("{}Validating returned value for variable '{}', nullable={}", logLineHeader, variableName,
                nullable);

        if (returned == null && !nullable) {
            log.warn("{}Returned value is null but variable '{}' is not nullable", logLineHeader, variableName);
            handleException(
                    runtimeName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + " is defined to store return in variable "
                                    + variableName
                                    + " but did not return any value and is not nullable"),
                    true, executableReference, null, logLineHeader);
            return;
        }

        if (returned != null) {
            log.debug("{}Storing returned value '{}' in variable '{}'", logLineHeader, returned, variableName);
            context.setVariable(variableName, returned);
        }
    }

    /**
     * Records an exception on the context and, when matched by a catch clause or
     * forced, marks the execution as aborted and sets the corresponding exit code.
     *
     * @param runtimeName         the owning runtime name
     * @param stepName            the step name
     * @param context             the runtime context to record on
     * @param exception           the exception to handle
     * @param forceAbort          whether to abort even without a matching catch clause
     * @param executableReference reference to the executable, for diagnostics
     * @param matchedCatch        the matching catch clause, or {@code null} if none
     * @param logLineHeader       prefix used in log lines
     * @throws ExecutorException when the exception aborts the step (matched or forced)
     */
    @SuppressWarnings("unchecked")
    static public void handleException(String runtimeName, String stepName,
            IRuntimeContext<?, ?> context,
            Throwable exception,
            boolean forceAbort, String executableReference, IRuntimeStepCatch matchedCatch, String logLineHeader)
            throws ExecutorException {

        Throwable reportException = findExceptionForReport(exception, matchedCatch);
        int reportCode = IRuntime.GENERIC_RUNTIME_ERROR_CODE;
        boolean aborted = forceAbort;

        log.warn("{}Handling exception: {} (forceAbort={})", logLineHeader, exception.getMessage(), forceAbort);

        try {

            if (matchedCatch != null) {
                reportCode = matchedCatch.code();
                aborted = true;
                log.debug("{}Matched catch found, setting report code={} and aborting", logLineHeader,
                        reportCode);
                throw new ExecutorException(logLineHeader + " Error during step execution", exception);
            }

            if (forceAbort) {
                aborted = true;
                log.error("{}Force aborting due to exception", logLineHeader, exception);
                throw new ExecutorException(logLineHeader + " Error during step execution", exception);
            }
        } finally {
            log.debug("{}Recording exception in context, aborted={}", logLineHeader, aborted);
            context.recordException(new RuntimeExceptionRecord(
                    runtimeName,
                    stepName,
                    (IClass<? extends Throwable>) IClass.getClass(reportException.getClass()),
                    reportException,
                    reportCode,
                    aborted, executableReference));
            if (aborted) {
                context.setCode(reportCode);
            }
        }
    }

    /**
     * Selects the most relevant throwable to report: the first occurrence matching
     * the catch clause's type if present, otherwise the exception's cause (or the
     * exception itself when it has no cause).
     *
     * @param exception    the raised exception
     * @param matchedCatch the matching catch clause, or {@code null}
     * @return the throwable to record
     */
    static public Throwable findExceptionForReport(Throwable exception, IRuntimeStepCatch matchedCatch) {
        Throwable reportException;
        Optional<? extends Throwable> found = Optional.empty();
        if (matchedCatch != null)
            found = CoreException
                    .findFirstInException(exception, matchedCatch.exception());

        if (found.isPresent()) {
            reportException = found.get();
            log.trace("[RuntimeStepExecutionTools.findExceptionForReport] Found exception for report: {}",
                    reportException);
        } else {
            reportException = exception.getCause() == null ? exception : exception.getCause();
            log.trace("[RuntimeStepExecutionTools.findExceptionForReport] Using exception cause for report: {}",
                    reportException);
        }
        return reportException;
    }

    /**
     * Validates a returned value against the runtime's output contract (nullability
     * and type compatibility) and, if valid and non-null, sets it as the output.
     * A {@code null} non-nullable value or an incompatible type triggers an aborting
     * exception.
     *
     * @param runtimeName         the owning runtime name
     * @param stepName            the step name
     * @param returned            the value returned by the step (may be {@code null})
     * @param context             the runtime context to mutate
     * @param nullable            whether a {@code null} return is permitted
     * @param logLineHeader       prefix used in log lines
     * @param executableReference reference to the executable, for diagnostics
     * @throws ExecutorException if the value violates the output nullability or type contract
     */
    @SuppressWarnings("unchecked")
    static public <InputType, OutputType, ExecutionReturned> void validateReturnedForOutput(String runtimeName,
            String stepName,
            ExecutionReturned returned,
            IRuntimeContext<InputType, OutputType> context, boolean nullable, String logLineHeader,
            String executableReference)
            throws ExecutorException {

        log.trace("{}Validating returned value for output, nullable={}", logLineHeader, nullable);

        if (returned == null && !nullable) {
            log.warn("{}Returned value is null but output is not nullable", logLineHeader);
            handleException(
                    runtimeName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + " is defined to be output but did not return any value and is not nullable"),
                    true, executableReference, null, logLineHeader);
            return;
        }

        if (returned != null && !context.isOfOutputType(IClass.getClass(returned.getClass()))) {
            log.warn("{}Returned value type '{}' is not compatible with output type '{}'", logLineHeader,
                    returned.getClass().getSimpleName(), context.getOutputType().getSimpleName());
            handleException(
                    runtimeName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + " is defined to be output, but returned type "
                                    + returned.getClass().getSimpleName()
                                    + " is not output type "
                                    + context.getOutputType().getSimpleName()),
                    true, executableReference, null, logLineHeader);
        }

        if (returned != null) {
            log.debug("{}Setting returned value '{}' as output", logLineHeader, returned);
            context.setOutput((OutputType) returned);
        }
    }

}
