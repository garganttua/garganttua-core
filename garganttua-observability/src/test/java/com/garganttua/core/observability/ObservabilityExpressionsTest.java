package com.garganttua.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObservabilityExpressionsTest {

	private ObservableRegistry registry;
	private List<ObservableEvent> received;

	@BeforeEach
	void setup() {
		registry = new ObservableRegistry();
		received = new ArrayList<>();
		registry.addObserver(received::add);
		ObservableContextHolder.push(registry, UUID.randomUUID());
		ObservabilityExpressions.clearStarts();
	}

	@AfterEach
	void teardown() {
		ObservableContextHolder.pop(null);
		ObservabilityExpressions.clearStarts();
	}

	@Test
	void observe_start_firesStartEvent() {
		ObservabilityExpressions.observe("start", "test:source");

		assertEquals(1, received.size());
		assertTrue(received.get(0) instanceof StartEvent, () -> "expected StartEvent, got: " + received.get(0));
		assertEquals("test:source", received.get(0).source());
	}

	@Test
	void observe_endAfterStart_computesDuration() throws InterruptedException {
		ObservabilityExpressions.observe("start", "test:source");
		Thread.sleep(5);
		ObservabilityExpressions.observe("end", "test:source");

		assertEquals(2, received.size());
		EndEvent end = (EndEvent) received.get(1);
		assertTrue(!end.duration().isNegative(), "duration must be non-negative");
		assertTrue(end.duration().toMillis() >= 1, "duration should reflect the elapsed time, got " + end.duration());
	}

	@Test
	void observe_endWithoutStart_yieldsZeroDuration() {
		ObservabilityExpressions.observe("end", "no-start");

		EndEvent end = (EndEvent) received.get(0);
		assertEquals(0, end.duration().toNanos());
	}

	@Test
	void observe_endWithCode_propagatesCode() {
		ObservabilityExpressions.observe("start", "with:code");
		ObservabilityExpressions.observe("end", "with:code", 42);

		EndEvent end = (EndEvent) received.get(1);
		assertEquals(Integer.valueOf(42), end.code());
	}

	@Test
	void observe_error_firesErrorEvent() {
		ObservabilityExpressions.observe("start", "err:source");
		ObservabilityExpressions.observe("error", "err:source");

		ErrorEvent err = (ErrorEvent) received.get(1);
		assertNotNull(err);
		assertEquals("err:source", err.source());
	}

	@Test
	void observe_noSession_isNoop() {
		ObservableContextHolder.pop(null);
		ObservabilityExpressions.observe("start", "anywhere");
		assertEquals(0, received.size());
		ObservableContextHolder.push(registry, UUID.randomUUID());
	}

	@Test
	void observe_unknownType_firesNoObserveEvent() {
		ObservabilityExpressions.observe("not-a-real-event", "ignored");
		// An unknown type fires no Start/End/Error observe event. It does emit a
		// warning — and now that logging is observable, that surfaces as a
		// LogEvent correlated with the current session, which is expected.
		long observeEvents = received.stream()
				.filter(e -> !(e instanceof LogEvent))
				.count();
		assertEquals(0, observeEvents);
	}
}
