package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;

public interface IRuntimeStepCatchBuilder extends IAutomaticLinkedBuilder<IRuntimeStepCatchBuilder, IRuntimeStepBuilder<?, ?>, IRuntimeStepCatch>{

    IRuntimeStepCatchBuilder code(int i);

    IRuntimeStepCatchBuilder fallback(boolean failback);

    IRuntimeStepCatchBuilder abort(boolean abord);

}
