package com.garganttua.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifecycle implements ILifecycle {

    // --- États internes du cycle de vie ---
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean flushed = new AtomicBoolean(false);
    protected final AtomicBoolean stopped = new AtomicBoolean(false);

    // --- Méthodes à implémenter par les sous-classes ---
    protected abstract ILifecycle doInit() throws LifecycleException;

    protected abstract ILifecycle doStart() throws LifecycleException;

    protected abstract ILifecycle doFlush() throws LifecycleException;

    protected abstract ILifecycle doStop() throws LifecycleException;

    // --- Méthodes publiques du cycle de vie ---
    @Override
    public synchronized ILifecycle onInit() throws LifecycleException {
        ensureNotInitialized();
        doInit();
        initialized.set(true);
        stopped.set(false);
        return this;
    }

    @Override
    public synchronized ILifecycle onStart() throws LifecycleException {
        ensureInitialized();
        ensureNotStarted();
        doStart();
        started.set(true);
        return this;
    }

    @Override
    public synchronized ILifecycle onFlush() throws LifecycleException {
        ensureStopped();
        doFlush();
        flushed.set(true);
        return this;
    }

    @Override
    public synchronized ILifecycle onStop() throws LifecycleException {
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
    public synchronized ILifecycle onReload() throws LifecycleException {
        if (!initialized.get() && !started.get()) {
            throw new LifecycleException("Lifecycle not initialized or started");
        }

        this.onStop();  // stop
        this.onFlush(); // flush
        this.initialized.set(false); // reset init flag
        this.onInit();  // re-init
        this.onStart(); // re-start

        return this;
    }

    // --- Vérifications d’état ---
    protected void ensureInitializedAndStarted() throws LifecycleException {
        if (!initialized.get()) {
            throw new LifecycleException("Lifecycle not initialized");
        }
        if (!started.get()) {
            throw new LifecycleException("Lifecycle not started");
        }
    }

    protected void ensureInitialized() throws LifecycleException {
        if (!initialized.get()) {
            throw new LifecycleException("Lifecycle not initialized");
        }
    }

    protected void ensureNotInitialized() throws LifecycleException {
        if (initialized.get()) {
            throw new LifecycleException("Lifecycle already initialized");
        }
    }

    protected void ensureStarted() throws LifecycleException {
        if (!started.get()) {
            throw new LifecycleException("Lifecycle not started");
        }
    }

    protected void ensureNotStarted() throws LifecycleException {
        if (started.get()) {
            throw new LifecycleException("Lifecycle already started");
        }
    }

    protected void ensureNotStopped() throws LifecycleException {
        if (stopped.get()) {
            throw new LifecycleException("Lifecycle already stopped");
        }
    }

    protected void ensureFlushed() throws LifecycleException {
        if (!flushed.get()) {
            throw new LifecycleException("Lifecycle not flushed");
        }
    }

    protected void ensureStopped() throws LifecycleException {
        if (!stopped.get()) {
            throw new LifecycleException("Lifecycle not flushed");
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
