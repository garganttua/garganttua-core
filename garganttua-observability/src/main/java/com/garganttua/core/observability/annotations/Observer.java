package com.garganttua.core.observability.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as an {@code IObserver<ObservableEvent>} that should be
 * auto-detected and registered by
 * {@code ObservabilityBuilder.autoDetect(...)}.
 *
 * <p>The annotation embeds the per-subscription filters that would otherwise
 * be configured via the DSL ({@code .onlyEvents(...)}, {@code .matchingSource(...)}).
 * No filter argument = no filter — the observer receives every event.
 *
 * <h2>Annotated class contract</h2>
 * <ul>
 *   <li>Must implement {@code IObserver<ObservableEvent>}.</li>
 *   <li>Must declare a public no-arg constructor (the auto-detector
 *       instantiates via {@code IReflection.newInstance(IClass)}).</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Observer(
 *     events = ErrorEvent.class,
 *     sources = { "workflow:critical:*", "runtime:script:*" })
 * public class CriticalErrorReporter implements IObserver<ObservableEvent> {
 *     @Override
 *     public void onEvent(ObservableEvent event) {
 *         // alert team — only critical errors will reach this method
 *     }
 * }
 * }</pre>
 *
 * <p>Wire up at startup:
 * <pre>{@code
 * ObservabilityBuilder.create()
 *     .observe(workflow, mapper, runtime)
 *     .withPackage("com.myapp.observers")
 *     .autoDetect(true)
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Qualifier
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Observer {

    /**
     * Event types this observer should receive. Empty = all events.
     * Multiple entries are OR-combined (deliver if instance-of ANY).
     */
    Class<? extends ObservableEvent>[] events() default {};

    /**
     * Glob patterns the {@link ObservableEvent#source()} must match.
     * Empty = all sources. Multiple entries are OR-combined (deliver if
     * the source matches ANY pattern). {@code *} is the only wildcard
     * supported and matches any substring; everything else is literal.
     */
    String[] sources() default {};
}
