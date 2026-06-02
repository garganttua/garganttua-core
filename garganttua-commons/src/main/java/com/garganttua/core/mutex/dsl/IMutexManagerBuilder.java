package com.garganttua.core.mutex.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.reflection.IClass;

/**
 * Fluent builder for constructing an {@link IMutexManager}.
 *
 * <p>
 * Supports automatic detection of annotated factories, package scanning, and dependency
 * tracking, while allowing explicit registration of per-type factories via
 * {@link #withFactory(IClass, IMutexFactory)}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see IMutexFactory
 */
public interface IMutexManagerBuilder
        extends IAutomaticBuilder<IMutexManagerBuilder, IMutexManager>, IPackageableBuilder<IMutexManagerBuilder, IMutexManager>, IDependentBuilder<IMutexManagerBuilder, IMutexManager> {

    /**
     * Registers a factory for the given mutex type.
     *
     * @param type    the mutex implementation type the factory produces
     * @param factory the factory responsible for creating mutexes of {@code type}
     * @return this builder for method chaining
     */
    IMutexManagerBuilder withFactory(IClass<? extends IMutex> type, IMutexFactory factory);

}
