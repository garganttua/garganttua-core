package com.garganttua.core.runtime;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.supplying.IObjectSupplier;
import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

@RuntimeDefinition(input=String.class, output=String.class)
@Named("runtime-1")
public class DummyRuntime {

    @Stages
    public Map<String, List<Class<?>>> stages = Map.of(
            "stage-1", List.of(DummyRuntimeProcessStep.class));

    @Variables 
    public Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> presetVariables= Map.of("variable", of("preset-variable"));
}
