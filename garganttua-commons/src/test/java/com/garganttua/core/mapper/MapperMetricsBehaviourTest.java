package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MapperMetricsBehaviourTest {

    @Test
    void recordMappingAccumulatesCountTimeAndRules() {
        MapperMetrics m = new MapperMetrics();
        m.recordMapping(100, 3);
        m.recordMapping(50, 2);
        assertEquals(2, m.getTotalMappings());
        assertEquals(5, m.getTotalRulesExecuted());
        assertEquals(150, m.getTotalMappingTimeNanos());
        assertEquals(0, m.getFailedMappings());
    }

    @Test
    void recordFailureIncrementsOnlyFailureCounter() {
        MapperMetrics m = new MapperMetrics();
        m.recordFailure();
        m.recordFailure();
        assertEquals(2, m.getFailedMappings());
        assertEquals(0, m.getTotalMappings());
    }

    @Test
    void resetClearsEverything() {
        MapperMetrics m = new MapperMetrics();
        m.recordMapping(10, 1);
        m.recordFailure();
        m.reset();
        assertEquals(0, m.getTotalMappings());
        assertEquals(0, m.getFailedMappings());
        assertEquals(0, m.getTotalRulesExecuted());
        assertEquals(0, m.getTotalMappingTimeNanos());
    }

    @Test
    void concurrentRecordingIsThreadSafe() throws InterruptedException {
        MapperMetrics m = new MapperMetrics();
        int threads = 8;
        int perThread = 1000;
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    m.recordMapping(1, 1);
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        assertEquals((long) threads * perThread, m.getTotalMappings());
        assertEquals((long) threads * perThread, m.getTotalRulesExecuted());
        assertEquals((long) threads * perThread, m.getTotalMappingTimeNanos());
    }
}
