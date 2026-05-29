# Garganttua AOT

## Description

Parent module for Garganttua AOT (Ahead-of-Time) compilation support. This POM aggregates all AOT submodules that provide compile-time class descriptor generation, reflection metadata, and annotation processing for AOT-compatible builds.

### Submodules

- `garganttua-aot-commons` - Shared AOT interfaces and types
- `garganttua-aot-reflection` - AOT reflection descriptors and registry
- `garganttua-aot-annotation-scanner` - Compile-time annotation scanner
- `garganttua-aot-annotation-processor` - Annotation processor for AOT descriptor generation
- `garganttua-aot-maven-plugin` - Maven plugin for AOT processing

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Strategy

The AOT pipeline consumes the same annotations as the GraalVM pipeline, decoupling **what the code does** from **how it's built**:

- `@Indexed` — annotation should be discoverable at compile time
- `@Reflected` — code is accessed by reflection (replaces the older `@Native`)
- `@ReflectedBuilder` — class uses reflection and exposes a usage map via `IReflectionUsageReporter`

Both pipelines (GraalVM and AOT) read these annotations and generate their own outputs:

```
@Reflected / @ReflectedBuilder / @Indexed
        │
        ├── garganttua-graalvm-*  → reflect-config.json / resource-config.json
        │
        └── garganttua-aot-*      → IClass<T> + direct binders + pre-indexed IAnnotationScanner
```

### Resolution model (the contract you can rely on)

At runtime, when code asks `IClass.getClass(SomeType.class)` or
`IClass.forName("…")`, the AOT provider resolves the type in **two tiers**:

| Tier | Source | Descriptor quality | Cost |
|---|---|---|---|
| 1 | **AOT registry** — populated by generated `AOTClass_*` (annotation processor) + `CoreInfrastructureSeed` (framework types) + extension seeds (higher-layer frameworks) | **Rich**: fields, methods, constructors, annotations, parameter types | Compile-time + cold-start `<clinit>` |
| 2 | **Fallback synthesis** — when tier 1 misses but the JVM has the `Class<?>` (literal or `Class.forName`-reachable) | **Type-identity only**: name, modifiers, interfaces, JVM flags. No members. | One synthesis call, cached forever |

This means **the AOT provider never throws "missing descriptor"** for any
type the JVM can resolve. The hand-curated seed list inside the framework is
an *optimisation* — it pre-warms common types with their full descriptors —
but it is no longer a correctness requirement. New framework types or new
user types you forgot to wire all fall back to the synth path and keep
working.

### What you need to write as a consumer

For 95% of cases, **`@Reflected` on your business classes is sufficient.**

| You wrote | What happens at AOT time |
|---|---|
| `@Reflected` on your entities / DTOs / services | Annotation processor generates a full `AOTClass_*` descriptor with all members. Self-registers via `META-INF/services/IAOTSelfRegistering`. |
| Your annotations on those classes (`@Inject`, `@MyQualifier`, `@GGEntity`, …) | Framework-shipped annotations are pre-seeded by `CoreInfrastructureSeed`. Your own custom annotations get a fallback-synthesised identity descriptor on first use — sufficient since `@Annotation.class` lookups only care about identity, not attribute introspection. |
| Framework types in your method signatures (`IDomain`, `IAuthenticatorDefinition`, JDK collections, …) | Either seeded by `CoreInfrastructureSeed` + extension seeds, or fallback-synthesised. |
| Plain JDK types as fields / params | Fallback synthesis covers all reachable classes. |

The only cases where `@Reflected` is **not enough**:

1. **Member introspection on a non-`@Reflected` type.** If your code calls
   `IClass.getClass(SomeInterface.class).getDeclaredMethods()` and
   `SomeInterface` has no full descriptor, you get an empty list (not a
   crash). To get real members, either `@Reflected` the type or ship a seed
   (see below).
2. **Dynamic class resolution by string in native-image.** If you do
   `Class.forName(stringComputedAtRuntime)`, GraalVM's static reachability
   analysis won't see it. The fallback runs too late — it executes after the
   image is built. Either `@Reflected` the type so the AOT Feature registers
   it at build time, or seed it explicitly.
3. **Higher-layer frameworks shipping their own public types.** If you
   write a reusable library (a garganttua-api-style framework) and your
   public interfaces appear in user `@Reflected` signatures with member
   queries downstream, ship an `IAOTInfrastructureSeed` so consumers get
   rich descriptors without manual wiring.

### Extension SPI for higher-layer frameworks

The garganttua core can't know about types declared in frameworks built
on top of it (garganttua-api, garganttua-events, your own libraries). The
**`IAOTInfrastructureSeed` SPI** lets each framework pre-register its
public types in the AOT registry on cold start:

```java
public class MyFrameworkSeed implements IAOTInfrastructureSeed {
    @Override
    public void seed(IAOTSeedContext ctx) {
        ctx.registerInterface(IMyPublicContract.class);
        ctx.registerClass(MyPublicAnnotation.class);
    }
}
```

Plus `META-INF/services/com.garganttua.core.aot.commons.IAOTInfrastructureSeed`
listing the FQN. Optional `@jakarta.annotation.Priority(N)` to order it
among other seeds. See `garganttua-aot-commons` for the SPI reference.

This SPI is **the integration point** for two layers that already live in
this repo and serve as worked examples: `garganttua-configuration` and
`garganttua-observability` each ship their own seed.

### Module Responsibilities

- **`garganttua-aot-commons`** — interfaces for the AOT registry, reflection provider, annotation scanner, and the `IAOTInfrastructureSeed` extension SPI; conventions and file formats for `META-INF/garganttua/aot/*`.
- **`garganttua-aot-reflection`** — pre-generated `IClass<T>` / `IMethod` / `IField` / `IConstructor` implementations + `CoreInfrastructureSeed` (framework-public types) + `AOTReflectionProvider` (registry-or-fallback resolver).
- **`garganttua-aot-annotation-scanner`** — pre-indexed `IAnnotationScanner` reading the AOT index files.
- **`garganttua-aot-annotation-processor`** — compile-time code generator that produces the descriptors and direct binders. Internally split into per-member source generators (`AOTConstructorSourceGenerator`, `AOTFieldSourceGenerator`, `AOTMethodSourceGenerator`) with shared helpers (`AOTNaming`, `TypeNames`, `MemberInclusion`).
- **`garganttua-aot-maven-plugin`** — Maven binding for the processor.
- **`garganttua-aot-native-feature`** — GraalVM `Feature` that registers every `AOTRegistry`-known type with `RuntimeReflection` at native-image analysis time.

## Usage

### Building the module suite

```bash
mvn clean install -pl garganttua-aot
```

### Annotating your code

```java
@Reflected
public record MyConfig(String name, int port) { }

@ReflectedBuilder
public class MyServiceBuilder implements IBuilder<MyService>, IReflectionUsageReporter {
    @Override
    public ReflectionUsage reportReflectionUsage() {
        return ReflectionUsage.builder()
            .reflectsOn(MyConfig.class)
            .build();
    }
}
```

The annotation processor scans these at compile time and emits AOT descriptors under `target/generated-sources/` plus the `META-INF/services/com.garganttua.core.aot.commons.IAOTSelfRegistering` descriptor so ServiceLoader force-loads them at cold start.

### Choosing a reflection mode

Don't wire `garganttua-aot-reflection` directly in your app — pick one of
the four consumption starters instead:

| Mode | Starter coordinate | Use case |
|---|---|---|
| AOT | `garganttua-starter-aot` | Pure-AOT JVM, no runtime classpath scan |
| Runtime | `garganttua-starter-runtime` | Legacy mode, fastest dev loop |
| Hybrid | `garganttua-starter-hybrid` | Recommended default — AOT first, runtime fallback |
| Native | `garganttua-starter-native` | GraalVM native-image |

See `garganttua-starters/README.md` for the full matrix.

## Tips and best practices

### What to mark `@Reflected`

- Yes: business entities, DTOs, services, configuration records, authenticators, handlers — anything the framework calls into reflectively.
- No: framework-shipped annotations (already covered by `CoreInfrastructureSeed`).
- No: your own custom annotations *unless* you introspect their attributes at runtime (rare).

### When to ship an `IAOTInfrastructureSeed`

- You write a reusable framework on top of garganttua-core (e.g. garganttua-api, garganttua-events, your in-house DSL).
- You expose public interfaces / annotations that downstream user `@Reflected` classes reference.
- You want consumers to get rich descriptors for your public types without having to seed them by hand.

### Native-image checklist

- Pick `garganttua-starter-native` (pulls AOT + the GraalVM Feature).
- Wire `org.graalvm.buildtools:native-maven-plugin` (the starter doesn't auto-add build plugins).
- Avoid `Class.forName(stringComputedAtRuntime)` — the fallback path runs at app runtime, too late for native-image build-time analysis. Either `@Reflected` the type or seed it.
- Optional: increase build memory if your `@Reflected` surface is large (the processor's per-class descriptor scales linearly with member count).

### Hybrid mode in dev

- `garganttua-starter-hybrid` is the recommended default during development. AOT @20 wins for processed types; runtime @10 transparently picks up anything you forgot to `@Reflected`.
- Useful for iterating without re-running the processor between minor edits.
- Switch to `-aot` or `-native` only when you're ready to optimise cold start.

### What stays a work-in-progress

- The annotation processor's `direct.binders=true` mode is still being hardened — leave it disabled (the default) unless you're actively iterating on it.
- `@ReflectedBuilder` reports must stay honest: anything reflected at runtime that isn't reported won't be in the AOT descriptors. The fallback synthesis covers type identity, not members.

## License
This module is distributed under the MIT License.
