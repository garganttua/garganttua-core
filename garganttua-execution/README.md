# ⛓️ Garganttua Execution

## Description

The Garganttua Execution module provides a flexible and robust chain-of-responsibility pattern implementation for orchestrating task execution with built-in fallback handling. It enables sequential processing of operations with automatic error recovery mechanisms.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-execution</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### ExecutorChain

The `ExecutorChain<T>` is the main implementation of the chain-of-responsibility pattern. It manages a queue of executors that process a request sequentially.

**Key components:**
- **IExecutor<T>**: Functional interface for defining execution logic
- **IFallBackExecutor<T>**: Functional interface for fallback handling
- **ExecutorException**: Exception thrown during execution failures

### Execution Flow

1. **Add executors** to the chain (optionally with fallback handlers)
2. **Execute** the chain with a request object
3. Each executor processes the request and calls `chain.execute()` to continue
4. If an executor throws an `ExecutorException`, fallback executors are triggered
5. Depending on configuration, exceptions can be rethrown or absorbed

### Key Features

- **Sequential execution**: FIFO queue-based executor chain
- **Fallback handling**: Optional fallback executors for error recovery
- **Type-safe generics**: Works with any request type
- **Configurable error propagation**: Choose to rethrow or absorb exceptions
- **Comprehensive logging**: Detailed execution traces for debugging

### Fallback Mechanism

When an executor fails:
1. Its associated fallback executor (if any) is added to the fallback queue
2. All fallback executors are executed in FIFO order
3. Fallback executors can perform cleanup, logging, or recovery operations

## Usage

### Basic Chain Execution

```java
// Create an executor chain for Integer processing
ExecutorChain<Integer> chain = new ExecutorChain<>();

// Add executors using lambda expressions
chain.addExecutor((value, nextChain) -> {
    value = value + 10;
    System.out.println("Step 1: " + value);
    nextChain.execute(value);
});

chain.addExecutor((value, nextChain) -> {
    value = value * 2;
    System.out.println("Step 2: " + value);
    nextChain.execute(value);
});

// Execute the chain
chain.execute(5);  // Output: Step 1: 15, Step 2: 30
```

### Chain with Fallback Handlers

```java
ExecutorChain<String> chain = new ExecutorChain<>();

// Add executor with fallback
chain.addExecutor(
    // Main executor
    (request, nextChain) -> {
        if (request.isEmpty()) {
            throw new ExecutorException("Empty request");
        }
        System.out.println("Processing: " + request);
        nextChain.execute(request);
    },
    // Fallback executor
    (request, nextChain) -> {
        System.out.println("Fallback: Using default value");
        nextChain.execute("default");
    }
);

chain.execute("");  // Triggers fallback
```

### Custom Request Types

```java
public class OrderRequest {
    private String orderId;
    private double amount;
    // getters, setters...
}

ExecutorChain<OrderRequest> orderChain = new ExecutorChain<>();

orderChain.addExecutor((order, chain) -> {
    // Validate order
    if (order.getAmount() <= 0) {
        throw new ExecutorException("Invalid amount");
    }
    chain.execute(order);
});

orderChain.addExecutor((order, chain) -> {
    // Process payment
    processPayment(order);
    chain.execute(order);
});

orderChain.addExecutor((order, chain) -> {
    // Send confirmation
    sendConfirmation(order);
    chain.execute(order);
});

orderChain.execute(new OrderRequest("ORD-123", 99.99));
```

### Configuring Exception Handling

```java
// Rethrow exceptions (default behavior)
ExecutorChain<String> strictChain = new ExecutorChain<>(true);

// Absorb exceptions after fallback execution
ExecutorChain<String> lenientChain = new ExecutorChain<>(false);
```

## Tips and best practices

1. **Always call `chain.execute()`**: Each executor should call `nextChain.execute()` to continue the chain, unless it's the final step.
2. **Use fallbacks for recovery**: Attach fallback executors to critical operations that may fail but have recovery strategies.
3. **Keep executors focused**: Each executor should perform a single, well-defined operation (validation, transformation, persistence, etc.).
4. **Leverage lambda expressions**: Use lambda expressions or method references for concise executor definitions.
5. **Handle state carefully**: Remember that primitive types are passed by value. For mutable objects like `StringBuilder` or custom POJOs, modifications are preserved across the chain.
6. **Configure rethrow appropriately**:
   - Use `rethrow = true` for critical workflows where failures must be handled by the caller
   - Use `rethrow = false` for best-effort processing where partial success is acceptable
7. **Monitor execution**: The module provides extensive logging at TRACE, DEBUG, INFO, WARN, and ERROR levels for troubleshooting.
8. **Test your chains**: Use unit tests to verify both success paths and fallback scenarios.

## License
This module is distributed under the MIT License.