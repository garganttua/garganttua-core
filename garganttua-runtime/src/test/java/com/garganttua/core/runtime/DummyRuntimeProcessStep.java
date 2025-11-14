package com.garganttua.core.runtime;

import javax.inject.Named;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.Exception;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Variable;

@Step
@Named("step-1")
public class DummyRuntimeProcessStep {

    @Operation
    @Output
    @Catch(exception = DiException.class, code = 401, fallback = true, abort = true)
    @Variable(name = "method-returned")
    @Code(201)
    String method(@Input String input, @Fixed(valueString = "input-parameter") String fixedValue,
            @Variable(name = "variable") String variable, @Context IRuntimeContext<String, String> context) throws DiException {
        return input + "-processed";
    }

    @FallBack
    @Output
    @Variable(name = "fallback-returned")
    String fallbackMethod(@Fixed(valueString = "input-parameter") String input, @Exception DiException exception,
            @Code Integer code, @ExceptionMessage String exceptionMessage, @Context IRuntimeContext<String, String> context) {
        return input + "-failback";
    }
}
