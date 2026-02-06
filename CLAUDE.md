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

# Regenerate ANTLR4 parser after grammar changes
mvn antlr4:antlr4 -pl garganttua-expression
mvn antlr4:antlr4 -pl garganttua-script

# Build executable script JAR (fat JAR with shade plugin)
mvn clean package -pl garganttua-script

# Build script module with debug logging
mvn clean package -pl garganttua-script -Pdebug

# Build script linux installer distribution
mvn clean package -pl garganttua-script -Plinux-installer

# Run a script file
java -jar garganttua-script/target/garganttua-script-*-executable.jar script.gs [args...]

# Start the REPL console
java -jar garganttua-script/target/garganttua-script-*-executable.jar --console

# Build native image (in application module)
mvn package -Pnative
```

## Architecture Overview

Garganttua Core (`com.garganttua:garganttua-core:2.0.0-ALPHA01`) is a modular Java 21 framework providing dependency injection, reflection utilities, expression language evaluation, scripting, and workflow orchestration. Base package: `com.garganttua.core`. The codebase follows a layered architecture with strict acyclic dependencies.

### Module Layers

1. **Foundation**: `garganttua-commons` (shared interfaces, annotations, exceptions), `garganttua-dsl` (builder framework), `garganttua-supply` (supplier/provider pattern), `garganttua-lifecycle` (state management), `garganttua-mutex` (locking primitives)

2. **Infrastructure**: `garganttua-reflection` (type-safe reflection binders), `garganttua-condition` (boolean condition DSL), `garganttua-execution` (chain-of-responsibility), `garganttua-crypto` (cryptographic utilities)

3. **Framework**: `garganttua-injection` (DI container), `garganttua-runtime` (workflow engine), `garganttua-mapper` (object mapping), `garganttua-expression` (ANTLR4 expression language), `garganttua-bootstrap` (application bootstrapping)

4. **Application**: `garganttua-script` (scripting engine & REPL), `garganttua-workflow` (high-level workflow DSL with script generation)

5. **Integration**: `garganttua-bindings/` (Spring, Reflections library bindings)

6. **Build Tools**: `garganttua-native`, `garganttua-native-image-maven-plugin` (GraalVM support), `garganttua-annotation-processor` (compile-time annotation indexing), `garganttua-script-maven-plugin` (script plugin JAR packaging)

### Key Design Patterns

**Hierarchical Builder Pattern**: All complex objects use fluent builders with `IBuilder<T>` and `ILinkedBuilder<Link, Built>` for navigable parent-child relationships via `up()` method.

**Supplier Pattern**: `ISupplier<T>` provides lazy evaluation throughout. Expressions evaluate to suppliers, enabling deferred computation.

**Binder Pattern** (reflection module): Type-safe wrappers for reflection operations:
- `IConstructorBinder<T>` - object instantiation
- `IMethodBinder<R>` - method invocation (static/instance)
- `IFieldBinder<O,F>` - field access

**Dependency Tracking**: `Dependent` interface declares type dependencies for resolution ordering and circular dependency detection.

### Annotation Processor & Indexing

`garganttua-annotation-processor` is a compile-time annotation processor (`IndexedAnnotationProcessor`) that generates index files in `META-INF/garganttua/index/` for fast annotation discovery at runtime (avoiding expensive classpath scanning). Annotations marked with `@Indexed` (from `garganttua-commons`) are automatically indexed. The processor also indexes standard JSR-330 annotations (`javax.inject.*`, `jakarta.inject.*`). Index entries use the format `C:fully.qualified.ClassName` for classes and `M:ClassName#methodName(ParamTypes)` for methods. The annotation processor is configured globally in the parent POM's `maven-compiler-plugin` alongside Lombok.

**Caveats**: The annotation processor module itself disables annotation processing (`-proc:none`) to avoid self-processing. The shade plugin in `garganttua-script` uses `AppendingTransformer` to merge annotation index files from multiple JARs into the fat JAR — this must be updated when adding new indexed annotations.

### Dependency Injection System

Bean identification uses a reference string format: `[provider::][class][!strategy][#name][@qualifier]`

Key interfaces:
- `IInjectionContext` - central DI hub with lifecycle support
- `IBeanProvider` - bean repository with query methods
- `IBeanFactory<T>` - creates bean instances with matching logic
- `BeanDefinition<T>` - immutable bean metadata (Java record)

Supports singleton/prototype strategies, child contexts, and property injection.

### Expression Language (ANTLR4)

Grammar source at `garganttua-expression/src/main/resources/antlr4/Expression.g4` (the `antlr4-maven-plugin` is configured to use this as its source directory). ANTLR4 parser/lexer classes are auto-generated during build; do not edit generated files in `target/generated-sources/`.

Syntax:
- Function call: `concatenate("a", "b")`
- Method call: `:methodName(arg1, arg2)`
- Constructor: `:(String.class, "value")`
- Variable references: `@myVar` (lazy), `.myVar` (eager), `@0` (positional arguments)
- Types: primitives, `java.lang.String`, generics `List<String>`, arrays `String[]`
- Array/object literals supported

Expression nodes implement `IExpressionNode<R, S extends ISupplier<R>>` - evaluation produces suppliers for lazy computation.

### Script Language

`garganttua-script` provides a scripting engine with its own ANTLR4 grammar (`garganttua-script/src/main/resources/antlr4/Script.g4`) that builds on top of the expression language. Scripts compose expressions into executable workflows with error handling and conditional routing.

Script syntax:
- Variable assignment with execution: `varName <- expression`
- Variable assignment without execution: `varName = expression`
- Exit code association: `statement -> exitCode`
- Immediate exception handling: `statement ! ExceptionType => handler`
- Downstream fallback: `statement * ExceptionType => handler`
- Conditional pipe: `statement | condition => handler`
- Statement groups: `(statement1; statement2)`
- Comments: `//`, `#`, `/* */`

Script files use the `.gs` extension and support shebang lines (`#!/usr/bin/env garganttua-script`). Positional script arguments are accessed via `@0`, `@1`, etc.

CLI entry point: `com.garganttua.core.script.Main` with flags `--console` (REPL), `--syntax`, `--man`, `--help`, `--version`. The REPL (`ScriptConsole`) uses JLine for terminal support with built-in functions: `help()`, `vars()`, `clear()`, `load("file")`, `man()`, `syntax()`, `exit()`.

The shade plugin produces an executable fat JAR (`garganttua-script-*-executable.jar`).

### Workflow Module

`garganttua-workflow` is a high-level orchestration DSL that generates Garganttua Script code from a fluent builder API. It organizes execution into stages containing scripts, with automatic variable collection and result tracking.

Key classes: `WorkflowBuilder` → `WorkflowStageBuilder` → `WorkflowScriptBuilder`. `ScriptGenerator` converts the builder definitions into script source code. `Workflow` executes the pre-generated scripts.

### Script Maven Plugin

`garganttua-script-maven-plugin` packages JARs for dynamic inclusion in scripts via `include("path/to/plugin.jar")`. It scans for annotated classes and writes discovered packages to the `Garganttua-Packages` manifest attribute.

### Runtime Workflow Engine

Orchestrates multi-stage workflows with annotation or programmatic definition:
- `@RuntimeDefinition` - declares input/output types
- `@Step` / `@Steps` - workflow structure
- `@Input`, `@Output`, `@Context`, `@Variable` - parameter injection
- `@Catch`, `@FallBack` - exception handling

### Reflection Utilities

`MethodResolver` finds methods by name/signature/return type. `MethodInvoker` handles execution with nested field traversal for deep object paths. All binders use `ISupplier<?>` for parameter values.

## Code Conventions

- Uses Lombok for boilerplate reduction (`@Getter`, `@Setter`, `@Builder`, etc.)
- Interfaces prefixed with `I` (e.g., `IBuilder`, `ISupplier`, `IBeanProvider`)
- Java records for immutable value objects (`BeanDefinition`, `BeanReference`)
- `Optional<T>` for nullable/conditional values
- Thread-safe collections via `Collections.synchronizedMap/List`
- SLF4J for logging (`@Slf4j` Lombok annotation)

## Module Dependencies

All modules depend on `garganttua-commons`. Key dependency chains:
- `injection` → `lifecycle`, `supply`, `dsl`, `reflection`, `reflections`, `native`
- `runtime` → `injection`, `execution`, `condition`
- `expression` → `injection`
- `mutex` → `dsl`, `injection`
- `script` → `expression`, `runtime`, `bootstrap`, `condition`, `mutex`, `annotation-processor`
- `workflow` → `script`, `expression`, `injection`, `dsl` (execution requires both `IInjectionContext` and `IExpressionContext`)
- `reflection` → `commons`, `supply`
