package com.garganttua.core.runtime.runtimes.twosteps;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Stages;
import com.garganttua.core.runtime.annotations.Synchronized;

@RuntimeDefinition(input = String.class, output = String.class)
@Synchronized(bean="garganttua:redis-synchronizaion", mutex="two-steps-runtime-mutex")
@Named("two-steps-runtime")
public class TwoStepsRuntimeDefinition {

        @Stages
        public Map<String, List<Class<?>>> stages = Map.of(
                        "stage-1", List.of(StepOne.class, StepOutput.class));

        

}
