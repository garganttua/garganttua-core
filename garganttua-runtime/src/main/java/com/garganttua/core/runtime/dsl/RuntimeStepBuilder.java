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
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends
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
        this.supplier = Objects.requireNonNull(supplier, "Supplier builder cannot be null");

        log.atTrace().log("{} Initialized RuntimeStepBuilder", logLineHeader());
        log.atDebug().log("{} Supplier type: {}", logLineHeader(), supplier.getSuppliedType());
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method()
            throws DslException {
        log.atTrace().log("{} Entering method() method", logLineHeader());
        if (this.methodBuilder == null) {
            this.methodBuilder = new RuntimeStepMethodBuilder<>(runtimeName, stageName, stepName, this, supplier,
                    context);
            this.methodBuilder.withReturn(executionReturn);
            log.atInfo().log("{} Method builder created", logLineHeader());
        } else {
            log.atDebug().log("{} Reusing existing method builder", logLineHeader());
        }
        log.atTrace().log("{} Exiting method() method", logLineHeader());
        return this.methodBuilder;
    }

    @Override
    public IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack()
            throws DslException {
        log.atTrace().log("{} Entering fallBack() method", logLineHeader());
        if (this.fallbackBuilder == null) {
            this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(runtimeName, stageName, stepName, this, supplier,
                    context);
            this.fallbackBuilder.withReturn(executionReturn);
            log.atInfo().log("{} Fallback builder created", logLineHeader());
        } else {
            log.atDebug().log("{} Reusing existing fallback builder", logLineHeader());
        }
        log.atTrace().log("{} Exiting fallBack() method", logLineHeader());
        return this.fallbackBuilder;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("{} Starting auto-detection", logLineHeader());
        detectOperationMethod();
        detectFallback();
        log.atTrace().log("{} Finished auto-detection", logLineHeader());
    }

    private void detectFallback() {
        log.atTrace().log("{} Detecting fallback method", logLineHeader());
        Method fallbackMethod = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedType(),
                FallBack.class);
        if (fallbackMethod != null) {
            try {
                fallBack().autoDetect(true).method(fallbackMethod).withReturn(executionReturn).handle(context);

                if (fallbackMethod.getAnnotation(Output.class) != null) {
                    fallBack().output(true);
                }

                Variable variable = fallbackMethod.getAnnotation(Variable.class);
                if (variable != null) {
                    fallBack().variable(variable.name());
                }

                log.atDebug().log("{} Detected fallback method [{}]", logLineHeader(), fallbackMethod.getName());
            } catch (DslException e) {
                log.atWarn().log("{} Exception while handling fallback method [{}]", logLineHeader(),
                        fallbackMethod.getName());
            }
        } else {
            log.atWarn().log("{} No fallback method detected", logLineHeader());
        }
    }

    @SuppressWarnings("unchecked")
    private Method detectOperationMethod() throws DslException {
        log.atTrace().log("{} Detecting operation method", logLineHeader());
        Method method = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedType(), Operation.class);
        if (method == null) {
            log.atError().log("{} No @Operation method found in class {}", logLineHeader(),
                    supplier.getSuppliedType().getSimpleName());
            throw new DslException("Class " + supplier.getSuppliedType().getSimpleName() +
                    " does not declare any @Operation method");
        }
        this.executionReturn = (Class<ExecutionReturn>) method.getReturnType();
        method().autoDetect(true).method(method).withReturn(executionReturn).handle(context);

        log.atInfo().log("{} Detected operation method [{}] returning [{}]", logLineHeader(), method.getName(),
                executionReturn.getSimpleName());
        return method;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected IRuntimeStep<ExecutionReturn, InputType, OutputType> doBuild() throws DslException {
        log.atTrace().log("{} Entering doBuild() method", logLineHeader());

        IMethodBinder<ExecutionReturn> fallback = null;
        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
            log.atDebug().log("{} Built fallback method", logLineHeader());
        } else {
            log.atDebug().log("{} No fallback to build", logLineHeader());
        }

        log.atInfo().log("{} Building RuntimeStep", logLineHeader());
        IRuntimeStep<ExecutionReturn, InputType, OutputType> step = new RuntimeStep(runtimeName, stageName, stepName,
                executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback));

        log.atTrace().log("{} Exiting doBuild() method", logLineHeader());
        return step;
    }

    @Override
    public void handle(IDiContext context) {
        log.atTrace().log("{} Entering handle() method", logLineHeader());

        this.context = Objects.requireNonNull(context, "Context cannot be null");
        if (methodBuilder != null) {
            methodBuilder.handle(context);
            log.atDebug().log("{} Handled method builder", logLineHeader());
        }
        if (fallbackBuilder != null) {
            fallbackBuilder.handle(context);
            log.atDebug().log("{} Handled fallback builder", logLineHeader());
        }

        log.atTrace().log("{} Context handled for step", logLineHeader());
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "] ";
    }
}