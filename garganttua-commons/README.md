# Garganttua Commons

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

### Dependency Injection Example

```java
// Quick example under construction
```

### Condition Evaluation

```java
// Quick example under construction
```

### Executor Chain Pattern

```java
// Quick example under construction
```

### Object Mapping

```java
// Quick example under construction
```

### Runtime Workflow

```java
// Quick example under construction
```

### Reflection Utilities

```java
// Quick example under construction
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