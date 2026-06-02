# Garganttua AOT Native Feature

## Description

GraalVM native-image
[Feature](https://www.graalvm.org/latest/reference-manual/native-image/Limitations/#features)
that registers every `AOTRegistry`-known type with `RuntimeReflection`
at native-image **analysis time** — replaces the need for a hand-written
or mojo-generated `reflect-config.json` on top of the consumer's
existing AOT pipeline.

Single source of truth: the same `AOTClass_*` source files that the
annotation processor generates for runtime `IClass.getClass(...)`
drive the native-image reflection config too.

## Installation

This module is auto-pulled by
[`garganttua-starter-native`](../../garganttua-starters/garganttua-starter-native/README.md).
You typically don't add it directly — pick the starter instead.

If you want it standalone (e.g. you're already using the AOT stack
explicitly without going through the starter):

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-native-feature</artifactId>
    <version>${garganttua.core.version}</version>
</dependency>
```

The Feature auto-activates through
`META-INF/native-image/com.garganttua.core/garganttua-aot-native-feature/native-image.properties`
— no command-line flag needed in your `native-maven-plugin` config.

## How it works

`GarganttuaAotFeature.beforeAnalysis(...)`:

1. Loads `AOTReflectionProvider`, which triggers
   `CoreInfrastructureSeed.bootstrap()`:
   - Pre-registers the framework's own infrastructure interfaces
     (`IReflectionBuilder`, `IObservabilityBuilder`, …)
   - Pre-registers JDK collection interfaces + primitives + common
     types (`Map`, `List`, `String`, `byte`, `byte[]`, …)
   - `ServiceLoader.load(IAOTSelfRegistering.class).iterator()` fires
     each consumer-generated `AOTClass_*`'s static init, registering
     them into `AOTRegistry`
2. Iterates `AOTRegistry.registeredClasses()` and for each entry:
   - `RuntimeReflection.register(Class)` so native-image keeps the
     type in the closed-world image
   - `RuntimeReflection.register(declaredConstructors / methods / fields)`
     for reflective access at runtime

Conservative-by-default: every member of every registered class is
exposed for reflection. The cost is a slight image-size increase, never
a correctness gap. Future tuning could read the original `@Reflected`
flags to scope this more tightly per-class.

## Requirements

- GraalVM JDK 25+
- `native-maven-plugin` wired in the consumer's `<build>` (see the
  native starter's README)

## License

MIT.
