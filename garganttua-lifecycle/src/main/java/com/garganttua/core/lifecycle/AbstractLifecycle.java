package com.garganttua.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLifecycle implements ILifecycle {

    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean flushed = new AtomicBoolean(false);
    protected final AtomicBoolean stopped = new AtomicBoolean(false);

    protected abstract ILifecycle doInit() throws LifecycleException;
    protected abstract ILifecycle doStart() throws LifecycleException;
    protected abstract ILifecycle doFlush() throws LifecycleException;
    protected abstract ILifecycle doStop() throws LifecycleException;

    protected final Object lifecycleMutex = new Object();

    protected <T extends Exception> void wrapLifecycle(RunnableWithException runnable, Class<T> exceptionType) throws T {
        log.atTrace().log("Entering wrapLifecycle with exceptionType={}", exceptionType.getSimpleName());
        try {
            runnable.run();
            log.atDebug().log("Lifecycle wrapped execution successful");
        } catch (LifecycleException e) {
            log.atError().log("LifecycleException caught in wrapLifecycle", e);
            throw ObjectReflectionHelper.instanciateNewObject(exceptionType, e);
        }
        log.atTrace().log("Exiting wrapLifecycle");
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws LifecycleException;
    }

    @Override
    public ILifecycle onInit() throws LifecycleException {
        log.atTrace().log("Entering onInit()");
        synchronized (this.lifecycleMutex) {
            ensureNotInitialized();
            log.atInfo().log("Initializing lifecycle");
            doInit();
            initialized.set(true);
            stopped.set(false);
            log.atInfo().log("Lifecycle initialized successfully");
        }
        log.atTrace().log("Exiting onInit()");
        return this;
    }

    @Override
    public ILifecycle onStart() throws LifecycleException {
        log.atTrace().log("Entering onStart()");
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            ensureNotStarted();
            log.atInfo().log("Starting lifecycle");
            doStart();
            started.set(true);
            log.atInfo().log("Lifecycle started successfully");
        }
        log.atTrace().log("Exiting onStart()");
        return this;
    }

    @Override
    public ILifecycle onFlush() throws LifecycleException {
        log.atTrace().log("Entering onFlush()");
        synchronized (this.lifecycleMutex) {
            ensureStopped();
            log.atInfo().log("Flushing lifecycle");
            doFlush();
            flushed.set(true);
            log.atInfo().log("Lifecycle flushed successfully");
        }
        log.atTrace().log("Exiting onFlush()");
        return this;
    }

    @Override
    public ILifecycle onStop() throws LifecycleException {
        log.atTrace().log("Entering onStop()");
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            if (!started.get()) {
                log.atDebug().log("Lifecycle not started, skipping onStop");
                return this;
            }
            log.atInfo().log("Stopping lifecycle");
            doStop();
            started.set(false);
            stopped.set(true);
            log.atInfo().log("Lifecycle stopped successfully");
        }
        log.atTrace().log("Exiting onStop()");
        return this;
    }

    @Override
    public ILifecycle onReload() throws LifecycleException {
        log.atTrace().log("Entering onReload()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get() && !started.get()) {
                log.atError().log("Cannot reload: lifecycle not initialized or started");
                throw new LifecycleException("Lifecycle not initialized or started");
            }

            log.atInfo().log("Reloading lifecycle: stopping");
            this.onStop();
            log.atInfo().log("Reloading lifecycle: flushing");
            this.onFlush();
            initialized.set(false);
            log.atInfo().log("Reloading lifecycle: re-initializing");
            this.onInit();
            log.atInfo().log("Reloading lifecycle: starting");
            this.onStart();
            log.atInfo().log("Lifecycle reloaded successfully");
        }
        log.atTrace().log("Exiting onReload()");
        return this;
    }

    protected void ensureInitializedAndStarted() throws LifecycleException {
        log.atTrace().log("Entering ensureInitializedAndStarted()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                log.atError().log("Lifecycle not initialized");
                throw new LifecycleException("Lifecycle not initialized");
            }
            if (!started.get()) {
                log.atError().log("Lifecycle not started");
                throw new LifecycleException("Lifecycle not started");
            }
        }
        log.atTrace().log("Exiting ensureInitializedAndStarted()");
    }

    protected void ensureInitialized() throws LifecycleException {
        log.atTrace().log("Entering ensureInitialized()");
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                log.atError().log("Lifecycle not initialized");
                throw new LifecycleException("Lifecycle not initialized");
            }
        }
        log.atTrace().log("Exiting ensureInitialized()");
    }

    protected void ensureNotInitialized() throws LifecycleException {
        log.atTrace().log("Entering ensureNotInitialized()");
        synchronized (this.lifecycleMutex) {
            if (initialized.get()) {
                log.atError().log("Lifecycle already initialized");
                throw new LifecycleException("Lifecycle already initialized");
            }
        }
        log.atTrace().log("Exiting ensureNotInitialized()");
    }

    protected void ensureStarted() throws LifecycleException {
        log.atTrace().log("Entering ensureStarted()");
        synchronized (this.lifecycleMutex) {
            if (!started.get()) {
                log.atError().log("Lifecycle not started");
                throw new LifecycleException("Lifecycle not started");
            }
        }
        log.atTrace().log("Exiting ensureStarted()");
    }

    protected void ensureNotStarted() throws LifecycleException {
        log.atTrace().log("Entering ensureNotStarted()");
        synchronized (this.lifecycleMutex) {
            if (started.get()) {
                log.atError().log("Lifecycle already started");
                throw new LifecycleException("Lifecycle already started");
            }
        }
        log.atTrace().log("Exiting ensureNotStarted()");
    }

    protected void ensureNotStopped() throws LifecycleException {
        log.atTrace().log("Entering ensureNotStopped()");
        synchronized (this.lifecycleMutex) {
            if (stopped.get()) {
                log.atError().log("Lifecycle already stopped");
                throw new LifecycleException("Lifecycle already stopped");
            }
        }
        log.atTrace().log("Exiting ensureNotStopped()");
    }

    protected void ensureFlushed() throws LifecycleException {
        log.atTrace().log("Entering ensureFlushed()");
        synchronized (this.lifecycleMutex) {
            if (!flushed.get()) {
                log.atError().log("Lifecycle not flushed");
                throw new LifecycleException("Lifecycle not flushed");
            }
        }
        log.atTrace().log("Exiting ensureFlushed()");
    }

    protected void ensureStopped() throws LifecycleException {
        log.atTrace().log("Entering ensureStopped()");
        synchronized (this.lifecycleMutex) {
            if (!stopped.get()) {
                log.atError().log("Lifecycle not stopped");
                throw new LifecycleException("Lifecycle not stopped");
            }
        }
        log.atTrace().log("Exiting ensureStopped()");
    }

    protected void ensureNotFlushed() throws LifecycleException {
        log.atTrace().log("Entering ensureNotFlushed()");
        synchronized (this.lifecycleMutex) {
            if (flushed.get()) {
                log.atError().log("Lifecycle already flushed");
                throw new LifecycleException("Lifecycle flushed");
            }
        }
        log.atTrace().log("Exiting ensureNotFlushed()");
    }

    public boolean isInitialized() {
        synchronized (this.lifecycleMutex) {
            log.atDebug().log("Checking isInitialized(): {}", initialized.get());
            return initialized.get();
        }
    }

    public boolean isStarted() {
        synchronized (this.lifecycleMutex) {
            log.atDebug().log("Checking isStarted(): {}", started.get());
            return started.get();
        }
    }

    public boolean isFlushed() {
        synchronized (this.lifecycleMutex) {
            log.atDebug().log("Checking isFlushed(): {}", flushed.get());
            return flushed.get();
        }
    }

    public boolean isStopped() {
        synchronized (this.lifecycleMutex) {
            log.atDebug().log("Checking isStopped(): {}", stopped.get());
            return stopped.get();
        }
    }
}