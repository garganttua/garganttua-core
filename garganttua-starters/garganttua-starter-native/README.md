# Garganttua Starter — Native

## Description

GraalVM native-image reflection stack: pure AOT runtime (same as
[`garganttua-starter-aot`](../garganttua-starter-aot/README.md)) plus
[`garganttua-aot-native-feature`](../../garganttua-aot/garganttua-aot-native-feature/README.md)
that auto-registers every `AOTRegistry` entry with `RuntimeReflection`
at native-image **analysis time** — no hand-written `reflect-config.json`
needed.

## Installation — two pieces

### 1. The starter dep (this module)

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-starter-native</artifactId>
    <version>${garganttua.core.version}</version>
    <type>pom</type>
</dependency>
```

Pulls transitively:
- `garganttua-starter-aot` (AOT provider + scanner)
- `garganttua-aot-native-feature` (the GraalVM Feature, auto-activated
  via `META-INF/native-image/.../native-image.properties`)

### 2. The GraalVM build plugin (in your `<build>`)

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

Typically wrapped in a `<profile id="native">` so `mvn package` stays
fast for dev and `mvn -Pnative package` builds the binary.

## Build & run

```bash
# Local GraalVM JDK 25+ required.
mvn -Pnative package
./target/<your-app-name>
```

Expected: startup well under 100 ms cold, including the framework's
SPI cold-start + every `@*Definition` auto-detection (since reflection
is registered eagerly at build time).

## Required at compile time

Same as `garganttua-starter-aot`: the AOT annotation processor must
run during your own `maven-compiler-plugin` execution, with
`-Agarganttua.direct.binders=true`. The Feature only registers
descriptors that already exist in `AOTRegistry` — the processor is
what populates it.

## How the Feature works

At native-image analysis time (well before runtime), the Feature:

1. Triggers `AOTReflectionProvider`'s class init, which runs
   `CoreInfrastructureSeed.bootstrap()` → seeds the framework's own
   interfaces + JDK primitives/arrays AND loads every
   `IAOTSelfRegistering` service from `META-INF/services/...` to fire
   each `AOTClass_*`'s static initialiser. By the end of this step
   `AOTRegistry` knows every descriptor the consumer's processor
   generated.
2. Iterates `AOTRegistry.registeredClasses()` and for each FQN:
   - `RuntimeReflection.register(Class)` — keeps the type in the
     closed-world image
   - `RuntimeReflection.register(declaredConstructors / methods / fields)`
     — keeps members reflectively callable

Net effect: the same `AOTClass_*` source files that drive runtime
`IClass.getClass(...)` also drive the native-image reflect-config,
auto-aligned, no duplication, no manual JSON.

## Caveats

- **Java 25 GraalVM**: this is the only currently supported native build.
- **Image size**: every registered class adds bytes. If your app has
  hundreds of `@Reflected` entities, expect a 10–20 MB binary (still
  ~10× smaller than the equivalent JAR + JVM).
- **Org.reflections forbidden**: the Feature deliberately doesn't pull
  `garganttua-reflections` — its classpath-scan idioms aren't
  native-image-compatible.

## License

MIT.
