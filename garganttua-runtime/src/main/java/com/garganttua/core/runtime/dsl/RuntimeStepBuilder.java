package com.garganttua.core.runtime.dsl;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.annotations.FallBack;
import com.garganttua.core.runtime.annotations.Operation;
import com.garganttua.core.runtime.annotations.Output;
import com.garganttua.core.runtime.annotations.Variable;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends
        AbstractAutomaticLinkedDependentBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>
        implements IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> {

    private String stepName;
    private String runtimeName;
    private ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier;
    private RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> methodBuilder;
    private Class<ExecutionReturn> executionReturn;
    private RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallbackBuilder;
    private IInjectableElementResolverBuilder resolverBuilder;
    private IInjectionContextBuilder injectionContextBuilder;

    public RuntimeStepBuilder(RuntimeBuilder<InputType, OutputType> runtimeBuilder, String runtimeName,
            String stepName,
            Class<ExecutionReturn> executionReturn,
            ISupplierBuilder<StepObjectType, ? extends ISupplier<StepObjectType>> supplier) {
        super(runtimeBuilder, Set.of(
                new DependencySpecBuilder(IInjectionContextBuilder.class).useForAutoDetect().build()));
        this.stepName = Objects.requireNonNull(stepName, "Step name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");
        this.executionReturn = Objects.requireNonNull(executionReturn, "Execution return type cannot be null");
        this.supplier = Objects.requireNonNull(supplier, "Supplier builder cannot be null");

        log.atTrace().log("{} Initialized RuntimeStepBuilder", logLineHeader());
        log.atDebug().log("{} Supplier type: {}", logLineHeader(), supplier.getSuppliedClass());
    }

    @Override
    public IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method()
            throws DslException {
        log.atTrace().log("{} Entering method() method", logLineHeader());
        if (this.methodBuilder == null) {
            this.methodBuilder = new RuntimeStepMethodBuilder<>(runtimeName, stepName, this, supplier);
            log.atDebug().log("{} Method builder created", logLineHeader());
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
            this.fallbackBuilder = new RuntimeStepFallbackBuilder<>(runtimeName, stepName, this, supplier);
            log.atDebug().log("{} Fallback builder created", logLineHeader());
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
        Method fallbackMethod = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedClass(),
                FallBack.class);
        if (fallbackMethod != null) {
            try {
                fallBack().provide(this.resolverBuilder).autoDetect(true).method(fallbackMethod);

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
        Method method = ObjectReflectionHelper.getMethodAnnotatedWith(supplier.getSuppliedClass(), Operation.class);
        if (method == null) {
            log.atError().log("{} No @Operation method found in class {}", logLineHeader(),
                    supplier.getSuppliedClass().getSimpleName());
            throw new DslException("Class " + supplier.getSuppliedClass().getSimpleName() +
                    " does not declare any @Operation method");
        }
        this.executionReturn = (Class<ExecutionReturn>) method.getReturnType();
        this.method().provide(this.resolverBuilder).autoDetect(true).method(method);

        log.atDebug().log("{} Detected operation method [{}] returning [{}]", logLineHeader(), method.getName(),
                executionReturn.getSimpleName());
        return method;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected IRuntimeStep<ExecutionReturn, InputType, OutputType> doBuild() throws DslException {
        log.atTrace().log("{} Entering doBuild() method", logLineHeader());

        IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> fallback = null;
        if (this.fallbackBuilder != null) {
            fallback = this.fallbackBuilder.build();
            log.atDebug().log("{} Built fallback method", logLineHeader());
        } else {
            log.atDebug().log("{} No fallback to build", logLineHeader());
        }

        log.atDebug().log("{} Building RuntimeStep", logLineHeader());
        IRuntimeStep<ExecutionReturn, InputType, OutputType> step = new RuntimeStep(runtimeName, stepName,
                executionReturn, this.methodBuilder.build(),
                Optional.ofNullable(fallback));

        log.atTrace().log("{} Exiting doBuild() method", logLineHeader());
        return step;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "] ";
    }

    @Override
    public IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> provide(
            IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder icb) {
            this.injectionContextBuilder = icb;
            this.resolverBuilder = icb.resolvers();
        }
        return super.provide(dependency);
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
    }
}
