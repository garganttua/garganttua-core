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

- When modifying `garganttua-script` and testing from `garganttua-workflow`, you must `mvn install -pl garganttua-script -DskipTests` first so the workflow module picks up the updated JAR.
- The shade plugin in `garganttua-script` and `garganttua-console` uses `AppendingTransformer` to merge annotation index files from multiple JARs. This must be updated when adding new `@Indexed` annotations.
- The `garganttua-annotation-processor` module disables annotation processing (`-proc:none`) to avoid self-processing. It is **commented out** of the root reactor `<modules>` — it must be pre-installed separately (`mvn install -pl garganttua-annotation-processor`).

## Architecture Overview

Garganttua Core (`com.garganttua:garganttua-core:2.0.0-ALPHA03`) is a modular Java 25 framework providing dependency injection, reflection utilities, expression language evaluation, scripting, and workflow orchestration. Base package: `com.garganttua.core`. The codebase follows a layered architecture with strict acyclic dependencies.

### Module Layers

1. **Foundation**: `garganttua-commons` (shared interfaces, annotations, exceptions), `garganttua-dsl` (builder framework), `garganttua-supply` (supplier/provider pattern), `garganttua-lifecycle` (state management), `garganttua-mutex` (locking primitives)

2. **Infrastructure**: `garganttua-reflection` (type-safe reflection binders + composite `IReflection` facade), `garganttua-runtime-reflection` (JVM runtime reflection provider), `garganttua-condition` (boolean condition DSL), `garganttua-execution` (chain-of-responsibility), `garganttua-crypto` (cryptographic utilities), `garganttua-configuration` (multi-format config loading & builder population)

3. **Framework**: `garganttua-injection` (DI container with `@BeanProviderAnnotation` / `@PropertyProviderAnnotation` auto-detection), `garganttua-runtime` (workflow engine), `garganttua-mapper` (object mapping with per-source rules), `garganttua-expression` (ANTLR4 expression language), `garganttua-bootstrap` (application bootstrapping), `garganttua-properties` (`.properties` file provider with `${VAR:default}` placeholders), `garganttua-observability` (sealed observer primitives + `:observe(...)` script expression)

4. **Application**: `garganttua-script` (scripting engine — script runtime construction delegated to `RuntimesBuilder`), `garganttua-console` (interactive REPL, extracted from script), `garganttua-workflow` (high-level workflow DSL with script generation and observability timing integration)

5. **Integration**: `garganttua-bindings/` (Spring, Reflections library bindings)

6. **Build Tools**: `garganttua-native-image-maven-plugin` (GraalVM support), `garganttua-annotation-processor` (compile-time annotation indexing — commented out of reactor), `garganttua-script-maven-plugin` (script plugin JAR packaging)

7. **AOT (Work in Progress)**: `garganttua-aot/` (parent) with submodules `garganttua-aot-commons` (shared AOT interfaces), `garganttua-aot-reflection` (pre-generated `IClass<T>` descriptors), `garganttua-aot-annotation-scanner`, `garganttua-aot-annotation-processor` (compile-time code generator for direct binders + class descriptors), `garganttua-aot-maven-plugin`. The annotation processor was recently refactored into per-member generators (`AOTConstructorSourceGenerator`, `AOTFieldSourceGenerator`, `AOTMethodSourceGenerator`, `AOTNaming`, `TypeNames`, `MemberInclusion`).

### Key Design Patterns

**Hierarchical Builder Pattern**: All complex objects use fluent builders with `IBuilder<T>` and `ILinkedBuilder<Link, Built>` for navigable parent-child relationships via `up()` method.

**Supplier Pattern**: `ISupplier<T>` provides lazy evaluation throughout. Expressions evaluate to suppliers, enabling deferred computation.

**Binder Pattern** (reflection module): Type-safe wrappers for reflection operations:
- `IConstructorBinder<T>` - object instantiation
- `IMethodBinder<R>` - method invocation (static/instance)
- `IFieldBinder<O,F>` - field access

**Dependency Tracking**: `Dependent` interface declares type dependencies for resolution ordering and circular dependency detection.

### Bootstrap SPI (ServiceLoader cold start)

`Bootstrap.builder().autoDetect(true).build()` is usable on a cold JVM with zero manual wiring as long as at least one `IReflectionProvider` JAR is on the classpath (typically `garganttua-runtime-reflection` for JVM mode or `garganttua-aot-reflection` for native).

**How it works:**
- Provider modules ship `META-INF/services/com.garganttua.core.reflection.IReflectionProvider` (and `...IAnnotationScanner`) descriptors. Currently shipped:
  - `garganttua-runtime-reflection` → `RuntimeReflectionProvider` (`@Priority(10)`)
  - `garganttua-aot-reflection` → `AOTReflectionProvider` (`@Priority(20)`)
  - `garganttua-bindings/garganttua-reflections` → `ReflectionsAnnotationScanner` (`@Priority(10)`)
  - `garganttua-aot-annotation-scanner` → `AOTAnnotationScanner` (`@Priority(20)`)
- `Bootstrap`'s constructor invokes `ServiceLoader.load(...)` for both interfaces, sorts results by `jakarta.annotation.Priority` (higher wins, default 0 when absent), builds a `ReflectionBuilder`, and installs the result via `IClass.setReflection()`.
- This is **only the cold-start fallback** — when a user explicitly calls `.provide(reflectionBuilder)`, the user's builder takes over the bootstrap's dep chain. Opt-out entirely with `bootstrap.disableSpiFallback()` (useful for tests).

**Native-image notes:**
- GraalVM auto-handles `ServiceLoader.load(X.class)` calls and includes `META-INF/services/*` natively.
- Reading `@Priority` via `Class.getAnnotation()` requires the provider's annotations to be preserved — usually automatic with ServiceLoader detection, but verify in your `reflect-config.json` if priority falls back to 0 unexpectedly.
- **Shade caveat (critical for fat JARs and native):** every shade-plugin config that produces an executable JAR MUST include `<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>`. Without it the META-INF/services files from each shaded JAR overwrite each other and the SPI silently sees no providers. Already enforced in `garganttua-script` and `garganttua-console`.

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

**Provider auto-detection** (added 2026-04-16): `@BeanProviderAnnotation("scope")` and `@PropertyProviderAnnotation("scope")` mark `IBeanProviderBuilder` / `IPropertyProviderBuilder` implementations. Both annotations are `@Indexed` and `@Reflected` so they're discovered at compile time. The `InjectionContextBuilder` auto-detects annotated classes during `doAutoDetectionWithDependency` and registers them under the declared scope.

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
- Statement groups: `(statement1; statement2)` — groups provide function scope isolation (functions defined inside do not leak to outer scope)
- User-defined functions: `myFunc = (param1, param2) => (body)` — parameters are scoped and restored after invocation
- Conditional execution: `if(condition, thenBlock)` or `if(condition, thenBlock, elseBlock)` — supports statement blocks for lazy evaluation
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

The generator uses `if()` blocks for conditional execution and wraps inline scripts in `(...)` statement groups for function scope isolation, preventing name collisions between stages.

### Script Maven Plugin

`garganttua-script-maven-plugin` packages JARs for dynamic inclusion in scripts via `include("path/to/plugin.jar")`. It scans for annotated classes and writes discovered packages to the `Garganttua-Packages` manifest attribute.

### Runtime Workflow Engine

Orchestrates multi-stage workflows with annotation or programmatic definition:
- `@RuntimeDefinition` - declares input/output types
- `@Step` / `@Steps` - workflow structure
- `@Input`, `@Output`, `@Context`, `@Variable` - parameter injection
- `@Catch`, `@FallBack` - exception handling

**`RuntimeExpressionContext` ThreadLocal** — used by step binders to pass the current `IRuntimeContext` to expressions during evaluation. Use `push(ctx)` / `pop(previous)` (not `set` / `clear`) when invoking nested step execution so the outer context survives. `RuntimeStepMethodBinder`, `RuntimeStepFallbackBinder`, and `CatchAwareExpression` all follow this pattern (fixed 2026-05-20). Lookup sites: `SubRuntimeExpression`, `MethodBinderExpression`, `RuntimeFunctions`, `ScriptVariableResolver`.

### Mapper Per-Source Rules

Both `@FieldMappingRule` and `@ObjectMappingRule` carry a `source()` attribute (default `void.class` = wildcard) and are `@Repeatable` (via `@FieldMappingRules` / `@ObjectMappingRules` container annotations). A single DTO can therefore be mapped from multiple source classes with distinct field paths or converter methods. `MappingRules.parse(source, destination)` picks the best matching rule per field via exact match > most-specific assignable > wildcard, throwing on duplicates or incomparable ambiguities. The single-arg `parse(destination)` overload is preserved for backward compatibility (wildcard-only resolution).

### Observability

The observability primitives live in **`garganttua-commons`** (package `com.garganttua.core.observability`) so any module — including foundation layers — can be made observable without creating a dependency cycle. `garganttua-observability` keeps only the script-side `:observe(...)` expression bridge (which needs `expression`).

**Primitives (in `commons`):**
- Sealed event hierarchy: `StartEvent`, `EndEvent`, `ErrorEvent` (each carrying `executionId`, `timestamp`, `source`).
- `IObserver<E>` with a single `onEvent(E)` callback — implementations use pattern matching to dispatch.
- `ObservableRegistry<E>` backed by `CopyOnWriteArrayList`, exception-isolated, with `hasObservers()` short-circuit.
- `ObservableContextHolder` ThreadLocal **stack-based** holder. Use `push(registry, uuid)` (returns previous `Session`) and `pop(previous)` to support nested engine invocations. Mirrors the pattern fixed for `RuntimeExpressionContext` (2026-05-20).
- `ObservabilityEmitter` helper — `open(localRegistry, localUuid)` opens a `Scope` that either reuses the parent session (if one is active) or pushes its own, then provides `fireStart/fireEnd/fireError` and auto-closes via try-with-resources. `joinCurrent()` is the passive variant used by nested units of work (e.g. steps) that piggy-back on a parent session.

**Engines instrumented (as of 2026-05-20):**
- **Workflow** — `stage:<name>`, `script:<stage>.<scriptName>` (generated `:observe`).
- **Runtime** — `runtime:<name>` for the whole execution and `runtime:<name>:step:<stepName>` (+ `:fallback`) per step.
- **ScriptContext** — `scriptcontext:compile`, `scriptcontext:execute` (renamed from `script:*` to avoid colliding with workflow-generated `script:<stage>.<name>`).
- **Mapper** — `mapper:<src>-><dst>` at the root mapping call only (nested mappings inherit).
- **InjectionContext** — `injection:bean:<beanRef>` fired by `BeanFactory.createBeanInstance()`; propagates via `joinCurrent()` to whichever parent (Bootstrap, Runtime, Workflow) is observing.
- **Bootstrap** — `bootstrap:build`, `bootstrap:phase:resolve`, `bootstrap:builder:<simpleName>` per builder.
- **InterruptibleLeaseMutex** — `mutex:<name>` start/end/error.
- **Expression** — deliberately not instrumented (per-node evaluation would flood the registry).

**Cross-engine propagation:** when a `Workflow` calls a `Script` that calls a `Runtime` that runs a step, the same `executionId` flows through all layers via `ObservableContextHolder` stack semantics. A single observer attached at the workflow level sees the entire chain with consistent correlation. Verified by `CrossEngineObservabilityTest` in `garganttua-workflow`.

**ObservabilityBuilder DSL** (`garganttua-observability/.../dsl/`): for wiring one observer to several observables in one expression, with per-subscription filters and a detachable handle. `ObservabilityBuilder.create().observe(workflow, mapper).observer(o).when(...)...up().build()` returns an `ObservabilityBinding` (AutoCloseable). Filters use the `garganttua-condition` DSL (same one as `RuntimeStepMethodBuilder.condition(...)`), via a framework-managed `EventHolderSupplier` that refreshes the current event before each `fullEvaluate()`. JDK `Predicate` is available as an escape hatch via `.where(...)`. Sugar methods: `.onlyEvents(StartEvent.class)`, `.matchingSource("workflow:*")`. Filter composition is AND.

**Log observers** (`garganttua-observability/.../log/`): `ConsoleLogObserver` and `FileLogObserver` (AutoCloseable, sync writes, NDJSON-default, parent-dir auto-created) plus `IEventFormatter` with two implementations: `PlainTextEventFormatter` (human single-line) and `JsonLineEventFormatter` (NDJSON, no external dep — manual escape). External sinks (Elasticsearch, Loki, …) belong in dedicated binding modules; `garganttua-observability` stays dependency-free (only commons / expression / condition / supply).

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

**Configure a DSL from a file (2.0.0-ALPHA03+)**: a config file can configure a DSL builder directly. The file is self-describing via a target *alias* ("shebang"): `#!injection` first line for text formats, `<?garganttua module="injection"?>` / root `module="injection"` for XML, reserved `"$module":"injection"` key for JSON (read by `ConfigurationShebang`; `$`-prefixed keys are skipped by the populator). A DSL builder opts in with `@ConfigurableBuilder("injection")` (in `garganttua-commons`, `@Indexed`+`@Reflected`); on `InjectionContextBuilder` ("injection"), `RuntimesBuilder` ("runtimes"), `MutexManagerBuilder` ("mutex"), `ExpressionContextBuilder` ("expression"), `ObservabilityBuilder` ("observability"), `ScriptsBuilder` ("scripts"), `WorkflowsBuilder` ("workflows"), `ClassLoaderManagerBuilder` ("classloader"). (`ReflectionBuilder` is intentionally not configurable — the reflection foundation is used before the CONFIGURATION stage.) `IConfigProvider` discovers sources (`ClasspathConfigProvider` scans `garganttua/config`, `FileSystemConfigProvider` a directory; a remote provider can plug in). Auto-wiring: `BootstrapConfigurationContributor` (an `IBootstrapConfigurationContributor` SPI in commons, `META-INF/services`-registered) runs at the bootstrap **CONFIGURATION stage** and applies each discovered file to the matching `@ConfigurableBuilder` before it builds — active only when `garganttua-configuration` is on the classpath (optional). Without a bootstrap: `new ConfigurationApplier(populator).apply(builder, source)` manually. The populator maps scalars/arrays to setters AND traverses **keyed child-builders** — a method taking the entry key as its single arg and returning a child builder (e.g. `beanProvider(String scope)` → `withBean(IClass)` → `strategy(enum)`), so beans are declarable: `beanProvider: { app: { withBean: { com.x.MyService: { strategy: singleton } } } }`. It also handles **keyed-scalar setters** — a fluent method shaped `(String key, V value)` fed an object of scalar entries, invoked once per entry — so **properties** are declarable: `propertyProvider: { garganttua: { withProperty: { "app.port": "8080", "db.url": "..." } } }` (added 2026-06-04 via `IPropertyProviderBuilder.withProperty(String,String)`; values stored as text, coerced to the requested scalar type on read). **No-arg config targets** also map (since 2026-06-04): `MethodMapping.isMappable` admits 0-arg methods that are flag setters (return `void`/the builder, invoked only when the value is `true`) or no-arg child-builder openers (return an `IBuilder`, descended into then ascended via `up()`/`and()`); structural methods (`build`/`up`/`setUp`/`and`) and plain getters are excluded so config keys never trip them. The populator **recurses to arbitrary depth** with no limit, mixing all four shapes at any level (proven by `DeepNestingPopulatorTest` to depth 5; every `@ConfigurableBuilder` across all modules is config-driven per `AllConfigurableBuildersConfigTest` in garganttua-workflow). `InjectionContextBuilder.beanProvider(scope)` and `propertyProvider(scope)` are both create-if-absent so any scope works; `TypeConverter` converts `String`→`IClass`/enum (enum match is exact-first, so lower-case `BeanStrategy.singleton` works); ascent uses `up()` (ILinkedBuilder) or `and()`. **Precedence**: config is applied last (CONFIGURATION stage) so it wins for scalar setters; a dedicated top-priority `MultiSourceCollector` `"configuration"` source for collection builders is a planned refinement.

## Code Conventions

- All modules must be thread-safe. Use `Collections.synchronizedMap/List` or concurrent collections for shared state.
- See `.claude/rules/java-conventions.md` for full naming and style conventions.
- Key points: Lombok for boilerplate, `I` prefix for interfaces, Java records for value objects, `Optional<T>` for nullable values, SLF4J logging via `@Slf4j`.
- **`Class<?>` usage is prohibited** — always use `IClass<?>` from `garganttua-commons` instead. Use `IClass.getClass(clazz)` to wrap a raw `Class<?>`. **Exception**: for pure type hierarchy checks that must not depend on `IReflection`, use the `IClass.isAssignableFrom(Class<?>)` surcharge or `IClass.represents(Class<?>)` — these accept a raw `Class<?>` directly. Never use `IClass.getClass()` solely to satisfy an `isAssignableFrom(IClass)` or `equals()` call; prefer the `Class<?>` surcharges instead. Note: `IClass.equals(Class<?>)` exists for backward compatibility but is deprecated — use `represents(Class<?>)` for new code.
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
- `workflow` → `script`, `expression`, `injection`, `dsl`, `observability` (execution requires both `IInjectionContext` and `IExpressionContext`)
- `properties` → `commons`, `injection`
- `observability` → `commons`, `expression`
- `reflection` → `commons`, `supply`
- `configuration` → `commons`, `dsl`, `reflection`, `jackson-databind`; `injection` as `provided`
- `runtime-reflection` → `commons`

## CI/CD

Two GitHub Actions workflows in `.github/workflows/`:
- **`maven-publish.yml`**: Builds on any branch push; deploys to GitHub Packages on tag creation.
- **`build-script-installer.yml`**: Builds script installer on pushes/PRs to `main` touching script-related modules. Manual trigger with optional `create_release` input creates a GitHub release tagged `garganttua-script-v{version}`.
