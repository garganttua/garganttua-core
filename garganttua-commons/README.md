# 🛠️ Garganttua Commons

## Description

The Garganttua Commons module is the foundational library of the Garganttua Core ecosystem. It provides essential building blocks, contracts, and utilities used across all other modules. With over 100 files organized in 13 specialized packages, it establishes the core architecture for dependency injection, workflow orchestration, reflection utilities, and common patterns.

This module serves as the central dependency for the entire framework, offering:
- **Dependency Injection** framework with context management and bean lifecycle
- **Runtime workflow** engine with stages, steps, and exception handling
- **Reflection utilities** for introspection and dynamic binding
- **Common patterns** including builders, conditions, executors, and mappers
- **Shared annotations** for declarative configuration
- **Exception hierarchy** for consistent error handling

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-commons</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `javax.inject:javax.inject`
 - `jakarta.annotation:jakarta.annotation-api`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Architecture Overview

Garganttua Commons is organized around three major subsystems:

1. **Dependency Injection System** - Complete IoC container with context management, bean factories, lifecycle hooks, and annotation-based configuration
2. **Runtime Workflow Engine** - Sophisticated orchestration framework for defining and executing multi-stage workflows with conditions, exception handling, and fallback mechanisms
3. **Reflection Framework** - Comprehensive utilities for introspection, annotation scanning, and dynamic field/method/constructor binding

### Cross-Cutting Patterns

The module implements several design patterns consistently across packages:

- **Builder Pattern** - Fluent DSL interfaces (`IBuilder`, `ILinkedBuilder`) for constructing complex objects
- **Chain of Responsibility** - Executor chains for sequential processing with fallback support
- **Strategy Pattern** - Pluggable implementations for conditions, suppliers, and mappers
- **Observer Pattern** - Context builders with observer support for lifecycle events
- **Template Method** - Lifecycle interfaces defining common initialization/shutdown sequences

### Key Design Principles

- **Interface Segregation** - Small, focused interfaces with single responsibilities
- **Contextual Variants** - Base interfaces with contextual versions for dependency injection scenarios
- **Exception Consistency** - Each package defines its own exception extending a common base
- **Annotation-Driven** - Declarative configuration through annotations alongside programmatic APIs
- **Immutable Records** - Use of Java records for data transfer objects (e.g., `BeanDefinition`, `RuntimeExceptionRecord`)

## Packages Overview

### Core Framework Packages

**condition** - Condition evaluation framework
- `ICondition` - Functional interface for boolean condition evaluation
- `ConditionException` - Exception for condition failures
- `dsl.IConditionBuilder` - Builder DSL for constructing conditions

**dsl** - Domain-Specific Language builder framework
- `IBuilder` - Base builder interface with `build()` method
- `ILinkedBuilder` - Builder with navigation (`up()`, `setUp()`) for hierarchical DSLs
- `IAutomaticBuilder` / `IAutomaticLinkedBuilder` - Automatic building capabilities
- `DslException` - Exception for DSL construction errors

**execution** - Executor chain pattern for sequential processing
- `IExecutor<T>` - Functional interface for executing operations in a chain
- `IExecutorChain<T>` - Manages the chain of executors with FIFO ordering
- `IFallBackExecutor<T>` - Fallback handlers for error recovery
- `ExecutorException` - Exception thrown during execution

**lifecycle** - Component lifecycle management
- `ILifecycle` - Interface defining lifecycle hooks: `onStart()`, `onStop()`, `onInit()`, `onReload()`, `onFlush()`
- `LifecycleException` - Exception for lifecycle errors

**supply** - Lazy object supplier framework
- `ISupplier<T>` - Generic supplier with `supply()` and `getSuppliedType()` methods
- `IContextualSupplier<T>` - Context-aware supplier variant
- `Supplier<T>` - Functional interface for object provision
- `SupplyException` - Exception for supply failures
- `dsl.ISupplierBuilder` / `dsl.ISupplierBuilder` - Builders for suppliers

**utils** - Common utility classes
- `OrderedMap<K,V>` - LinkedHashMap with positional insertion (BEFORE/AFTER specific keys)
- `OrderedMapPosition` - Record for positioning elements
- `Copyable` - Interface for copyable objects with `copy()` method
- `CopyException` - Exception for copy operations

### Specialized Framework Packages

**injection** - Comprehensive dependency injection framework (35 files)

Core Interfaces:
- `IDiContext` - Central DI context managing beans, properties, and lifecycle
- `IBeanFactory` - Factory for creating bean instances
- `IBeanQuery` / `IBeanQueryBuilder` - Query interface for finding beans with filters
- `IPropertyProvider` / `IPropertySupplier` - Property injection support
- `IInjectableElementResolver` - Resolves injectable elements (fields, constructors, methods)
- `IDiChildContextFactory` - Factory for creating child contexts

Key Types:
- `BeanDefinition` - Record defining bean metadata (type, strategy, name, qualifiers, binders)
- `BeanStrategy` - Enum for bean scopes: `singleton` vs `prototype`
- `NotResolvedAction` - Enum for handling unresolved dependencies

Annotations:
- `@Property` - Injects property values
- `@Provider` - Marks provider methods
- `@Prototype` - Prototype-scoped beans
- `@Fixed` - Injects fixed primitive values
- `@Null` - Injects null values

DSL Builders (in `injection.context.dsl`):
- `IDiContextBuilder` - Builds DI contexts
- `IBeanProviderBuilder` / `IBeanSupplierBuilder` - Builds bean providers
- `IPropertyBuilder` / `IPropertyProviderBuilder` - Builds properties
- `IBeanConstructorBinderBuilder` - Binds bean constructors
- `IContextBuilderObserver` - Observer for context building events

**mapper** - Object-to-object mapping framework (9 files)
- `IMapper<S,T>` - Maps between source and target objects
- `MapperConfiguration` - Configuration flags: `FAIL_ON_ERROR`, `DO_VALIDATION`
- `MappingRule` - Rules for field-level mapping
- `MappingDirection` - Enum: `REGULAR` or `REVERSE` mapping
- `IMappingRuleExecutor` - Executes mapping rules
- `@ObjectMappingRule` - Class-level mapping annotation
- `@FieldMappingRule` - Field-level mapping annotation
- `MapperException` - Exception for mapping errors

**reflection** - Reflection utilities and binding framework (19 files)

Core Classes:
- `ObjectAddress` - Parses and navigates dot-notation paths (e.g., `"object.field.nested"`) with loop detection
- `IAnnotationScanner` - Finds classes with specific annotations in packages
- `IObjectQuery` - Queries objects using reflection
- `ReflectionException` - Exception for reflection errors

Binders:
- `IFieldBinder` / `IContextualFieldBinder` - Binds and retrieves field values
- `IMethodBinder` / `IContextualMethodBinder` - Binds and invokes methods
- `IConstructorBinder` / `IContextualConstructorBinder` - Binds constructor invocation
- `IExecutableBinder` / `IContextualExecutableBinder` - Base interface for executable binding
- `Dependent` - Interface marking dependent elements

DSL Builders (in `reflection.binders.dsl`):
- `IFieldBinderBuilder` - Builds field binders
- `IMethodBinderBuilder` - Builds method binders
- `IConstructorBinderBuilder` - Builds constructor binders
- `IExecutableBinderBuilder` - Base builder for executables

**runtime** - Complex workflow execution framework (44 files)

Core Interfaces:
- `IRuntime<I,O>` - Base runtime with `execute()` methods supporting UUID tracking
- `IRuntimes` - Manager for multiple runtimes
- `IRuntimeResult<O>` - Execution result with duration, output, and exception tracking
- `IRuntimeContext` - Execution context for runtime workflows
- `IRuntimeExecutor` - Executes runtime steps
- `IEventRuntime` / `IDomainRuntime` - Specialized runtime types
- `IEvent` - Event definition interface

Workflow Components:
- `IRuntimeStage` - Stage within a runtime workflow
- `IRuntimeStep` - Step within a stage
- `IRuntimeStepCatch` - Catch clause for exception handling
- `IRuntimeStepOnException` - Exception handler definition
- `IRuntimeStepMethodBinder` / `IRuntimeStepFallbackBinder` - Bind methods to steps

Position Enums:
- `Position` - `BEFORE` / `AFTER` positioning
- `RuntimeStagePosition` - Stage positioning in workflow
- `RuntimeStepPosition` - Step positioning in stage
- `RuntimeStepOperationPosition` - Operation positioning in step

Annotations (in `runtime.annotations` - 16 total):
- `@RuntimeDefinition` - Marks runtime definitions with input/output types
- `@Stages` / `@Stage` - Define workflow stages
- `@Step` - Step within stage
- `@Operation` - Operations on steps
- `@Code` - Code snippet in operation
- `@Input` / `@Output` - Input/output parameters
- `@Variable` / `@Variables` - Variable definitions
- `@Context` - Context injection
- `@Condition` - Conditional execution
- `@Catch` - Exception handling
- `@OnException` - Exception handler
- `@FallBack` - Fallback mechanism
- `@Exception` / `@ExceptionMessage` - Exception metadata

DSL Builders (in `runtime.dsl`):
- `IRuntimeBuilder` - Builds runtime definitions
- `IRuntimesBuilder` - Builds multiple runtimes
- `IRuntimeStageBuilder` - Builds stages
- `IRuntimeStepBuilder` - Builds steps
- `IRuntimeStepCatchBuilder` - Builds catch clauses
- `IRuntimeStepOnExceptionBuilder` - Builds exception handlers

Exception Classes:
- `RuntimeException` - Base exception for runtime errors
- `RuntimeExceptionRecord` - Records exception information

### Placeholder Packages

**binding** - Reserved for future binding utilities

**crypto** - Reserved for cryptographic operations and utilities

**native** - Reserved for GraalVM native image support and metadata

## Usage

### OrderedMap - Positional Map Operations

The `OrderedMap` class provides a LinkedHashMap with the ability to insert elements at specific positions relative to existing keys.

```java
import com.garganttua.core.utils.OrderedMap;
import com.garganttua.core.runtime.Position;

// Create an ordered map
OrderedMap<String, String> map = new OrderedMap<>();

// Basic put operations
map.put("a", "Alpha");
map.put("c", "Charlie");

// Insert "b" BEFORE "c"
map.putAt("b", "Bravo", "c", Position.BEFORE);
// Result order: a, b, c

// Insert "d" AFTER "a"
map.putAt("d", "Delta", "a", Position.AFTER);
// Result order: a, d, b, c

// If reference key doesn't exist, element is added at the end
map.putAt("e", "Echo", "nonexistent", Position.BEFORE);
// Result order: a, d, b, c, e

// Duplicate keys throw IllegalArgumentException
// map.put("a", "Another Alpha"); // Throws: "Key already exists: a"

// Convert to standard Map
Map<String, String> standardMap = map.asMap();
```

### BeanReference - Bean Lookup DSL

`BeanReference` provides a powerful DSL for parsing and matching beans in the dependency injection system.

```java
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;

// Parse bean reference by fully qualified class name
var ref1 = BeanReference.parse("java.lang.String");
// Type: String.class

// With strategy specification
var ref2 = BeanReference.parse("java.lang.String!singleton");
// Type: String.class, Strategy: singleton

// With named bean
var ref3 = BeanReference.parse("java.lang.String#main");
// Type: String.class, Name: "main"

// With provider prefix
var ref4 = BeanReference.parse("local::#Mail");
// Provider: "local", Name: "Mail"

// Full syntax: provider::class!strategy#name@qualifier
var ref5 = BeanReference.parse("local::java.lang.String!prototype#bean1");
// Provider: "local", Type: String.class, Strategy: prototype, Name: "bean1"

// With qualifiers (using fully qualified annotation class names)
var ref6 = BeanReference.parse("java.lang.String@com.example.Q1@com.example.Q2");
// Type: String.class, Qualifiers: [Q1.class, Q2.class]

// Check if references match
BeanReference<String> ref7 = new BeanReference<>(
    String.class,
    Optional.of(BeanStrategy.singleton),
    Optional.of("myBean"),
    Set.of()
);
BeanReference<String> ref8 = new BeanReference<>(
    String.class,
    Optional.of(BeanStrategy.singleton),
    Optional.of("myBean"),
    Set.of()
);
assertTrue(ref7.matches(ref8)); // true - same type, strategy, name
```

### BeanDefinition - Bean Metadata

`BeanDefinition` encapsulates complete bean metadata including reference, supplier, binders, and dependencies.

```java
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;

// Create bean reference
BeanReference<MyBean> ref = new BeanReference<>(
    MyBean.class,
    Optional.of(BeanStrategy.singleton),
    Optional.of("myBean"),
    Set.of(TestQualifier.class)
);

// Create bean definition
BeanDefinition<MyBean> def = new BeanDefinition<>(
    ref,                    // Bean reference
    Optional.empty(),       // Optional supplier
    Set.of(),              // Field binders
    Set.of()               // Method binders
);

// Get effective name (uses explicit name or falls back to simple class name)
String name = def.reference().effectiveName(); // "myBean"

// Bean definitions with same reference are equal
BeanReference<MyBean> ref2 = new BeanReference<>(
    MyBean.class,
    Optional.of(BeanStrategy.singleton),
    Optional.of("myBean"),
    Set.of(TestQualifier.class)
);
BeanDefinition<MyBean> def2 = new BeanDefinition<>(ref2, Optional.empty(), Set.of(), Set.of());
assertTrue(def.equals(def2)); // true
```

### Supplier Framework - Contextual Object Supply

The Supplier framework provides utilities for supplying objects with optional context awareness and recursive resolution.

```java
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.Supplier;

// Simple non-contextual supplier
class SimpleStringSupplier implements ISupplier<String> {
    private final String value;

    public SimpleStringSupplier(String value) {
        this.value = value;
    }

    @Override
    public Optional<String> supply() throws SupplyException {
        return Optional.ofNullable(value);
    }

    @Override
    public Type getSuppliedType() {
        return String.class;
    }
}

// Use non-contextual supplier
ISupplier<String> supplier = new SimpleStringSupplier("Hello World");
String result = Supplier.contextualSupply(supplier);
// result: "Hello World"

// Contextual supplier that requires specific context type
class ContextualStringSupplier implements IContextualSupplier<String, String> {
    private final String prefix;

    public ContextualStringSupplier(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Optional<String> supply(String context, Object... otherContexts) throws SupplyException {
        return Optional.of(prefix + context);
    }

    @Override
    public Class<String> getOwnerContextType() {
        return String.class;
    }

    @Override
    public Type getSuppliedType() {
        return String.class;
    }
}

// Use contextual supplier with matching context
IContextualSupplier<String, String> contextualSupplier = new ContextualStringSupplier("Hello, ");
String result2 = Supplier.contextualSupply(contextualSupplier, "World");
// result2: "Hello, World"

// Contextual supplier with multiple contexts (finds matching one)
IContextualSupplier<Integer, Integer> intSupplier = new ContextualIntegerSupplier(10);
Integer result3 = Supplier.contextualSupply(intSupplier, "ignored", 5, "also ignored");
// result3: 50 (5 * 10)

// Nested suppliers - recursive resolution
class NestedSupplier implements ISupplier<ISupplier<String>> {
    private final ISupplier<String> innerSupplier;

    public NestedSupplier(ISupplier<String> innerSupplier) {
        this.innerSupplier = innerSupplier;
    }

    @Override
    public Optional<ISupplier<String>> supply() throws SupplyException {
        return Optional.ofNullable(innerSupplier);
    }

    @Override
    public Type getSuppliedType() {
        return ISupplier.class;
    }
}

// Recursive supply resolves nested suppliers automatically
ISupplier<String> innerSupplier = new SimpleStringSupplier("Nested value");
ISupplier<ISupplier<String>> outerSupplier = new NestedSupplier(innerSupplier);
String result4 = (String) Supplier.contextualRecursiveSupply(outerSupplier);
// result4: "Nested value" (resolved through 2 levels)

// Void context supplier (no context needed)
class VoidContextSupplier implements IContextualSupplier<String, Void> {
    @Override
    public Optional<String> supply(Void context, Object... otherContexts) throws SupplyException {
        return Optional.of("No context needed");
    }

    @Override
    public Class<Void> getOwnerContextType() {
        return Void.class;
    }

    @Override
    public Type getSuppliedType() {
        return String.class;
    }
}

IContextualSupplier<String, Void> voidSupplier = new VoidContextSupplier();
String result5 = Supplier.contextualSupply(voidSupplier);
// result5: "No context needed"
```

## Tips and best practices

### Dependency Injection
1. **Prefer singleton scope** for stateless services to reduce memory overhead
2. **Use prototype scope** for stateful objects that need per-request isolation
3. **Leverage property injection** for configuration values using `@Property`
4. **Define clear bean qualifiers** when multiple implementations of the same interface exist
5. **Use child contexts** for request-scoped or module-scoped bean isolation

### Runtime Workflows
1. **Design stages logically** - Group related steps into stages for better organization
2. **Add fallback handlers** to critical steps that may fail but have recovery strategies
3. **Use conditions** to create dynamic workflows that adapt based on input
4. **Leverage annotations** for declarative workflow definitions when possible
5. **Track execution with UUIDs** for debugging and monitoring distributed workflows

### Reflection and Binding
1. **Cache binders** when repeatedly invoking the same methods/fields to avoid reflection overhead
2. **Use contextual binders** when working within a DI context for automatic dependency resolution
3. **Validate ObjectAddress paths** early to fail fast on invalid navigation
4. **Prefer method binders** over direct reflection for type safety and error handling

### General Best Practices
1. **Extend common exceptions** - Create package-specific exceptions extending the base hierarchy
2. **Follow the builder pattern** - Use `IBuilder` and `ILinkedBuilder` for complex object construction
3. **Implement ILifecycle** for components that need startup/shutdown hooks
4. **Use OrderedMap** when insertion order and positioning matter
5. **Make objects Copyable** when they need to be cloned for isolation
6. **Leverage annotations** for metadata and configuration to keep code clean
7. **Choose contextual variants** when working with DI-managed components
8. **Document custom DSLs** thoroughly as they define domain-specific APIs
9. **Reuse enums and interfaces** to maintain modular interoperability across modules
10. **When creating new DSLs**, extend the commons DSL package to maintain standard patterns

## License
This module is distributed under the MIT License.