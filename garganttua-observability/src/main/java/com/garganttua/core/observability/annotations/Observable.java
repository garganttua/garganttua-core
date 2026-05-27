package com.garganttua.core.observability.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.observability.IObservable;
import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as an {@link IObservable} that should be auto-detected and
 * attached to the {@link com.garganttua.core.observability.ObservabilityBinding
 * ObservabilityBinding} produced by
 * {@link com.garganttua.core.observability.dsl.ObservabilityBuilder
 * ObservabilityBuilder} when {@code autoDetect(true)} is enabled.
 *
 * <p>This annotation is itself a {@code @Qualifier}: the
 * {@code InjectionContextBuilder} registers every {@code @Observable}
 * class as a managed bean, and {@code ObservabilityBuilder} queries the
 * injection context for those beans to auto-attach them as sources.
 *
 * <h2>Annotated class contract</h2>
 * <ul>
 *   <li>Must implement {@link IObservable}.</li>
 *   <li>Must be in a package scanned by the {@code InjectionContextBuilder}
 *       (typically declared via {@code .withPackage(...)} on the bean
 *       provider).</li>
 * </ul>
 *
 * <p>Manual attachment via
 * {@link com.garganttua.core.observability.ObservabilityBinding#attachSource(IObservable)
 * binding.attachSource(...)} remains the supported path for engines that are
 * created outside the injection context (e.g. via dedicated builders such as
 * {@code WorkflowBuilder} or {@code RuntimeBuilder}).
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Qualifier
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Observable {
}
