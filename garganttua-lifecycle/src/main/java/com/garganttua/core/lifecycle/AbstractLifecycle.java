package com.garganttua.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionUser;

/**
 * Thread-safe skeletal implementation of {@link ILifecycle} that drives the
 * {@code NEW -> INITIALIZED -> STARTED -> STOPPED -> FLUSHED} state machine and
 * delegates the actual work to the {@code doInit}/{@code doStart}/{@code doFlush}/
 * {@code doStop} hooks supplied by subclasses.
 *
 * <p>All transitions are guarded by {@link #lifecycleMutex} and the {@code onXxx}
 * entry points are idempotent where it is safe to be so (see {@link #onInit()}).
 * Subclasses can validate the current state via the {@code ensureXxx} helpers.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public abstract class AbstractLifecycle implements ILifecycle, IReflectionUser {
    private static final Logger log = Logger.getLogger(AbstractLifecycle.class);

    /** Whether {@link #onInit()} has completed and {@link #onStop()} has not reset it. */
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    /** Whether the component is currently started (between {@link #onStart()} and {@link #onStop()}). */
    protected final AtomicBoolean started = new AtomicBoolean(false);
    /** Whether {@link #onFlush()} has completed. */
    protected final AtomicBoolean flushed = new AtomicBoolean(false);
    /** Whether {@link #onStop()} has completed. */
    protected final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Performs the subclass-specific initialization work.
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if initialization fails
     */
    protected abstract ILifecycle doInit() throws LifecycleException;

    /**
     * Performs the subclass-specific start work.
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if starting fails
     */
    protected abstract ILifecycle doStart() throws LifecycleException;

    /**
     * Performs the subclass-specific flush work (clearing buffers/caches).
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if flushing fails
     */
    protected abstract ILifecycle doFlush() throws LifecycleException;

    /**
     * Performs the subclass-specific stop work.
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if stopping fails
     */
    protected abstract ILifecycle doStop() throws LifecycleException;

    /** Monitor guarding every state transition and query in this lifecycle. */
    protected final Object lifecycleMutex = new Object();

    /**
     * Runs the given lifecycle action and, if it raises a {@link LifecycleException},
     * rethrows it wrapped in a freshly constructed instance of {@code exceptionType}.
     *
     * @param runnable      the lifecycle action to execute
     * @param exceptionType the exception type to wrap any {@link LifecycleException} into
     * @param <T>           the wrapping exception type
     * @throws T if {@code runnable} throws a {@link LifecycleException}
     */
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

    /**
     * A {@link Runnable}-like action whose body may throw a {@link LifecycleException}.
     */
    @FunctionalInterface
    public interface RunnableWithException {
        /**
         * Executes the action.
         *
         * @throws LifecycleException if the action fails
         */
        void run() throws LifecycleException;
    }

    /**
     * Returns the current lifecycle state derived from the internal flags.
     *
     * @return the current {@link LifecycleStatus}
     */
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

    /**
     * Initializes the component by invoking {@link #doInit()}.
     *
     * <p>Idempotent: if already initialized this is a no-op and does not throw,
     * so multiple owners may safely attempt to initialize the same object.</p>
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if {@link #doInit()} fails
     */
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

    /**
     * Starts the component by invoking {@link #doStart()} after ensuring it is initialized.
     *
     * <p>Idempotent: if already started this is a no-op.</p>
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if the component is not initialized or {@link #doStart()} fails
     */
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

    /**
     * Flushes the component by invoking {@link #doFlush()} after ensuring it is stopped.
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if the component is not stopped or {@link #doFlush()} fails
     */
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

    /**
     * Stops the component by invoking {@link #doStop()} after ensuring it is initialized.
     *
     * <p>If the component was never started this is a no-op.</p>
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if the component is not initialized or {@link #doStop()} fails
     */
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

    /**
     * Reloads the component by running the full {@code stop -> flush -> init -> start} cycle.
     *
     * @return this lifecycle, for chaining
     * @throws LifecycleException if the component is neither initialized nor started, or if any
     *                            phase of the reload fails
     */
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

    /**
     * Asserts that the component is both initialized and started.
     *
     * @throws LifecycleException if the component is not initialized or not started
     */
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

    /**
     * Asserts that the component is initialized.
     *
     * @throws LifecycleException if the component is not initialized
     */
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

    /**
     * Asserts that the component is not yet initialized.
     *
     * @throws LifecycleException if the component is already initialized
     */
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

    /**
     * Asserts that the component is started.
     *
     * @throws LifecycleException if the component is not started
     */
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

    /**
     * Asserts that the component is not started.
     *
     * @throws LifecycleException if the component is already started
     */
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

    /**
     * Asserts that the component is not stopped.
     *
     * @throws LifecycleException if the component is already stopped
     */
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

    /**
     * Asserts that the component is flushed.
     *
     * @throws LifecycleException if the component is not flushed
     */
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

    /**
     * Asserts that the component is stopped.
     *
     * @throws LifecycleException if the component is not stopped
     */
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

    /**
     * Asserts that the component is not flushed.
     *
     * @throws LifecycleException if the component is already flushed
     */
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

    /**
     * Returns whether the component is currently initialized.
     *
     * @return {@code true} if initialized
     */
    public boolean isInitialized() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isInitialized(): {}", initialized.get());
            return initialized.get();
        }
    }

    /**
     * Returns whether the component is currently started.
     *
     * @return {@code true} if started
     */
    public boolean isStarted() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isStarted(): {}", started.get());
            return started.get();
        }
    }

    /**
     * Returns whether the component has been flushed.
     *
     * @return {@code true} if flushed
     */
    public boolean isFlushed() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isFlushed(): {}", flushed.get());
            return flushed.get();
        }
    }

    /**
     * Returns whether the component is currently stopped.
     *
     * @return {@code true} if stopped
     */
    public boolean isStopped() {
        synchronized (this.lifecycleMutex) {
            log.debug("Checking isStopped(): {}", stopped.get());
            return stopped.get();
        }
    }
}