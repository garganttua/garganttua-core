package com.garganttua.core.runtime.perfs.runtimes;

import static com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder.*;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Variables;
import com.garganttua.core.runtime.runtimes.onestep.DummyRuntimeProcessOutputStep;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

@RuntimeDefinition(input=String.class, output=String.class)
@Named("RuntimeWithCatchedExceptionAndHandledByFallback")
public class RuntimeWithCatchedExceptionAndHandledByFallback {

    @Stages
    public Map<String, List<Class<?>>> stages = Map.of(
            "stage-1", List.of(DummyRuntimeProcessOutputStep.class));

    @Variables
    public Map<String, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>>> presetVariables= Map.of("variable", of("di-exception"));

}
