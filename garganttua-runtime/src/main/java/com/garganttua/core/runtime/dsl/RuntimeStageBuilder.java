package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.injection.context.beans.Beans.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Named;

import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeStage;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.utils.OrderedMapPosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStageBuilder<InputType, OutputType>
        extends
        AbstractAutomaticLinkedDependentBuilder<IRuntimeStageBuilder<InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStage<InputType, OutputType>>
        implements IRuntimeStageBuilder<InputType, OutputType> {

    private final String stageName;
    private final OrderedMapBuilder<String, IRuntimeStepBuilder<?, ?, InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>> steps = new OrderedMapBuilder<>();
    private IInjectionContext context;
    private IInjectionContextBuilder injectionContextBuilder;
    private List<Class<Object>> stepsForAutoDetection;
    private String runtimeName;

    protected RuntimeStageBuilder(IRuntimeBuilder<InputType, OutputType> up, String runtimeName, String stageName) {
        super(Objects.requireNonNull(up, "Parent RuntimeBuilder cannot be null"),
                Set.of(
                        // InjectionContext needed for BUILD phase (used in doAutoDetection)
                        DependencySpec.use(IInjectionContextBuilder.class, DependencyPhase.BUILD)
                ));
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        this.runtimeName = Objects.requireNonNull(runtimeName, "Runtime name cannot be null");

        log.atInfo().log(logLineHeader() + "RuntimeStageBuilder initialized with phase-aware dependencies");
    }

    protected RuntimeStageBuilder(IRuntimeBuilder<InputType, OutputType> up, String runtimeName, String stageName,
            List<Class<Object>> stepsForAutoDetection) {
        this(up, runtimeName, stageName);
        this.stepsForAutoDetection = Objects.requireNonNull(stepsForAutoDetection,
                "stepsForAutoDetection cannot be null");
        log.atInfo().log(logLineHeader() + "RuntimeStageBuilder initialized for auto-detection");
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
            String stepName,
            ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {

        log.atTrace()
                .log(logLineHeader() + "Entering step() method");

        Objects.requireNonNull(stepName, "Step name cannot be null");
        Objects.requireNonNull(returnType, "Return type cannot be null");
        Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");

        IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                this, runtimeName, stageName, stepName, returnType, objectSupplier);

        this.steps.put(stepName, stepBuilder);
        log.atInfo().log(logLineHeader() + "Added step");

        log.atTrace().log(logLineHeader() + "Exiting step() method");
        return stepBuilder;
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(
            String stepName,
            OrderedMapPosition<String> position,
            ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {

        log.atTrace()
                .log(logLineHeader() + "Entering step() with position method");

        Objects.requireNonNull(stepName, "Step name cannot be null");
        Objects.requireNonNull(returnType, "Return type cannot be null");
        Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
        Objects.requireNonNull(position, "RuntimeStepPosition cannot be null");

        IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(
                this, runtimeName, stageName, stepName, returnType, objectSupplier);

        this.steps.putAt(stepName, stepBuilder, position);
        log.atInfo()
                .log(logLineHeader() + "Added step at specified position");

        log.atTrace().log(logLineHeader() + "Exiting step() with position method");
        return stepBuilder;
    }

    @Override
    protected IRuntimeStage<InputType, OutputType> doBuild() throws DslException {
        log.atTrace().log(logLineHeader() + "Entering doBuild() method");
        log.atInfo().log(logLineHeader() + "Building stage");

        OrderedMap<String, IRuntimeStep<?, InputType, OutputType>> builtSteps = this.steps.build();
        IRuntimeStage<InputType, OutputType> stage = new RuntimeStage<>(this.stageName, builtSteps);

        log.atTrace()
                .log(logLineHeader() + "Exiting doBuild() method");
        return stage;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log(logLineHeader() + "Entering doAutoDetection() method");

        Objects.requireNonNull(this.stepsForAutoDetection, "stepsForAutoDetection cannot be null");

        log.atInfo()
                .log(logLineHeader() + "Performing auto-detection of steps");

        this.stepsForAutoDetection.forEach(c -> {
            String stepName = UUID.randomUUID().toString();
            Named stepNamedAnnotation = c.getAnnotation(Named.class);
            if (stepNamedAnnotation != null) {
                stepName = stepNamedAnnotation.value();
            }

            log.atDebug()
                    .log(logLineHeader() + "Creating auto-detected step");

            ISupplierBuilder<Object, IBeanSupplier<Object>> supplierBuilder = bean(c);
            IRuntimeStepBuilder<?, ?, InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(this, runtimeName,
                    stageName, stepName, Void.class, supplierBuilder)
                    .autoDetect(true);

            // Provide dependency to sub-builders created during auto-detection
            if (this.injectionContextBuilder != null) {
                stepBuilder.provide(this.injectionContextBuilder);
            }
            this.steps.put(stepName, stepBuilder);

            log.atInfo().log(logLineHeader() + "Auto-detected step registered");
        });

        log.atTrace().log(logLineHeader() + "Exiting doAutoDetection() method");
    }

    @Override
    public IRuntimeStageBuilder<InputType, OutputType> provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder injCtxBuilder) {
            this.injectionContextBuilder = injCtxBuilder;
        }
        return super.provide(dependency);
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "] ";
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