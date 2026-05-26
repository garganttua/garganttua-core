package com.garganttua.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

class ObservableRegistryTest {

	@Test
	void addObserver_thenFire_dispatchesEvent() {
		ObservableRegistry reg = new ObservableRegistry();
		List<ObservableEvent> received = new ArrayList<>();
		reg.addObserver(received::add);

		StartEvent e = new StartEvent(UUID.randomUUID(), Instant.now(), "test");
		reg.fire(e);

		assertEquals(1, received.size());
		assertEquals(e, received.get(0));
	}

	@Test
	void removeObserver_stopsReceiving() {
		ObservableRegistry reg = new ObservableRegistry();
		List<ObservableEvent> received = new ArrayList<>();
		IObserver<ObservableEvent> obs = received::add;
		reg.addObserver(obs);
		reg.removeObserver(obs);

		reg.fire(new StartEvent(UUID.randomUUID(), Instant.now(), "test"));
		assertEquals(0, received.size());
	}

	@Test
	void noObservers_hasObserversReturnsFalse() {
		ObservableRegistry reg = new ObservableRegistry();
		assertFalse(reg.hasObservers());
		assertEquals(0, reg.size());
	}

	@Test
	void emptyRegistry_fireIsNoop() {
		ObservableRegistry reg = new ObservableRegistry();
		reg.fire(new StartEvent(UUID.randomUUID(), Instant.now(), "test"));
		// no exception
	}

	@Test
	void observerThrowing_doesNotBreakOthers() {
		ObservableRegistry reg = new ObservableRegistry();
		List<ObservableEvent> survivor = new ArrayList<>();
		reg.addObserver(e -> {
			throw new RuntimeException("boom");
		});
		reg.addObserver(survivor::add);

		StartEvent e = new StartEvent(UUID.randomUUID(), Instant.now(), "test");
		reg.fire(e);
		assertEquals(1, survivor.size(), "downstream observers must still receive the event");
	}

	@Test
	void multipleObservers_allReceiveEvent() {
		ObservableRegistry reg = new ObservableRegistry();
		List<ObservableEvent> a = new CopyOnWriteArrayList<>();
		List<ObservableEvent> b = new CopyOnWriteArrayList<>();
		reg.addObserver(a::add);
		reg.addObserver(b::add);

		reg.fire(new StartEvent(UUID.randomUUID(), Instant.now(), "x"));
		assertEquals(1, a.size());
		assertEquals(1, b.size());
		assertEquals(2, reg.size());
	}

	@Test
	void hasObservers_trueAfterAdd_falseAfterRemove() {
		ObservableRegistry reg = new ObservableRegistry();
		IObserver<ObservableEvent> obs = e -> {};
		reg.addObserver(obs);
		assertTrue(reg.hasObservers());
		reg.removeObserver(obs);
		assertFalse(reg.hasObservers());
	}
}
