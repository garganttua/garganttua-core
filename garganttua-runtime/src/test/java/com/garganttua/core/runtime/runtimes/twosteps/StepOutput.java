package com.garganttua.core.runtime.runtimes.twosteps;

import javax.inject.Named;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.runtime.runtimes.onestep.CustomException;

import jakarta.annotation.Nullable;

@Step
@Named("output-step")
public class StepOutput {

    @Output
    @Code(222)
    @Operation(abortOnUncatchedException = true)
    @Catch(exception = DiException.class, code = 444)
    @Variable(name = "output-step-returned")
    @Nullable
    String method(
            @Variable(name = "step-one-returned") String input,
            @Variable(name = "output-step-variable") String outputStepVariable)
            throws DiException, CustomException {

        if (outputStepVariable.equals("di-exception")) {
            throw new DiException(input + "-output-step-processed-" + outputStepVariable);
        }

        if (outputStepVariable.equals("custom-exception")) {
            throw new CustomException(input +"-output-step-processed-" + outputStepVariable);
        }

        return input + "-output-step-processed-" + outputStepVariable;
    }

    @FallBack
    @Output
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "output-step-fallback-returned")
    String fallbackMethod(
            @Variable(name = "step-one-returned") String input) {
        return input + "-output-step-fallback";
    }
}
