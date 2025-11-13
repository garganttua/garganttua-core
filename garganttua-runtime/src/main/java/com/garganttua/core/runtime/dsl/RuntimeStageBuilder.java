package com.garganttua.core.runtime.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeStage;
import com.garganttua.core.runtime.RuntimeStepPosition;
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
        extends AbstractLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimeStage>
        implements IRuntimeStageBuilder<InputType, OutputType> {

    private final String stageName;
    private final OrderedMap<String, IRuntimeStepBuilder<?,?>> steps = new OrderedMap<>();

    protected RuntimeStageBuilder(IRuntimeBuilder<InputType, OutputType> up, String stageName) {
        super(Objects.requireNonNull(up, "Parent RuntimeBuilder cannot be null"));
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
    }

    /* @Override
    public IRuntimeStepBuilder<?,?,InputType, OutputType> step(String stepName, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType) {
        Objects.requireNonNull(stepName, "Step name cannot be null");
        String key = stepName.trim();

        if (steps.containsKey(key)) {
            throw new IllegalArgumentException("Step already exists in stage [" + stageName + "]: " + key);
        }

        IRuntimeStepBuilder<?,?,InputType, OutputType> stepBuilder = new RuntimeStepBuilder<>(this, key);
        steps.put(key, stepBuilder);
        log.info("Added step [{}] to stage [{}]", key, stageName);

        return stepBuilder;
    }

    @Override
    public IRuntimeStepBuilder<?,?,InputType, OutputType> step(String stepName, RuntimeStepPosition position) {
        Objects.requireNonNull(stepName, "Step name cannot be null");
        Objects.requireNonNull(position, "RuntimeStepPosition cannot be null");

        String key = stepName.trim();

        if (steps.containsKey(key)) {
            throw new IllegalArgumentException("Step already exists in stage [" + stageName + "]: " + key);
        }

        Map<String, IRuntimeStepBuilder<?,?,InputType, OutputType>> reordered = new LinkedHashMap<>();
        boolean inserted = false;

        for (Map.Entry<String, IRuntimeStepBuilder<?,?,InputType, OutputType>> entry : steps.entrySet()) {
            String existingKey = entry.getKey();

            if (position.position() == Position.BEFORE && existingKey.equals(position.elementName())) {
                reordered.put(key, new RuntimeStepBuilder<>(this, key));
                inserted = true;
            }

            reordered.put(existingKey, entry.getValue());

            if (position.position() == Position.AFTER && existingKey.equals(position.elementName())) {
                reordered.put(key, new RuntimeStepBuilder<>(this, key));
                inserted = true;
            }
        }

        if (!inserted) {
            reordered.put(key, new RuntimeStepBuilder<>(this, key));
            log.warn("Reference step [{}] not found — inserted [{}] at the end of stage [{}]",
                    position.elementName(), key, stageName);
        }

        steps.clear();
        steps.putAll(reordered);

        log.info("Added step [{}] {} [{}] in stage [{}]", key, position.position(), position.elementName(), stageName);

        return steps.get(key);
    } */

    /**
     * Construit un IRuntimeStage à partir des steps enregistrés.
     * @throws DslException 
     */
    @Override
    public IRuntimeStage build() throws DslException {
        log.info("Building stage [{}] with {} step(s)", stageName, steps.size());

        Map<String, IMethodBinder<?>> builtSteps = new LinkedHashMap<>();
        for (Map.Entry<String, IRuntimeStepBuilder<?,?>> entry : steps.entrySet()) {
            String key = entry.getKey();
            IRuntimeStepBuilder<?,?> builder = entry.getValue();
            Objects.requireNonNull(builder, "StepBuilder for " + key + " cannot be null");

            IRuntimeStep<?> step = builder.build(); 
            builtSteps.put(key, step);
        }

        return new RuntimeStage(stageName, builtSteps);
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String string,
            IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'step'");
    }

    @Override
    public <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String string,
            OrderedMapPosition<String> position,
            IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier,
            Class<ExecutionReturn> returnType) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'step'");
    }

}
