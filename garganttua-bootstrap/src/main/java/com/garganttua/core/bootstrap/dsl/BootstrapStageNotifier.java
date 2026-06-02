package com.garganttua.core.bootstrap.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import com.garganttua.core.bootstrap.dsl.IBootstrapStageListener.Stage;
import com.garganttua.core.observability.Logger;

/**
 * Holds and notifies {@link IBootstrapStageListener}s of high-level Bootstrap
 * stages (REGISTRATION / RESOLVE / CONFIGURATION / BUILD). Manual registrations
 * (via {@code add}) fire first; the rest are auto-discovered once via
 * {@link ServiceLoader}. Listener exceptions are isolated (logged, never rethrown).
 *
 * <p>Extracted from {@link Bootstrap} to keep that class focused on orchestration.
 */
final class BootstrapStageNotifier {

    private static final Logger log = Logger.getLogger(BootstrapStageNotifier.class);

    private final List<IBootstrapStageListener> stageListeners = new ArrayList<>();
    private boolean loadedFromSpi = false;

    void add(IBootstrapStageListener listener) {
        if (listener != null) {
            this.stageListeners.add(listener);
        }
    }

    void ensureLoaded() {
        if (this.loadedFromSpi) {
            return;
        }
        this.loadedFromSpi = true;
        try {
            for (IBootstrapStageListener spi : ServiceLoader.load(IBootstrapStageListener.class)) {
                this.stageListeners.add(spi);
                log.debug("SPI: registered Bootstrap stage listener: {}", spi.getClass().getName());
            }
        } catch (Throwable t) {
            log.warn("Failed to load Bootstrap stage listeners via SPI: {}", t.getMessage());
        }
    }

    void fireStageStart(Stage stage) {
        ensureLoaded();
        for (IBootstrapStageListener l : this.stageListeners) {
            try {
                l.onStageStart(stage);
            } catch (RuntimeException e) {
                log.warn("Stage listener {} failed at start of {}: {}",
                        l.getClass().getName(), stage, e.getMessage());
            }
        }
    }

    void fireStageEnd(Stage stage) {
        for (IBootstrapStageListener l : this.stageListeners) {
            try {
                l.onStageEnd(stage);
            } catch (RuntimeException e) {
                log.warn("Stage listener {} failed at end of {}: {}",
                        l.getClass().getName(), stage, e.getMessage());
            }
        }
    }

    void fireStageError(Stage stage, Throwable error) {
        for (IBootstrapStageListener l : this.stageListeners) {
            try {
                l.onStageError(stage, error);
            } catch (RuntimeException e) {
                log.warn("Stage listener {} failed at error of {}: {}",
                        l.getClass().getName(), stage, e.getMessage());
            }
        }
    }
}
