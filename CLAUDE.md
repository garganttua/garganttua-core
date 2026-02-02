# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl garganttua-injection

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ExecutorChainTest

# Run tests in a specific module
mvn test -pl garganttua-expression

# Run tests with coverage
mvn clean test jacoco:report

# Build native image (in application module)
mvn package -Pnative
```

## Architecture Overview

Garganttua Core is a modular Java 21 framework providing dependency injection, reflection utilities, expression language evaluation, and workflow orchestration. The codebase follows a layered architecture with strict acyclic dependencies.

### Module Layers

1. **Foundation**: `garganttua-commons` (shared interfaces, annotations, exceptions), `garganttua-dsl` (builder framework), `garganttua-supply` (supplier/provider pattern), `garganttua-lifecycle` (state management), `garganttua-mutex` (locking primitives)

2. **Infrastructure**: `garganttua-reflection` (type-safe reflection binders), `garganttua-condition` (boolean condition DSL), `garganttua-execution` (chain-of-responsibility), `garganttua-crypto` (cryptographic utilities)

3. **Framework**: `garganttua-injection` (DI container), `garganttua-runtime` (workflow engine), `garganttua-mapper` (object mapping), `garganttua-expression` (ANTLR4 expression language), `garganttua-bootstrap` (application bootstrapping)

4. **Integration**: `garganttua-bindings/` (Spring, Reflections library bindings)

5. **Build Tools**: `garganttua-native`, `garganttua-native-image-maven-plugin` (GraalVM support)

### Key Design Patterns

**Hierarchical Builder Pattern**: All complex objects use fluent builders with `IBuilder<T>` and `ILinkedBuilder<Link, Built>` for navigable parent-child relationships via `up()` method.

**Supplier Pattern**: `ISupplier<T>` provides lazy evaluation throughout. Expressions evaluate to suppliers, enabling deferred computation.

**Binder Pattern** (reflection module): Type-safe wrappers for reflection operations:
- `IConstructorBinder<T>` - object instantiation
- `IMethodBinder<R>` - method invocation (static/instance)
- `IFieldBinder<O,F>` - field access

**Dependency Tracking**: `Dependent` interface declares type dependencies for resolution ordering and circular dependency detection.

### Dependency Injection System

Bean identification uses a reference string format: `[provider::][class][!strategy][#name][@qualifier]`

Key interfaces:
- `IInjectionContext` - central DI hub with lifecycle support
- `IBeanProvider` - bean repository with query methods
- `IBeanFactory<T>` - creates bean instances with matching logic
- `BeanDefinition<T>` - immutable bean metadata (Java record)

Supports singleton/prototype strategies, child contexts, and property injection.

### Expression Language (ANTLR4)

Grammar at `garganttua-expression/src/main/antlr4/com/garganttua/core/expression/parser/Expression.g4`. ANTLR4 parser/lexer classes are auto-generated during build via the `antlr4-maven-plugin`; do not edit generated files in `target/generated-sources/`.

Syntax examples:
- Function call: `concatenate("a", "b")`
- Method call: `:methodName(arg1, arg2)`
- Constructor: `:(String.class, "value")`
- Types: primitives, `java.lang.String`, generics `List<String>`, arrays `String[]`

Expression nodes implement `IExpressionNode<R, S extends ISupplier<R>>` - evaluation produces suppliers for lazy computation.

### Runtime Workflow Engine

Orchestrates multi-stage workflows with annotation or programmatic definition:
- `@RuntimeDefinition` - declares input/output types
- `@Stage` / `@Step` - workflow structure
- `@Input`, `@Output`, `@Context`, `@Variable` - parameter injection
- `@Catch`, `@FallBack` - exception handling

### Reflection Utilities

`MethodResolver` finds methods by name/signature/return type. `MethodInvoker` handles execution with nested field traversal for deep object paths. All binders use `ISupplier<?>` for parameter values.

## Code Conventions

- Uses Lombok for boilerplate reduction (`@Getter`, `@Setter`, `@Builder`, etc.)
- Java records for immutable value objects (`BeanDefinition`, `BeanReference`)
- `Optional<T>` for nullable/conditional values
- Thread-safe collections via `Collections.synchronizedMap/List`
- SLF4J for logging

## Module Dependencies

All modules depend on `garganttua-commons`. Key dependency chains:
- `injection` → `lifecycle`, `supply`, `dsl`, `reflection`, `reflections`, `native`
- `runtime` → `injection`, `execution`, `condition`
- `expression` → `injection`
- `reflection` → `commons`, `supply`
