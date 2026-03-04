# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Additional per-topic rules are in `.claude/rules/` (module-architecture, dependency-injection, runtime-workflow, testing, java-conventions, design-patterns, antlr-grammar) — they are auto-loaded based on file path patterns.

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

# Build and run the REPL console (separate module from script)
mvn clean package -pl garganttua-console
java -jar garganttua-console/target/garganttua-console-*-executable.jar

# Build native image (in application module)
mvn package -Pnative

# Bump project version (requires xmllint / libxml2-utils)
./new-major.sh   # or new-minor.sh / new-patch.sh

# Regenerate README architecture/dependency sections
python3 scripts/run_all.py
```

## Build Caveats

- The `garganttua-reflection` module has a pre-existing compilation issue (`FieldBinder` missing `ISupplier` import) when building from root. Use `-pl <module>` to build specific modules when the root build fails.
- When modifying `garganttua-script` and testing from `garganttua-workflow`, you must `mvn install -pl garganttua-script -DskipTests` first so the workflow module picks up the updated JAR.
- The shade plugin in `garganttua-script` and `garganttua-console` uses `AppendingTransformer` to merge annotation index files from multiple JARs. This must be updated when adding new `@Indexed` annotations.
- The `garganttua-annotation-processor` module disables annotation processing (`-proc:none`) to avoid self-processing. It is **commented out** of the root reactor `<modules>` — it must be pre-installed separately (`mvn install -pl garganttua-annotation-processor`).

## Architecture Overview

Garganttua Core (`com.garganttua:garganttua-core:2.0.0-ALPHA01`) is a modular Java 21 framework providing dependency injection, reflection utilities, expression language evaluation, scripting, and workflow orchestration. Base package: `com.garganttua.core`. The codebase follows a layered architecture with strict acyclic dependencies.

### Module Layers

1. **Foundation**: `garganttua-commons` (shared interfaces, annotations, exceptions), `garganttua-dsl` (builder framework), `garganttua-supply` (supplier/provider pattern), `garganttua-lifecycle` (state management), `garganttua-mutex` (locking primitives)

2. **Infrastructure**: `garganttua-reflection` (type-safe reflection binders + composite `IReflection` facade), `garganttua-runtime-reflection` (JVM runtime reflection provider), `garganttua-condition` (boolean condition DSL), `garganttua-execution` (chain-of-responsibility), `garganttua-crypto` (cryptographic utilities), `garganttua-configuration` (multi-format config loading & builder population)

3. **Framework**: `garganttua-injection` (DI container), `garganttua-runtime` (workflow engine), `garganttua-mapper` (object mapping), `garganttua-expression` (ANTLR4 expression language), `garganttua-bootstrap` (application bootstrapping)

4. **Application**: `garganttua-script` (scripting engine), `garganttua-console` (interactive REPL, extracted from script), `garganttua-workflow` (high-level workflow DSL with script generation)

5. **Integration**: `garganttua-bindings/` (Spring, Reflections library bindings)

6. **Build Tools**: `garganttua-native`, `garganttua-native-image-maven-plugin` (GraalVM support), `garganttua-annotation-processor` (compile-time annotation indexing — commented out of reactor), `garganttua-script-maven-plugin` (script plugin JAR packaging)

7. **AOT (Work in Progress)**: `garganttua-aot-commons` (shared AOT interfaces), `garganttua-aot/` (parent with submodules: `garganttua-aot-reflection`, `garganttua-aot-annotation-scanner`, `garganttua-aot-annotation-processor`, `garganttua-aot-maven-plugin`)

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

CLI entry point: `com.garganttua.core.script.Main` with flags `--syntax`, `--man`, `--help`, `--version`.

The shade plugin produces an executable fat JAR (`garganttua-script-*-executable.jar`). Linux installer (`-Plinux-installer`) installs as `garganttua-script` and `gs` CLI aliases.

### Console Module (REPL)

The interactive REPL was extracted from `garganttua-script` into its own module `garganttua-console`. Entry point: `com.garganttua.core.console.ConsoleMain`. Uses JLine for terminal support with tab completion for expression functions, session variables, and keywords. History file: `~/.garganttua_script_history`.

Built-in REPL functions: `help()`, `vars()`, `clear()`, `load("file")`, `man()`, `syntax()`, `exit()` / `quit()`.

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

### Reflection Abstraction (`IReflection` Facade)

The reflection subsystem uses a pluggable provider architecture:
- `IReflection` — unified facade combining `IReflectionProvider` (class resolution) and `IAnnotationScanner` (annotation discovery).
- `IReflectionProvider` — pluggable provider with `getClass(Class)`, `forName(...)`, `supports(Class)`. Multiple providers are prioritized (higher priority wins).
- `IClass<T>`, `IMethod`, `IField`, `IConstructor`, `IParameter`, `IRecordComponent` — abstract mirrors of `java.lang.reflect` types, enabling AOT-compatible implementations.
- `ReflectionBuilder.builder()` → `CompositeReflection` — built via `withProvider(provider, priority)` and `withScanner(scanner, priority)`.
- `garganttua-runtime-reflection` provides `RuntimeReflectionProvider` — the standard JVM runtime implementation.
- Old utility classes (`ObjectReflectionHelper`, `FieldAccessManager`, `MethodAccessManager`, `ConstructorAccessManager`, `ObjectAccessor`) are deleted, replaced by this abstraction.

`MethodResolver` finds methods by name/signature/return type. `MethodInvoker` handles execution with nested field traversal for deep object paths. All binders use `ISupplier<?>` for parameter values.

### Configuration Module

`garganttua-configuration` provides multi-format configuration loading and automatic builder population:
- **Formats**: JSON (built-in), YAML, XML, TOML, Properties — format support is conditional on classpath (optional Jackson dataformat dependencies).
- **Sources**: `FileConfigurationSource`, `ClasspathConfigurationSource`, `StringConfigurationSource`, `InputStreamConfigurationSource`, `EnvironmentConfigurationSource`.
- **Builder population**: Recursively maps config keys to builder methods. Auto-detects child `IBuilder`/`ILinkedBuilder` and recurses, calling `up()` when done.
- **Method mapping strategies**: `SMART` (default), `DIRECT`, `CAMEL_CASE`, `KEBAB_CASE`.
- **DI integration**: `ConfigurationPropertyProvider` adapts a parsed config tree as an `IPropertyProvider` (flat dot-notation + `[index]` for arrays).
- **Annotations**: `@Configurable`, `@ConfigProperty("key")`, `@ConfigIgnore`, `@ConfigurationFormat`.
- **Strict/lax modes**: strict mode fails on unknown config keys.

## Code Conventions

- All modules must be thread-safe. Use `Collections.synchronizedMap/List` or concurrent collections for shared state.
- See `.claude/rules/java-conventions.md` for full naming and style conventions.
- Key points: Lombok for boilerplate, `I` prefix for interfaces, Java records for value objects, `Optional<T>` for nullable values, SLF4J logging via `@Slf4j`.
- **`Class<?>` usage is prohibited** — always use `IClass<?>` from `garganttua-commons` instead. Use `IClass.getClass(clazz)` to wrap a raw `Class<?>`.
- Use `FieldAccessor` and `MethodInvoker` from `garganttua-reflection` for field access/method invocation instead of raw `IField.get()/set()` and `IMethod.invoke()`.
- `InjectionContextBuilder` requires `IReflectionBuilder` as a build dependency. Tests must create an `IReflectionBuilder`, build it, then provide it via `.provide(reflectionBuilder)`.

## Cross-Module Concerns

### Identifier Sanitization

Variable names in generated scripts must be valid identifiers (alphanumeric + underscore). When script/stage names contain hyphens or special characters, sanitize them before constructing variable names:
```java
name.replaceAll("[^a-zA-Z0-9_]", "_")
```
Both `ScriptGenerator` and `Workflow.collectVariables()` must use the same sanitization logic for variable name lookup to match.

### Annotation Processor Indexing

`@Expression(name = "foo")` on a static method registers it as `foo(ParamTypes)` in the expression context. Index entries are generated at compile time into `META-INF/garganttua/index/`. New expression functions are auto-discovered when the JAR is rebuilt. The `include()` + `execute_script()` + `script_variable()` pattern is used by the workflow generator for file-based scripts.

## Module Dependencies

All modules depend on `garganttua-commons`. Key dependency chains:
- `injection` → `lifecycle`, `supply`, `dsl`, `reflection`, `reflections`, `native`
- `runtime` → `injection`, `execution`, `condition`
- `expression` → `injection`
- `mutex` → `dsl`, `injection`
- `script` → `expression`, `runtime`, `bootstrap`, `condition`, `mutex`, `annotation-processor`
- `console` → `script`, `expression`, `injection`, `bootstrap`, `annotation-processor`, `mutex`, `reflections`
- `workflow` → `script`, `expression`, `injection`, `dsl` (execution requires both `IInjectionContext` and `IExpressionContext`)
- `reflection` → `commons`, `supply`
- `configuration` → `commons`, `dsl`, `reflection`, `jackson-databind`; `injection` as `provided`
- `runtime-reflection` → `commons`

## CI/CD

Two GitHub Actions workflows in `.github/workflows/`:
- **`maven-publish.yml`**: Builds on any branch push; deploys to GitHub Packages on tag creation.
- **`build-script-installer.yml`**: Builds script installer on pushes/PRs to `main` touching script-related modules. Manual trigger with optional `create_release` input creates a GitHub release tagged `garganttua-script-v{version}`.
