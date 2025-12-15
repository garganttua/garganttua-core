# Garganttua Supply

## Description

Garganttua Supply is a **flexible, type-safe object supplying framework** that provides dynamic object instantiation and dependency resolution at runtime. It offers a unified API for creating suppliers with varying complexity levels—from fixed values to context-aware factory patterns—making it a foundational component for dependency injection, factory patterns, and dynamic object creation throughout the Garganttua ecosystem.

The Supply framework enables you to define **declarative object suppliers** that can produce values in multiple ways: fixed values, null values, new instances via constructor bindings, or context-aware instances resolved from external systems. It seamlessly integrates with Garganttua Reflection for constructor parameter injection and supports both simple and contextual supplier patterns.

**Key Features:**
- **Type-Safe API** - Generic `<Supplied>` type parameters for compile-time safety
- **Multiple Supplier Types** - Fixed, null, new instance, and contextual suppliers
- **Fluent Builder DSL** - Intuitive API for supplier definition with static factory methods
- **Null Safety** - Declarative nullable/non-nullable value enforcement
- **Constructor Binding** - Integration with Garganttua Reflection for parameter injection
- **Context-Aware Suppliers** - Dynamic object resolution from external contexts (DI containers, Spring, etc.)
- **Wrapper Pattern** - Automatic nullable wrapping for runtime validation
- **Optional-Based API** - Consistent use of `Optional<T>` for null handling
- **Exception Handling** - Dedicated `SupplyException` for supply failures
- **Unified Interface** - Single `ISupplier<T>` interface for all supplier types
- **Static Import Support** - Concise syntax with static factory methods
- **Flexible Configuration** - Mix and match supplier capabilities as needed

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-supply</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Object Supplier

An `ISupplier<Supplied>` is the core abstraction representing a source of objects of type `Supplied`. All suppliers implement this interface and provide:
- **supply()** - Produces an `Optional<Supplied>` value
- **getSuppliedType()** - Returns the `Class<Supplied>` of supplied objects

Suppliers are **reusable** - they can be invoked multiple times to produce values. The behavior depends on the supplier type (fixed value vs. new instance).

### Fixed Object Supplier

A `FixedSupplier<Supplied>` always returns the same object instance. It stores a reference to a pre-existing object and returns it on every `supply()` call.

**Characteristics:**
- Immutable reference to supplied object
- Same instance returned on every call
- Cannot supply null (constructor validates non-null)
- Use for constants, configuration values, singletons

### Null Object Supplier

A `NullSupplier<Supplied>` always returns `Optional.empty()`. It represents the absence of a value in a type-safe manner.

**Characteristics:**
- Always returns empty Optional
- Type-safe null representation
- Useful for optional dependencies
- No state beyond type information

### New Object Supplier

A `NewSupplier<Supplied>` creates a new instance on every `supply()` call using an `IConstructorBinder<Supplied>`. The constructor binder handles parameter injection and instantiation.

**Characteristics:**
- Fresh instance on each call
- Delegates to `IConstructorBinder` for instantiation
- Returns empty Optional if constructor binding fails
- Supports complex constructor parameter injection

### Contextual Object Supplier

An `IContextualSupplier<Supplied, Context>` extends `ISupplier` to require a context object for supply. It has two supply methods:
- **supply()** - Throws exception (context required)
- **supply(Context, Object...)** - Supplies object using provided context

**Characteristics:**
- Requires external context for object resolution
- Supports owner context + additional contexts
- Type-safe context parameter
- Useful for DI container integration

### Contextual Implementations

**ContextualSupplier**: Delegates to an `IContextualObjectSupply<Supplied, Context>` function to resolve the object from context.

**NewContextualSupplier**: Creates new instances using an `IContextualConstructorBinder<Supplied>` that can access context during construction.

### Nullable Wrapper Suppliers

**NullableSupplier**: Wraps an `ISupplier<Supplied>` and validates null values according to the `allowNull` flag. If `allowNull=false` and the delegate returns empty/null, throws `SupplyException`.

**NullableContextualSupplier**: Same concept but wraps `IContextualSupplier<Supplied, Context>`.

**Purpose**: Enforce null contracts declaratively at the supplier level rather than at call sites.

### Supplier Builder

The `SupplierBuilder<Supplied>` provides a fluent DSL for constructing suppliers with automatic type selection. Based on configuration, it automatically creates the appropriate supplier implementation:

- **withValue()** → `FixedSupplier`
- **withConstructor()** → `NewSupplier`
- **withContext()** → `ContextualSupplier`
- **withConstructor() + contextType** → `NewContextualSupplier`
- **No configuration** → `NullSupplier`

All built suppliers are automatically wrapped in nullable wrappers based on the `nullable()` setting.

**Static Factory Methods:**
- `fixed(Class<T>, T)` - Fixed value supplier
- `newObject(Class<T>, IConstructorBinder<T>)` - New instance supplier
- `nullObject(Class<T>)` - Null supplier
- `contextual(Class<T>, Class<C>, IContextualObjectSupply<T,C>)` - Contextual supplier
- `newContextual(Class<T>, Class<C>, IContextualConstructorBinder<T>)` - New contextual instance supplier

## Usage

### 1. Fixed Value Supplier

Supply the same value on every call:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

// Using static factory method
ISupplier<String> supplier = fixed(String.class, "Hello World")
    .build();

String value1 = supplier.supply().get(); // "Hello World"
String value2 = supplier.supply().get(); // Same instance
```

### 2. Null Value Supplier

Represent optional values that are absent:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

ISupplier<String> supplier = nullObject(String.class)
    .build();

Optional<String> result = supplier.supply();
assert result.isEmpty(); // true
```

### 3. New Instance Supplier

Create fresh instances using constructor binding:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;
import com.garganttua.core.reflection.binders.ConstructorBinder;

// Assuming User has constructor User(String name, int age)
IConstructorBinder<User> ctorBinder = ConstructorBinder.of(User.class)
    .withParameter(() -> "Alice")
    .withParameter(() -> 30)
    .build();

ISupplier<User> supplier = newObject(User.class, ctorBinder)
    .build();

User user1 = supplier.supply().get(); // new User("Alice", 30)
User user2 = supplier.supply().get(); // Another new instance
assert user1 != user2; // Different instances
```

### 4. Contextual Supplier

Resolve objects from external context:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

// Define context type
class AppContext {
    public String getMessage() {
        return "Hello from context";
    }
}

// Create contextual supplier
ISupplier<String> supplier = contextual(
    String.class,
    AppContext.class,
    (context, otherContexts) -> Optional.of(context.getMessage())
).build();

// Supply with context
AppContext context = new AppContext();
String message = ((IContextualSupplier<String, AppContext>) supplier)
    .supply(context)
    .get(); // "Hello from context"
```

### 5. New Contextual Instance Supplier

Create instances using context-aware constructor binding:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

// Assuming contextual constructor binder that uses DI context
IContextualConstructorBinder<DatabaseService> ctorBinder = ...;

ISupplier<DatabaseService> supplier = newContextual(
    DatabaseService.class,
    DiContext.class,
    ctorBinder
).build();

// Supply with DI context
DiContext diContext = new DiContext();
DatabaseService service = ((IContextualSupplier<DatabaseService, DiContext>) supplier)
    .supply(diContext)
    .get();
```

### 6. Nullable Validation

Enforce non-null contracts declaratively:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

// Non-nullable supplier - will throw if null
ISupplier<String> nonNull = new SupplierBuilder<>(String.class)
    .withValue("Valid")
    .nullable(false)  // Enforce non-null
    .build();

String value = nonNull.supply().get(); // OK

// Nullable supplier - allows null
ISupplier<String> nullable = new SupplierBuilder<>(String.class)
    .nullable(true)
    .build();

Optional<String> result = nullable.supply(); // Empty Optional, no exception
```

### 7. Builder Pattern - Full Configuration

Use the builder for complex supplier configuration:

```java
IConstructorBinder<Service> ctorBinder = ...;

ISupplier<Service> supplier = new SupplierBuilder<>(Service.class)
    .withConstructor(ctorBinder)
    .nullable(false)
    .build();

Service service = supplier.supply().get();
```

### 8. Integration with Dependency Injection

Supply beans from DI context:

```java
import static com.garganttua.core.supply.dsl.SupplierBuilder.*;

// Contextual supplier that resolves from DI
ISupplier<UserService> supplier = contextual(
    UserService.class,
    IDiContext.class,
    (diContext, others) -> diContext.getBean(UserService.class)
).build();

// Use in runtime
IDiContext diContext = new DiContext();
diContext.registerBean(new UserService());

UserService service = ((IContextualSupplier<UserService, IDiContext>) supplier)
    .supply(diContext)
    .get();
```

### 9. Factory Pattern Implementation

Implement factory with suppliers:

```java
public class UserFactory {
    private ISupplier<User> userSupplier;

    public UserFactory(IConstructorBinder<User> ctorBinder) {
        this.userSupplier = newObject(User.class, ctorBinder).build();
    }

    public User createUser() {
        return userSupplier.supply()
            .orElseThrow(() -> new RuntimeException("Failed to create user"));
    }
}
```

### 10. Supplier Composition

Combine suppliers for complex scenarios:

```java
public class ConfigurableService {
    private ISupplier<String> configSupplier;
    private ISupplier<Logger> loggerSupplier;

    public ConfigurableService(
        ISupplier<String> config,
        ISupplier<Logger> logger) {

        this.configSupplier = config;
        this.loggerSupplier = logger;
    }

    public void initialize() {
        String config = configSupplier.supply().orElse("default");
        Logger logger = loggerSupplier.supply().orElseThrow();

        logger.info("Initialized with config: {}", config);
    }
}
```

### 11. Dynamic Supplier Selection

Choose supplier implementation at runtime:

```java
public ISupplier<Database> createDatabaseSupplier(Environment env) {
    if (env.isProduction()) {
        // Use fixed instance in production (singleton)
        return fixed(Database.class, ProductionDatabase.getInstance())
            .build();
    } else {
        // Create new instance for each test
        IConstructorBinder<Database> binder = ...;
        return newObject(Database.class, binder)
            .build();
    }
}
```

### 12. Error Handling

Handle supply failures gracefully:

```java
ISupplier<Service> supplier = newObject(Service.class, ctorBinder)
    .build();

try {
    Optional<Service> result = supplier.supply();

    if (result.isPresent()) {
        Service service = result.get();
        // Use service
    } else {
        // Handle absence
        log.warn("Service could not be supplied");
    }
} catch (SupplyException e) {
    log.error("Supply failed", e);
    // Handle exception
}
```

## Advanced Patterns

### Custom Supplier Implementation

Create custom suppliers for specialized needs:

```java
public class LazyObjectSupplier<T> implements ISupplier<T> {
    private final Class<T> type;
    private final Supplier<T> factory;
    private volatile T instance;

    public LazyObjectSupplier(Class<T> type, Supplier<T> factory) {
        this.type = type;
        this.factory = factory;
    }

    @Override
    public Optional<T> supply() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = factory.get();
                }
            }
        }
        return Optional.ofNullable(instance);
    }

    @Override
    public Class<T> getSuppliedType() {
        return type;
    }
}
```

### Supplier Chain Pattern

Chain suppliers for fallback behavior:

```java
public class FallbackSupplier<T> implements ISupplier<T> {
    private final ISupplier<T> primary;
    private final ISupplier<T> fallback;

    public FallbackSupplier(
        ISupplier<T> primary,
        ISupplier<T> fallback) {

        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Optional<T> supply() throws SupplyException {
        try {
            Optional<T> result = primary.supply();
            if (result.isPresent()) {
                return result;
            }
        } catch (SupplyException e) {
            // Log and continue to fallback
        }

        return fallback.supply();
    }

    @Override
    public Class<T> getSuppliedType() {
        return primary.getSuppliedType();
    }
}
```

### Supplier Registry Pattern

Manage multiple suppliers with a registry:

```java
public class SupplierRegistry {
    private final Map<String, ISupplier<?>> suppliers = new ConcurrentHashMap<>();

    public <T> void register(String name, ISupplier<T> supplier) {
        suppliers.put(name, supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> ISupplier<T> get(String name, Class<T> type) {
        ISupplier<?> supplier = suppliers.get(name);
        if (supplier != null && type.isAssignableFrom(supplier.getSuppliedType())) {
            return (ISupplier<T>) supplier;
        }
        throw new IllegalArgumentException("No supplier found for: " + name);
    }

    public <T> T supply(String name, Class<T> type) throws SupplyException {
        return get(name, type).supply()
            .orElseThrow(() -> new SupplyException("Failed to supply: " + name));
    }
}
```

## Performance

### Supply Overhead

The framework introduces minimal overhead for object supplying:

- **FixedSupplier**: ~0.1-0.5ns (field access)
- **NullSupplier**: ~0.1-0.5ns (constant return)
- **NewSupplier**: Depends on constructor binding (~1-10µs typically)
- **ContextualSupplier**: Depends on context resolution (~1-50µs typically)
- **NullableWrapper**: ~0.5-2ns additional overhead (null check + delegation)

**Total Framework Overhead**: Negligible (<1µs) for simple suppliers, dominated by constructor/context resolution for complex suppliers.

### Optimization Strategies

1. **Reuse Suppliers** - Create suppliers once, reuse for all invocations
2. **Cache Suppliers** - Store suppliers in final fields or singletons
3. **Fixed Over New** - Prefer `FixedSupplier` for singleton patterns
4. **Avoid Unnecessary Wrapping** - Only use nullable wrapping when needed
5. **Lazy Initialization** - Create suppliers lazily if rarely used
6. **Batch Operations** - Resolve multiple values in single context access
7. **Profile Constructor Bindings** - Optimize expensive constructor parameter resolution
8. **Type Caching** - Cache `Class<?>` objects to avoid repeated lookups

## Tips and Best Practices

### Supplier Design

1. **Type Safety First** - Always use specific generic types, avoid raw `ISupplier`
2. **Meaningful Types** - Use specific classes, not `Object` or overly generic types
3. **Reusability** - Design suppliers to be reusable across multiple invocations
4. **Immutability** - Prefer immutable suppliers that don't change behavior
5. **Single Responsibility** - Each supplier should have one clear supplying strategy

### Builder Usage

6. **Static Imports** - Use static factory methods for concise syntax
7. **Explicit Types** - Always specify `Class<T>` explicitly for type safety
8. **Nullable Declaration** - Explicitly set `nullable(true/false)` for clarity
9. **Builder Reuse** - Don't reuse builder instances; create new for each supplier
10. **Validation** - Validate builder state before `build()` if implementing custom builders

### Contextual Suppliers

11. **Context Types** - Use specific context types, not generic `Object`
12. **Context Validation** - Validate context type compatibility at supply time
13. **Context Lifecycle** - Understand context lifecycle (session, request, singleton)
14. **Context Threading** - Be aware of thread-safety in concurrent contexts
15. **Context Fallback** - Provide fallback for missing context values

### Null Handling

16. **Optional Consistently** - Always return `Optional<T>`, never return null
17. **Nullable Wrappers** - Use nullable wrappers to enforce contracts
18. **Empty vs Exception** - Decide whether absence is normal (empty) or error (exception)
19. **Non-Null Enforcement** - Use `nullable(false)` for required values
20. **Document Nullability** - Clearly document whether supplier can return empty

### Exception Handling

21. **Catch SupplyException** - Always handle `SupplyException` at call sites
22. **Fail Fast** - Throw exceptions early for invalid configurations
23. **Meaningful Messages** - Provide clear exception messages with context
24. **Don't Swallow Exceptions** - Log or rethrow, don't silently fail
25. **Exception Wrapping** - Wrap underlying exceptions in `SupplyException`

### Integration

26. **DI Integration** - Use contextual suppliers for DI container integration
27. **Factory Pattern** - Implement factories using `NewSupplier`
28. **Singleton Pattern** - Use `FixedSupplier` for singletons
29. **Prototype Pattern** - Use `NewSupplier` for prototype beans
30. **Service Locator** - Combine with registry pattern for service location

### Testing

31. **Mock Suppliers** - Create mock suppliers for testing
32. **Fixed Suppliers in Tests** - Use `FixedSupplier` for predictable test values
33. **Test Null Cases** - Test both present and empty Optional cases
34. **Test Exceptions** - Verify exception handling for supply failures
35. **Test Context Validation** - Test contextual suppliers with wrong context types

### Performance

36. **Prefer Fixed** - Use `FixedSupplier` when value doesn't change
37. **Lazy Construction** - Delay supplier creation until first use if expensive
38. **Cache Results** - Cache supplied values if appropriate for use case
39. **Avoid Recreating Suppliers** - Create once, store in fields
40. **Profile Complex Suppliers** - Measure performance of constructor bindings

### Common Pitfalls to Avoid

41. **Don't Create Per Call** - Don't create new suppliers on every supply call
42. **Don't Ignore Empty** - Always check `Optional.isPresent()` or use `orElse()`
43. **Don't Mix Concerns** - Keep supplier logic separate from business logic
44. **Don't Hardcode Values** - Use configuration or parameters, not literals in suppliers
45. **Don't Skip Type Parameters** - Always specify generic type parameters
46. **Don't Assume Non-Null** - Use nullable wrappers instead of assuming non-null
47. **Version Compatibility** - Ensure all Garganttua modules are same version

## License

This module is distributed under the MIT License.
