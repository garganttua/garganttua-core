package com.garganttua.core.runtime.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.Position;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.RuntimeStagePosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeBuilder<InputType, OutputType>
        extends AbstractAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
        implements IRuntimeBuilder<InputType, OutputType> {

    private String name;
    private final Map<String, IRuntimeStageBuilder> stages = new LinkedHashMap<>();
    private IDiContext context;
    private Class<InputType> inputType;
    private Class<OutputType> outputType;

    public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
            Class<OutputType> outputType) {
        super(Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"));
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
    }

    @Override
    public IRuntimeStageBuilder stage(String stageName) {
        Objects.requireNonNull(stageName, "Stage name cannot be null");
        String key = stageName.trim();

        if (stages.containsKey(key)) {
            throw new IllegalArgumentException("Stage already exists: " + key);
        }

        IRuntimeStageBuilder stageBuilder = new RuntimeStageBuilder(this, key);
        stages.put(key, stageBuilder);
        log.info("Added stage [{}]", key);

        return stageBuilder;
    }

    @Override
    public IRuntimeStageBuilder stage(String stageName, RuntimeStagePosition position) {
        Objects.requireNonNull(stageName, "Stage name cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");

        String key = stageName.trim();
        if (stages.containsKey(key)) {
            throw new IllegalArgumentException("Stage already exists: " + key);
        }

        // On crée un nouveau LinkedHashMap pour réinsérer les éléments dans le bon
        // ordre
        Map<String, IRuntimeStageBuilder> reordered = new LinkedHashMap<>();
        boolean inserted = false;

        for (Map.Entry<String, IRuntimeStageBuilder> entry : stages.entrySet()) {
            String existingKey = entry.getKey();

            // Insérer avant ou après le stage cible
            if (position.position() == Position.BEFORE && existingKey.equals(position.elementName())) {
                reordered.put(key, new RuntimeStageBuilder(this, key));
                inserted = true;
            }

            reordered.put(existingKey, entry.getValue());

            if (position.position() == Position.AFTER && existingKey.equals(position.elementName())) {
                reordered.put(key, new RuntimeStageBuilder(this, key));
                inserted = true;
            }
        }

        // Si la cible n'existait pas, on ajoute simplement à la fin
        if (!inserted) {
            reordered.put(key, new RuntimeStageBuilder(this, key));
            log.warn("Reference stage [{}] not found — inserted [{}] at the end", position.elementName(), key);
        }

        stages.clear();
        stages.putAll(reordered);
        log.info("Added stage [{}] {} [{}]", key, position.position(), position.elementName());

        return stages.get(key);
    }

    @Override
    protected IRuntime<InputType, OutputType> doBuild() throws DslException {
        Objects.requireNonNull(this.context, "Context cannot be null");
        log.info("Building Runtime [{}] with {} stage(s)", name, stages.size());

        Map<String, IRuntimeStage> builtStages = new LinkedHashMap<>();

        for (Map.Entry<String, IRuntimeStageBuilder> entry : stages.entrySet()) {
            String key = entry.getKey();
            IRuntimeStageBuilder stageBuilder = entry.getValue();
            Objects.requireNonNull(stageBuilder, "StageBuilder for " + key + " cannot be null");

            IRuntimeStage stage = stageBuilder.build();
            builtStages.put(key, stage);
        }

        return new Runtime<>(name, builtStages, this.context, this.inputType, this.outputType);
    }

    @Override
    protected void doAutoDetection() {

    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
    }
}