package com.garganttua.core.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractLifecycleTest {

    private static class TestLifecycle extends AbstractLifecycle {

        AtomicInteger initCount = new AtomicInteger();
        AtomicInteger startCount = new AtomicInteger();
        AtomicInteger flushCount = new AtomicInteger();
        AtomicInteger stopCount = new AtomicInteger();

        @Override
        protected ILifecycle doInit() {
            initCount.incrementAndGet();
            return this;
        }

        @Override
        protected ILifecycle doStart() {
            startCount.incrementAndGet();
            return this;
        }

        @Override
        protected ILifecycle doFlush() {
            flushCount.incrementAndGet();
            return this;
        }

        @Override
        protected ILifecycle doStop() {
            stopCount.incrementAndGet();
            return this;
        }
    }

    private TestLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = new TestLifecycle();
    }

    @Test
    void testInitAndStart() throws LifecycleException {
        lifecycle.onInit().onStart();

        assertTrue(lifecycle.isInitialized());
        assertTrue(lifecycle.isStarted());
        assertFalse(lifecycle.isStopped());

        assertEquals(1, lifecycle.initCount.get());
        assertEquals(1, lifecycle.startCount.get());
    }

    @Test
    void testFlushAfterStart() throws LifecycleException {
        assertThrows(LifecycleException.class, () -> lifecycle.onInit().onStart().onFlush());
    }

    @Test
    void testStop() throws LifecycleException {
        lifecycle.onInit().onStart().onStop();

        assertTrue(lifecycle.isStopped());
        assertFalse(lifecycle.isStarted());
        assertEquals(1, lifecycle.stopCount.get());
    }

    @Test
    void testReload() throws LifecycleException {
        lifecycle.onInit().onStart();

        lifecycle.onReload();

        assertTrue(lifecycle.isInitialized());
        assertTrue(lifecycle.isStarted());
        assertTrue(lifecycle.isFlushed());
        assertTrue(lifecycle.isStopped() == false); // redémarré

        // Compteurs : chaque phase doit être exécutée
        assertTrue(lifecycle.initCount.get() >= 2, "init doit être rappelé");
        assertTrue(lifecycle.startCount.get() >= 2, "start doit être rappelé");
        assertTrue(lifecycle.flushCount.get() >= 1, "flush doit être exécuté");
        assertTrue(lifecycle.stopCount.get() >= 1, "stop doit être exécuté");
    }

    @Test
    void testStartWithoutInit() {
        assertThrows(LifecycleException.class, () -> lifecycle.onStart());
    }

    @Test
    void testInitTwiceThrows() throws LifecycleException {
        lifecycle.onInit();
        assertThrows(LifecycleException.class, () -> lifecycle.onInit());
    }

    @Test
    void testStartTwiceThrows() throws LifecycleException {
        lifecycle.onInit().onStart();
        assertThrows(LifecycleException.class, () -> lifecycle.onStart());
    }
}
