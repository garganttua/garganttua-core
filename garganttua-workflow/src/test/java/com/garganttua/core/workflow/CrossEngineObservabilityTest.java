package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.dsl.ExpressionContextBuilder;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.workflow.dsl.WorkflowsBuilder;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.script.dsl.ScriptsBuilder;

/**
 * End-to-end test verifying that a single observer attached at the Workflow
 * level receives correlated events from every engine in the call chain:
 * Workflow → Script → Runtime → Step. Every event must share the same
 * executionId, proving cross-engine session propagation through
 * ObservableContextHolder.
 */
class CrossEngineObservabilityTest {

	private static IReflectionBuilder reflectionBuilder;
	private IInjectionContextBuilder injectionContextBuilder;
	private IExpressionContextBuilder expressionContextBuilder;
	private IRuntimesBuilder runtimesBuilder;
    private IScriptsBuilder scriptsBuilder;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void setupClass() throws Exception {
		Class<? extends IReflectionProvider> providerClass = (Class<? extends IReflectionProvider>) Class
				.forName("com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
		reflectionBuilder = ReflectionBuilder.builder()
				.withProvider(providerClass.getDeclaredConstructor().newInstance())
				.withScanner(new ReflectionsAnnotationScanner());
		reflectionBuilder.build();
	}

	@BeforeEach
	void setup() {
		injectionContextBuilder = InjectionContext.builder()
				.provide(reflectionBuilder)
				.autoDetect(true)
				.withPackage("com.garganttua.core.runtime");

		expressionContextBuilder = ExpressionContextBuilder.builder();
		expressionContextBuilder.withPackage("com.garganttua").autoDetect(true).provide(injectionContextBuilder);

		injectionContextBuilder.build().onInit().onStart();
		expressionContextBuilder.build();

		runtimesBuilder = RuntimesBuilder.builder().provide(injectionContextBuilder);
        scriptsBuilder = ScriptsBuilder.builder()
                .provide(injectionContextBuilder)
                .provide(expressionContextBuilder)
                .provide(runtimesBuilder);
	}

	@Test
	void workflowObserverSeesEventsFromEveryNestedEngine() {
		IWorkflow workflow = WorkflowsBuilder.builder()
				.provide(injectionContextBuilder)
				.provide(scriptsBuilder)
				.workflow("cross-engine")
				.stage("phase")
					.script("result <- \"hello\"").name("greet").output("out", "result").up()
					.up()
				.timing(WorkflowTimingConfig.of())
				.build();

		List<ObservableEvent> received = new CopyOnWriteArrayList<>();
		((Workflow) workflow).addObserver(received::add);

		WorkflowResult result = workflow.execute();
		assertTrue(result.isSuccess(), () -> "workflow failed: " + result.exceptionMessage().orElse("?"));

		// All events must share the same executionId
		Set<UUID> ids = received.stream().map(ObservableEvent::executionId).collect(java.util.stream.Collectors.toSet());
		assertEquals(1, ids.size(),
				"all events must share the same executionId — cross-engine propagation broken. Got: " + ids);

		// We expect events from at least these engine layers
		List<String> sources = received.stream().map(ObservableEvent::source).distinct().toList();

		assertTrue(sources.stream().anyMatch(s -> s.startsWith("stage:")),
				"missing workflow stage events. Sources: " + sources);
		assertTrue(sources.stream().anyMatch(s -> s.startsWith("script:")),
				"missing workflow script events. Sources: " + sources);
		assertTrue(sources.stream().anyMatch(s -> s.startsWith("runtime:")),
				"missing Runtime engine events. Sources: " + sources);
		assertTrue(sources.stream().anyMatch(s -> s.matches("runtime:.*:step:.*")),
				"missing Runtime step events. Sources: " + sources);

		// Sanity check: at least one Start and one End per emitting engine
		long startCount = received.stream().filter(e -> e instanceof com.garganttua.core.observability.StartEvent).count();
		long endCount = received.stream().filter(e -> e instanceof com.garganttua.core.observability.EndEvent).count();
		assertTrue(startCount >= 4, "expected at least 4 StartEvents across the call chain, got " + startCount);
		assertTrue(endCount >= 4, "expected at least 4 EndEvents across the call chain, got " + endCount);
		assertFalse(received.stream().anyMatch(e -> e instanceof com.garganttua.core.observability.ErrorEvent),
				"successful execution must not emit ErrorEvent");
	}
}
