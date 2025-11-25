package com.garganttua.core.runtime.runtimes.twosteps;

import javax.inject.Named;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.runtime.runtimes.onestep.CustomException;

import jakarta.annotation.Nullable;

@Step
@Named("step-one")
public class StepOne {

    @Operation(abortOnUncatchedException = false)
    @Catch(exception = DiException.class, code = 401)
    @Variable(name = "step-one-returned")
    @Nullable
    String method(
            @Input String input,
            @Variable(name = "step-one-variable") String variable)
            throws DiException, CustomException {

        if (variable.equals("di-exception")) {
            throw new DiException(input + "-step-one-processed-" + variable);
        }

        if (variable.equals("custom-exception")) {
            throw new CustomException(input + "-step-one-processed-" + variable);
        }

        return input + "-step-one-processed-" + variable;
    }

    @FallBack
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "step-one-fallback-returned")
    String fallbackMethod(
            @Input String input) {
        return input + "-step-one-fallback";
    }

}
