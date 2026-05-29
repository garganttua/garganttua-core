# Garganttua Starter — Runtime

## Description

Legacy / dev-loop reflection stack for Garganttua applications. JDK
reflection (`Class.forName`, `Class.getMethods`…) + `org.reflections`
classpath scanning. No AOT processor required at compile time, the
quickest iteration cycle.

## Installation

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-starter-runtime</artifactId>
    <version>${garganttua.core.version}</version>
    <type>pom</type>
</dependency>
```

Pulls transitively:
- `garganttua-runtime-reflection` (provider `RuntimeReflectionProvider@10`)
- `garganttua-reflections` (scanner `ReflectionsAnnotationScanner@10`)

## Expected startup line

```
SPI bootstrap: providers=[RuntimeReflectionProvider@10],
               scanners=[ReflectionsAnnotationScanner@10]
```

## When to pick this starter

- Fastest dev iteration — no annotation processor, no AOT JAR
  regeneration on each `@Reflected` change
- Quick scratch / spike apps where startup time doesn't matter
- Drop-in fallback when an AOT mode reveals a bug to investigate

## Tradeoffs

- Startup time: slowest of the three JVM modes (classpath scan +
  cold JIT)
- Native-image: NOT viable — the runtime providers rely on
  Class.forName(stringFromBean) patterns that GraalVM's closed-world
  rejects. Use `garganttua-starter-native` for that target.

## License

MIT.
