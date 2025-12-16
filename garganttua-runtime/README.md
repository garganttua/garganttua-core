# ▶️ Garganttua Runtime

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
 - `com.garganttua.core:garganttua-reflections:provided`
 - `com.garganttua.core:garganttua-native:provided`
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

### 1. Auto-Detection with Annotation-Based Runtime Definition

The simplest way to define a runtime is using annotations with auto-detection:

```java
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;

// Define the runtime with annotations
@RuntimeDefinition(input=String.class, output=String.class)
@Named("runtime-1")
public class OneStepRuntime {

    @Stages
    public Map<String, List<Class<?>>> stages = Map.of(
            "stage-1", List.of(DummyRuntimeProcessOutputStep.class));

    @Variables
    public Map<String, ISupplierBuilder<?, ? extends ISupplier<?>>> presetVariables =
        Map.of("variable", of("preset-variable"));
}

// Build with auto-detection
IDiContextBuilder contextBuilder = DiContext.builder()
    .autoDetect(true)
    .withPackage("com.garganttua.core.runtime.annotations")
    .withPackage("com.garganttua.core.runtime");

contextBuilder.build().onInit().onStart();

IRuntimesBuilder runtimesBuilder = RuntimesBuilder.builder()
    .context(contextBuilder)
    .autoDetect(true);

Map<String, IRuntime<?, ?>> runtimes = runtimesBuilder.build();

// Execute
IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
IRuntimeResult<String, String> result = runtime.execute("input").orElseThrow();
```

### 2. Defining Runtime Steps with Operations and Exception Handling

Define a step with method operation, exception catching, and fallback:

```java
import static com.garganttua.core.condition.Conditions.custom;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;

@Step
@Named("output-step")
public class DummyRuntimeProcessOutputStep {

    @Condition
    IConditionBuilder condition = custom(of(10), i -> 1 > 0);

    @Operation(abortOnUncatchedException=true)
    @Output
    @Catch(exception = DiException.class, code = 401)
    @Variable(name = "method-returned")
    @Code(201)
    @Nullable
    String method(
            @Input String input,
            @Fixed(valueString = "fixed-value-in-method") String fixedValue,
            @Variable(name = "variable") String variable,
            @Context IRuntimeContext<String, String> context)
            throws DiException, CustomException {

        if (variable.equals("di-exception")) {
            throw new DiException(input + "-processed-" + fixedValue + "-" + variable);
        }

        if (variable.equals("custom-exception")) {
            throw new CustomException(input + "-processed-" + fixedValue + "-" + variable);
        }

        return input + "-processed-" + fixedValue + "-" + variable;
    }

    @FallBack
    @Output
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "fallback-returned")
    String fallbackMethod(
            @Input String input,
            @Fixed(valueString = "fixed-value-in-fallback") String fixedValue,
            @Exception DiException exception,
            @Code Integer code,
            @Nullable @ExceptionMessage String exceptionMessage,
            @Context IRuntimeContext<String, String> context) {
        return input + "-fallback-" + fixedValue + "-" + code + "-" + exceptionMessage;
    }
}
```

### 3. Programmatic Runtime Building with Fluent DSL

Build a runtime programmatically using the fluent builder API:

```java
import static com.garganttua.core.condition.Conditions.custom;
import static com.garganttua.core.runtime.RuntimeContext.*;
import static com.garganttua.core.supply.dsl.FixedSupplierBuilder.of;

DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

IRuntimesBuilder builder = RuntimesBuilder.builder()
    .context(contextBuilder)
    .runtime("runtime-1", String.class, String.class)
        .stage("stage-1")
            .step("step-1", of(step), String.class)
                .method()
                    .condition(custom(of(10), i -> true))
                    .output(true)
                    .variable("method-returned")
                    .method("method")
                    .code(201)
                    .katch(DiException.class).code(401).up()
                    .withParam(input(String.class))
                    .withParam(of("fixed-value-in-method"))
                    .withParam(variable("variable", String.class))
                    .withParam(context()).up()
                .fallBack()
                    .onException(DiException.class).up()
                    .output(true)
                    .variable("fallback-returned")
                    .method("fallbackMethod")
                    .withParam(input(String.class))
                    .withParam(of("fixed-value-in-method"))
                    .withParam(exception(DiException.class))
                    .withParam(code())
                    .withParam(exceptionMessage())
                    .withParam(context())
                    .up().up().up()
        .variable("variable", of("preset-variable"))
        .up();

Map<String, IRuntime<?, ?>> runtimes = builder.build();
IRuntime<String, String> runtime = (IRuntime<String, String>) runtimes.get("runtime-1");
```

### 4. Multi-Step Runtime with Variables

Create a runtime with multiple steps that share state via variables:

```java
// Runtime definition
@RuntimeDefinition(input = String.class, output = String.class)
@Named("two-steps-runtime")
public class TwoStepsRuntimeDefinition {
    @Stages
    public Map<String, List<Class<?>>> stages = Map.of(
        "stage-1", List.of(StepOne.class, StepOutput.class));
}

// First step
@Step
@Named("step-one")
public class StepOne {
    @Operation(abortOnUncatchedException = false)
    @Catch(exception = DiException.class, code = 401)
    @Variable(name = "step-one-returned")
    @Nullable
    String method(
            @Input String input,
            @Variable(name = "step-one-variable") String variable)
            throws DiException, CustomException {

        if (variable.equals("di-exception")) {
            throw new DiException(input + "-step-one-processed-" + variable);
        }

        return input + "-step-one-processed-" + variable;
    }

    @FallBack
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "step-one-fallback-returned")
    String fallbackMethod(@Input String input) {
        return input + "-step-one-fallback";
    }
}

// Second step using output from first
@Step
@Named("output-step")
public class StepOutput {
    @Output
    @Code(222)
    @Operation(abortOnUncatchedException = true)
    @Catch(exception = DiException.class, code = 444)
    @Variable(name = "output-step-returned")
    @Nullable
    String method(
            @Variable(name = "step-one-returned") String input,
            @Variable(name = "output-step-variable") String outputStepVariable)
            throws DiException, CustomException {

        if (outputStepVariable.equals("di-exception")) {
            throw new DiException(input + "-output-step-processed-" + outputStepVariable);
        }

        return input + "-output-step-processed-" + outputStepVariable;
    }

    @FallBack
    @Output
    @Nullable
    @OnException(exception = DiException.class)
    @Variable(name = "output-step-fallback-returned")
    String fallbackMethod(@Variable(name = "step-one-returned") String input) {
        return input + "-output-step-fallback";
    }
}

// Execute
IRuntime<String, String> runtime = (IRuntime<String, String>)
    runtimes.get("two-steps-runtime");

IRuntimeResult<String, String> result = runtime.execute("test").orElseThrow();

// Result: "test-step-one-processed-step-one-variable-output-step-processed-output-step-variable"
// Code: 222
```

### 5. Execution Results and Timing

Access execution results, status codes, and timing information:

```java
IRuntimeResult<String, String> result = runtime.execute("input").orElseThrow();

// Get output and status code
String output = result.output();  // "input-processed-fixed-value-in-method-preset-variable"
int code = result.code();         // 201

// Get timing information
long durationNanos = result.durationInNanos();

// Check for exceptions
List<Exception> exceptions = result.getExceptions();
boolean hasAborted = result.hasAborted();
Optional<Exception> abortingException = result.getAbortingException();

// Get execution UUID for tracking
UUID executionId = result.uuid();
```

### 6. Exception Handling Strategies

Different strategies for handling uncaught exceptions:

```java
// Strategy 1: Abort on uncaught exception (abortOnUncatchedException = true)
@Operation(abortOnUncatchedException = true)
String method(@Input String input) throws CustomException {
    throw new CustomException("error");
}
// Result: Runtime aborts, code = GENERIC_RUNTIME_ERROR_CODE

// Strategy 2: Continue on uncaught exception with nullable step (abortOnUncatchedException = false)
@Operation(abortOnUncatchedException = false)
@Nullable
String method(@Input String input) throws CustomException {
    throw new CustomException("error");
}
// Result: Exception recorded, execution continues, step returns null

// Strategy 3: Caught exception handled by fallback
@Operation
@Catch(exception = DiException.class, code = 401)
String method(@Input String input) throws DiException {
    throw new DiException("error");
}

@FallBack
@OnException(exception = DiException.class)
String fallbackMethod(@Input String input) {
    return input + "-fallback";
}
// Result: Fallback executed, custom logic runs
```

## Advanced Patterns

### Testing Runtime Execution

From the test suite, here's how to verify runtime behavior:

```java
@Test
void simpleRuntimeBuilderTest() {
    DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

    IRuntimesBuilder b = baseFallback(
            baseRuntime(builder(), step)).up().up()
        .variable("variable", of("preset-variable")).up();

    IRuntime<String, String> runtime = get(b);

    assertDoesNotThrow(() -> {
        var result = runtime.execute("input").orElseThrow();
        assertEquals("input-processed-fixed-value-in-method-preset-variable", result.output());
    });
}
```

### Testing Exception Handling with Abort

Test uncaught exceptions with abort enabled:

```java
@Test
void uncatchedException_abort_true() {
    DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

    IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step).method()
            .abortOnUncatchedException(true).up()).up().up()
        .variable("variable", of("custom-exception")).up();

    IRuntime<String, String> runtime = get(b);

    var r = runtime.execute("input").orElseThrow();

    assertEquals(1, r.getExceptions().size());
    assertTrue(r.hasAborted());
    assertTrue(r.getAbortingException().isPresent());
    assertEquals(IRuntime.GENERIC_RUNTIME_ERROR_CODE, r.code());
}
```

### Testing Exception Handling with Nullable Steps

Test uncaught exceptions with nullable steps (abort disabled):

```java
@Test
void uncatchedException_abort_false_stepNullable() {
    DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

    IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step).method()
            .nullable(true).abortOnUncatchedException(false).up()).up().up()
        .variable("variable", of("custom-exception")).up();

    IRuntime<String, String> runtime = get(b);

    var r = runtime.execute("input").orElseThrow();

    assertEquals(1, r.getExceptions().size());
    assertFalse(r.hasAborted());
    assertEquals(201, r.code());
}
```

### Testing Caught Exceptions with Fallback

Test exceptions caught and handled by fallback methods:

```java
@Test
void catchedExceptionHandledByFallback() {
    DummyRuntimeProcessOutputStep step = new DummyRuntimeProcessOutputStep();

    IRuntimesBuilder b = baseFallback(baseRuntime(builder(), step)).up().up()
        .variable("variable", of("di-exception")).up();

    IRuntime<String, String> runtime = get(b);

    var r = runtime.execute("input").orElseThrow();

    assertEquals(1, r.getExceptions().size());
    assertTrue(r.hasAborted());
    assertTrue(r.getAbortingException().isPresent());
}
```

### Performance Testing Pattern

From the performance tests, here's the pattern for load testing runtimes:

```java
private TestPerfReport runTest(int runs, IRuntime<String, String> runtime, int poolThreadSize)
        throws InterruptedException, ExecutionException {
    Instant start = Instant.now();
    ExecutorService executor = Executors.newFixedThreadPool(poolThreadSize);

    List<Callable<Long>> tasks = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger(0);

    for (int i = 0; i < runs; i++) {
        tasks.add(() -> {
            String input = "input-" + counter.getAndIncrement();
            Optional<IRuntimeResult<String, String>> r = runtime.execute(input);
            return r.get().durationInNanos();
        });
    }

    List<Future<Long>> futures = executor.invokeAll(tasks);
    executor.shutdown();

    long total = 0;
    long min = Long.MAX_VALUE;
    long max = 0;

    for (Future<Long> f : futures) {
        long d = f.get();
        total = total + d;
        if (d < min) min = d;
        if (d > max) max = d;
    }

    Long avg = total / runs;
    Instant stop = Instant.now();

    return new TestPerfReport(runs, futures.size(), avg, min, max, total,
        Duration.between(start, stop));
}
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
