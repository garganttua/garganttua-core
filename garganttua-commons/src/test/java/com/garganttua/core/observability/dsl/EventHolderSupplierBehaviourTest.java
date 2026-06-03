package com.garganttua.core.observability.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.StartEvent;

class EventHolderSupplierBehaviourTest {

    @Test
    void supplyEmptyWhenNoEventSet() throws Exception {
        EventHolderSupplier s = new EventHolderSupplier();
        assertTrue(s.supply().isEmpty());
    }

    @Test
    void supplyReturnsCurrentEventAfterSet() throws Exception {
        EventHolderSupplier s = new EventHolderSupplier();
        ObservableEvent e = new StartEvent(UUID.randomUUID(), Instant.now(), "src", null);
        s.setCurrent(e);
        assertSame(e, s.supply().get());
    }

    @Test
    void setCurrentOverwrites() throws Exception {
        EventHolderSupplier s = new EventHolderSupplier();
        ObservableEvent first = new StartEvent(UUID.randomUUID(), Instant.ofEpochMilli(1), "a", null);
        ObservableEvent second = new StartEvent(UUID.randomUUID(), Instant.ofEpochMilli(2), "b", null);
        s.setCurrent(first);
        s.setCurrent(second);
        assertSame(second, s.supply().get());
    }

    @Test
    void suppliedTypeIsObservableEvent() {
        EventHolderSupplier s = new EventHolderSupplier();
        assertEquals(ObservableEvent.class, s.getSuppliedType());
    }
}
