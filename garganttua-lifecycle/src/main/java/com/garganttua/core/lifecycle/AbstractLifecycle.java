package com.garganttua.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionUser;

public abstract class AbstractLifecycle implements ILifecycle, IReflectionUser {
    private static final Logger log = Logger.getLogger(AbstractLifecycle.class);

    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean flushed = new AtomicBoolean(false);
    protected final AtomicBoolean stopped = new AtomicBoolean(false);

    protected abstract ILifecycle doInit() throws LifecycleException;

    protected abstract ILifecycle doStart() throws LifecycleException;

    protected abstract ILifecycle doFlush() throws LifecycleException;

    protected abstract ILifecycle doStop() throws LifecycleException;

    protected final Object lifecycleMutex = new Object();

    protected <T extends Exception> void wrapLifecycle(RunnableWithException runnable, IClass<T> exceptionType)
            throws T {
        log.trace("Entering wrapLifecycle with exceptionType={}", exceptionType.getSimpleName());
        try {
            runnable.run();
            log.debug("Lifecycle wrapped execution successful");
        } catch (LifecycleException e) {
            log.error("LifecycleException caught in wrapLifecycle", e);
            throw reflection().newInstance(exceptionType, e);
        }
        log.trace("Exiting wrapLifecycle");
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws LifecycleException;
    }

    @Override
    public LifecycleStatus status() {
        synchronized (this.lifecycleMutex) {

            if (!initialized.get()) {
                return LifecycleStatus.NEW;
            }

            if (started.get()) {
                return LifecycleStatus.STARTED;
            }

            if (stopped.get()) {
                if (flushed.get()) {
                    return LifecycleStatus.FLUSHED;
                }
                return LifecycleStatus.STOPPED;
            }

            return LifecycleStatus.INITIALIZED;
        }
    }

    @Override
    public ILifecycle onInit() throws LifecycleException {
        log.trace("Entering onInit()");
        synchronized (this.lifecycleMutex) {
            // Idempotent: silently no-op if already initialized. Multiple
            // owners may legitimately try to init the same object (Bootstrap
            // Phase 3 + a top-level consumer such as ApiBuilder) and we
            // should not throw on the second call.
            if (initialized.get()) {
                log.trace("Lifecycle already initialized — no-op");
                return this;
            }
            log.debug("Initializing lifecycle");
            doInit();
            initialized.set(true);
            stopped.set(false);
            log.debug("Lifecycle initialized successfully");
        }
        log.trace("Exiting onInit()");
        return this;
    }

    @Override
    public ILifecycle onStart() throws LifecycleException {
        log.trace("Entering onStart()");
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            // Idempotent: see {@link #onInit()} for the rationale.
            if (started.get()) {
                log.trace("Lifecycle already started — no-op");
                return this;
            }
            log.debug("Starting lifecycle");
            doStart();
            started.set(true);
            log.debug("Lifecycle started successfully");
        }
        log.trace("Exiting onStart()");
        return this;
    }

    @Override
    public ILifecycle onFlush() throws LifecycleException {
        log.trace("Entering onFlush()");
        synchronized (this.lifecycleMutex) {
            ensureStopped();
            log.debug("Flushing lifecycle");
            doFlush();
            flushed.set(true);
            log.debug("Lifecycle flushed successfully");
        }
        log.trace("Exiting onFlush()");
        return this;
    }

    @Override
    public ILifecycle onStop() throws LifecycleException {
        log.trace("Entering onStop()");
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            if (!started.get()) {
                log.debug("Lifecycle not started, skipping onStop");
                return this;
            }
            log.debug("Stopping lifecycle");
            doStop();
            started.set(false);
            stopped.set(true);
            log.debug("Lifecycle stopped successfully");
        }
        log.trace("Exiting onStop()");
        return this;
    }

    @Override
    public ILifecycle onReload() throws LifecycleException {
        log.trace("Entering onReload()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get() && !started.get()) {
                log.error("Cannot reload: lifecycle not initialized or started");
                throw new LifecycleException("Lifecycle not initialized or started");
            }

            log.debug("Reloading lifecycle: stopping");
            this.onStop();
            log.debug("Reloading lifecycle: flushing");
            this.onFlush();
            initialized.set(false);
            log.debug("Reloading lifecycle: re-initializing");
            this.onInit();
            log.debug("Reloading lifecycle: starting");
            this.onStart();
            log.debug("Lifecycle reloaded successfully");
        }
        log.trace("Exiting onReload()");
        return this;
    }

    protected void ensureInitializedAndStarted() throws LifecycleException {
        log.trace("Entering ensureInitializedAndStarted()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                log.error("Lifecycle not initialized");
                throw new LifecycleException("Lifecycle not initialized");
            }
            if (!started.get()) {
                log.error("Lifecycle not started");
                throw new LifecycleException("Lifecycle not started");
            }
        }
        log.trace("Exiting ensureInitializedAndStarted()");
    }

    protected void ensureInitialized() throws LifecycleException {
        log.trace("Entering ensureInitialized()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                log.error("Lifecycle not initialized");
                throw new LifecycleException("Lifecycle not initialized");
            }
        }
        log.trace("Exiting ensureInitialized()");
    }

    protected void ensureNotInitialized() throws LifecycleException {
        log.trace("Entering ensureNotInitialized()");
        synchronized (this.lifecycleMutex) {
            if (initialized.get()) {
                log.error("Lifecycle already initialized");
                throw new LifecycleException("Lifecycle already initialized");
            }
        }
        log.trace("Exiting ensureNotInitialized()");
    }

    protected void ensureStarted() throws LifecycleException {
        log.trace("Entering ensureStarted()");
        synchronized (this.lifecycleMutex) {
            if (!started.get()) {
                log.error("Lifecycle not started");
                throw new LifecycleException("Lifecycle not started");
            }
        }
        log.trace("Exiting ensureStarted()");
    }

    protected void ensureNotStarted() throws LifecycleException {
        log.trace("Entering ensureNotStarted()");
        synchronized (this.lifecycleMutex) {
            if (started.get()) {
                log.error("Lifecycle already started");
                throw new LifecycleException("Lifecycle already started");
            }
        }
        log.trace("Exiting ensureNotStarted()");
    }

    protected void ensureNotStopped() throws LifecycleException {
        log.trace("Entering ensureNotStopped()");
        synchronized (this.lifecycleMutex) {
            if (stopped.get()) {
                log.error("Lifecycle already stopped");
                throw new LifecycleException("Lifecycle already stopped");
            }
        }
        log.trace("Exiting ensureNotStopped()");
    }

    protected void ensureFlushed() throws LifecycleException {
        log.trace("Entering ensureFlushed()");
        synchronized (this.lifecycleMutex) {
            if (!flushed.get()) {
                log.error("Lifecycle not flushed");
                throw new LifecycleException("Lifecycle not flushed");
            }
        }
        log.trace("Exiting ensureFlushed()");
    }

    protected void ensureStopped() throws LifecycleException {
        log.trace("Entering ensureStopped()");
        synchronized (this.lifecycleMutex) {
            if (!stopped.get()) {
                log.error("Lifecycle not stopped");
                throw new LifecycleException("Lifecycle not stopped");
            }
        }
        log.trace("Exiting ensureStopped()");
    }

    protected void ensureNotFlushed() throws LifecycleException {
        log.trace("Entering ensureNotFlushed()");
        synchronized (this.lifecycleMutex) {
            if (flushed.get()) {
                log.error("Lifecycle already flushed");
                throw new LifecycleException("Lifecycle flushed");
            }
        }
        log.trace("Exiting ensureNotFlushed()");
    }

    public boolean isInitialized() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isInitialized(): {}", initialized.get());
            return initialized.get();
        }
    }

    public boolean isStarted() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isStarted(): {}", started.get());
            return started.get();
        }
    }

    public boolean isFlushed() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isFlushed(): {}", flushed.get());
            return flushed.get();
        }
    }

    public boolean isStopped() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isStopped(): {}", stopped.get());
            return stopped.get();
        }
    }
}