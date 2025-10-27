package com.garganttua.injection;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.injection.spec.ILifecycle;

public abstract class AbstractLifecycle implements ILifecycle {

    // --- États internes du cycle de vie ---
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // --- Méthodes à implémenter par les sous-classes ---
    protected abstract ILifecycle doInit() throws DiException;

    protected abstract ILifecycle doStart() throws DiException;

    protected abstract ILifecycle doFlush() throws DiException;

    protected abstract ILifecycle doStop() throws DiException;

    // --- Méthodes publiques du cycle de vie ---
    @Override
    public synchronized ILifecycle onInit() throws DiException {
        ensureNotInitialized();
        doInit();
        initialized.set(true);
        stopped.set(false);
        return this;
    }

    @Override
    public synchronized ILifecycle onStart() throws DiException {
        ensureInitialized();
        ensureNotStarted();
        doStart();
        started.set(true);
        return this;
    }

    @Override
    public synchronized ILifecycle onFlush() throws DiException {
        ensureStopped();
        doFlush();
        flushed.set(true);
        return this;
    }

    @Override
    public synchronized ILifecycle onStop() throws DiException {
        ensureInitialized();
        if (!started.get()) {
            // déjà arrêté
            return this;
        }
        doStop();
        started.set(false);
        stopped.set(true);
        return this;
    }

    @Override
    public synchronized ILifecycle onReload() throws DiException {
        if (!initialized.get() && !started.get()) {
            throw new DiException("Lifecycle not initialized or started");
        }

        this.onStop();  // stop
        this.onFlush(); // flush
        this.initialized.set(false); // reset init flag
        this.onInit();  // re-init
        this.onStart(); // re-start

        return this;
    }

    // --- Vérifications d’état ---
    protected void ensureInitializedAndStarted() throws DiException {
        if (!initialized.get()) {
            throw new DiException("Lifecycle not initialized");
        }
        if (!started.get()) {
            throw new DiException("Lifecycle not started");
        }
    }

    protected void ensureInitialized() throws DiException {
        if (!initialized.get()) {
            throw new DiException("Lifecycle not initialized");
        }
    }

    protected void ensureNotInitialized() throws DiException {
        if (initialized.get()) {
            throw new DiException("Lifecycle already initialized");
        }
    }

    protected void ensureStarted() throws DiException {
        if (!started.get()) {
            throw new DiException("Lifecycle not started");
        }
    }

    protected void ensureNotStarted() throws DiException {
        if (started.get()) {
            throw new DiException("Lifecycle already started");
        }
    }

    protected void ensureNotStopped() throws DiException {
        if (stopped.get()) {
            throw new DiException("Lifecycle already stopped");
        }
    }

    protected void ensureFlushed() throws DiException {
        if (!flushed.get()) {
            throw new DiException("Lifecycle not flushed");
        }
    }

    protected void ensureStopped() throws DiException {
        if (!stopped.get()) {
            throw new DiException("Lifecycle not flushed");
        }
    }

    // --- Getters d’état (utile pour les tests ou les diagnostics) ---
    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isStarted() {
        return started.get();
    }

    public boolean isFlushed() {
        return flushed.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
