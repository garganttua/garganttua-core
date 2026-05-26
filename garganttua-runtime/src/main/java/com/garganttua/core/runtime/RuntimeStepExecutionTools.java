package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.CoreException;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.reflection.IClass;

public class RuntimeStepExecutionTools {
    private static final IDiagnostic log = Diagnostics.of(RuntimeStepExecutionTools.class);

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
