package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.CoreException;
import com.garganttua.core.execution.ExecutorException;

public class RuntimeStepExecutionTools {

    static public void validateAndStoreReturnedValueInVariable(String runtimeName, String stageName, String stepName,
            String variableName,
            Object returned,
            IRuntimeContext<?, ?> context, boolean nullable, String logLineHeader, String executableReference)
            throws ExecutorException {

        if (returned == null && !nullable) {
            handleException(
                    runtimeName,
                    stageName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + "is defined to store return in variable "
                                    + variableName
                                    + " but did not return any value and is not nullable"),
                    true, executableReference, null, logLineHeader);
            return;
        }

        if (returned != null)
            context.setVariable(variableName, returned);
    }

    static public void handleException(String runtimeName, String stageName, String stepName,
            IRuntimeContext<?, ?> context,
            Throwable exception,
            boolean forceAbort, String executableReference, IRuntimeStepCatch matchedCatch, String logLineHeader)
            throws ExecutorException {

        Throwable reportException = findExceptionForReport(exception, matchedCatch);
        int reportCode = IRuntime.GENERIC_RUNTIME_ERROR_CODE;
        boolean aborted = forceAbort;

        try {

            if (matchedCatch != null) {
                reportCode = matchedCatch.code();
                aborted = true;
                throw new ExecutorException(logLineHeader + "Error during step execution", exception);
            }

            if (forceAbort) {
                aborted = true;
                throw new ExecutorException(logLineHeader + "Error during step execution", exception);
            }
        } finally {
            context.recordException(new RuntimeExceptionRecord(
                    runtimeName,
                    stageName,
                    stepName,
                    reportException.getClass(),
                    reportException,
                    reportCode,
                    aborted, executableReference));
            if (aborted) {
                context.setCode(reportCode);
            }
        }
    }

    static public Throwable findExceptionForReport(Throwable exception, IRuntimeStepCatch matchedCatch) {
        Throwable reportException;
        Optional<? extends Throwable> found = Optional.empty();
        if (matchedCatch != null)
            found = CoreException
                    .findFirstInException(exception, matchedCatch.exception());

        if (found.isPresent()) {
            reportException = found.get();
        } else {
            reportException = exception.getCause() == null ? exception : exception.getCause();
        }
        return reportException;
    }

    @SuppressWarnings("unchecked")
    static public <InputType, OutputType, ExecutionReturned> void validateReturnedForOutput(String runtimeName,
            String stageName, String stepName,
            ExecutionReturned returned,
            IRuntimeContext<InputType, OutputType> context, boolean nullable, String logLineHeader,
            String executableReference)
            throws ExecutorException {

        if (returned == null && !nullable) {
            handleException(
                    runtimeName,
                    stageName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + "is defined to be output but did not return any value and is not nullable"),
                    true, executableReference, null, logLineHeader);
            return;
        }

        if (returned != null && !context.isOfOutputType(returned.getClass())) {
            handleException(
                    runtimeName,
                    stageName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + "is defined to be output, but returned type "
                                    + returned.getClass().getSimpleName()
                                    + " is not output type "
                                    + context.getOutputType().getSimpleName()),
                    true, executableReference, null, logLineHeader);
        }

        if (returned != null)
            context.setOutput((OutputType) returned);
    }

}
