package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Declares that a fallback handles a specific exception type originating from a
 * given step of a given runtime.
 *
 * @param exception   the exception type to handle
 * @param runtimeName the runtime the exception must originate from
 * @param fromStep    the step the exception must originate from
 */
public record RuntimeStepOnException(IClass<? extends Throwable> exception, String runtimeName, String fromStep) implements IRuntimeStepOnException {
    private static final Logger log = Logger.getLogger(RuntimeStepOnException.class);

}
