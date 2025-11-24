package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.CoreException;
import com.garganttua.core.execution.ExecutorException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepExecutionTools {

    static public void validateAndStoreReturnedValueInVariable(String runtimeName, String stageName, String stepName,
            String variableName,
            Object returned,
            IRuntimeContext<?, ?> context, boolean nullable, String logLineHeader, String executableReference)
            throws ExecutorException {

        log.atTrace().log("{}Validating returned value for variable '{}', nullable={}", logLineHeader, variableName,
                nullable);

        if (returned == null && !nullable) {
            log.atWarn().log("{}Returned value is null but variable '{}' is not nullable", logLineHeader, variableName);
            handleException(
                    runtimeName,
                    stageName,
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
            log.atDebug().log("{}Storing returned value '{}' in variable '{}'", logLineHeader, returned, variableName);
            context.setVariable(variableName, returned);
        }
    }

    static public void handleException(String runtimeName, String stageName, String stepName,
            IRuntimeContext<?, ?> context,
            Throwable exception,
            boolean forceAbort, String executableReference, IRuntimeStepCatch matchedCatch, String logLineHeader)
            throws ExecutorException {

        Throwable reportException = findExceptionForReport(exception, matchedCatch);
        int reportCode = IRuntime.GENERIC_RUNTIME_ERROR_CODE;
        boolean aborted = forceAbort;

        log.atWarn().log("{}Handling exception: {} (forceAbort={})", logLineHeader, exception.getMessage(), forceAbort);

        try {

            if (matchedCatch != null) {
                reportCode = matchedCatch.code();
                aborted = true;
                log.atInfo().log("{}Matched catch found, setting report code={} and aborting", logLineHeader,
                        reportCode);
                throw new ExecutorException(logLineHeader + " Error during step execution", exception);
            }

            if (forceAbort) {
                aborted = true;
                log.atError().log("{}Force aborting due to exception", logLineHeader, exception);
                throw new ExecutorException(logLineHeader + " Error during step execution", exception);
            }
        } finally {
            log.atDebug().log("{}Recording exception in context, aborted={}", logLineHeader, aborted);
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
            log.atTrace().log("[RuntimeStepExecutionTools.findExceptionForReport] Found exception for report: {}",
                    reportException);
        } else {
            reportException = exception.getCause() == null ? exception : exception.getCause();
            log.atTrace().log("[RuntimeStepExecutionTools.findExceptionForReport] Using exception cause for report: {}",
                    reportException);
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

        log.atTrace().log("{}Validating returned value for output, nullable={}", logLineHeader, nullable);

        if (returned == null && !nullable) {
            log.atWarn().log("{}Returned value is null but output is not nullable", logLineHeader);
            handleException(
                    runtimeName,
                    stageName,
                    stepName,
                    context,
                    new ExecutorException(
                            logLineHeader
                                    + " is defined to be output but did not return any value and is not nullable"),
                    true, executableReference, null, logLineHeader);
            return;
        }

        if (returned != null && !context.isOfOutputType(returned.getClass())) {
            log.atWarn().log("{}Returned value type '{}' is not compatible with output type '{}'", logLineHeader,
                    returned.getClass().getSimpleName(), context.getOutputType().getSimpleName());
            handleException(
                    runtimeName,
                    stageName,
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
            log.atDebug().log("{}Setting returned value '{}' as output", logLineHeader, returned);
            context.setOutput((OutputType) returned);
        }
    }

}
