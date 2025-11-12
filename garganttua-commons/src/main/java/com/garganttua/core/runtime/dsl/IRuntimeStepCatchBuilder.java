package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;

public interface IRuntimeStepCatchBuilder extends IAutomaticLinkedBuilder<IRuntimeStepCatchBuilder, IRuntimeStepOperationBuilder<?>, IRuntimeStepCatch>{

    IRuntimeStepCatchBuilder code(int i);

    IRuntimeStepCatchBuilder failback(boolean failback);

    IRuntimeStepCatchBuilder abort(boolean abord);

}
