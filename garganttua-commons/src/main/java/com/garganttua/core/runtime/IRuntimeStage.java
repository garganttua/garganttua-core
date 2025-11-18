package com.garganttua.core.runtime;

import java.util.Map;

public interface IRuntimeStage<InputType, OutputType> {

    String getStageName();

    IRuntimeStep<?, InputType, OutputType> getStep(String stepName);

    Map<String, IRuntimeStep<?, InputType, OutputType>> getSteps();

}
