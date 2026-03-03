package com.garganttua.core.mutex.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.reflection.IClass;

public interface IMutexManagerBuilder
        extends IAutomaticBuilder<IMutexManagerBuilder, IMutexManager>, IPackageableBuilder<IMutexManagerBuilder, IMutexManager>, IDependentBuilder<IMutexManagerBuilder, IMutexManager> {

    IMutexManagerBuilder withFactory(IClass<? extends IMutex> type, IMutexFactory factory);

}
