package com.garganttua.core.runtime;

import java.util.Map;

public interface IRuntimeStage {

    String getStageName();

    IRuntimeStep getStep(String stepName);

    Map<String, IRuntimeStep> getSteps();

}
