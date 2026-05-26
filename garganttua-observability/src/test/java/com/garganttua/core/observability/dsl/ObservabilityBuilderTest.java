package com.garganttua.core.observability.dsl;

import static com.garganttua.core.condition.Conditions.and;
import static com.garganttua.core.condition.Conditions.custom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

/**
 * Exercises the four use cases from the design plan plus the negative paths.
 */
@DisplayName("Observability DSL Builder Tests")
class ObservabilityBuilderTest {

    static {
        // The condition DSL uses IClass.getClass(...) internally — initialise a
        // runtime reflection facade once for the whole suite.
        try {
            IClass.setReflection(ReflectionBuilder.builder()
                    .withProvider(new RuntimeReflectionProvider(), 0)
                    .build());
        } catch (DslException e) {
            throw new RuntimeException(e);
        }
    }

    private static StartEvent start(String source) {
        return new StartEvent(UUID.randomUUID(), Instant.now(), source);
    }

    private static EndEvent end(String source, Integer code) {
        return new EndEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO, code);
    }

    private static ErrorEvent error(String source, Throwable cause) {
        return new ErrorEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO, cause);
    }

    private static <E extends ObservableEvent> IObserver<ObservableEvent> collector(List<ObservableEvent> sink) {
        return sink::add;
    }

    @Test
    @DisplayName("Cas 1: simple wiring without filter — observer hears every event from every source")
    void simpleWiring_noFilter() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");
        List<ObservableEvent> received = new ArrayList<>();

        try (ObservabilityBinding binding = ObservabilityBuilder.create()
                .observe(workflow, mapper)
                .observer(collector(received))
                .up()
                .build()) {

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
                .observe(workflow)
                .observer(collector(received))
                    .when(events -> and(
                            custom(events, ObservableEvent::source,
                                    src -> src != null && src.startsWith("workflow:critical:")),
                            custom(events, e -> e instanceof EndEvent ee && ee.code() != null && ee.code() >= 400)))
                .up()
                .build()) {

            workflow.fire(end("workflow:routine:run", 0));        // wrong source AND code
            workflow.fire(end("workflow:critical:save", 200));     // wrong code
            workflow.fire(end("workflow:critical:save", 500));     // matches both
            workflow.fire(start("workflow:critical:save"));        // wrong event type

            assertEquals(1, received.size(), "Only the 500-code critical EndEvent should pass");
            assertTrue(received.get(0) instanceof EndEvent);
            assertEquals(500, ((EndEvent) received.get(0)).code());
            assertNotNull(binding);
        }
    }

    @Test
    @DisplayName("Cas 3: predicate filter — JDK Predicate escape hatch")
    void filteredViaPredicate() throws DslException {
        TestObservable runtime = new TestObservable("runtime");
        AtomicInteger slowCount = new AtomicInteger();

        try (var binding = ObservabilityBuilder.create()
                .observe(runtime)
                .observer(e -> slowCount.incrementAndGet())
                    .where(e -> e instanceof EndEvent ee && ee.duration().toMillis() > 1000)
                .up()
                .build()) {

            // Fast — filtered out.
            runtime.fire(new EndEvent(UUID.randomUUID(), Instant.now(),
                    "runtime:fast", Duration.ofMillis(10), 0));
            // Slow — should match.
            runtime.fire(new EndEvent(UUID.randomUUID(), Instant.now(),
                    "runtime:slow", Duration.ofSeconds(5), 0));
            // Start — filtered out by event-type predicate clause.
            runtime.fire(start("runtime:abc"));

            assertEquals(1, slowCount.get(), "Only one slow end event should be counted");
            assertNotNull(binding);
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
                .observe(script, workflow)
                .observer(collector(errors))
                    .onlyEvents(ErrorEvent.class)
                    .matchingSource("workflow:*")
                .up()
                .observer(collector(starts))
                    .onlyEvents(StartEvent.class)
                .up()
                .build()) {

            workflow.fire(start("workflow:a"));
            workflow.fire(error("workflow:a", new RuntimeException("boom")));
            script.fire(error("script:b", new RuntimeException("nope")));       // wrong source
            script.fire(start("script:c"));

            assertEquals(1, errors.size(), "Only the workflow ErrorEvent should be in 'errors'");
            assertEquals("workflow:a", errors.get(0).source());

            assertEquals(2, starts.size(), "Both StartEvents should be in 'starts'");
            assertNotNull(binding);
        }
    }

    @Test
    @DisplayName("close() detaches every wrapper from its source")
    void closeDetaches() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");
        List<ObservableEvent> received = new ArrayList<>();

        ObservabilityBinding binding = ObservabilityBuilder.create()
                .observe(workflow, mapper)
                .observer(collector(received))
                .up()
                .build();

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
    @DisplayName("toObservable overrides the default source set for one binding")
    void overrideSources() throws DslException {
        TestObservable a = new TestObservable("a");
        TestObservable b = new TestObservable("b");
        List<ObservableEvent> onlyA = new ArrayList<>();
        List<ObservableEvent> both = new ArrayList<>();

        try (var binding = ObservabilityBuilder.create()
                .observe(a, b)                         // default: both
                .observer(collector(onlyA))
                    .toObservable(a)                   // narrow to A
                .up()
                .observer(collector(both))             // inherits default (a + b)
                .up()
                .build()) {

            a.fire(start("a:1"));
            b.fire(start("b:1"));

            assertEquals(1, onlyA.size(), "narrowed observer hears only a");
            assertEquals(2, both.size(), "default observer hears both");
            assertNotNull(binding);
        }
    }

    @Test
    @DisplayName("Observer with no sources (no .observe and no .toObservable) raises a clear error")
    void noSources_throwsAtBuild() {
        DslException e = assertThrows(DslException.class, () -> ObservabilityBuilder.create()
                .observer(ev -> {})
                .up()
                .build());
        assertTrue(e.getMessage().toLowerCase().contains("source"),
                "Error message should mention the missing source: " + e.getMessage());
    }

    @Test
    @DisplayName("Null observer is rejected immediately")
    void nullObserver_throws() {
        assertThrows(NullPointerException.class, () -> ObservabilityBuilder.create().observer(null));
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
