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
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.utils.OrderedMapPosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeBuilder<InputType, OutputType>
        extends AbstractAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
        implements IRuntimeBuilder<InputType, OutputType> {

    private String name;
    private final OrderedMap<String, IRuntimeStageBuilder<InputType, OutputType>> stages = new OrderedMap<>();
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
    public IRuntimeStageBuilder<InputType, OutputType> stage(String stageName) {
        Objects.requireNonNull(stageName, "Stage name cannot be null");
        String key = stageName.trim();

        IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, key);
        stages.put(key, stageBuilder);
        log.info("Added stage [{}]", key);

        return stageBuilder;
    }

    @Override
    public IRuntimeStageBuilder<InputType, OutputType> stage(String stageName, OrderedMapPosition<String> position) {
        Objects.requireNonNull(stageName, "Stage name cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");

        String key = stageName.trim();
        IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, key);

        stages.putAt(key, stageBuilder, position);

        log.info("Added stage [{}] {}", key, position);

        return stages.get(key);
    }

    @Override
    protected IRuntime<InputType, OutputType> doBuild() throws DslException {
        Objects.requireNonNull(this.context, "Context cannot be null");
        log.info("Building Runtime [{}] with {} stage(s)", name, stages.size());

        Map<String, IRuntimeStage> builtStages = new LinkedHashMap<>();

        for (Map.Entry<String, IRuntimeStageBuilder<InputType, OutputType>> entry : stages.entrySet()) {
            String key = entry.getKey();
            IRuntimeStageBuilder<InputType, OutputType> stageBuilder = entry.getValue();
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