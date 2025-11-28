# Garganttua Runtime

## Description

Garganttua Runtime is a **high-level orchestration framework** that provides a structured, type-safe approach to building complex execution workflows in Java. It combines dependency injection, lifecycle management, and execution chains into a unified runtime system for processing inputs and producing outputs through configurable stages and steps.

The Runtime framework enables you to define **multi-stage execution pipelines** where each stage consists of multiple steps that can access shared context, handle exceptions, and manage variables. It's designed for building robust, maintainable business processes, data pipelines, request handlers, and workflow engines.

**Key Features:**
- **Type-Safe Pipelines** - Generic `<InputType, OutputType>` for compile-time safety
- **Multi-Stage Execution** - Organize work into logical stages with ordered steps
- **Dependency Injection Integration** - Full integration with Garganttua DI container
- **Exception Handling** - Declarative catch blocks and fallback mechanisms
- **Variable Management** - Shared variables accessible across all steps
- **Lifecycle Management** - Complete lifecycle control (init, start, stop, flush)
- **Context Propagation** - Rich runtime context with input, output, variables, and exceptions
- **UUID Tracking** - Automatic correlation ID generation for tracing
- **MDC Integration** - Automatic MDC population for logging
- **Fluent Builder API** - Intuitive DSL for runtime definition
- **Auto-Detection** - Annotation-based automatic runtime configuration
- **Execution Chain** - Sequential step execution with conditional flow control

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-runtime</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-execution`
 - `com.garganttua.core:garganttua-condition`
 - `com.garganttua.core:garganttua-reflections:test`
 - `ch.qos.logback:logback-classic:test`
 - `com.github.f4b6a3:uuid-creator:5.0.0`
 - `org.jfree:jfreechart:1.5.4:test`
 - `com.github.librepdf:openpdf:1.3.40:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Runtime

A `Runtime<InputType, OutputType>` represents a complete execution pipeline that processes input of type `InputType` and produces output of type `OutputType`. It consists of:
- **Name** - Unique identifier for the runtime
- **Stages** - Ordered collection of execution stages
- **DI Context** - Dependency injection container for bean resolution
- **Preset Variables** - Initial variables available at runtime start
- **Input/Output Types** - Generic type parameters for type safety

**Lifecycle:**
1. Create `RuntimeContext` with input
2. Initialize and start context
3. Build execution chain from stages/steps
4. Execute chain
5. Collect result
6. Stop and flush context

### Runtime Stage

A `RuntimeStage` is a logical grouping of related steps within a runtime. Stages execute sequentially, and each stage contains one or more steps that execute in order.

**Characteristics:**
- **Name** - Stage identifier
- **Steps** - Ordered map of runtime steps
- **Sequential Execution** - Steps execute in definition order

### Runtime Step

A `RuntimeStep` is the basic unit of execution within a stage. Each step executes a method on a bean with optional exception handling.

**Components:**
- **Operation Binder** - Method to execute
- **Fallback Binder** - Optional exception handler
- **Execution Return** - Return type of the operation
- **Step Name** - Unique identifier within the stage

### Runtime Context

`RuntimeContext<InputType, OutputType>` is a rich execution context passed to every step containing:
- **Input** - Original input to the runtime
- **Output** - Current output value (settable)
- **Variables** - Shared state accessible by all steps
- **Exceptions** - Recorded exceptions during execution
- **Code** - Execution status code
- **UUID** - Unique execution identifier
- **DI Context** - Access to dependency injection capabilities
- **Timing** - Start/stop timestamps for performance tracking

### Runtime Result

`RuntimeResult<InputType, OutputType>` captures the outcome of runtime execution:
- **Input** - Original input value
- **Output** - Final output value
- **Start/Stop Times** - Execution timestamps (Instant and nano precision)
- **Status Code** - Integer result code
- **Exceptions** - Collection of caught exceptions
- **UUID** - Execution correlation ID

### Builder DSL

The framework provides a fluent DSL for defining runtimes:
- `RuntimesBuilder` - Root builder for multiple runtimes
- `RuntimeBuilder` - Builds a single runtime
- `RuntimeStageBuilder` - Builds a stage
- `RuntimeStepBuilder` - Builds a step
- `RuntimeStepMethodBuilder` - Configures step method binding
- `RuntimeStepCatchBuilder` - Configures exception handling
- `RuntimeStepFallbackBuilder` - Configures fallback behavior

## Usage

### 1. Simple Runtime with Single Stage

Create a basic runtime that validates and processes user data:

```java
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.injection.DiContext;

// Define input/output types
class UserInput {
    String username;
    String email;
}

class UserOutput {
    Long userId;
    boolean success;
}

// Create DI context
IDiContext diContext = new DiContext();

// Define runtime
IRuntime<UserInput, UserOutput> runtime = RuntimesBuilder
    .runtimes()
    .runtime("user-registration", UserInput.class, UserOutput.class)
        .stage("validation")
            .step("validate-input", UserOutput.class)
                .method(ValidationService.class, "validate")
                    .with(context -> context.getInput())
                .build()
            .build()
        .build()
    .handle(diContext)
    .build()
    .getRuntime("user-registration");

// Execute
UserInput input = new UserInput();
input.username = "alice";
input.email = "alice@example.com";

Optional<IRuntimeResult<UserInput, UserOutput>> result = runtime.execute(input);
```

### 2. Multi-Stage Pipeline

Create a runtime with multiple stages processing data sequentially:

```java
IRuntime<OrderRequest, OrderResponse> runtime = RuntimesBuilder
    .runtimes()
    .runtime("order-processing", OrderRequest.class, OrderResponse.class)
        .stage("validation")
            .step("validate-order", Void.class)
                .method(OrderValidator.class, "validate")
                    .with(context -> context.getInput())
                .build()
            .build()
        .build()
        .stage("inventory")
            .step("check-stock", StockResult.class)
                .method(InventoryService.class, "checkAvailability")
                    .with(context -> context.getInput())
                .build()
            .build()
        .build()
        .stage("payment")
            .step("process-payment", PaymentResult.class)
                .method(PaymentService.class, "charge")
                    .with(context -> context.getInput())
                .build()
            .build()
        .build()
    .handle(diContext)
    .build()
    .getRuntime("order-processing");
```

### 3. Exception Handling with Catch Blocks

Handle exceptions declaratively at the step level:

```java
IRuntime<PaymentRequest, PaymentResponse> runtime = RuntimesBuilder
    .runtimes()
    .runtime("payment-processing", PaymentRequest.class, PaymentResponse.class)
        .stage("payment")
            .step("charge-card", PaymentResult.class)
                .method(PaymentGateway.class, "charge")
                    .with(context -> context.getInput())
                .build()
                .catch_(PaymentDeclinedException.class)
                    .then(RetryService.class, "retryPayment")
                        .with(context -> context.getException(PaymentDeclinedException.class))
                    .build()
                .build()
            .build()
        .build()
    .handle(diContext)
    .build()
    .getRuntime("payment-processing");
```

### 4. Variable Management

Share state between steps using variables:

```java
IRuntime<DataRequest, DataResponse> runtime = RuntimesBuilder
    .runtimes()
    .runtime("data-pipeline", DataRequest.class, DataResponse.class)
        // Set preset variables
        .variable("batchSize", 100)
        .variable("timeout", 30000)
        .stage("extraction")
            .step("fetch-data", List.class)
                .method(DataExtractor.class, "extract")
                    .with(context -> context.getVariable("batchSize", Integer.class))
                .build()
            .build()
        .build()
    .handle(diContext)
    .build()
    .getRuntime("data-pipeline");
```

### 5. Setting Output and Status Codes

Control output and execution status:

```java
@Singleton
public class OrderProcessor {

    public void processOrder(IRuntimeContext<OrderRequest, OrderResponse> context) {
        OrderRequest request = context.getInput().get();

        // Process order
        OrderResponse response = new OrderResponse();
        response.setOrderId(generateOrderId());
        response.setStatus("CONFIRMED");

        // Set output
        context.setOutput(response);

        // Set status code
        context.setCode(200);
    }
}
```

### 6. UUID-Based Execution Tracking

Track executions with correlation IDs:

```java
// Automatic UUID generation
Optional<IRuntimeResult<Request, Response>> result = runtime.execute(request);
UUID executionId = result.get().uuid();

// Manual UUID for correlation
UUID correlationId = UUID.randomUUID();
Optional<IRuntimeResult<Request, Response>> result = runtime.execute(correlationId, request);
```

### 7. Timing and Performance Tracking

Access execution timing information:

```java
Optional<IRuntimeResult<Request, Response>> result = runtime.execute(request);

if (result.isPresent()) {
    IRuntimeResult<Request, Response> r = result.get();

    Instant start = r.start();
    Instant stop = r.stop();
    long durationMs = Duration.between(start, stop).toMillis();

    log.info("Execution took {}ms", durationMs);
}
```

## Advanced Patterns

### Fallback Mechanisms

Provide fallback behavior when steps fail:

```java
IRuntime<Request, Response> runtime = RuntimesBuilder
    .runtimes()
    .runtime("resilient-service", Request.class, Response.class)
        .stage("processing")
            .step("primary-service", Data.class)
                .method(PrimaryService.class, "process")
                    .with(context -> context.getInput())
                .build()
                .fallback(FallbackService.class, "processWithFallback")
                    .with(context -> context.getInput())
                .build()
            .build()
        .build()
    .handle(diContext)
    .build()
    .getRuntime("resilient-service");
```

### Auto-Detection with Annotations

Define runtimes using annotations:

```java
public class OrderProcessingRuntime {

    @Stages
    private Map<String, List<Class<?>>> stages = Map.of(
        "validation", List.of(ValidationService.class),
        "inventory", List.of(InventoryService.class),
        "payment", List.of(PaymentService.class)
    );

    @Variables
    private Map<String, IObjectSupplierBuilder<?, ?>> variables = Map.of(
        "timeout", of(30000)
    );
}

// Build with auto-detection
OrderProcessingRuntime definition = new OrderProcessingRuntime();

IRuntime<OrderRequest, OrderResponse> runtime = RuntimesBuilder
    .runtimes()
    .runtime("order-processing", OrderRequest.class, OrderResponse.class, definition)
        .autoDetect(true)
    .handle(diContext)
    .build()
    .getRuntime("order-processing");
```

## Performance

### Execution Overhead

Runtime execution introduces minimal overhead:

- **Context Creation**: ~1-5ms
- **Chain Building**: ~0.5-2ms per step
- **Step Execution**: Depends on business logic
- **Result Collection**: ~0.1-1ms

**Total Framework Overhead**: ~2-10ms for typical runtimes (5-10 steps)

### Optimization Strategies

1. **Reuse Runtime Instances** - Build once, execute many times
2. **Minimize Variable Access** - Cache frequently-accessed variables
3. **Batch Processing** - Process multiple inputs with same runtime
4. **Asynchronous Execution** - Wrap runtime.execute() in CompletableFuture
5. **Profile Executions** - Use timing information to identify bottlenecks

## Tips and best practices

### Runtime Design

1. **Single Responsibility** - Each runtime should handle one business process
2. **Type Safety** - Use specific input/output types, avoid Object
3. **Meaningful Names** - Name runtimes, stages, and steps descriptively
4. **Stage Granularity** - Group related steps into logical stages
5. **Step Atomicity** - Each step should perform one clear action

### Error Handling

6. **Catch Specific Exceptions** - Use specific exception types in catch blocks
7. **Fallback Strategy** - Provide fallbacks for critical operations
8. **Exception Recording** - Record exceptions for debugging and monitoring
9. **Status Codes** - Use meaningful status codes (HTTP-style recommended)
10. **Graceful Degradation** - Handle errors without cascading failures

### Variable Management

11. **Preset Variables** - Use for configuration and constants
12. **Runtime Variables** - Use for inter-step communication
13. **Variable Naming** - Use clear, descriptive variable names
14. **Type Safety** - Always specify variable types when retrieving
15. **Variable Scope** - Understand variables are runtime-scoped

### Context Usage

16. **Input Immutability** - Don't modify input objects
17. **Output Setting** - Set output in the last stage
18. **Context Reuse** - Don't try to reuse contexts across executions
19. **Resource Cleanup** - Context lifecycle handles cleanup automatically
20. **Thread Safety** - Contexts are not thread-safe; one per execution

### Dependency Injection

21. **Bean Registration** - Register all service beans before runtime creation
22. **Singleton Services** - Use @Singleton for stateless services
23. **Inject Dependencies** - Use DI instead of manual instantiation
24. **Context Propagation** - Runtime context is a child DI context
25. **Bean Lifecycle** - Services participate in DI lifecycle

### Logging

26. **Structured Logging** - Use MDC for correlation IDs
27. **Log Levels** - TRACE for details, DEBUG for flow, INFO for milestones
28. **Performance Logging** - Log execution times for monitoring
29. **Exception Logging** - Log exceptions at appropriate levels
30. **Log Context** - Include runtime/stage/step names in logs

### Testing

31. **Unit Test Steps** - Test individual service methods
32. **Integration Test Runtimes** - Test complete runtime execution
33. **Mock Dependencies** - Use mocks for external dependencies
34. **Test Exception Paths** - Verify catch/fallback behavior
35. **Performance Testing** - Measure runtime execution times

### Common Pitfalls to Avoid

36. **Don't Share Contexts** - Each execution needs its own context
37. **Don't Ignore Exceptions** - Always handle or record exceptions
38. **Don't Skip Lifecycle** - Let framework manage lifecycle
39. **Don't Mutate Input** - Treat input as immutable
40. **Don't Hardcode Values** - Use variables for configuration
41. **Don't Over-Engineer** - Keep runtimes simple and focused
42. **Version Compatibility** - Ensure all Garganttua modules are same version

## License

This module is distributed under the MIT License.
