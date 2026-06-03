package com.garganttua.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the sealed {@link ObservableEvent} record family: the
 * backward-compatible (payload-less) constructors must default payload to null,
 * accessors must round-trip, and record equality/hashCode must hold.
 */
class ObservableEventBehaviourTest {

	private final UUID id = UUID.randomUUID();
	private final Instant ts = Instant.ofEpochMilli(1_000L);

	@Test
	void startEvent_payloadlessConstructor_defaultsPayloadToNull() {
		StartEvent ev = new StartEvent(id, ts, "src");
		assertNull(ev.payload());
		assertEquals(id, ev.executionId());
		assertEquals(ts, ev.timestamp());
		assertEquals("src", ev.source());
	}

	@Test
	void startEvent_withPayload_roundTrips() {
		Object payload = new Object();
		StartEvent ev = new StartEvent(id, ts, "src", payload);
		assertSame(payload, ev.payload());
	}

	@Test
	void endEvent_payloadlessConstructor_keepsCodeAndDurationDropsPayload() {
		Duration d = Duration.ofMillis(42);
		EndEvent ev = new EndEvent(id, ts, "src", d, 7);
		assertEquals(d, ev.duration());
		assertEquals(7, ev.code());
		assertNull(ev.payload());
	}

	@Test
	void endEvent_allowsNullCode() {
		EndEvent ev = new EndEvent(id, ts, "src", Duration.ZERO, null);
		assertNull(ev.code());
	}

	@Test
	void errorEvent_payloadlessConstructor_keepsFailureDropsPayload() {
		RuntimeException failure = new RuntimeException("x");
		ErrorEvent ev = new ErrorEvent(id, ts, "src", Duration.ofMillis(5), failure);
		assertSame(failure, ev.failure());
		assertNull(ev.payload());
	}

	@Test
	void logEvent_payloadlessConstructor_defaultsPayloadToNull() {
		LogEvent ev = new LogEvent(id, ts, "src", LogEvent.Level.INFO, "msg");
		assertEquals(LogEvent.Level.INFO, ev.level());
		assertEquals("msg", ev.message());
		assertNull(ev.payload());
	}

	@Test
	void logEvent_levelOrdinalLadder_matchesSlf4jOrder() {
		assertEquals(0, LogEvent.Level.TRACE.ordinal());
		assertEquals(1, LogEvent.Level.DEBUG.ordinal());
		assertEquals(2, LogEvent.Level.INFO.ordinal());
		assertEquals(3, LogEvent.Level.WARN.ordinal());
		assertEquals(4, LogEvent.Level.ERROR.ordinal());
	}

	@Test
	void recordEquality_sameComponents_areEqual() {
		StartEvent a = new StartEvent(id, ts, "src", "p");
		StartEvent b = new StartEvent(id, ts, "src", "p");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void recordEquality_differentSource_notEqual() {
		StartEvent a = new StartEvent(id, ts, "src1");
		StartEvent b = new StartEvent(id, ts, "src2");
		assertNotEquals(a, b);
	}
}
