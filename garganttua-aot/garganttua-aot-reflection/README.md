# Garganttua AOT Reflection

## Description

AOT reflection descriptors and registry for Garganttua Core. This module provides the data structures and registry needed to describe reflection usage at compile time, enabling AOT-compatible builds that do not rely on runtime reflection discovery.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-reflection</artifactId>
    <version>2.0.0-ALPHA03</version>
</dependency>
```

### Actual version
2.0.0-ALPHA03

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-aot-commons:${project.version}`
 - `com.garganttua.core:garganttua-dsl`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### `AOTReflectionProvider` — registry-then-fallback resolver

This is the `IReflectionProvider` implementation that gets selected
(at `@Priority(20)`) when an AOT starter is on the consumer's classpath.
It resolves types in two tiers:

```
                        ┌──────────────────────────────────────────┐
   IClass.getClass(X) ──┤ AOTRegistry.get(X.name)?                 │
                        │                                          │
                        │  hit  → return rich descriptor           │
                        │  miss → synthesize type-identity         │
                        │         descriptor from X.class +        │
                        │         cache in registry                │
                        └──────────────────────────────────────────┘
```

#### Tier 1 — registry

Three sources populate the registry:

1. **`CoreInfrastructureSeed`** — static `<clinit>` of this provider seeds
   the framework's own public surface (builder interfaces, annotation
   classes, JDK essentials, crypto/jakarta/javax.inject annotations).
2. **`IAOTInfrastructureSeed` extension SPI** — higher-layer frameworks
   (garganttua-api, garganttua-events, your library) ship seeds via
   `ServiceLoader`; the core seed runs them after its own pass.
3. **Generated `AOTClass_*` descriptors** — the annotation processor
   produces one per user `@Reflected` class. Force-loaded at startup via
   `ServiceLoader.load(IAOTSelfRegistering.class)`.

Registry hits return **rich descriptors** with full member metadata
(fields, methods, constructors, annotations).

#### Tier 2 — fallback synthesis

If the type is not in the registry, the provider synthesizes a minimal
descriptor on the fly using **class-metadata accessors only**
(`getName()`, `getInterfaces()`, `getModifiers()`, `isInterface()`,
`isAnnotation()`, `isEnum()`, …). These accessors are AOT-friendly by
construction — they don't require `reflect-config.json` on native-image
for already-loaded classes.

Synthesized descriptors carry **type identity** (name, modifiers,
interfaces, JVM flags) but **no member metadata** (empty
methods/fields/constructors). They are cached in the registry, so a type
is only synthesized once.

The fallback fires:

- `getClass(Class<T>)` — always on miss, since we have the class literal.
- `forName(String)` — tries intrinsic resolution (primitives, arrays,
  void), then `Class.forName(loader)`. If the JVM can resolve the name,
  the class gets a synthesized descriptor; otherwise `ClassNotFoundException`.

#### Hybrid-mode contract — `supports()` stays strict

`supports(Class<?>)` returns `true` **only** for actually-registered (or
intrinsic) types. In hybrid mode (AOT @20 + `garganttua-runtime-reflection`
@10), the runtime provider keeps ownership of types the AOT processor
didn't see — its full reflection beats our shallow synthesis.

The fallback synthesis fires only when AOT is the sole provider
(pure-AOT mode, native-image).

### `CoreInfrastructureSeed` — what's pre-registered

Seeded under the principle "things the framework's own classes resolve
at static-init time, before user code runs". The list is documented
in-source; current contents:

| Group | Examples |
|---|---|
| Builder interfaces | `IReflectionBuilder`, `IObservabilityBuilder`, `IInjectionContextBuilder`, `IExpressionContextBuilder`, `IRuntimesBuilder`, `IScriptsBuilder`, `IWorkflowsBuilder`, `IConditionBuilder` |
| Full injection surface | `IBeanFactory`, `IBeanProvider`, `IBeanQuery`, `IInjectionContext`, `IInjectionChildContextFactory`, `IPropertyProvider`, … (13 interfaces) |
| Crypto interfaces | `IKey`, `IHash`, `IKeyAlgorithm`, `IKeyRealm`, `IKeyRealmBuilder` |
| Framework annotations | `@Reflected`, `@Indexed`, `@Expression`, `@BeanProvider`, `@Property`, `@Step`, `@RuntimeDefinition`, `@WorkflowDefinition`, `@ScriptDefinition`, `@Mutex`, `@FieldMappingRule`, … (~30 annotations) |
| Jakarta / JSR-330 | `@Nullable`, `@Nonnull`, `@Inject`, `@Qualifier`, `@Singleton`, `@Scope`, `@Named`, `javax.inject.Provider` |
| JDK essentials | `Map`, `List`, `Set`, `Collection`, `Iterable`, `String`, `Object`, `Optional`, `UUID`, primitives, arrays, void |

The list is **deliberately conservative** — types that warrant a rich
descriptor at startup. Anything not on the list is still covered by the
fallback path; the seed is an optimisation, not a correctness contract.

## Usage

```bash
mvn clean install -pl garganttua-aot/garganttua-aot-reflection
```

This module is **not meant to be added by consumers directly**. Pick a
starter instead:

- `garganttua-starter-aot` (pure AOT)
- `garganttua-starter-hybrid` (AOT + runtime fallback — recommended default)
- `garganttua-starter-native` (AOT + GraalVM Feature)

See `garganttua-starters/README.md` for the picker matrix.

## Tips and best practices

### Diagnosing a "type identity vs. member metadata" issue

If your code worked in runtime mode but returns empty `getDeclaredMethods()`
in AOT mode, the type was fallback-synthesized rather than registered:

- **Best fix** — `@Reflected` on the type (if it's a user class) so the
  annotation processor generates a full descriptor.
- **Alternative** — if it's a framework type you don't own, ship an
  `IAOTInfrastructureSeed` that uses `IAOTSeedContext.registry()` to
  install a pre-built descriptor (advanced; usually overkill — most
  framework wiring only needs type identity).

### Don't fight the fallback

Adding more types to `CoreInfrastructureSeed` to "pre-register everything"
is a losing game — the seed is already conservative for a reason
(predictable startup, explicit framework contract). Trust the fallback:
in pure-AOT JVM mode it is correct for type-identity resolution, which
is the dominant pattern.

### Native-image specifics

The native build path is different — `GarganttuaAotFeature.beforeAnalysis`
iterates `AOTRegistry.registeredClasses()` at **build time** and registers
each one with `RuntimeReflection`. Fallback-synthesized types are only
known at **runtime**, too late for the Feature.

This means in native mode you need to ensure every type that needs
introspection (or even just `Class.forName`-by-string at runtime) is in
the registry **before the image is built**:

- User types annotated with `@Reflected` → handled.
- Framework types declared by `CoreInfrastructureSeed` or extension seeds → handled.
- Types reached only via dynamic name resolution at runtime → NOT handled.
  Either annotate them, or ship a seed.

### Hybrid mode safety

The hybrid mode is the safest production default. It lets:

- AOT win for `@Reflected` user types (rich, processor-generated descriptors).
- AOT win for framework-seeded types (rich seeded descriptors).
- Runtime win for everything else (full JDK reflection, slowest but most complete).

You only switch to pure-AOT when you've validated cold-start and
introspection needs against your actual workload.

## License
This module is distributed under the MIT License.
