package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.RuntimeStagePosition;

public interface IRuntimeBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>> {

    IRuntimeStageBuilder stage(String string);

    IRuntimeStageBuilder stage(String string, RuntimeStagePosition position);

}
