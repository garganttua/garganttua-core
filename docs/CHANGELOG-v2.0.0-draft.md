<!--
  DRAFT release log for v2.0.0 — NOT final.
  Top sections (Highlights, Breaking changes, Migration) are HAND-CURATED.
  The "Features / Bug Fixes / …" lists below are auto-grouped from
  `git log v2.0.0-ALPHA01..HEAD` (the same grouping cliff.toml produces).
  Trim, merge by theme, and verify each breaking-change migration before shipping.
-->

# garganttua-core 2.0.0

**A ground-up rework of the foundation.** v2 replaces the v1 reflection helpers with a
pluggable, AOT-ready `IReflection` facade, makes seven engines observable without dependency
cycles, lets you configure any DSL builder from a file, drops Lombok and SLF4J in favour of a
pure-JDK core, and moves the build to **Java 25**.

> If you consume garganttua-core (garganttua-api, garganttua-events, applications), read
> **Breaking changes** and **Migration** first — the reflection-provider and logging changes
> affect every downstream.

---

## ✨ Highlights

- **Pluggable reflection (`IReflection`)** — `Class<?>` is gone from the API; everything goes
  through `IClass<T>`/`IMethod`/`IField`/`IConstructor` mirrors, with runtime (JVM) and AOT
  providers selected by priority via ServiceLoader.
- **AOT / native-image suite** — pre-generated `IClass` descriptors, a GraalVM `Feature`,
  `@Reflected`-index seeding, and four consumption starters (runtime / AOT / hybrid / native).
- **Native observability** — sealed event family in `garganttua-commons`; Workflow, Runtime,
  Script, Mapper, Injection, Bootstrap and Mutex emit start/end/error with cross-engine
  correlation. `ObservabilityBuilder` DSL + console/file (NDJSON) log observers.
- **Configure DSLs from a file** — `#!injection` / `$module` shebang binds a `.json/.yml/.xml`
  config to a `@ConfigurableBuilder`; declare beans **and** properties, at arbitrary depth,
  across all eight configurable builders.
- **Pure-JDK core** — Lombok and SLF4J removed reactor-wide; hand-written builders and an
  observable `Logger` replace them.
- **Bootstrap SPI cold-start** — `Bootstrap.builder().autoDetect(true).build()` on a bare JVM,
  with a per-builder timing breakdown and dependency-graph banner in the startup summary.
- **Maintainability sweep** — every identified god-class split and long method extracted across
  reflection, aot, bootstrap, injection, mapper, expression, script, console, workflow.

---

## ⚠️ Breaking changes

> No `BREAKING CHANGE:` footers existed in the alpha history; the list below is curated by hand.
> Adopt `feat(x)!:` / `BREAKING CHANGE:` footers going forward so future majors build themselves.

- **Java 21 → Java 25 required** to build (uses ScopedValue + preview-era APIs).
- **`Class<?>` removed from the public API → use `IClass<?>`.** Wrap raw classes with
  `IClass.getClass(clazz)`. For pure hierarchy checks, `IClass.isAssignableFrom(Class<?>)` /
  `represents(Class<?>)` accept a raw class. `IClass.equals(Class<?>)` is deprecated.
- **Deleted reflection utilities** — `ObjectReflectionHelper`, `FieldAccessManager`,
  `MethodAccessManager`, `ConstructorAccessManager`, `ObjectAccessor`. Replaced by the
  `IReflection` facade + `FieldAccessor` / `MethodInvoker`.
- **Reflection provider is no longer transitive.** `garganttua-bootstrap` dropped its transitive
  `garganttua-runtime-reflection` dependency — **consumers must now add a provider explicitly**
  (`garganttua-runtime-reflection` for JVM, `garganttua-aot-reflection` for native), or cold-start
  SPI finds nothing.
- **Logging: SLF4J / Lombok `@Slf4j` removed** → pure-JDK observable `Logger`
  (`Logger.getLogger(Class)`; level via `garganttua.log.level`). The `Diagnostics` package was
  deleted.
- **Observability API changed** — engines self-attach at build; `.observe()` / `.toObservable()`
  are gone; `subscribe()` replaces `observer()` on the binding DSL.
- **Module topology** — `WorkflowsBuilder` / `ScriptsBuilder` / `ClassLoaderManagerBuilder`
  introduced and the dep graph realigned to the execution chain; the REPL moved out of
  `garganttua-script` into `garganttua-console`; observability primitives now live in
  `garganttua-commons`.

## 🧭 Migration (1.x → 2.0)

1. **Build on JDK 25.**
2. **Add a reflection provider** to your runtime classpath:
   `garganttua-runtime-reflection` (JVM) or `garganttua-aot-reflection` (native).
3. **Replace `Class<?>`** at API boundaries with `IClass<?>` (`IClass.getClass(x)`); swap deleted
   helper calls for `FieldAccessor` / `MethodInvoker`.
4. **Logging** — drop SLF4J wiring against the framework; read `garganttua.log.level` and attach a
   log observer if you want sinks.
5. **Observability** — replace `observer(...)` with `subscribe(...)`; remove `.observe()` /
   `.toObservable()` calls.

---

## 🚀 Features

### Configuration (DSL from file)
- **config**: make builder configuration integral — properties, no-arg targets, all builders
- **config**: make observability/scripts/workflows/classloader builders configurable
- **config**: declare beans in any scope from config + harden keyed-child
- **config**: declare beans (keyed child-builders) from a config file
- **config**: native seed + annotate more builders + docs (#3, #4, #5)
- **config**: bootstrap auto-wiring of configuration files (step D)
- **config**: bind a config file to a DSL builder via shebang (step C)
- **config**: shebang extraction + alias→builder registry (step B)
- **config**: foundations for DSL configuration files (step A)

### AOT / native-image
- **native**: write native-image configs under `META-INF/native-image/<groupId>/<artifactId>/`
- **script/aot**: seed script AST node classes for native reflection
- **aot/native-feature**: register `@Reflected` index classes + include `.gs` scripts
- **aot/seed**: JDK time / math / collection essentials in `CoreInfrastructureSeed`
- **aot/processor**: auto-promote classes with `@Indexed`-meta annotations
- **aot**: `IAOTInfrastructureSeed` SPI for cross-framework type seeding
- **starters**: four consumption starters + GraalVM `Feature` for native-image
- **aot**: native-image-friendly ServiceLoader path + reachability metadata
- **aot**: runtime loader for generated `AOTClass_*` descriptors
- **aot**: implement AOT reflection library suite (bump to 2.0.0-ALPHA02)

### Observability
- **observability**: `@Observable` annotation + DI-driven scans for `@Observer`/`@Observable`
- **observability**: `ObservabilityBinding` contributes to Bootstrap banner summary
- **observability**: split deps — `IReflectionBuilder` for autodetect, `IInjectionContextBuilder` post-build
- **observability**: `@Bootstrap`-discoverable `ObservabilityBuilder` with DI-aware `@Observer` scan
- **observability**: `@Observer` annotation + `ObservabilityBuilder.autoDetect()`
- **observability**: log observers — console + file, NDJSON or plain text
- ObservabilityBuilder DSL — multi-observable subscriptions with filters
- extend observability across runtime, script, mapper, injection, bootstrap, mutex
- add garganttua-observability module with workflow timing integration
- meaningful script step names for observability labels

### Bootstrap / DI / workflow
- **bootstrap**: per-builder timing breakdown in startup summary
- **bootstrap**: expose SPI discovery as an explicit intermediate step
- **bootstrap**: Step 5+6 — wire centralised CONFIGURATION stage + tests
- **bootstrap/dsl**: `@DependsOn`, dep-graph banner, stage-failure diagnostics, per-stage timings, pluggable listeners
- **dsl/dep**: stage/kind-aware hooks, `BuilderDependency` tracking, iterators (Steps 1-4)
- **workflow**: make `WorkflowBuilder` bootstrap-discoverable + depend on `IRuntimesBuilder`
- **workflow**: make garganttua-observability an optional dependency
- **workflow**: allow pinning the execution id for observability correlation
- **workflow/script**: thread-safe precompiled scripts for hot-path workflows
- **workflow/script**: surface precompile counts + engine stats in Bootstrap summary
- ServiceLoader-based cold-start discovery of reflection providers

### Other
- **crypto**: expose Key/KeyRealm material factories + cache JDK key
- support per-source mapping rules in `@FieldMappingRule` / `@ObjectMappingRule`
- add `@BeanProviderAnnotation`, `@PropertyProviderAnnotation` + placeholder resolution
- add garganttua-properties module; refactor ScriptContext to use `IRuntimesBuilder`

## 🐛 Bug Fixes

### AOT / native
- **aot/native-feature**: mark descriptor classes via raw backing arrays; initialize-at-build-time
- **aot/processor**: preserve field generic type; populate annotations array; index annotation-type declarations
- **aot/processor**: index entries use JVM binary names (`Outer$Inner`); user `@Qualifier`-meta annotations indexed
- **aot/scanner**: walk meta-annotations so `@Observer` surfaces under `@Qualifier`
- **aot/reflection**: recover parameter annotations from the live `Method`
- **aot**: live-class fallback for bulk `getDeclared*` arrays; lazy member synthesis; unblock pure-AOT instantiation
- **aot**: registry-or-fallback resolution ends the "missing descriptor" loop; seed JDK + framework types

### Expression / mapper / condition
- **expression**: framework `@Expression` direct-reflection registration; seed function packages; null-safe lookup
- **expression**: include the 14 Condition classes + literal-wrap built-ins in the default scan
- **expression**: mark `ExpressionContext` `@Reflected` for native built-in lookup
- **mapper**: instantiate concrete collection fields directly; leaf-type pass-through; NPE on unresolved generics
- **condition**: equals/notEquals/ordering + notNull/null built-ins are Optional-aware

### Reflection / bootstrap / runtime
- **reflection**: unwrap any `IClass` (not only `RuntimeClass`); `getDeclaredAnnotationsByType` uses `getType()`
- **reflections**: package-prefix filter so siblings outside the package don't leak
- **bootstrap**: propagate `autoDetect` to SPI-loaded builders; idempotent onInit/onStart
- **runtime**: degrade gracefully when output type can't be resolved
- **framework**: `@Reflected` on factory classes carrying `@Indexed`/`@Expression` members
- cold-start SPI auto-loads runtime-reflection + standard builders; provide() vs withBuilder() init fixes
- preserve `RuntimeExpressionContext` across nested step execution; prevent script-include StackOverflow
- print bootstrap banner + glyphs in UTF-8 regardless of stdout charset

### Tooling / CLI
- **quality**: SpotBugs + PMD (offline Sonar proxy) and the real bugs they found; locale-independent case conversions
- **script/cli**: standalone cold-start fat-JAR / installer; CLI version from `GarganttuaVersion`
- **native**: native-config goal generates the `.gs` resource-config

## ⚡ Performance
- **reflection**: single-provider / single-scanner fast paths
- **aot/reflection**: memoise live-class + bulk-fallback descriptors
- **aot/scanner**: cache wrapped `IClass`/`IMethod` lists per `IndexData`
- **reflections**: cache Reflections instance per package

## 🛠️ Refactoring (internal)
- Removed Lombok and SLF4J reactor-wide; replaced with hand-written builders + observable `Logger`.
- Migrated 3 propagation ThreadLocals to ScopedValue; removed `Mapper.VISITED` ThreadLocal.
- Pure-SPI `@Bootstrap` discovery (no reflection scan).
- God-class splits + long-method extractions across reflection, aot, bootstrap, injection, mapper,
  expression, script, console, workflow.
- Inverted observability wiring (engines self-register); `WorkflowsBuilder`/`ScriptsBuilder`/`ClassLoader`
  introduced; dep graph aligned to the execution chain.

## ⏪ Reverts
- **observability**: dropped `@Observable` annotation + DI auto-detection of sources (superseded).
