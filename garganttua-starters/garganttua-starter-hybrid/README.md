# Garganttua Starter — Hybrid

## Description

Belt-and-suspenders reflection stack: AOT prioritised at
`@Priority(20)`, runtime / `org.reflections` fallback at
`@Priority(10)` for types the AOT processor didn't see. **The
recommended default for development.**

When `IClass.getClass(SomeType.class)` runs:
1. AOT provider tries first — instant hit if `SomeType` has an
   `AOTClass_*` descriptor in `AOTRegistry`
2. Runtime provider falls back if not — full JDK reflection resolves
   anything reachable on the classpath

## Installation

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-starter-hybrid</artifactId>
    <version>${garganttua.core.version}</version>
    <type>pom</type>
</dependency>
```

Pulls transitively both
[`garganttua-starter-aot`](../garganttua-starter-aot/README.md) and
[`garganttua-starter-runtime`](../garganttua-starter-runtime/README.md).

## Expected startup line

```
SPI bootstrap: providers=[AOTReflectionProvider@20, RuntimeReflectionProvider@10],
               scanners=[AOTAnnotationScanner@20, ReflectionsAnnotationScanner@10]
```

## When to pick this starter

- Default for development — you get AOT speed when descriptors are
  available, runtime forgiveness when they're not (e.g. you added a new
  `@Reflected` class but haven't re-run the annotation processor yet)
- Production with mixed AOT coverage — internal types AOT'd, external
  library types resolved via runtime
- Migration scenarios — switch from runtime to AOT a few classes at a
  time, hybrid keeps the app booting until coverage is complete

## When to pick something else

- Pure cold-start optimisation → `garganttua-starter-aot` (no
  org.reflections JAR on the classpath = smaller fat-jar, no scan cost)
- GraalVM native-image → `garganttua-starter-native` (closed-world
  forbids the runtime fallback path anyway)

## License

MIT.
