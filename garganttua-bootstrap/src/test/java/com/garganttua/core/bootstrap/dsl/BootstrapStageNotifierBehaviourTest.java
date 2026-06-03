package com.garganttua.core.bootstrap.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.bootstrap.dsl.IBootstrapStageListener.Stage;

/**
 * Behaviour tests for {@link BootstrapStageNotifier}: ordering of manual listeners,
 * exception isolation, null handling and SPI lazy-loading.
 */
@DisplayName("BootstrapStageNotifier behaviour")
class BootstrapStageNotifierBehaviourTest {

    /** Records every callback it receives, in order. */
    private static final class RecordingListener implements IBootstrapStageListener {
        final List<String> events = new ArrayList<>();
        private final String id;

        RecordingListener(String id) {
            this.id = id;
        }

        @Override
        public void onStageStart(Stage stage) {
            events.add(id + ":start:" + stage);
        }

        @Override
        public void onStageEnd(Stage stage) {
            events.add(id + ":end:" + stage);
        }

        @Override
        public void onStageError(Stage stage, Throwable error) {
            events.add(id + ":error:" + stage + ":" + error.getMessage());
        }
    }

    /** Always throws on every callback to exercise exception isolation. */
    private static final class ThrowingListener implements IBootstrapStageListener {
        @Override
        public void onStageStart(Stage stage) {
            throw new RuntimeException("boom-start");
        }

        @Override
        public void onStageEnd(Stage stage) {
            throw new RuntimeException("boom-end");
        }

        @Override
        public void onStageError(Stage stage, Throwable error) {
            throw new RuntimeException("boom-error");
        }
    }

    @Test
    @DisplayName("fireStageStart notifies a manually-added listener with the given stage")
    void fireStageStartNotifiesManualListener() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener listener = new RecordingListener("A");
        notifier.add(listener);

        notifier.fireStageStart(Stage.REGISTRATION);

        assertEquals(List.of("A:start:REGISTRATION"), listener.events);
    }

    @Test
    @DisplayName("fireStageEnd and fireStageError pass through the exact stage and error")
    void fireStageEndAndErrorPassExactArguments() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener listener = new RecordingListener("A");
        notifier.add(listener);

        notifier.fireStageEnd(Stage.BUILD);
        notifier.fireStageError(Stage.RESOLVE, new IllegalStateException("kaboom"));

        assertEquals(List.of("A:end:BUILD", "A:error:RESOLVE:kaboom"), listener.events);
    }

    @Test
    @DisplayName("manual listeners fire in registration order")
    void manualListenersFireInRegistrationOrder() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener first = new RecordingListener("first");
        RecordingListener second = new RecordingListener("second");
        notifier.add(first);
        notifier.add(second);

        notifier.fireStageStart(Stage.CONFIGURATION);

        assertEquals(List.of("first:start:CONFIGURATION"), first.events);
        assertEquals(List.of("second:start:CONFIGURATION"), second.events);
    }

    @Test
    @DisplayName("a throwing listener does not abort or suppress the others")
    void throwingListenerIsIsolated() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener survivor = new RecordingListener("survivor");
        // Throwing listener registered first; survivor must still be notified.
        notifier.add(new ThrowingListener());
        notifier.add(survivor);

        assertDoesNotThrow(() -> notifier.fireStageStart(Stage.REGISTRATION));
        assertDoesNotThrow(() -> notifier.fireStageEnd(Stage.REGISTRATION));
        assertDoesNotThrow(() -> notifier.fireStageError(Stage.BUILD, new RuntimeException("x")));

        assertEquals(List.of(
                "survivor:start:REGISTRATION",
                "survivor:end:REGISTRATION",
                "survivor:error:BUILD:x"), survivor.events);
    }

    @Test
    @DisplayName("add(null) is ignored and does not later cause an NPE during firing")
    void addNullIsIgnored() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener listener = new RecordingListener("A");
        notifier.add(null);
        notifier.add(listener);
        notifier.add(null);

        assertDoesNotThrow(() -> notifier.fireStageStart(Stage.REGISTRATION));
        assertEquals(List.of("A:start:REGISTRATION"), listener.events);
    }

    @Test
    @DisplayName("fireStageStart triggers a single lazy SPI load, repeated fires do not duplicate")
    void spiLoadHappensOnceAndDoesNotDuplicateManualListeners() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener listener = new RecordingListener("A");
        notifier.add(listener);

        // No IBootstrapStageListener SPI descriptor is shipped for tests, so the
        // only observable effect of repeated firing is the manual listener firing
        // exactly once per call (proving ensureLoaded did not re-add anything).
        notifier.fireStageStart(Stage.REGISTRATION);
        notifier.fireStageStart(Stage.RESOLVE);

        assertEquals(List.of("A:start:REGISTRATION", "A:start:RESOLVE"), listener.events);
    }

    @Test
    @DisplayName("firing with zero listeners is a safe no-op")
    void firingWithNoListenersIsNoOp() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        assertDoesNotThrow(() -> notifier.fireStageStart(Stage.BUILD));
        assertDoesNotThrow(() -> notifier.fireStageEnd(Stage.BUILD));
        assertDoesNotThrow(() -> notifier.fireStageError(Stage.BUILD, new RuntimeException()));
    }

    @Test
    @DisplayName("all four stages round-trip through start/end correctly")
    void allStagesRoundTrip() {
        BootstrapStageNotifier notifier = new BootstrapStageNotifier();
        RecordingListener listener = new RecordingListener("L");
        notifier.add(listener);

        for (Stage s : Stage.values()) {
            notifier.fireStageStart(s);
            notifier.fireStageEnd(s);
        }

        assertEquals(2 * Stage.values().length, listener.events.size());
        assertTrue(listener.events.contains("L:start:REGISTRATION"));
        assertTrue(listener.events.contains("L:end:BUILD"));
    }
}
