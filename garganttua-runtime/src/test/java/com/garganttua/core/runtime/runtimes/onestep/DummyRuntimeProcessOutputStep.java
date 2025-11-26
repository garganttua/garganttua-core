package com.garganttua.core.runtime.runtimes.onestep;

import static com.garganttua.core.condition.Conditions.*;
import static com.garganttua.core.supply.dsl.FixedObjectSupplierBuilder.*;

import javax.inject.Named;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.annotations.Catch;
import com.garganttua.core.runtime.annotations.Code;
import com.garganttua.core.runtime.annotations.Condition;
import com.garganttua.core.runtime.annotations.Context;
import com.garganttua.core.runtime.annotations.Exception;
import com.garganttua.core.runtime.annotations.ExceptionMessage;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Input;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Step;
import com.garganttua.core.runtime.annotations.Variable;

import jakarta.annotation.Nullable;

@Step
@Named("output-step")
public class DummyRuntimeProcessOutputStep {

    @Condition
    IConditionBuilder condition = custom(of(10), i -> 1 > 0);

    @Operation(abortOnUncatchedException=true)
    @Output
    @Catch(exception = DiException.class, code = 401)
    @Variable(name = "method-returned")
    @Code(201)
    @Nullable
    String method(
            @Input String input,
            @Fixed(valueString = "fixed-value-in-method") String fixedValue,
            @Variable(name = "variable") String variable,
            @Context IRuntimeContext<String, String> context)
            throws DiException, CustomException {

        if (variable.equals("di-exception")) {
            throw new DiException(input + "-processed-" + fixedValue + "-" + variable);
        }

        if (variable.equals("custom-exception")) {
            throw new CustomException(input + "-processed-" + fixedValue + "-" + variable);
        }

        return input + "-processed-" + fixedValue + "-" + variable;
    }

    @FallBack
    @Output
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "fallback-returned")
    String fallbackMethod(
            @Input String input,
            @Fixed(valueString = "fixed-value-in-fallback") String fixedValue,
            @Exception DiException exception,
            @Code Integer code,
            @Nullable @ExceptionMessage String exceptionMessage,
            @Context IRuntimeContext<String, String> context) {
        return input + "-fallback-" + fixedValue + "-" + code + "-" + exceptionMessage;
    }
}
