package com.garganttua.core.runtime.dsl;

import static com.garganttua.core.injection.context.beans.Beans.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Named;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeStage;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.utils.OrderedMapPosition;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder d’un stage dans un runtime.
 * Contient une séquence ordonnée de steps.
 */
@Slf4j
public class RuntimeStageBuilder<InputType, OutputType>
        extends
        AbstractAutomaticLinkedBuilder<IRuntimeStageBuilder<InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStage>
        implements IRuntimeStageBuilder<InputType, OutputType> {

    private final String stageName;
    private final OrderedMapBuilder<String, IRuntimeStepBuilder<?, ?>, IRuntimeStep> steps = new OrderedMapBuilder<>();
    private IDiContext context;
    private List<Class<Object>> stepsForAutoDetection;

    protected RuntimeStageBuilder(IRuntimeBuilder<InputType, OutputType> up, String stageName) {
        super(Objects.requireNonNull(up, "Parent RuntimeBuilder cannot be null"));
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
    }

    /**
     * Secondary ctor used only for auto detection
     * @param up
     * @param stageName
     * @param stepsForAutoDetection
     */
    protected RuntimeStageBuilder(IRuntimeBuilder<InputType, OutputType> up, String stageName, List<Class<Object>> stepsForAutoDetection) {
        this(up, stageName);
        this.stepsForAutoDetection = Objects.requireNonNull(stepsForAutoDetection, "stepsForAutoDetection name cannot be null");
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String stepName,
            IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {
        Objects.requireNonNull(stepName, "Step name cannot be null");
        Objects.requireNonNull(returnType, "Return type cannot be null");
        Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
        IRuntimeStepBuilder<ExecutionReturn, StepObjectType> stepBuilder = new RuntimeStepBuilder<>(this, stepName,
                returnType, objectSupplier);
        this.steps.put(stepName, stepBuilder);
        return stepBuilder;
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String stepName,
            OrderedMapPosition<String> position,
            IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {
        Objects.requireNonNull(stepName, "Step name cannot be null");
        Objects.requireNonNull(returnType, "Return type cannot be null");
        Objects.requireNonNull(objectSupplier, "Object supplier builder cannot be null");
        Objects.requireNonNull(position, "RuntimeStepPosition cannot be null");
        IRuntimeStepBuilder<ExecutionReturn, StepObjectType> stepBuilder = new RuntimeStepBuilder<>(this, stepName,
                returnType, objectSupplier);
        this.steps.putAt(stepName, stepBuilder, position);
        return stepBuilder;
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.steps.values().forEach(s -> s.handle(context));
    }

    @Override
    protected IRuntimeStage doBuild() throws DslException {
        log.info("Building stage [{}] with {} step(s)", stageName, steps.size());
        OrderedMap<String, IRuntimeStep> builtSteps = this.steps.build();
        return new RuntimeStage(this.stageName, builtSteps);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        Objects.requireNonNull(this.context, "Context cannot be null");
        Objects.requireNonNull(this.stepsForAutoDetection, "stepsForAutoDetection cannot be null");

        this.stepsForAutoDetection.stream().forEach(c -> {
            String stepName = UUID.randomUUID().toString();
            
            Named stepNamedAnnotation = c.getAnnotation(Named.class);
            if( stepNamedAnnotation != null ){
                stepName = stepNamedAnnotation.value();
            }
            IObjectSupplierBuilder<Object, IBeanSupplier<Object>> supplierBuilder = bean(c);

            IRuntimeStepBuilder<?,?> stepBuilder = new RuntimeStepBuilder<>(this, stepName, Void.class, supplierBuilder).autoDetect(true);
            stepBuilder.handle(context);
            this.steps.put(stepName, stepBuilder);
        });
    }

}
