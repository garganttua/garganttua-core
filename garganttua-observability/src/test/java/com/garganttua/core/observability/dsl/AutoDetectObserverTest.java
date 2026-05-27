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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.EndEvent;
import com.garganttua.core.observability.ErrorEvent;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Validates {@code @Observer} class-level auto-detection.
 *
 * <p>The annotated observers below sit in this same test package so the
 * package-scoped scan can find them without polluting other modules.
 */
@DisplayName("@Observer auto-detection tests")
class AutoDetectObserverTest {

    /** Aggregator for cross-observer assertions. Populated by the public observers below. */
    static final List<ObservableEvent> ERRORS_ONLY = new ArrayList<>();
    static final List<ObservableEvent> WORKFLOW_END = new ArrayList<>();
    static final List<ObservableEvent> EVERYTHING = new ArrayList<>();

    @BeforeAll
    static void wireReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
        ERRORS_ONLY.clear();
        WORKFLOW_END.clear();
        EVERYTHING.clear();
    }

    private static StartEvent start(String source) {
        return new StartEvent(UUID.randomUUID(), Instant.now(), source);
    }

    private static EndEvent end(String source, Integer code) {
        return new EndEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO, code);
    }

    private static ErrorEvent error(String source) {
        return new ErrorEvent(UUID.randomUUID(), Instant.now(), source, Duration.ZERO,
                new RuntimeException("boom"));
    }

    @Test
    @DisplayName("autoDetect wires @Observer classes with their declared filters")
    void autoDetect_wiresFilters() throws DslException {
        TestObservable workflow = new TestObservable("workflow");
        TestObservable mapper = new TestObservable("mapper");

        // @Observer scan now consumes ONLY the IInjectionContext: the
        // injection context's own autodetect registers any @Observer-qualified
        // class as a bean (because @Observer is now @Qualifier-marked), and
        // ObservabilityBuilder queries those beans during its doAutoDetection().
        var reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0);
        IInjectionContextBuilder injCtxBuilder = InjectionContext.builder()
                .autoDetect(true)
                .withPackage(AutoDetectObserverTest.class.getPackageName())
                .provide(reflectionBuilder);

        ObservabilityBuilder obsBuilder = (ObservabilityBuilder) ObservabilityBuilder.create()
                .autoDetect(true)
                .provide(injCtxBuilder);

        // Without Bootstrap orchestrating, the CONFIGURATION stage must be
        // fired manually so doConfigureWithDependencyBuilder runs (which is
        // what declares @Observer as a qualifier on the InjectionContext).
        obsBuilder.runConfigurationStage();

        // Then build + start the InjectionContext — queryBeans() requires
        // init+started lifecycle.
        injCtxBuilder.build().onInit().onStart();

        try (ObservabilityBinding binding = obsBuilder.build()) {

            binding.attachSource(workflow);
            binding.attachSource(mapper);

            workflow.fire(start("workflow:run"));            // EVERYTHING only
            workflow.fire(end("workflow:run", 0));            // EVERYTHING + WORKFLOW_END
            workflow.fire(error("workflow:run"));             // EVERYTHING + ERRORS_ONLY
            mapper.fire(error("mapper:UserDto->User"));       // EVERYTHING + ERRORS_ONLY
            mapper.fire(end("mapper:run", 0));                // EVERYTHING (not WORKFLOW_END)

            assertEquals(2, ERRORS_ONLY.size(), "errors-only observer must see both ErrorEvents");
            assertTrue(ERRORS_ONLY.stream().allMatch(e -> e instanceof ErrorEvent));

            assertEquals(1, WORKFLOW_END.size(), "workflow-end observer must see only workflow:* EndEvent");
            assertTrue(WORKFLOW_END.get(0) instanceof EndEvent);
            assertEquals("workflow:run", WORKFLOW_END.get(0).source());

            assertEquals(5, EVERYTHING.size(), "no-filter observer must see all events");
            assertNotNull(binding);
        }
    }

    // ---------- @Observer-annotated test fixtures (public so the scanner sees them) ----------

    /** Catch-all observer — no events/sources filters. */
    @Observer
    public static class CatchAllObserver implements IObserver<ObservableEvent> {
        @Override
        public void onEvent(ObservableEvent event) {
            EVERYTHING.add(event);
        }
    }

    /** Errors only — any source. */
    @Observer(events = ErrorEvent.class)
    public static class ErrorsOnlyObserver implements IObserver<ObservableEvent> {
        @Override
        public void onEvent(ObservableEvent event) {
            ERRORS_ONLY.add(event);
        }
    }

    /** Workflow EndEvents only. */
    @Observer(events = EndEvent.class, sources = "workflow:*")
    public static class WorkflowEndObserver implements IObserver<ObservableEvent> {
        @Override
        public void onEvent(ObservableEvent event) {
            WORKFLOW_END.add(event);
        }
    }
}
