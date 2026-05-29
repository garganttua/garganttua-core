# Garganttua Starter — AOT

## Description

Pure-AOT reflection stack for Garganttua applications running on a plain
JVM. Cold-start optimised: no runtime classpath scanning, no JDK
reflection — every `IClass.getClass(...)` lookup hits the
`AOTRegistry` populated at compile time by
`garganttua-aot-annotation-processor`.

This starter is also the foundation of
[`garganttua-starter-native`](../garganttua-starter-native/README.md).

## Installation

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-starter-aot</artifactId>
    <version>${garganttua.core.version}</version>
    <type>pom</type>
</dependency>
```

Pulls transitively:
- `garganttua-aot-reflection` (provider `AOTReflectionProvider@20`)
- `garganttua-aot-annotation-scanner` (scanner `AOTAnnotationScanner@20`)

## Expected startup line

```
SPI bootstrap: providers=[AOTReflectionProvider@20],
               scanners=[AOTAnnotationScanner@20]
```

## Required at compile time

To AOT-process your own `@Reflected` classes, wire
`garganttua-aot-annotation-processor` in your `maven-compiler-plugin`'s
`<annotationProcessorPaths>` with `-Agarganttua.direct.binders=true` —
see that module's README. The runtime starter does NOT auto-configure
the compile-time processor (different concern).

## When to pick this starter

- You target a plain JVM but want production-grade cold-start (~100ms
  off vs. the runtime starter on a non-trivial app)
- You're preparing for a future native-image build
- You want the AOT registry to be the single source of truth for type
  discovery (debuggable, deterministic)

## License

MIT.
