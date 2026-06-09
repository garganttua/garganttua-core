# Garganttua Bootstrap

## Description

The **garganttua-bootstrap** module is the **build orchestrator** of a
Garganttua application. It discovers framework modules via SPI, resolves
their dependency graph in a 3-stage pipeline (CONFIGURATION → AUTO_DETECT
→ BUILD), wires them together topologically, and exposes a built registry
of every produced artefact along with a structured startup summary.

It is a build orchestrator only — not a runtime classpath manager. JAR
hot-loading lives in the separate `garganttua-classloader` module.
Bootstrap merely auto-registers itself as a rebuild hook on any
`IClassLoaderManager` it discovers, so plugin loading transparently
triggers `bootstrap.rebuild()`.

**Key features:**

- **Two-pass SPI discovery** — `IReflectionProvider` + `IAnnotationScanner`
  resolved in the Bootstrap constructor (installs the global `IClass`
  reflection), then `IBootstrapBuilderFactory` resolved on `load()` to
  populate the registry of framework module builders.
- **Explicit `load()` step** between configuration and `build()` — gives
  callers a hook to inspect / amend the SPI-discovered builders before
  dependency resolution kicks in.
- **3-stage dependency pipeline** — `CONFIGURATION` (mutate upstreams),
  `AUTO_DETECT` (annotation/bean scanning), `BUILD` (final construction).
  Combined with `BUILDER` vs `BUILT` dependency kinds and `REQUIRED` vs
  `OPTIONAL` requirements gives 12 combinations covered by single-line
  factory helpers.
- **Topological build ordering** — `requireBuilder`/`require` deps drive a
  toposort so no builder is built before its dependencies.
- **Auto-wired rebuild hook on `IClassLoaderManager`** — after build,
  Bootstrap iterates the built objects, finds every classloader manager,
  and registers itself as a rebuild hook with idempotent guard.
- **Startup summary** — every built object implementing
  `IBootstrapSummaryContributor` gets its category + items printed in the
  banner. Stage timings + per-builder timings break down where startup
  time actually went.
- **Rebuild support** — `bootstrap.rebuild()` re-runs the whole pipeline
  with any newly-registered packages / builders, while the
  classloader-manager hook deduplication ensures no hooks pile up.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-bootstrap</artifactId>
    <version>2.0.0-ALPHA03</version>
</dependency>
```

### Actual version
2.0.0-ALPHA03

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-reflection`
 - `javax.inject:javax.inject`
 - `jakarta.annotation:jakarta.annotation-api`
 - `com.garganttua.core:garganttua-reflections:test`
 - `com.garganttua.core:garganttua-runtime-reflection:test`
 - `com.garganttua.core:garganttua-configuration:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Lifecycle

```
new Bootstrap()                    ← Phase 0
    ↓ (ServiceLoader inside the constructor)
    install IClass.setReflection() ← global JVM reflection live
    
bootstrap.autoDetect(true)         ← opt into SPI discovery
         .withPackage("com.myapp") ← packages propagated to packageable builders
         .provide(extraBuilder)    ← optional user-side wiring
         .load();                  ← Phase 0.5: SPI module factories discovered

bootstrap.build();                 ← runs the full pipeline below
```

### The 3 stages of `build()`

| Stage | Order | What fires | Hook on the consumer |
|---|---|---|---|
| **Phase 1 — RESOLVE** | first | match every declared `DependencySpec` against provided/registered builders | `provide()` stores refs |
| **Phase 1.5 — CONFIGURATION** | before any build | iterate every `IDependentBuilder` and fire its CONFIGURATION-stage specs | `doConfigureWithDependencyBuilder(upstreamBuilder)` |
| **Phase 2 — TOPO SORT** | between configure and build | order builders so REQUIRED deps come first | — |
| **Phase 3 — BUILD** | last | for each builder: AUTO_DETECT hooks, pre-build hooks, `build()`, post-build hooks | `doAutoDetectionWithDependency`, `doPreBuildWithDependency`, `doPostBuildWithDependency` |
| **Phase 3 — POST** | tail | wire classloader hooks, print summary | `wireClassLoaderManagerHooks`, `printSummary` |

### DependencySpec — three orthogonal axes

| Axis | Values |
|---|---|
| **Stage** | `CONFIGURATION`, `AUTO_DETECT`, `BUILD` |
| **Kind** | `BUILDER` (the upstream builder itself), `BUILT` (its built result) |
| **Requirement** | `REQUIRED`, `OPTIONAL` |

Factory helpers cover the common combos: `require`, `use`, `requireBuilder`,
`useBuilder`, `requireAutoDetect`, `autoDetect`, `autoDetectBuilder`,
`requireConfigure`, `configure`, and the multi-stage helper
`configureAndStage` (the "contributor pattern" enabler).

### SPI module discovery

Two `ServiceLoader` passes:

1. **Constructor** — `IReflectionProvider` + `IAnnotationScanner` are
   merged into a single composite `IReflectionBuilder`, then
   `IClass.setReflection()` makes it globally available. Done eagerly so
   any subsequent `IClass.getClass(...)` call (including those happening
   inside other builders' constructors) just works.
2. **`load()`** — `IBootstrapBuilderFactory` factories produce one
   `IBuilder<?>` each (InjectionContextBuilder, ExpressionContextBuilder,
   RuntimesBuilder, ScriptsBuilder, WorkflowsBuilder,
   ClassLoaderManagerBuilder, ObservabilityBuilder, MutexManagerBuilder,
   …). Registered via `withBuilder(...)`.

### Auto-wiring of `IClassLoaderManager`

Once build completes, Bootstrap iterates `builtObjects`, finds every
`IClassLoaderManager`, and adds itself as a rebuild hook:

```java
mgr.addRebuildHook(packages -> {
    for (String pkg : packages) this.withPackage(pkg);
    this.rebuild();
});
```

Deduplication is via `IdentityHashMap<IClassLoaderManager, Boolean>` so a
`bootstrap.rebuild()` cycle does NOT pile up duplicate hooks.

### Startup summary

Built objects implementing `IBootstrapSummaryContributor` (each engine's
registry: InjectionContext, ExpressionContext, RuntimesRegistry,
WorkflowsRegistry, ScriptingEnvironment, MutexManager,
ObservabilityBinding) contribute category + items.

Plus two timing sections:

- **Stage timings** — phase totals (`resolve`, `configure`, `build`)
- **Per-builder timings** — `configure:<BuilderName>` and
  `build:<BuilderName>` sorted slowest-first, lets operators see exactly
  who dominates cold-start without reaching for a profiler.

## Usage

### Minimal cold-start

```java
IBootstrap bootstrap = new Bootstrap();
bootstrap.autoDetect(true)
         .withPackage("com.myapp")
         .load();
IBuiltRegistry registry = bootstrap.build();

// Pull anything you need by class:
IApi api = registry.request(IClass.getClass(IApi.class)).orElseThrow();
```

### Adding a non-SPI builder

```java
ApiBuilder api = ApiBuilder.builder().multiTenant(true);
bootstrap.autoDetect(true)
         .withPackage("com.myapp")
         .withBuilder(api)         // ← manual contribution
         .load();
bootstrap.build();
```

### Rebuilding after hot JAR load (auto-wired)

```java
// In a script :
include("/opt/plugins/my-plugin.jar")
// → ClassLoaderManager.loadJar() fires hooks
// → Bootstrap.withPackage(<pkg from manifest>) + Bootstrap.rebuild()
// → the framework picks up @Bootstrap-annotated classes in the JAR
```

### Disabling SPI fallback (deterministic builds, tests)

```java
new Bootstrap()
    .autoDetect(true)
    .disableSpiFallback()  // no auto-SPI of reflection / module builders
    .provide(myReflectionBuilder)
    .load()
    .build();
```

## Tips and best practices

- **Always call `load()` explicitly** when you use `autoDetect(true)` — the
  SPI sweep is gated by both `autoDetect` and `spiFallback` being on, and
  it's no longer triggered automatically inside `build()` (changed in
  this iteration so callers can inspect / amend the SPI-loaded registry
  before the build runs).
- **Read the "Per-builder timings" section** of the summary to find your
  cold-start hotspot. The total `build:` and `configure:` lines hide the
  one or two builders that actually dominate.
- **Use `bootstrap.printDependencyGraph(true)` (or `withDependencyGraph()`)**
  if you want to visualise the resolved topological order before
  inspecting timings — useful for "why is X built after Y?" questions.
- **Don't extend `Bootstrap`** — it's a final orchestrator. The extension
  points are `IBootstrapBuilderFactory`,
  `IBootstrapSummaryContributor`, `IBootstrapStageListener`, and
  `IClassLoaderRebuildHook`. Use those.
- **Tests can opt out of SPI** via `disableSpiFallback()` for tighter
  control over the registered builders.
- **Native-image notes**: GraalVM auto-detects `ServiceLoader.load(X.class)`
  and includes `META-INF/services/*`. Shade-plugin configs producing fat
  JARs must include `ServicesResourceTransformer` to merge service
  descriptors — without it the SPI silently sees no providers. Already
  enforced in `garganttua-script` and `garganttua-console`.

## License
This module is distributed under the MIT License.
