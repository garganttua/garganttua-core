<!--
  DRAFT release log for v2.0.0 — NOT final.
  Everything down to "## 📋 Full change list" is HAND-CURATED narrative.
  At release time, git-cliff (cliff.toml) appends the exhaustive, auto-grouped
  per-commit list from `git log <prev>..<tag>`. Keep the story on top; let the
  tool own the long tail. Verify each breaking-change migration before shipping.
-->

# garganttua-core 2.0.0

**A ground-up rework of the foundation.** Where 1.x reached into `java.lang.reflect` directly and
leaned on Lombok and SLF4J, 2.0 stands on a pluggable, AOT-ready reflection facade, makes seven
engines observable without a single dependency cycle, lets you configure any DSL builder from a
file, and ships a pure-JDK core. The build moves to **Java 25**.

> Consuming garganttua-core (garganttua-api, garganttua-events, applications)? Read **Breaking
> changes** and **Migration** first — the reflection-provider and logging changes touch every
> downstream.

---

## ✨ The story of 2.0

### Reflection, reimagined
The headline change: `Class<?>` no longer appears in the API. Everything now flows through an
`IClass<T>` facade with `IMethod` / `IField` / `IConstructor` / `IParameter` mirrors, backed by
**pluggable `IReflectionProvider`s** selected by priority. The old grab-bag of static helpers
(`ObjectReflectionHelper`, `FieldAccessManager`, `MethodAccessManager`, `ConstructorAccessManager`,
`ObjectAccessor`) is gone, replaced by `FieldAccessor` / `MethodInvoker` over the facade. Single
god-classes (`ObjectQuery`, the long field/method accessors) were split into focused units
(`MemberLookup`, …), and single-provider / single-scanner **fast paths** keep the abstraction cheap.

This is what makes everything below possible — the same code runs on the JVM or in a native image
just by swapping the provider.

### Ahead-of-time & native-image
A full AOT suite lands (work-in-progress, but usable): a compile-time processor generates
`AOTClass_*` descriptors, a runtime loader resolves them, and a GraalVM **`Feature`** wires up
reachability metadata. An `IAOTInfrastructureSeed` SPI lets each framework contribute the JDK and
domain types it needs (time, math, collections, framework interfaces). Native-image configs are
written under `META-INF/native-image/<groupId>/<artifactId>/`, `.gs` scripts are bundled, and four
**consumption starters** (runtime / AOT / hybrid / native) give downstreams a one-dependency entry
point. A long tail of AOT correctness fixes (binary names for inner classes, parameter-annotation
recovery, lazy member synthesis, fallback-to-live-class) closes the "missing descriptor" loop.

### Observability, end to end
The observability primitives (sealed `StartEvent` / `EndEvent` / `ErrorEvent` / `LogEvent`,
`IObserver`, `ObservableRegistry`, the stack-based context holder) live in **`garganttua-commons`**,
so any layer can be instrumented without a cycle. Seven engines now emit correlated events —
Workflow, Runtime, Script, Mapper, Injection, Bootstrap, Mutex — and a single observer attached at
the top sees the whole cross-engine chain under one execution id. Wiring was **inverted** so engines
self-register at build (no more `.observe()` / `.toObservable()`); the `ObservabilityBuilder` DSL
adds filtered multi-source subscriptions, and console / file (NDJSON or plain-text) log observers
ship in `garganttua-observability` while it stays dependency-free.

### Configure anything from a file
A configuration file can now drive a DSL builder directly. A self-describing shebang
(`#!injection`, `$module`, or the XML/JSON equivalents) binds a `.json/.yml/.xml/.toml/.properties`
file to a `@ConfigurableBuilder`. All eight bootstrap builders opt in (injection, runtimes, mutex,
expression, observability, scripts, workflows, classloader). You can declare **beans** (keyed
child-builders: `beanProvider → withBean → strategy/qualifier`) **and properties**
(`propertyProvider → withProperty`), at **arbitrary nesting depth**, mixing scalar setters, no-arg
flags, keyed children and keyed-scalars. Bootstrap auto-wires discovered files at the CONFIGURATION
stage when the (optional) configuration module is present.

### Bootstrap & dependency orchestration
`Bootstrap.builder().autoDetect(true).build()` now cold-starts on a bare JVM via **ServiceLoader**
discovery of reflection providers and scanners — no manual wiring, provided one provider JAR is on
the classpath. The `@Bootstrap` reflection scan was replaced by pure SPI. A real dependency model
(`DependencyStage` / `DependencyKind`, `BuilderDependency` tracking, `@DependsOn`) drives ordering,
a centralised CONFIGURATION stage runs before builds, and the startup banner gained a dependency
graph, per-builder timing breakdown, stage-failure diagnostics and pluggable stage listeners.

### A leaner, purer core
**Lombok and SLF4J are gone** from the entire reactor — builders are hand-written and an observable
`Logger` (level via `garganttua.log.level`) replaces `@Slf4j` and the old `Diagnostics` package.
Three propagation `ThreadLocal`s became Java 21 `ScopedValue`s and `Mapper.VISITED` an explicit
recursion token. A maintainability sweep split every identified god-class (across bootstrap,
injection, mapper, expression, script, console, aot, reflection) and extracted long methods, with an
opt-in **Checkstyle size gate** and **SpotBugs + PMD** (offline) catching real bugs along the way.

### Workflow & scripting
Script-runtime construction is delegated to `RuntimesBuilder`; `WorkflowsBuilder`, `ScriptsBuilder`
and a `ClassLoaderManager` were introduced and the dependency graph realigned to the execution
chain. Workflows gained **thread-safe precompiled scripts** for hot paths, pinned execution ids for
observability correlation, and engine/precompile stats in the Bootstrap summary. `garganttua-
observability` is an **optional** dependency of workflow.

### Mapper & expression
The mapper supports **per-source mapping rules** (`@FieldMappingRule` / `@ObjectMappingRule` gained a
`source()` attribute, repeatable), with safer concrete-collection instantiation and leaf-type
pass-through. Expression built-in discovery was hardened (every framework `@Expression` and the 14
Condition classes are seeded and registered, null-safe), and condition built-ins
(`equals`/`notEquals`/ordering/`notNull`/`null`) became `Optional`-aware.

---

## ⚠️ Breaking changes

> No `BREAKING CHANGE:` footers existed in the alpha history; this list is curated by hand. Adopt
> `feat(x)!:` / `BREAKING CHANGE:` footers going forward so future majors build their own.

- **Java 21 → Java 25 required** to build.
- **`Class<?>` removed from the API → use `IClass<?>`.** Wrap raw classes with `IClass.getClass(c)`;
  for pure hierarchy checks `IClass.isAssignableFrom(Class<?>)` / `represents(Class<?>)` accept a raw
  class. `IClass.equals(Class<?>)` is deprecated.
- **Deleted reflection utilities** — `ObjectReflectionHelper`, `FieldAccessManager`,
  `MethodAccessManager`, `ConstructorAccessManager`, `ObjectAccessor` → `IReflection` +
  `FieldAccessor` / `MethodInvoker`.
- **Reflection provider is no longer transitive** — `garganttua-bootstrap` dropped its transitive
  `garganttua-runtime-reflection`; **consumers must add a provider explicitly**
  (`garganttua-runtime-reflection` for JVM, `garganttua-aot-reflection` for native).
- **Logging: SLF4J / Lombok `@Slf4j` removed** → pure-JDK observable `Logger`; the `Diagnostics`
  package was deleted.
- **Observability API changed** — engines self-attach at build; `.observe()` / `.toObservable()`
  removed; `subscribe()` replaces `observer()`.
- **Module topology** — `WorkflowsBuilder` / `ScriptsBuilder` / `ClassLoaderManagerBuilder` added and
  the dep graph realigned; the REPL moved from `garganttua-script` into `garganttua-console`;
  observability primitives moved into `garganttua-commons`.

## 🧭 Migration (1.x → 2.0)

1. **Build on JDK 25.**
2. **Add a reflection provider** to the runtime classpath — `garganttua-runtime-reflection` (JVM) or
   `garganttua-aot-reflection` (native).
3. **Replace `Class<?>`** at API boundaries with `IClass<?>` (`IClass.getClass(x)`); swap deleted
   helper calls for `FieldAccessor` / `MethodInvoker`.
4. **Logging** — drop SLF4J wiring against the framework; use `garganttua.log.level` and attach a log
   observer for sinks.
5. **Observability** — replace `observer(...)` with `subscribe(...)`; remove `.observe()` /
   `.toObservable()`.

---

## 📋 Full change list

The exhaustive, per-commit breakdown (Features / Bug Fixes / Performance / Refactoring / …) is
**auto-generated by git-cliff** (`cliff.toml`) and appended to the GitHub Release at tag time. To
preview it locally:

```bash
git cliff --latest                       # notes for the most recent tag
git cliff v2.0.0-ALPHA02..HEAD           # an explicit range
```
