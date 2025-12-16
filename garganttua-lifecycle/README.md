# ♻️ Garganttua Lifecycle

## Description

The **garganttua-lifecycle** module provides a robust, thread-safe abstract implementation for component lifecycle management in Java applications. It defines a predictable state machine that ensures components transition through well-defined phases with proper synchronization and validation.

This module is the foundation for lifecycle management across the Garganttua ecosystem, offering:

- **Thread-Safe State Machine** - Atomic transitions protected by internal mutex
- **Four Lifecycle Phases** - Init, Start, Stop, Flush with clear semantics
- **Automatic State Validation** - Built-in guards prevent illegal transitions
- **Clean Abstraction** - Simple template methods (`doInit`, `doStart`, `doFlush`, `doStop`)
- **Exception Wrapping** - Automatic conversion to domain-specific exceptions
- **Reload Support** - Full lifecycle reset with `onReload()` operation
- **Inspection API** - Thread-safe state query methods
- **SLF4J Logging** - Comprehensive trace, debug, info, and error logging

The lifecycle state machine follows this flow:

```
[NEW] -> onInit() -> [INITIALIZED] -> onStart() -> [STARTED]
                                                        |
                                                        v
[RELOADED] <- onInit() <- onFlush() <- onStop() <- [STOPPED]
```

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

`AbstractLifecycle` is the core base class providing a complete lifecycle implementation. It manages:

**State Tracking** - Four atomic boolean flags:
- `initialized` - Component has been initialized
- `started` - Component is actively running
- `stopped` - Component has been stopped
- `flushed` - Component has been flushed (cleaned up)

**Thread Safety** - All lifecycle operations are synchronized using an internal `lifecycleMutex` to prevent race conditions during concurrent access.

**Template Methods** - Subclasses implement these hooks:
```java
protected abstract ILifecycle doInit() throws LifecycleException;
protected abstract ILifecycle doStart() throws LifecycleException;
protected abstract ILifecycle doFlush() throws LifecycleException;
protected abstract ILifecycle doStop() throws LifecycleException;
```

**State Validation Helpers** - Enforce correct lifecycle transitions:
- `ensureInitialized()` - Verify component is initialized
- `ensureNotInitialized()` - Verify component is not yet initialized
- `ensureStarted()` - Verify component is started
- `ensureNotStarted()` - Verify component is not started
- `ensureStopped()` - Verify component is stopped
- `ensureNotStopped()` - Verify component is not stopped
- `ensureFlushed()` - Verify component is flushed
- `ensureNotFlushed()` - Verify component is not flushed
- `ensureInitializedAndStarted()` - Verify component is fully operational

### Lifecycle Phases

**1. Initialization (`onInit`)**
- **Purpose**: Allocate resources, establish connections, load configuration
- **When**: Called once before the component can be used
- **Preconditions**: Component must not be initialized
- **Postconditions**: Component is initialized but not yet active
- **Example Actions**: Open database connections, allocate buffers, load metadata

**2. Start (`onStart`)**
- **Purpose**: Activate the component and begin normal operations
- **When**: Called after initialization to make the component operational
- **Preconditions**: Component must be initialized but not started
- **Postconditions**: Component is fully operational
- **Example Actions**: Start background threads, begin accepting requests, activate listeners

**3. Stop (`onStop`)**
- **Purpose**: Gracefully shutdown active operations
- **When**: Called to deactivate the component
- **Preconditions**: Component must be initialized and started
- **Postconditions**: Component is stopped, active operations halted
- **Example Actions**: Stop threads, close listeners, finish pending work

**4. Flush (`onFlush`)**
- **Purpose**: Clear internal state and release temporary resources
- **When**: Called after stop, typically before reload
- **Preconditions**: Component must be stopped
- **Postconditions**: Component is flushed, ready for re-initialization
- **Example Actions**: Clear caches, release buffers, reset counters

**5. Reload (`onReload`)**
- **Purpose**: Full lifecycle reset (stop → flush → init → start)
- **When**: Configuration changes, resource pool refresh, hot reload scenarios
- **Preconditions**: Component must be initialized or started
- **Postconditions**: Component is restarted with fresh state
- **Sequence**: `onStop()` → `onFlush()` → reset init flag → `onInit()` → `onStart()`

### State Diagram

```
┌─────────┐
│   NEW   │
└────┬────┘
     │ onInit()
     v
┌──────────────┐
│ INITIALIZED  │
└────┬─────────┘
     │ onStart()
     v
┌──────────────┐      onReload()
│   STARTED    │◄──────────────┐
└────┬─────────┘               │
     │ onStop()                │
     v                         │
┌──────────────┐               │
│   STOPPED    │               │
└────┬─────────┘               │
     │ onFlush()               │
     v                         │
┌──────────────┐               │
│   FLUSHED    │───────────────┘
└──────────────┘
```

### Exception Wrapping

The `wrapLifecycle` utility automatically converts `LifecycleException` to domain-specific exceptions:

```java
protected <T extends Exception> void wrapLifecycle(
    RunnableWithException runnable,
    Class<T> exceptionType
) throws T
```

This allows components to expose lifecycle operations with custom exception types while internally using `LifecycleException`.

## Usage

All examples below are extracted from the actual working test file [AbstractLifecycleTest.java](src/test/java/com/garganttua/core/lifecycle/AbstractLifecycleTest.java).

### Basic Lifecycle Implementation (from AbstractLifecycleTest)

```java
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import java.util.concurrent.atomic.AtomicInteger;

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
```

### Init and Start (from testInitAndStart)

```java
TestLifecycle lifecycle = new TestLifecycle();
lifecycle.onInit().onStart();

assertTrue(lifecycle.isInitialized());
assertTrue(lifecycle.isStarted());
assertFalse(lifecycle.isStopped());

assertEquals(1, lifecycle.initCount.get());
assertEquals(1, lifecycle.startCount.get());
```

### Stop After Start (from testStop)

```java
lifecycle.onInit().onStart().onStop();

assertTrue(lifecycle.isStopped());
assertFalse(lifecycle.isStarted());
assertEquals(1, lifecycle.stopCount.get());
```

### Reload Sequence (from testReload)

```java
lifecycle.onInit().onStart();

lifecycle.onReload();

assertTrue(lifecycle.isInitialized());
assertTrue(lifecycle.isStarted());
assertTrue(lifecycle.isFlushed());
assertTrue(lifecycle.isStopped() == false); // restarted

// Counters: each phase must be executed
assertTrue(lifecycle.initCount.get() >= 2, "init must be recalled");
assertTrue(lifecycle.startCount.get() >= 2, "start must be recalled");
assertTrue(lifecycle.flushCount.get() >= 1, "flush must be executed");
assertTrue(lifecycle.stopCount.get() >= 1, "stop must be executed");
```

### Error Handling - Flush After Start Fails (from testFlushAfterStart)

```java
assertThrows(LifecycleException.class, () -> lifecycle.onInit().onStart().onFlush());
```

### Error Handling - Start Without Init (from testStartWithoutInit)

```java
assertThrows(LifecycleException.class, () -> lifecycle.onStart());
```

### Error Handling - Double Init (from testInitTwiceThrows)

```java
lifecycle.onInit();
assertThrows(LifecycleException.class, () -> lifecycle.onInit());
```

### Error Handling - Double Start (from testStartTwiceThrows)

```java
lifecycle.onInit().onStart();
assertThrows(LifecycleException.class, () -> lifecycle.onStart());
```

## Advanced Usage Patterns

### Reload Support

```java
public class ConfigurableService extends AbstractLifecycle {

    private Configuration config;
    private ServiceClient client;

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        // Load configuration
        this.config = Configuration.load("service.properties");
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Create client with loaded config
        this.client = new ServiceClient(config);
        this.client.connect();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        // Disconnect client
        this.client.disconnect();
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        // Release old configuration
        this.config = null;
        this.client = null;
        return this;
    }

    // Reload when configuration changes
    public void reloadConfiguration() throws LifecycleException {
        // Automatic: stop -> flush -> init -> start
        this.onReload();
        // Service now running with new configuration
    }
}

// Usage
ConfigurableService service = new ConfigurableService();
service.onInit().onStart();

// ... service running ...

// Configuration file changed externally
service.reloadConfiguration(); // Seamless reload

// Service continues with new config
```

### Exception Wrapping

```java
public class CustomServiceException extends Exception {
    public CustomServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class ExternalService extends AbstractLifecycle {

    private ApiClient client;

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        this.client = new ApiClient();
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        this.client.authenticate();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        this.client.logout();
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        this.client = null;
        return this;
    }

    // Public API with custom exception
    public void initialize() throws CustomServiceException {
        // Wrap LifecycleException as CustomServiceException
        wrapLifecycle(this::onInit, CustomServiceException.class);
    }

    public void start() throws CustomServiceException {
        wrapLifecycle(this::onStart, CustomServiceException.class);
    }

    public void shutdown() throws CustomServiceException {
        wrapLifecycle(this::onStop, CustomServiceException.class);
    }
}

// Usage - callers see CustomServiceException, not LifecycleException
ExternalService service = new ExternalService();
try {
    service.initialize();
    service.start();
} catch (CustomServiceException e) {
    // Handle service-specific exception
}
```

### State Inspection

```java
public class MonitoredComponent extends AbstractLifecycle {

    // ... lifecycle methods ...

    public String getStatus() {
        if (isInitialized() && isStarted()) {
            return "RUNNING";
        } else if (isInitialized() && !isStarted()) {
            return "INITIALIZED";
        } else if (isStopped()) {
            return "STOPPED";
        } else if (isFlushed()) {
            return "FLUSHED";
        } else {
            return "NEW";
        }
    }

    public boolean isOperational() {
        return isInitialized() && isStarted() && !isStopped();
    }
}

// Usage
MonitoredComponent component = new MonitoredComponent();
System.out.println(component.getStatus()); // "NEW"

component.onInit();
System.out.println(component.getStatus()); // "INITIALIZED"

component.onStart();
System.out.println(component.getStatus()); // "RUNNING"
System.out.println(component.isOperational()); // true

component.onStop();
System.out.println(component.getStatus()); // "STOPPED"
System.out.println(component.isOperational()); // false
```

### Multiple Managed Components

```java
public class Application extends AbstractLifecycle {

    private DatabaseConnection database;
    private EventProcessor eventProcessor;
    private ConfigurableService externalService;

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        // Initialize all components
        database = new DatabaseConnection();
        database.onInit();

        eventProcessor = new EventProcessor();
        eventProcessor.onInit();

        externalService = new ConfigurableService();
        externalService.onInit();

        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Start in dependency order
        database.onStart();
        eventProcessor.onStart();
        externalService.onStart();

        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        // Stop in reverse order
        try {
            externalService.onStop();
        } catch (Exception e) {
            // Log but continue stopping others
        }

        try {
            eventProcessor.onStop();
        } catch (Exception e) {
            // Log but continue
        }

        try {
            database.onStop();
        } catch (Exception e) {
            // Log error
        }

        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        // Flush all components
        externalService.onFlush();
        eventProcessor.onFlush();
        database.onFlush();

        return this;
    }
}

// Usage
Application app = new Application();
app.onInit();
app.onStart();

// Application running with all components

app.onStop();
app.onFlush();
```

### Graceful Shutdown Hook

```java
public class ShutdownAwareService extends AbstractLifecycle {

    private ExecutorService workers;
    private List<Task> pendingTasks = new CopyOnWriteArrayList<>();

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        this.workers = Executors.newFixedThreadPool(10);
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Start accepting work
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        // Graceful shutdown with timeout
        workers.shutdown();

        try {
            // Wait up to 30 seconds for tasks to complete
            if (!workers.awaitTermination(30, TimeUnit.SECONDS)) {
                // Force shutdown after timeout
                List<Runnable> droppedTasks = workers.shutdownNow();

                // Wait a bit more for cancellation to complete
                if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new LifecycleException("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
            throw new LifecycleException("Interrupted during shutdown", e);
        }

        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        pendingTasks.clear();
        this.workers = null;
        return this;
    }
}

// Add JVM shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        service.onStop();
        service.onFlush();
    } catch (LifecycleException e) {
        System.err.println("Error during shutdown: " + e.getMessage());
    }
}));
```

## Advanced Patterns

### Idempotent Lifecycle Operations

```java
public class IdempotentComponent extends AbstractLifecycle {

    private AtomicInteger initCount = new AtomicInteger(0);
    private volatile Resource resource;

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        // Idempotent: safe to call multiple times
        if (resource == null) {
            resource = new Resource();
            initCount.incrementAndGet();
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Idempotent start
        if (resource != null && !resource.isActive()) {
            resource.activate();
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        // Idempotent stop
        if (resource != null && resource.isActive()) {
            resource.deactivate();
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        // Always safe to flush
        if (resource != null) {
            resource.release();
            resource = null;
        }
        return this;
    }
}
```

### Nested Lifecycle with State Guards

```java
public class ComplexService extends AbstractLifecycle {

    private Cache cache;
    private ConnectionPool pool;

    public void performOperation() throws LifecycleException {
        // Guard ensures component is ready
        ensureInitializedAndStarted();

        // Safe to use resources
        pool.execute(() -> {
            Object data = cache.get("key");
            // Process data
        });
    }

    public void updateCache(String key, Object value) throws LifecycleException {
        // Only need initialization
        ensureInitialized();

        cache.put(key, value);
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        this.cache = new Cache();
        this.pool = new ConnectionPool();
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        pool.start();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        pool.stop();
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        cache.clear();
        pool.releaseAll();
        return this;
    }
}
```

## Tips and best practices

### Lifecycle Implementation

1. **Favor idempotent lifecycle methods** - Implement `doInit`, `doStart`, `doFlush`, and `doStop` to safely handle repeated calls. Check state before allocating resources to avoid duplicates.

2. **Keep lifecycle methods lightweight** - Delegate heavy work (long-running tasks, blocking I/O) to dedicated worker threads started in `doStart()` and stopped in `doStop()`. Lifecycle methods should coordinate, not execute extended processing.

3. **Return `this` from lifecycle hooks** - Always return `this` from `doInit`, `doStart`, `doStop`, and `doFlush` to enable fluent chaining: `component.onInit().onStart()`.

4. **Use state guards in business methods** - Call `ensureInitializedAndStarted()` at the beginning of public methods to fail fast if the component isn't ready.

5. **Initialize in dependency order** - In `doInit()`, initialize components in the order they depend on each other. In `doStop()`, reverse the order.

### Thread Safety

6. **Respect the lifecycle mutex** - If you access shared mutable state from outside lifecycle methods, synchronize on `lifecycleMutex` or use concurrent collections to avoid races.

7. **Don't call lifecycle methods from within lifecycle methods** - Avoid calling `onInit()` from `doInit()` or similar. This can cause deadlocks due to mutex re-entry.

8. **Make state transitions observable** - Use `isInitialized()`, `isStarted()`, `isStopped()`, and `isFlushed()` in monitoring, health checks, and unit tests.

### Resource Management

9. **Allocate resources in doInit, activate in doStart** - Separate resource allocation (`doInit()`) from activation (`doStart()`). This allows pre-configuration without immediate activation.

10. **Release resources in reverse order** - In `doStop()` and `doFlush()`, release resources in the reverse order they were acquired to prevent dependency issues.

11. **Handle partial initialization failures** - If `doInit()` fails partway through, clean up any partially allocated resources before throwing the exception.

12. **Be conservative with onFlush() logic** - `onFlush()` is for lightweight cleanup between reload cycles (clearing caches). Avoid releasing long-lived resources unless they're re-acquirable in `onInit()`.

### Exception Handling

13. **Handle exceptions explicitly and use wrapLifecycle** - Wrap checked or domain-specific exceptions so callers get consistent exception types. Use `wrapLifecycle()` to convert `LifecycleException` into higher-level exceptions.

14. **Fail fast on invalid transitions** - Throw `LifecycleException` immediately on invalid transitions (the base class does this). This protects against entering inconsistent states.

15. **Log exceptions at appropriate levels** - Use `error` for unexpected failures, `warn` for recoverable issues, `info` for major transitions, `debug` for state checks, `trace` for entry/exit.

### Testing

16. **Test illegal transitions** - Write tests that verify guards work correctly: starting twice, flushing before stop, initializing twice, etc.

17. **Verify state transitions** - Assert that `isInitialized()`, `isStarted()`, etc. return expected values after each lifecycle call.

18. **Test reload behavior** - Verify `onReload()` produces deterministic results and doesn't leak resources.

19. **Test concurrent access** - Use multiple threads to call lifecycle methods and business methods concurrently to verify thread safety.

### Production Readiness

20. **Provide graceful shutdown** - In `doStop()`, wait for a bounded time for threads/tasks to finish, then force-close to prevent hangs during system shutdown.

21. **Add JVM shutdown hooks** - Register shutdown hooks to call `onStop()` and `onFlush()` when the JVM terminates.

22. **Design for reloads** - Ensure `onReload()` results in a clean state. Avoid external side-effects in `doFlush()` that cannot be safely repeated.

23. **Document side effects** - In subclass Javadoc, document resources created, required operation order, and whether init/start are blocking or asynchronous.

24. **Expose health check methods** - Provide methods like `isOperational()` or `getStatus()` that combine lifecycle states for monitoring systems.

25. **Use lifecycle in composition** - When managing multiple components, embed their lifecycles in your component's lifecycle for coordinated startup/shutdown.

## License
This module is distributed under the MIT License.
