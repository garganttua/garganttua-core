package com.garganttua.core.observability.dsl;

import static com.garganttua.core.condition.Conditions.and;
import static com.garganttua.core.condition.Conditions.custom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Black-box tests for {@code ObservabilityBuilder} / {@code ObservabilityBinding}
 * under the dependency-inversion wiring model: sources self-attach to the
 * binding after build (here we drive that explicitly via
 * {@link ObservabilityBinding#attachSource(com.garganttua.core.observability.IObservable)}).
 *
 * @since 2.0.0-ALPHA02
 */
@DisplayName("Observability DSL Builder Tests")
class ObservabilityBuilderTest {

    @BeforeAll
    static void wireReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
    }

    private static StartEvent start(String source) {
        return new StartEvent(UUID.randomUUID(), Instant.now(), source);
    }

    private static EndEvent end(String source, Integer code) {
        return new EndEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO, code);
    }

    private static ErrorEvent error(String source, Throwable t) {
        return new ErrorEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO, t);
    }

    private static IObserver<ObservableEvent> collector(List<ObservableEvent> sink) {
        return sink::add;
    }

    @Test
    @DisplayName("Cas 1: simple wiring without filter — observer hears every event from every attached source")
    void simpleWiring_noFilter() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");
        List<ObservableEvent> received = new ArrayList<>();

        try (ObservabilityBinding binding = ObservabilityBuilder.create()
                .subscribe(collector(received))
                .up()
                .build()) {

            binding.attachSource(workflow);
            binding.attachSource(mapper);

            workflow.fire(start("workflow:foo"));
            mapper.fire(end("mapper:bar->baz", 0));

            assertEquals(2, received.size(), "Both events should be delivered");
            assertEquals(2, binding.count(), "Binding should record 2 (source, wrapper) registrations");
        }
    }

    @Test
    @DisplayName("Cas 2: condition DSL filter — only matching events reach observer")
    void filteredViaConditionDsl() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        List<ObservableEvent> received = new ArrayList<>();

        try (ObservabilityBinding binding = ObservabilityBuilder.create()
                .subscribe(collector(received))
                    .when(events -> and(
                            custom(events, ObservableEvent::source,
                                    src -> src != null && src.startsWith("workflow:critical:")),
                            custom(events, e -> e instanceof EndEvent ee && ee.code() != null && ee.code() >= 400)))
                .up()
                .build()) {

            binding.attachSource(workflow);

            workflow.fire(end("workflow:routine:run", 0));        // wrong source AND code
            workflow.fire(end("workflow:critical:save", 200));     // wrong code
            workflow.fire(end("workflow:critical:save", 500));     // matches both
            workflow.fire(start("workflow:critical:save"));        // wrong event type

            assertEquals(1, received.size(), "Only the 500-code critical EndEvent should pass");
            assertTrue(received.get(0) instanceof EndEvent);
            assertEquals(500, ((EndEvent) received.get(0)).code());
        }
    }

    @Test
    @DisplayName("Cas 3: predicate filter — JDK Predicate escape hatch")
    void filteredViaPredicate() throws DslException {
        TestObservable runtime = new TestObservable("runtime");
        AtomicInteger slowCount = new AtomicInteger();

        try (var binding = ObservabilityBuilder.create()
                .subscribe(e -> slowCount.incrementAndGet())
                    .where(e -> e instanceof EndEvent ee && ee.duration().toMillis() > 1000)
                .up()
                .build()) {

            binding.attachSource(runtime);

            // Fast — filtered out.
            runtime.fire(new EndEvent(UUID.randomUUID(), Instant.now(),
                    "runtime:fast", Duration.ofMillis(10), 0));
            // Slow — should match.
            runtime.fire(new EndEvent(UUID.randomUUID(), Instant.now(),
                    "runtime:slow", Duration.ofSeconds(5), 0));
            // Start — filtered out by event-type predicate clause.
            runtime.fire(start("runtime:abc"));

            assertEquals(1, slowCount.get(), "Only one slow end event should be counted");
        }
    }

    @Test
    @DisplayName("Cas 4: onlyEvents + matchingSource sugar — composes with AND")
    void sugarFilters() throws DslException {
        TestObservable script = new TestObservable("script");
        TestObservable workflow = new TestObservable("workflow");
        List<ObservableEvent> errors = new ArrayList<>();
        List<ObservableEvent> starts = new ArrayList<>();

        try (var binding = ObservabilityBuilder.create()
                .subscribe(collector(errors))
                    .onlyEvents(ErrorEvent.class)
                    .matchingSource("workflow:*")
                .up()
                .subscribe(collector(starts))
                    .onlyEvents(StartEvent.class)
                .up()
                .build()) {

            binding.attachSource(script);
            binding.attachSource(workflow);

            workflow.fire(start("workflow:a"));
            workflow.fire(error("workflow:a", new RuntimeException("boom")));
            script.fire(error("script:b", new RuntimeException("nope")));       // wrong source
            script.fire(start("script:c"));

            assertEquals(1, errors.size(), "Only the workflow ErrorEvent should be in 'errors'");
            assertEquals("workflow:a", errors.get(0).source());

            assertEquals(2, starts.size(), "Both StartEvents should be in 'starts'");
        }
    }

    @Test
    @DisplayName("close() detaches every wrapper from its source")
    void closeDetaches() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");
        List<ObservableEvent> received = new ArrayList<>();

        ObservabilityBinding binding = ObservabilityBuilder.create()
                .subscribe(collector(received))
                .up()
                .build();

        binding.attachSource(workflow);
        binding.attachSource(mapper);

        assertEquals(1, workflow.observerCount());
        assertEquals(1, mapper.observerCount());

        binding.close();

        assertEquals(0, workflow.observerCount());
        assertEquals(0, mapper.observerCount());
        assertTrue(binding.isClosed());

        // Events fired post-close are silently dropped (no observer to receive them).
        workflow.fire(start("workflow:after-close"));
        assertEquals(0, received.size());

        // close() is idempotent.
        binding.close();
        assertTrue(binding.isClosed());
    }

    @Test
    @DisplayName("attachSource after close() is a silent no-op")
    void attachAfterClose_noop() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        ObservabilityBinding binding = ObservabilityBuilder.create()
                .subscribe(ev -> {})
                .up()
                .build();
        binding.close();
        binding.attachSource(workflow);
        assertEquals(0, workflow.observerCount(), "post-close attach must not register anything");
    }

    @Test
    @DisplayName("Binding with zero observers tolerates attachSource() without error")
    void emptyBinding_attachIsNoop() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        try (var binding = ObservabilityBuilder.create().build()) {
            binding.attachSource(workflow);
            assertEquals(0, workflow.observerCount());
            assertEquals(0, binding.count());
            assertEquals(0, binding.wrapperCount());
        }
    }

    @Test
    @DisplayName("Null observer is rejected immediately")
    void nullObserver_throws() {
        assertThrows(NullPointerException.class,
                () -> ObservabilityBuilder.create().subscribe(null));
    }

    @Test
    @DisplayName("observe(sources) declares manual observables that are attached at build")
    void manualObserveAttachesSources() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");
        List<ObservableEvent> received = new ArrayList<>();

        try (ObservabilityBinding binding = ObservabilityBuilder.create()
                .observe(workflow, mapper)
                .subscribe(collector(received))
                .up()
                .build()) {

            workflow.fire(start("workflow:foo"));
            mapper.fire(end("mapper:bar", 0));

            assertEquals(2, received.size(),
                    "Manually-observed sources must reach the subscribed observer");
            assertEquals(2, binding.count(),
                    "Binding should record 2 (source, wrapper) registrations");
        }
    }

    @Test
    @DisplayName("observe(null) is rejected")
    void nullObserve_throws() {
        assertThrows(NullPointerException.class,
                () -> ObservabilityBuilder.create().observe((com.garganttua.core.observability.IObservable[]) null));
    }

    @Test
    @DisplayName("globToRegex translates star and escapes literals")
    void globRegex_basics() {
        assertTrue("workflow:foo".matches(ObserverBindingBuilder.globToRegex("workflow:*")));
        assertTrue("workflow:critical:db".matches(ObserverBindingBuilder.globToRegex("*:critical:*")));
        assertFalse("script:foo".matches(ObserverBindingBuilder.globToRegex("workflow:*")));
        // Dot inside the source name must be literal, not regex-any.
        assertTrue("script:foo.bar".matches(ObserverBindingBuilder.globToRegex("script:foo.bar")));
        assertFalse("script:fooxbar".matches(ObserverBindingBuilder.globToRegex("script:foo.bar")));
    }
}
