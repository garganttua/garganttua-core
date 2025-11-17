package com.garganttua.core.runtime.dsl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.OrderedMapBuilder;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflection.utils.ParameterizedTypeImpl;
import com.garganttua.core.reflection.utils.WildcardTypeImpl;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeBuilder<InputType, OutputType>
        extends
        AbstractAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>
        implements IRuntimeBuilder<InputType, OutputType> {

    private String name;
    private final OrderedMapBuilder<String, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStage> stages = new OrderedMapBuilder<>();
    private IDiContext context;
    private Class<InputType> inputType;
    private Class<OutputType> outputType;
    private Object objectForAutoDetection;
    private Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> presetVariables = new HashMap<>();

    public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
            Class<OutputType> outputType) {
        super(Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"));
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.inputType = Objects.requireNonNull(inputType, "Input type cannot be null");
        this.outputType = Objects.requireNonNull(outputType, "Output Type cannot be null");
    }

    /**
     * Secondary ctor used only for auto detection
     * 
     * @param runtimesBuilder
     * @param name
     * @param inputType
     * @param outputType
     * @param obj
     */
    protected RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, Class<InputType> inputType,
            Class<OutputType> outputType, Object objectForAutoDetection) {
        this(runtimesBuilder, name, inputType, outputType);
        this.objectForAutoDetection = Objects.requireNonNull(objectForAutoDetection,
                "objectForAutoDetection cannot be null");

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

        Map<String, IRuntimeStage> builtStages = this.stages.build();

        Map<String, IObjectSupplier<?>> variables = this.presetVariables.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().build()));
        return new Runtime<>(name, builtStages, this.context, this.inputType, this.outputType, variables);
    }

    @Override
    protected void doAutoDetection() {
        Objects.requireNonNull(this.objectForAutoDetection, "objectForAutoDetection cannot be null");
        Objects.requireNonNull(this.context, "Context cannot be null");
        this.collectStages();
        this.collectPresetVariables();
    }

    @SuppressWarnings("unchecked")
    private void collectPresetVariables() {
        ParameterizedType mapType = getVariablesMapType();
        String address = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                this.objectForAutoDetection.getClass(), Variables.class, mapType.getRawType());

        if (address == null)
            return;

        Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> variables = (Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>>) ObjectQueryFactory
                .objectQuery(this.objectForAutoDetection).getValue(address);

        variables.entrySet().stream().forEach(e -> this.variable(e.getKey(), e.getValue()));

    }

    @SuppressWarnings("unchecked")
    private void collectStages() {
        ParameterizedType mapType = getStagesMapType();

        String address = ObjectReflectionHelper.getFieldAddressAnnotatedWithAndCheckType(
                this.objectForAutoDetection.getClass(), Stages.class, mapType.getRawType());

        if (address == null)
            throw new DslException("Runtime Definition " + this.objectForAutoDetection.getClass().getSimpleName()
                    + " does not have any field annotated with @Stages");

        Map<String, List<Class<Object>>> stages = (Map<String, List<Class<Object>>>) ObjectQueryFactory
                .objectQuery(this.objectForAutoDetection).getValue(address);

        stages.entrySet().stream().forEach(e -> {
            String stageName = e.getKey();
            List<Class<Object>> steps = e.getValue();
            IRuntimeStageBuilder<InputType, OutputType> stageBuilder = new RuntimeStageBuilder<>(this, stageName, steps)
                    .autoDetect(true);
            stageBuilder.handle(this.context);
            this.stages.put(stageName, stageBuilder);
        });
    }

    private ParameterizedType getVariablesMapType() {
        WildcardType wildcardIObjectSupplier = WildcardTypeImpl.extends_(new ParameterizedTypeImpl(
                IObjectSupplier.class,
                new Type[] { WildcardTypeImpl.unbounded() }));

        ParameterizedType supplierBuilderType = new ParameterizedTypeImpl(
                IObjectSupplierBuilder.class,
                new Type[] {
                        WildcardTypeImpl.unbounded(),
                        wildcardIObjectSupplier
                });

        return new ParameterizedTypeImpl(
                Map.class,
                new Type[] {
                        String.class,
                        supplierBuilderType
                });
    }

    private ParameterizedType getStagesMapType() {
        ParameterizedType listOfClass = new ParameterizedTypeImpl(
                List.class,
                new Type[] { Class.class });

        return new ParameterizedTypeImpl(
                Map.class,
                new Type[] { String.class, listOfClass });
    }

    @Override
    public void handle(IDiContext context) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.stages.values().stream().forEach(s -> s.handle(context));
    }

    @Override
    public IRuntimeBuilder<InputType, OutputType> variable(String name,
            IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> value) {
        this.presetVariables.put(Objects.requireNonNull(name, "Variable name cannot be null"),
                Objects.requireNonNull(value, "Value supplier builder cannot be null"));
        return this;
    }
}