package com.garganttua.core.runtime;

import java.util.Map;

import com.garganttua.core.reflection.binders.IMethodBinder;

public interface IRuntimeStage {

    String getStageName();

    IMethodBinder<?> getStep(String stepName);

    Map<String, IMethodBinder<?>> getSteps();

}
