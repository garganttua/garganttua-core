package com.garganttua.core.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Guards a runtime step or operation with a named mutex so it executes under mutual exclusion.
 *
 * <p>
 * The Synchronized annotation is applied to a step class or an {@code @Operation} method to
 * serialize its execution. Before the annotated unit runs, the framework acquires the mutex
 * identified by {@link #mutex()} from the bean identified by {@link #bean()}, releasing it once
 * execution completes.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see Operation
 * @see Step
 */
@Indexed
@Reflected
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Synchronized {

    /**
     * The bean reference of the mutex provider used to acquire the lock.
     *
     * @return the mutex provider bean reference
     */
    String bean();

    /**
     * The name of the mutex to acquire around the annotated step or operation.
     *
     * @return the mutex name
     */
    String mutex();

}
