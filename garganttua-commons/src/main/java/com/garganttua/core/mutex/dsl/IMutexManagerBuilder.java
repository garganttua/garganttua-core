package com.garganttua.core.mutex.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.injection.context.dsl.IContextReadinessBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.IMutexManager;

public interface IMutexManagerBuilder
        extends IAutomaticBuilder<IMutexManagerBuilder, IMutexManager>, IContextReadinessBuilder<IMutexManagerBuilder>, IPackageableBuilder<IMutexManagerBuilder, IMutexManager> {

    IMutexManagerBuilder withFactory(Class<? extends IMutex> type, IMutexFactory factory);

}
