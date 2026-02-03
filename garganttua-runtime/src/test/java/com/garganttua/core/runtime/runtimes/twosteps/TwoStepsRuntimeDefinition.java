package com.garganttua.core.runtime.runtimes.twosteps;

import java.util.List;

import javax.inject.Named;

import com.garganttua.core.runtime.annotations.RuntimeDefinition;
import com.garganttua.core.runtime.annotations.Steps;
import com.garganttua.core.runtime.annotations.Synchronized;

@RuntimeDefinition(input = String.class, output = String.class)
@Synchronized(bean="garganttua:redis-synchronizaion", mutex="two-steps-runtime-mutex")
@Named("two-steps-runtime")
public class TwoStepsRuntimeDefinition {

        @Steps
        public List<Class<?>> steps = List.of(StepOne.class, StepOutput.class);

        

}
