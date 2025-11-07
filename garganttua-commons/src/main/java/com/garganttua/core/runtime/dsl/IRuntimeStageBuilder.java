package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.runtime.RuntimeStepPosition;

public interface IRuntimeStageBuilder extends ILinkedBuilder<IRuntimeBuilder<?,?>, IRuntimeStage> {

    IRuntimeStepBuilder step(String string);

    IRuntimeStepBuilder step(String string, RuntimeStepPosition position);

}
