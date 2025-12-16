package com.garganttua.core.runtime.runtimes.onestep;

import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.*;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.garganttua.core.runtime.annotations.Mutex;
import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

@RuntimeDefinition(input=String.class, output=String.class)
@Named("runtime-1")
@Mutex(name="OneStepRuntime-Mutex", strategy=IMutex.Strategy.awit, timout=5000, timeoutUnit=TimeUnit.SECONDS, fallbackStrategy=IMutex.FallbackStrategy.abort, retries=3)
public class OneStepRuntime {

    @Stages
    public Map<String, List<Class<?>>> stages = Map.of(
            "stage-1", List.of(DummyRuntimeProcessOutputStep.class));

    @Variables 
    public Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> presetVariables= Map.of("variable", of("preset-variable"));
}
