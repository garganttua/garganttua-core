package com.garganttua.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ObservableContextHolderTest {

	@AfterEach
	void teardown() {
		ObservableContextHolder.pop(null);
	}

	@Test
	void push_onEmptyThread_returnsNull() {
		ObservableRegistry reg = new ObservableRegistry();
		ObservableContextHolder.Session previous = ObservableContextHolder.push(reg, UUID.randomUUID());

		assertNull(previous);
		assertSame(reg, ObservableContextHolder.current().registry());
	}

	@Test
	void nestedPush_pop_restoresOuter() {
		ObservableRegistry outer = new ObservableRegistry();
		ObservableRegistry inner = new ObservableRegistry();
		UUID outerId = UUID.randomUUID();
		UUID innerId = UUID.randomUUID();

		ObservableContextHolder.Session beforeOuter = ObservableContextHolder.push(outer, outerId);
		assertNull(beforeOuter);
		assertEquals(outerId, ObservableContextHolder.current().executionId());

		ObservableContextHolder.Session beforeInner = ObservableContextHolder.push(inner, innerId);
		assertEquals(outerId, beforeInner.executionId(), "push must return previous outer session");
		assertEquals(innerId, ObservableContextHolder.current().executionId());

		ObservableContextHolder.pop(beforeInner);
		assertSame(outer, ObservableContextHolder.current().registry(),
				"after inner pop, outer session must be restored");
		assertEquals(outerId, ObservableContextHolder.current().executionId());

		ObservableContextHolder.pop(beforeOuter);
		assertNull(ObservableContextHolder.current(), "after outer pop, thread must be clean");
	}

	@Test
	void pop_withNull_clearsBinding() {
		ObservableContextHolder.push(new ObservableRegistry(), UUID.randomUUID());
		ObservableContextHolder.pop(null);
		assertNull(ObservableContextHolder.current());
	}
}
