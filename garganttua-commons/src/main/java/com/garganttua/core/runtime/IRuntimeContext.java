package com.garganttua.core.runtime;

import com.garganttua.core.injection.IDiContext;

public interface IRuntimeContext<InputType, OutputType> extends IDiContext {

    IRuntimeResult<InputType, OutputType> getResult();

}
