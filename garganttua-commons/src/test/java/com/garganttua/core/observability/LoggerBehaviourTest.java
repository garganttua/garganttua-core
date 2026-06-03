package com.garganttua.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link Logger}. The static {@code THRESHOLD} is resolved
 * once at class load from {@code garganttua.log.level} (default INFO), so these
 * tests only exercise INFO/WARN/ERROR levels to guarantee deterministic
 * emission regardless of the running JVM's system property.
 */
class LoggerBehaviourTest {

	@AfterEach
	void clearHolder() {
		ObservableContextHolder.pop(null);
	}

	private List<ObservableEvent> attach(Logger log) {
		List<ObservableEvent> received = new ArrayList<>();
		log.addObserver(received::add);
		return received;
	}

	@Test
	void getLogger_byName_isCachedReturnsSameInstance() {
		Logger a = Logger.getLogger("com.example.Same");
		Logger b = Logger.getLogger("com.example.Same");
		assertSame(a, b, "loggers with the same name must be cached");
	}

	@Test
	void getLogger_differentNames_areDistinct() {
		Logger a = Logger.getLogger("com.example.A");
		Logger b = Logger.getLogger("com.example.B");
		assertNotSame(a, b);
	}

	@Test
	void getLogger_byClass_usesFullyQualifiedName() {
		Logger log = Logger.getLogger(String.class);
		assertEquals("java.lang.String", log.name());
	}

	@Test
	void name_returnsConstructionName() {
		Logger log = Logger.getLogger("my.custom.Logger.name");
		assertEquals("my.custom.Logger.name", log.name());
	}

	@Test
	void info_withObserver_emitsLogEventWithLevelAndFormattedMessage() {
		Logger log = Logger.getLogger("test.info." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("hello {}", "world");

		assertEquals(1, received.size());
		LogEvent ev = (LogEvent) received.get(0);
		assertEquals(LogEvent.Level.INFO, ev.level());
		assertEquals("hello world", ev.message());
		assertEquals(log.name(), ev.source());
	}

	@Test
	void warn_andError_setCorrespondingLevels() {
		Logger log = Logger.getLogger("test.levels." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.warn("a warning");
		log.error("an error");

		assertEquals(2, received.size());
		assertEquals(LogEvent.Level.WARN, ((LogEvent) received.get(0)).level());
		assertEquals(LogEvent.Level.ERROR, ((LogEvent) received.get(1)).level());
	}

	@Test
	void noObserver_isNoop() {
		Logger log = Logger.getLogger("test.noop." + UUID.randomUUID());
		// no observer attached anywhere — must not throw and must produce nothing observable
		log.info("nobody is listening {}", 42);
		assertFalse(log.isEnabled(LogEvent.Level.INFO));
	}

	@Test
	void trailingThrowable_becomesPayloadNotSubstitution() {
		Logger log = Logger.getLogger("test.throwable." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		RuntimeException boom = new RuntimeException("boom");
		log.error("operation failed for {}", "item-1", boom);

		LogEvent ev = (LogEvent) received.get(0);
		assertSame(boom, ev.payload(), "trailing Throwable must be the payload");
		assertEquals("operation failed for item-1", ev.message(),
				"the Throwable must NOT be consumed by a placeholder");
	}

	@Test
	void placeholders_substituteInOrder_extraPlaceholdersLeftLiteral() {
		Logger log = Logger.getLogger("test.placeholders." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("{} + {} = {}", 1, 2);

		LogEvent ev = (LogEvent) received.get(0);
		assertEquals("1 + 2 = {}", ev.message(),
				"unmatched placeholders remain literal when args run out");
	}

	@Test
	void extraArgs_areIgnored() {
		Logger log = Logger.getLogger("test.extraargs." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("only {}", "one", "two", "three");

		LogEvent ev = (LogEvent) received.get(0);
		assertEquals("only one", ev.message());
	}

	@Test
	void nullFormat_rendersLiteralNull() {
		Logger log = Logger.getLogger("test.nullfmt." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info(null);

		assertEquals("null", ((LogEvent) received.get(0)).message());
	}

	@Test
	void nullArgument_rendersLiteralNull() {
		Logger log = Logger.getLogger("test.nullarg." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("value={}", new Object[] { null });

		assertEquals("value=null", ((LogEvent) received.get(0)).message());
	}

	@Test
	void arrayArgument_isDeepToStringed() {
		Logger log = Logger.getLogger("test.array." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("arr={}", (Object) new int[] { 1, 2, 3 });

		assertEquals("arr=[1, 2, 3]", ((LogEvent) received.get(0)).message());
	}

	@Test
	void objectArrayArgument_isDeepToStringed() {
		Logger log = Logger.getLogger("test.objarray." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("arr={}", (Object) new String[] { "a", "b" });

		assertEquals("arr=[a, b]", ((LogEvent) received.get(0)).message());
	}

	@Test
	void noExecutionSession_usesNilUuid() {
		Logger log = Logger.getLogger("test.niluuid." + UUID.randomUUID());
		List<ObservableEvent> received = attach(log);

		log.info("standalone");

		assertEquals(new UUID(0L, 0L), received.get(0).executionId(),
				"without a bound session the event carries the nil UUID");
	}

	@Test
	void boundSession_adoptsExecutionIdAndFiresToSessionRegistry() {
		Logger log = Logger.getLogger("test.session." + UUID.randomUUID());
		ObservableRegistry sessionReg = new ObservableRegistry();
		List<ObservableEvent> sessionReceived = new ArrayList<>();
		sessionReg.addObserver(sessionReceived::add);

		UUID execId = UUID.randomUUID();
		ObservableContextHolder.Session previous = ObservableContextHolder.push(sessionReg, execId);
		try {
			log.info("correlated");
		} finally {
			ObservableContextHolder.pop(previous);
		}

		assertEquals(1, sessionReceived.size(), "session registry must receive the event");
		assertEquals(execId, sessionReceived.get(0).executionId(),
				"event must adopt the bound session's executionId");
	}

	@Test
	void global_observer_seesEventsFromAnyLogger() {
		List<ObservableEvent> globalReceived = new ArrayList<>();
		IObserver<ObservableEvent> obs = globalReceived::add;
		Logger.global().addObserver(obs);
		try {
			Logger.getLogger("test.global." + UUID.randomUUID()).warn("seen globally");
			assertEquals(1, globalReceived.size());
		} finally {
			Logger.global().removeObserver(obs);
		}
	}

	@Test
	void isEnabled_falseWhenNoObserverAnywhere() {
		Logger log = Logger.getLogger("test.enabled.none." + UUID.randomUUID());
		assertFalse(log.isEnabled(LogEvent.Level.ERROR));
	}

	@Test
	void isEnabled_trueWhenObserverAttachedAndLevelMeetsThreshold() {
		Logger log = Logger.getLogger("test.enabled.yes." + UUID.randomUUID());
		log.addObserver(e -> {});
		assertTrue(log.isEnabled(LogEvent.Level.ERROR),
				"ERROR is always at/above the default INFO threshold and an observer is attached");
	}

	@Test
	void removeObserver_stopsReceiving() {
		Logger log = Logger.getLogger("test.remove." + UUID.randomUUID());
		List<ObservableEvent> received = new ArrayList<>();
		IObserver<ObservableEvent> obs = received::add;
		log.addObserver(obs);
		log.removeObserver(obs);

		log.error("after removal");
		assertEquals(0, received.size());
	}
}
