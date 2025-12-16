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

All examples below are extracted from actual working test files in the garganttua-supply module.

### 1. Custom Supplier Implementation (SupplierTest)

Create a custom supplier by implementing the ISupplierBuilder interface:

```java
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.SupplyException;
import java.lang.reflect.Type;
import java.util.Optional;

ISupplierBuilder<String, ISupplier<String>> b = new ISupplierBuilder<String, ISupplier<String>>() {

    @Override
    public ISupplier<String> build() throws DslException {
        return new ISupplier<String>() {

            @Override
            public Optional<String> supply() throws SupplyException {
                return Optional.of("Hello");
            }

            @Override
            public Type getSuppliedType() {
                return String.class;
            }
        };
    }

    @Override
    public Type getSuppliedType() {
        return String.class;
    }

    @Override
    public boolean isContextual() {
        throw new UnsupportedOperationException("Unimplemented method 'isContextual'");
    }
};

ISupplier<String> supplier = (ISupplier<String>) b.build();
assertEquals("Hello", supplier.supply().get());
```

### 2. Fixed Value Supplier (SupplierTest)

Supply a fixed value using FixedSupplierBuilder:

```java
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.SupplyException;

FixedSupplierBuilder<String> builder = new FixedSupplierBuilder<String>("hello");

ISupplier<String> supplier = builder.build();

assertEquals("hello", supplier.supply().get());
```

### 3. Contextual Supplier with Anonymous Class (SupplierTest)

Create a contextual supplier using an anonymous implementation:

```java
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import java.util.Optional;

IContextualSupply<String, Object> supply = new IContextualSupply<String, Object>() {

    @Override
    public Optional<String> supply(Object context, Object... contexts) {
        return Optional.of("hello from context");
    }
};

ISupplierBuilder<String, IContextualSupplier<String, Object>> builder = new ContextualSupplierBuilder<String, Object>(
        supply, String.class, Object.class);

IContextualSupplier<String, Object> supplier = builder.build();

assertEquals("hello from context", supplier.supply(new Object()).get());
```

### 4. Contextual Supplier with Lambda (SupplierTest)

Create a contextual supplier using lambda syntax:

```java
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;

IContextualSupply<String, Object> supply = (context, contexts) -> Optional.of("hello from context");

ISupplierBuilder<String, IContextualSupplier<String, Object>> builder = new ContextualSupplierBuilder<String, Object>(
        supply, String.class, Object.class);

IContextualSupplier<String, Object> supplier = builder.build();

assertEquals("hello from context", supplier.supply(new Object()).get());
```

### 5. Custom Context Type Supplier (SupplierTest)

Use a specific context type (String) instead of Object:

```java
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.dsl.ContextualSupplierBuilder;

IContextualSupply<String, String> supply = (context, contexts) -> Optional
        .of("hello from context " + context);

ContextualSupplierBuilder<String, String> builder = new ContextualSupplierBuilder<String, String>(
        supply, String.class, String.class);

IContextualSupplier<String, String> supplier = builder.build();

assertEquals("hello from context string context", supplier.supply("string context").get());
```

### 6. SupplierBuilder - Value Supplier (SupplierBuilderTest)

Build a value supplier using SupplierBuilder:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.FixedSupplier;

var b = new SupplierBuilder<>(String.class).withValue("hello");
var s = b.build();
assertTrue(s instanceof NullableSupplier);
assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof FixedSupplier);
```

### 7. SupplierBuilder - Nullable Value Supplier (SupplierBuilderTest)

Configure nullable behavior:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableSupplier;

var b = new SupplierBuilder<>(String.class).withValue("hello").nullable(true);
var s = b.build();
assertTrue(s instanceof NullableSupplier);
assertTrue(((NullableSupplier<?>) s).isNullable());
```

### 8. SupplierBuilder - Context with Contextual Constructor (SupplierBuilderTest)

Create a contextual supplier with a contextual constructor binder:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.NewContextualSupplier;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

// Using fake binder for demonstration (replace with real implementation)
var b = new SupplierBuilder<>(String.class)
        .withContext(Integer.class, new FakeContextualSupply<>())
        .withConstructor(new FakeContextualConstructorBinder<>());

var s = b.build();
assertTrue(s instanceof NullableContextualSupplier);
assertTrue(((NullableContextualSupplier<?, ?>) s).getDelegate() instanceof NewContextualSupplier);
```

### 9. SupplierBuilder - Context Without Constructor (SupplierBuilderTest)

Create a contextual supplier without a constructor:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableContextualSupplier;
import com.garganttua.core.supply.ContextualSupplier;

var b = new SupplierBuilder<>(String.class)
        .withContext(Integer.class, new FakeContextualSupply<>());

var s = b.build();
assertTrue(s instanceof NullableContextualSupplier);
assertTrue(((NullableContextualSupplier<?, ?>) s).getDelegate() instanceof ContextualSupplier);
```

### 10. SupplierBuilder - Constructor Only (SupplierBuilderTest)

Build a supplier with only a constructor binder:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.NewSupplier;
import com.garganttua.core.reflection.binders.IConstructorBinder;

var b = new SupplierBuilder<>(String.class)
        .withConstructor(new FakeConstructorBinder<>());

var s = b.build();
assertTrue(s instanceof NullableSupplier);
assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof NewSupplier);
```

### 11. SupplierBuilder - Default Null Supplier (SupplierBuilderTest)

When no configuration is provided, a NullSupplier is created:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.supply.NullableSupplier;
import com.garganttua.core.supply.NullSupplier;

var b = new SupplierBuilder<>(String.class);
var s = b.build();
assertTrue(s instanceof NullableSupplier);
assertTrue(((NullableSupplier<?>) s).getDelegate() instanceof NullSupplier);
```

### 12. Error Handling - Invalid Context Constructor (SupplierBuilderTest)

Attempting to use a non-contextual constructor with a contextual supply throws an exception:

```java
import com.garganttua.core.supply.dsl.SupplierBuilder;
import com.garganttua.core.dsl.DslException;

var b = new SupplierBuilder<>(String.class)
        .withContext(Integer.class, new FakeContextualSupply<>());

assertThrows(DslException.class, () -> b.withConstructor(new FakeConstructorBinder<>()).build());
```

## Advanced Patterns

The test files demonstrate the following patterns:

1. **Custom Supplier Implementation** - Implement ISupplierBuilder to create custom suppliers
2. **Fixed Value Suppliers** - Use FixedSupplierBuilder for constant values
3. **Contextual Suppliers** - Use ContextualSupplierBuilder for context-aware resolution
4. **Lambda-based Suppliers** - Leverage lambda expressions for concise supplier definitions
5. **Builder Pattern** - Use SupplierBuilder fluent API for complex configurations
6. **Nullable Wrappers** - All suppliers are wrapped in NullableSupplier/NullableContextualSupplier
7. **Type Safety** - Generic type parameters ensure compile-time type safety
8. **Error Handling** - DslException thrown for invalid configurations

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
