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

        context.setVariable(variableName, returned);
    }

    static public void handleException(String runtimeName, String stageName, String stepName,
            IRuntimeContext<?, ?> context,
            Throwable exception,
            boolean forceAbort, String executableReference, IRuntimeStepCatch matchedCatch, String logLineHeader)
            throws ExecutorException {

        Throwable reportException = exception;
        int reportCode = -1;
        boolean aborted = forceAbort;

        try {
            if (forceAbort) {
                reportCode = IRuntime.GENERIC_RUNTIME_ERROR_CODE;
                aborted = true;
                throw new ExecutorException(logLineHeader + "Error during step execution", exception);
            }

            if (matchedCatch != null) {
                reportException = findExceptionForReport(exception, matchedCatch);
                reportCode = matchedCatch.code();
                aborted = true;
                context.setCode(reportCode);

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
        Optional<? extends Throwable> found = CoreException
                .findFirstInException(exception, matchedCatch.exception());
        if (found.isPresent()) {
            reportException = found.get();
        } else {
            reportException = exception;
        }
        return reportException;
    }

    static public void validateReturnedForOutput(String runtimeName, String stageName, String stepName,
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
    }

}
