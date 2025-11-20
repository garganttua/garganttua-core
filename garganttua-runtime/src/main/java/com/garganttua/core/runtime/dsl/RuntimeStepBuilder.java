package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        AbstractAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String stepName;
    private String stageName;
    private String runtimeName;
    private IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier;
    private IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> methodBuilder;
    private Class<ExecutionReturn> executionReturn;
    private IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallbackBuilder;
    private IDiContext context;

    public RuntimeStepBuilder(RuntimeStageBuilder<InputType, OutputType> runtimeStageBuilder, String runtimeName,
            String stageName, String stepName,
            Class<ExecutionReturn> executionReturn,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier) {
        super(runtimeStageBuilder);
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.executionReturn = Objects.requireNonNull(executionReturn, "Execution return type cannot be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier builder cannot be null");
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method()
            throws DslException {
        if (this.methodBuilder == null) {
            this.methodBuilder = new RuntimeStepMethodBuilder<>(this.runtimeName, this.stageName, this.stepName, this, this.supplier, this.context);
            this.methodBuilder.withReturn(this.executionReturn);
        }
        return this.methodBuilder;

    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack()
            throws DslException {
        if (this.fallbackBuilder == null) {
            this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(this.runtimeName, this.stageName, this.stepName, this, this.supplier, this.context);
            this.fallbackBuilder.withReturn(this.executionReturn);
        }
        return this.fallbackBuilder;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        detectOperationMethod();
        detectFallback();
    }

    private void detectFallback() {
        Method fallbackMethod = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedType(), FallBack.class);

        if (fallbackMethod != null) {
            fallBack().autoDetect(true).method(fallbackMethod).withReturn(executionReturn).handle(context);

            if (fallbackMethod.getAnnotation(Output.class) != null) {
                fallBack().output(true);
            }

            Variable variable = fallbackMethod.getAnnotation(Variable.class);

            if (variable != null) {
                fallBack().variable(variable.name());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Method detectOperationMethod() throws DslException {
        Method method = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedType(), Operation.class);
        if (method == null) {
            throw new DslException("Class " + supplier.getSuppliedType().getSimpleName() +
                    " does not declare any @Operation method");
        }
        this.executionReturn = (Class<ExecutionReturn>) method.getReturnType();

        method().autoDetect(true).method(method).withReturn(executionReturn).handle(context);
        return method;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected IRuntimeStep<ExecutionReturn, InputType, OutputType> doBuild() throws DslException {
        IMethodBinder<ExecutionReturn> fallback = null;

        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
        }

        return new RuntimeStep(runtimeName, stageName, stepName, this.executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback));
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        if (methodBuilder != null) {
            methodBuilder.handle(context);
        }
        if (fallbackBuilder != null) {
            fallbackBuilder.handle(context);
        }
    }
}
