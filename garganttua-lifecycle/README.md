# Garganttua Lifecycle

## Description

The **garganttua-lifecycle** module provides a robust and thread-safe abstract implementation for component lifecycle management.
It defines a consistent state machine with the following phases:

 - **init** → initialization
 - **start** → component activation
 - **stop** → component shutdown
 - **flush** → cleanup between reload cycles
 - **reload** → full cycle: stop → flush → init → start

This module offers:
 - Thread-safe lifecycle transitions
 - Automatic state checking and enforcement
 - A clean abstraction for subclasses to implement (doInit, doStart, doFlush, doStop)
 - Useful guards (ensureInitialized, ensureNotStarted, etc.)
 - Seamless error wrapping with automatic exception type instantiation

It is meant to be extended by any component requiring predictable, consistent, and resilient lifecycle behavior

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-lifecycle</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-reflection`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### AbstractLifecycle

AbstractLifecycle is the core base class. It provides:
 - A mutex securing all lifecycle operations
 - Four atomic states:
    - initialized
    - started
    - flushed
    - stopped
 - Template lifecycle methods to override:
    - doInit()
    - doStart()
    - doFlush()
    - doStop()
 - State validation helpers:
    - ensureInitialized()
    - ensureNotStarted()
    - ensureStopped()
    - ensureFlushed()

State guards keep lifecycle transitions safe and prevent illegal operations such as starting twice, flushing before stopping, or re-init without stop.

### Lifecycle flow

The expected flow is:
 - onInit()
 - onStart()
 - onStop()
 - onFlush()
Optional:
 - onReload() → stop → flush → init → start

### Exception Wrapping

The method:
```java
wrapLifecycle(RunnableWithException runnable, Class<T> exceptionType)
```
allows components to automatically wrap lifecycle exceptions into a caller-specified exception type, using reflection.

## Usage

### Extending the lifecycle

A typical usage looks like:
```java
public class MyComponent extends AbstractLifecycle {

    @Override
    protected ILifecycle doInit() {
        // allocate resources
        return this;
    }

    @Override
    protected ILifecycle doStart() {
        // open connections or start threads
        return this;
    }

    @Override
    protected ILifecycle doFlush() {
        // flush internal caches for reload
        return this;
    }

    @Override
    protected ILifecycle doStop() {
        // cleanup resources
        return this;
    }
}
```
### Running the lifecycle
```java
MyComponent c = new MyComponent();

c.onInit();
c.onStart();

// component running

c.onStop();
c.onFlush(); // optional

// full reload
c.onReload();
```

### State inspection
```java
c.isInitialized();
c.isStarted();
c.isFlushed();
c.isStopped();
```
These methods are thread-safe and protected by the same internal mutex.

### Exception wrapping

Add the code below inside your **Component** to encapsulate throwned exception into your own custom exception.

```java
wrapLifecycle(this::onInit, MyCustomException.class);
```

## Tips and best practices
- **Favor idempotent lifecycle methods**
  Implement `doInit`, `doStart`, `doFlush` and `doStop` so they can be safely re-run (or detect and no-op on repeated calls). This reduces surprises during reloads and in error recovery.

- **Keep `do*` methods lightweight**
  Delegate heavy work (long-running tasks, blocking I/O) to dedicated worker threads started in `doStart()` and stopped in `doStop()`. Lifecycle methods should primarily coordinate, not perform extended processing.

- **Handle exceptions explicitly and use `wrapLifecycle` when appropriate**
  Wrap any checked or domain-specific exceptions so callers get consistent exception types. Use `wrapLifecycle` to convert `LifecycleException` into higher-level exceptions when exposing lifecycle APIs.

- **Respect the lifecycle mutex**
  If you access shared mutable state from outside `do*` methods, synchronize on the same `lifecycleMutex` or ensure thread-safety to avoid races with lifecycle transitions.

- **Be conservative with `onFlush()` logic**
  `onFlush()` is intended for lightweight cleanup between reload cycles (e.g., clearing caches). Avoid releasing long-lived resources there unless they are re-acquirable in `onInit()`.

- **Make state transitions observable and testable**
  Use the provided `isInitialized()`, `isStarted()`, `isFlushed()` and `isStopped()` methods in unit tests to assert correct transitions. Write tests for illegal transitions (e.g., start twice, flush before stop) to ensure guards behave as expected.

- **Log at the right level**
  Use `trace` for entry/exit of lifecycle methods, `debug` for state checks, `info` for major transitions (init/start/stop/reload) and `error` only for unexpected failures. This helps operators diagnose issues without noisy logs.

- **Fail fast on invalid transitions**
  Throw `LifecycleException` on invalid/illegal transitions (the base class does this). This protects the system from entering inconsistent states.

- **Design for reloads**
  Ensure `onReload()` results in a deterministic state (stop → flush → init → start). Avoid external side-effects in `doFlush()` that cannot be safely repeated during reload.

- **Document side effects**
  In your subclass Javadoc, document any external resources created, required order of operations, and whether initialization/start are blocking or asynchronous.

- **Provide graceful shutdown hooks**
  If your component owns threads or external connections, ensure `doStop()` waits for a bounded time for graceful termination, then force-close as a last resort to prevent hangs during system shutdown.

## License
This module is distributed under the MIT License.