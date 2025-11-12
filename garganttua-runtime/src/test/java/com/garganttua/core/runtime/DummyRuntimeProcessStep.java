package com.garganttua.core.runtime;

import javax.inject.Named;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.Exception;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.FailBack;
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
    @Catch(exception = DiException.class, code = 401, failback = true, abort = true)
    @Variable(name = "method-returned")
    String method(@Input String input, @Fixed(valueString = "input-parameter") String fixedValue,
            @Variable(name = "variable") String variable, @Context IRuntimeContext<?, ?> context) {
        return input + "-processed";
    }

    @FailBack
    @Output
    @Variable(name = "failback-returned")
    String failBackMethod(@Fixed(valueString = "input-parameter") String input, @Exception DiException exception,
            @Code int code, @ExceptionMessage String exceptionMessage, @Context IRuntimeContext<?, ?> context) {
        return input + "-failback";
    }
}
