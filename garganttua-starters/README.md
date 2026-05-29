# Garganttua Starters

## Description

Aggregator for the four **consumption starters** that bundle the reflection
provider + annotation scanner stacks a downstream Garganttua application
needs. Each starter is a `pom`-packaged module — no classes, just a
`<dependencies>` block. Switching reflection mode is a one-line change of
the starter coordinate in your application's `pom.xml`.

## The four modes

| Mode | Starter coordinate | Provider(s) @priority | Scanner(s) @priority | Target |
|---|---|---|---|---|
| **AOT** | `garganttua-starter-aot` | `AOTReflectionProvider@20` | `AOTAnnotationScanner@20` | JVM, AOT-only (cold-start optimised, prep for native-image) |
| **Runtime** | `garganttua-starter-runtime` | `RuntimeReflectionProvider@10` | `ReflectionsAnnotationScanner@10` | Legacy / fastest dev loop, no AOT processor required |
| **Hybrid** | `garganttua-starter-hybrid` | both | both | Recommended default — AOT prioritised, runtime as fallback for types the processor didn't see |
| **Native** | `garganttua-starter-native` | `AOTReflectionProvider@20` + GraalVM Feature | `AOTAnnotationScanner@20` | GraalVM native-image builds |

## Usage

In your application's `pom.xml`, pick one:

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-starter-aot</artifactId>     <!-- or -runtime, -hybrid, -native -->
    <version>${garganttua.core.version}</version>
    <type>pom</type>
</dependency>
```

The starter pulls every transitive dep you need for that mode. No
`<exclusion>`, no Maven profile duplication, no boilerplate.

To switch mode, change the coordinate suffix. To pin the mode at a
profile level rather than at the pom root, declare each starter inside
its own `<profile>` and activate with `mvn -Pxxx` — but **the single-dep
form above is preferred** because it's visible to code review and
reproducible without a CLI flag.

## Caveats

### `garganttua-starter-native` requires the GraalVM build plugin

The native starter ships the Feature that auto-registers AOT descriptors
with `RuntimeReflection` at native-image analysis time — that part is
automatic. But the actual native-image build still requires the
`native-maven-plugin` in your `<build>` section:

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>${native.maven.plugin.version}</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>build-native</id>
            <goals><goal>compile-no-fork</goal></goals>
            <phase>package</phase>
        </execution>
    </executions>
</plugin>
```

Typically wrapped in a `<profile id="native">` and activated with
`mvn -Pnative package`.

The three other starters need no build-plugin addition.

### Where the AOT processor itself runs

The reflection stack at runtime is one half of the AOT story. The other
half — generating `AOTClass_*` source descriptors at compile time for
the consumer's `@Reflected` classes — is driven by
`garganttua-aot-annotation-processor`, which the consumer wires through
the `maven-compiler-plugin`'s `<annotationProcessorPaths>` with
`-Agarganttua.direct.binders=true`. See
`garganttua-aot/garganttua-aot-annotation-processor/README.md` for the
exact snippet.

The starters do NOT auto-configure the annotation processor — that's a
build-plugin concern, not a runtime classpath concern.

## What's expected in the startup banner

Look at the `SPI bootstrap` log line at app startup to confirm the
discovered stack matches the starter you picked:

```
garganttua-starter-aot:
  SPI bootstrap: providers=[AOTReflectionProvider@20], scanners=[AOTAnnotationScanner@20]

garganttua-starter-runtime:
  SPI bootstrap: providers=[RuntimeReflectionProvider@10], scanners=[ReflectionsAnnotationScanner@10]

garganttua-starter-hybrid:
  SPI bootstrap: providers=[AOTReflectionProvider@20, RuntimeReflectionProvider@10],
                 scanners=[AOTAnnotationScanner@20, ReflectionsAnnotationScanner@10]

garganttua-starter-native:
  (same as -aot, plus the native-image build registers reflect-config from AOTRegistry)
```

If you see a mismatch, check that you have **exactly one** starter on
the classpath and no manual `garganttua-aot-reflection` /
`garganttua-runtime-reflection` deps competing with it.

## License

This module aggregator is distributed under the MIT License.
