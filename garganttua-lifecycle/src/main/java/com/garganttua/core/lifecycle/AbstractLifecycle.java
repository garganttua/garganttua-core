package com.garganttua.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

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

    protected <T extends Exception> void  wrapLifecycle(RunnableWithException runnable, Class<T> exceptionType) throws T {
        try {
            runnable.run();
        } catch (LifecycleException e) {
            throw ObjectReflectionHelper.instanciateNewObject(exceptionType, e);
        }
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws LifecycleException;
    }

    @Override
    public ILifecycle onInit() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            ensureNotInitialized();
            doInit();
            initialized.set(true);
            stopped.set(false);
        }
        return this;
    }

    @Override
    public ILifecycle onStart() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            ensureNotStarted();
            doStart();
            started.set(true);
        }
        return this;
    }

    @Override
    public ILifecycle onFlush() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            ensureStopped();
            doFlush();
            flushed.set(true);
        }
        return this;
    }

    @Override
    public ILifecycle onStop() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            ensureInitialized();
            if (!started.get()) {
                return this;
            }
            doStop();
            started.set(false);
            stopped.set(true);
        }
        return this;
    }

    @Override
    public ILifecycle onReload() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (!initialized.get() && !started.get()) {
                throw new LifecycleException("Lifecycle not initialized or started");
            }

            this.onStop();
            this.onFlush();
            this.initialized.set(false);
            this.onInit();
            this.onStart();
        }
        return this;
    }

    protected void ensureInitializedAndStarted() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                throw new LifecycleException("Lifecycle not initialized");
            }
            if (!started.get()) {
                throw new LifecycleException("Lifecycle not started");
            }
        }
    }

    protected void ensureInitialized() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (!initialized.get()) {
                throw new LifecycleException("Lifecycle not initialized");
            }
        }
    }

    protected void ensureNotInitialized() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (initialized.get()) {
                throw new LifecycleException("Lifecycle already initialized");
            }
        }
    }

    protected void ensureStarted() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (!started.get()) {
                throw new LifecycleException("Lifecycle not started");
            }
        }
    }

    protected void ensureNotStarted() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (started.get()) {
                throw new LifecycleException("Lifecycle already started");
            }
        }
    }

    protected void ensureNotStopped() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (stopped.get()) {
                throw new LifecycleException("Lifecycle already stopped");
            }
        }
    }

    protected void ensureFlushed() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (flushed.get()) {
                throw new LifecycleException("Lifecycle not flushed");
            }
        }
    }

    protected void ensureStopped() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (!stopped.get()) {
                throw new LifecycleException("Lifecycle not stopped");
            }
        }
    }

    protected void ensureNotFlushed() throws LifecycleException {
        synchronized (this.lifecycleMutex) {
            if (flushed.get()) {
                throw new LifecycleException("Lifecycle flushed");
            }
        }
    }

    public boolean isInitialized() {
        synchronized (this.lifecycleMutex) {
            return initialized.get();
        }
    }

    public boolean isStarted() {
        synchronized (this.lifecycleMutex) {
            return started.get();
        }
    }

    public boolean isFlushed() {
        synchronized (this.lifecycleMutex) {
            return flushed.get();
        }
    }

    public boolean isStopped() {
        synchronized (this.lifecycleMutex) {
            return stopped.get();
        }
    }
}
