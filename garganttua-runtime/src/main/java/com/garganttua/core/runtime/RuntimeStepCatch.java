package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * A catch clause for a runtime step: maps a caught exception type to the exit
 * code recorded when that exception aborts the step.
 *
 * @param exception the exception type this clause matches
 * @param code      the exit code to report when the clause matches
 */
public record RuntimeStepCatch(IClass<? extends Throwable> exception, Integer code) implements IRuntimeStepCatch {
    private static final Logger log = Logger.getLogger(RuntimeStepCatch.class);

}
