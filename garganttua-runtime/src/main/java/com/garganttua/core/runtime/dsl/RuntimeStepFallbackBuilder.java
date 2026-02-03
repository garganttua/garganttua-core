package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.AbstractMethodArgInjectBinderBuilder;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;
import com.garganttua.core.runtime.MethodBinderExpression;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.annotations.OnException;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractMethodArgInjectBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>
        implements IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String storeReturnInVariable = null;
    private Boolean output = false;
    private List<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>> onExceptions = new ArrayList<>();
    private String stepName;
    private String runtimeName;
    private Boolean nullable = false;

    protected RuntimeStepFallbackBuilder(String runtimeName,
            String stepName,
            IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> up,
            ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier)
            throws DslException {
        super(up, supplier);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        log.atTrace().log("{} Initialized RuntimeStepFallbackBuilder", logLineHeader());
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
            String variableName) {
        this.storeReturnInVariable = Objects.requireNonNull(variableName, "Variable name cannot be null");
        log.atDebug().log("{} Variable set for fallback", logLineHeader());
        return this;
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output) {
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        log.atDebug().log("{} Output flag set for fallback", logLineHeader());
        return this;
    }

    @Override
    public IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> build()
            throws DslException {
        log.atTrace().log("{} Entering build() method", logLineHeader());
        IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>> binder = (IContextualMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>) super.build();
        MethodBinderExpression<ExecutionReturn, IRuntimeContext<InputType, OutputType>> expression = new MethodBinderExpression<>(binder);
        RuntimeStepFallbackBinder<ExecutionReturn, InputType, OutputType> fallbackBinder = new RuntimeStepFallbackBinder<ExecutionReturn, InputType, OutputType>(
                this.runtimeName,
                this.stepName, expression, Optional.ofNullable(this.storeReturnInVariable), this.output,
                this.onExceptions.stream().map(b -> b.build()).collect(Collectors.toList()), this.nullable,
                binder.getExecutableReference());
        log.atDebug()
                .log("{} RuntimeStepFallbackBinder built successfully", logLineHeader());
        log.atTrace().log("{} Exiting build() method", logLineHeader());
        return fallbackBinder;
    }

    @Override
    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
            Class<? extends Throwable> exception) throws DslException {
        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException = new RuntimeStepOnExceptionBuilder<>(
                this, this.runtimeName, Objects.requireNonNull(exception, "Exception cannot be null"));
        this.onExceptions.add(onException);
        log.atDebug()
                .log("{} Added onException handler", logLineHeader());
        return onException;
    }

    public IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
            Class<? extends Throwable> exception, OnException oneException) throws DslException {
        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException = new RuntimeStepOnExceptionBuilder<>(
                this, this.runtimeName, Objects.requireNonNull(exception, "Exception cannot be null"),
                Objects.requireNonNull(oneException, "On exception annotation cannot be null"));
        this.onExceptions.add(onException);
        log.atDebug()
                .log("{} Added onException handler with annotation", logLineHeader());
        return onException;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("{} Starting auto-detection for fallback builder", logLineHeader());
        super.doAutoDetection();

        Method method = this.method();
        detectOutput(method);
        detectVariable(method);
        detectOnExceptions(method);
        detectNullable(method);
        log.atTrace().log("{} Finished auto-detection for fallback builder", logLineHeader());
    }

    private void detectNullable(Method method) {
        Nullable nullable = method.getAnnotation(Nullable.class);
        if (nullable != null) {
            this.nullable = true;
            log.atDebug().log("{} Nullable detected for fallback", logLineHeader());
        }
    }

    private void detectOnExceptions(Method method) {
        OnException[] onExceptionAnnotations = method.getAnnotationsByType(OnException.class);
        for (OnException onExceptionAnnotation : onExceptionAnnotations) {
            onException(onExceptionAnnotation.exception(), onExceptionAnnotation).autoDetect(true);
            log.atDebug()
                    .log("{} Auto-detected onException", logLineHeader());
        }
    }

    private void detectVariable(Method operationMethod) {
        Variable variable = operationMethod.getAnnotation(Variable.class);
        if (variable != null) {
            this.variable(variable.name());
            log.atDebug()
                    .log("{} Auto-detected variable for fallback", logLineHeader());
        }
    }

    private void detectOutput(Method operationMethod) {
        if (operationMethod.getAnnotation(Output.class) != null) {
            this.output(true);
            log.atDebug().log("{} Auto-detected output for fallback", logLineHeader());
        }
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
            boolean nullable) {
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        log.atDebug().log("{} Nullable flag set manually", logLineHeader());
        return this;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "][Fallback] ";
    }
}