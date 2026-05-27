package com.garganttua.core.observability.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.observability.annotations.Observable;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Verifies that classes annotated with {@code @Observable} are auto-detected
 * via the {@link com.garganttua.core.injection.IInjectionContext} and
 * auto-attached as sources on the {@link ObservabilityBinding} produced by
 * {@code ObservabilityBuilder.build()}.
 */
@DisplayName("@Observable auto-attach tests")
class AutoDetectObservableTest {

    /** Captures every event the catch-all observer sees during the test. */
    static final List<ObservableEvent> RECEIVED = new ArrayList<>();

    @BeforeAll
    static void wireReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
    }

    @BeforeEach
    void resetSink() {
        RECEIVED.clear();
    }

    @Test
    @DisplayName("@Observable bean is auto-attached as a source; events flow through to the observer")
    void observableBeanIsAutoAttached() throws DslException {
        var reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0);
        IInjectionContextBuilder injCtxBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage(AutoDetectObservableTest.class.getPackageName())
                .provide(reflectionBuilder);

        ObservabilityBuilder obsBuilder = (ObservabilityBuilder) ObservabilityBuilder.create()
                .autoDetect(true)
                .subscribe(RECEIVED::add)        // sink for verification
                    .up()
                .provide(injCtxBuilder);          // registers @Observer / @Observable qualifiers

        injCtxBuilder.build().onInit().onStart();

        try (ObservabilityBinding binding = obsBuilder.build()) {
            // The auto-attached observable bean must already be wired.
            assertTrue(binding.count() >= 1,
                    "Expected at least one registration from the auto-attached @Observable bean");

            // Resolve the bean and fire — events must reach our manual observer.
            TestObservableBean bean = injCtxBuilder.build()
                    .queryBean(new com.garganttua.core.injection.BeanReference<>(
                            IClass.getClass(TestObservableBean.class),
                            java.util.Optional.empty(),
                            java.util.Optional.empty(),
                            java.util.Set.of(IClass.getClass(Observable.class))))
                    .orElseThrow();

            bean.fire(new StartEvent(UUID.randomUUID(), Instant.now(), "test:source"));
            bean.fire(new EndEvent(UUID.randomUUID(), Instant.now(), "test:source",
                    Duration.ofMillis(42), 0));

            assertEquals(2, RECEIVED.size(), "Both events must reach the subscribed observer");
            assertNotNull(binding);
        }
    }

    // ---------- @Observable-annotated fixture ----------

    /** Public so the bean provider scanner can pick it up. */
    @Observable
    public static class TestObservableBean implements IObservable {

        private final ObservableRegistry registry = new ObservableRegistry();

        @Override
        public void addObserver(IObserver<ObservableEvent> observer) {
            this.registry.addObserver(observer);
        }

        @Override
        public void removeObserver(IObserver<ObservableEvent> observer) {
            this.registry.removeObserver(observer);
        }

        void fire(ObservableEvent event) {
            this.registry.fire(event);
        }
    }
}
