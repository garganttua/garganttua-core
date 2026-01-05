# 🔒 Garganttua Mutex

## Description

The Garganttua Mutex module provides a thread-safe mutex synchronization framework for managing mutually exclusive access to critical sections. It supports configurable acquisition strategies including timeout, retry logic, and automatic lease expiration for robust distributed locking scenarios.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-mutex</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-supply`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### IMutex

The `IMutex` interface provides a mechanism to execute code within a mutually exclusive lock, ensuring that only one thread can execute the protected code at a time.

**Key components:**
- **ThrowingFunction<R>**: Functional interface for code to be executed within mutex protection
- **MutexStrategy**: Configuration for acquisition behavior (timeout, retries, lease time)
- **MutexException**: Exception thrown when mutex operations fail

### IMutexManager

The `IMutexManager` interface provides a registry of mutexes identified by string names. It extends `IContextualSupplier` to integrate with the Garganttua supply framework for dependency injection.

### MutexStrategy

The `MutexStrategy` record defines the parameters controlling how a mutex is acquired:

- **Wait Time**: Time to wait for mutex availability
  - `-1`: Wait forever until mutex is available
  - `0`: Try immediately, fail if mutex is not available
  - `> 0`: Wait for specified duration
- **Retries**: Number of acquisition attempts after initial failure
- **Retry Interval**: Delay between retry attempts
- **Lease Time**: Maximum time to hold the mutex before automatic release

### Key Features

- **Thread-safe critical sections**: Ensures mutual exclusion for protected code
- **Simple acquisition**: Wait indefinitely for mutex availability
- **Strategy-based acquisition**: Fine-grained control over timeout, retries, and lease time
- **Named mutexes**: Registry-based mutex management via IMutexManager
- **Dependency injection integration**: Works with IContextualSupplier framework
- **Automatic release**: Lease time prevents deadlocks from failed releases
- **GraalVM native image support**: @Mutex annotation for native configuration

## Usage

### Basic Mutex Acquisition

Example from javadoc:

```java
// Simple acquisition (wait forever)
IMutex mutex = mutexManager.mutex("my-resource");
String result = mutex.acquire(() -> {
    // Thread-safe critical section
    return performCriticalOperation();
});
```

### Strategy-Based Acquisition

Example from javadoc:

```java
// Configure acquisition strategy
MutexStrategy strategy = new MutexStrategy(
    5,                    // Wait 5 seconds
    TimeUnit.SECONDS,
    3,                    // Retry 3 times
    1,                    // Wait 1 second between retries
    TimeUnit.SECONDS,
    30,                   // Lease time: 30 seconds
    TimeUnit.SECONDS
);

IMutex mutex = mutexManager.mutex("my-resource");
String result = mutex.acquire(() -> {
    // Thread-safe critical section with timeout and retry
    return performCriticalOperation();
}, strategy);
```

### Cache Synchronization

Example from @Mutex annotation javadoc:

```java
@Mutex
public class DistributedCacheService {
    private final IMutexManager mutexManager;

    public void updateCache(String key, Object value) {
        IMutex mutex = mutexManager.mutex("cache:" + key);
        mutex.acquire(() -> {
            // Thread-safe cache update
            cache.put(key, value);
            return null;
        });
    }
}
```

## Usage Patterns

1. **Database transaction serialization**: Ensure only one transaction modifies a specific record at a time
2. **Shared resource access control**: Protect access to shared files, network resources, or memory structures
3. **Distributed lock coordination**: Coordinate operations across multiple application instances
4. **Cache invalidation synchronization**: Prevent race conditions during cache updates

## Exception Handling

All mutex operations throw `MutexException` on failure:

- **Mutex acquisition timeout**: Wait time exceeded without acquiring mutex
- **All retry attempts exhausted**: Failed to acquire after configured retries
- **Execution failure within protected code**: Exception thrown by ThrowingFunction
- **Mutex manager unavailable**: Cannot create or retrieve mutex
- **Deadlock detection**: Potential deadlock scenario detected

## GraalVM Native Image Support

Types that use mutexes should be annotated with `@Mutex` to ensure proper native image configuration:

```java
@Mutex
public class MyService {
    // Class uses IMutex or IMutexManager
}
```

## Tips and Best Practices

1. **Use named mutexes**: Consistently name mutexes based on the resource they protect (e.g., "db:user:123", "cache:session:abc")
2. **Configure appropriate timeouts**: Use realistic wait times based on expected operation duration
3. **Set lease times**: Always configure lease time to prevent deadlocks when holders fail
4. **Handle MutexException**: Always catch and handle MutexException appropriately for your use case
5. **Keep critical sections short**: Minimize the code executed within mutex.acquire() to reduce lock contention
6. **Use strategy-based acquisition**: For production systems, prefer strategy-based acquisition over simple acquisition
7. **Test retry logic**: Verify that retry configuration works correctly under failure scenarios
8. **Avoid nested mutexes**: Be careful when acquiring multiple mutexes to prevent deadlocks
9. **Monitor mutex contention**: Log mutex acquisition failures to identify performance bottlenecks

## License
This module is distributed under the MIT License.
