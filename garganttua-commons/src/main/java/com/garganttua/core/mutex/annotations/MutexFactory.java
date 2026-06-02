package com.garganttua.core.mutex.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks an {@link com.garganttua.core.mutex.IMutexFactory} implementation as the factory
 * for a specific {@link IMutex} type.
 *
 * <p>
 * Acts as a JSR-330 {@link Qualifier} so the declared {@link #type()} can be used to
 * disambiguate and select the appropriate factory when resolving mutexes through the
 * dependency injection container.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.mutex.IMutexFactory
 * @see IMutex
 */
@Indexed
@Reflected
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface MutexFactory {

    /**
     * The mutex implementation type produced by the annotated factory.
     *
     * @return the {@link IMutex} subtype this factory creates
     */
    Class<? extends IMutex> type();
}
