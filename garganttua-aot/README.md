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

AOT pipeline consumes the same annotations as the GraalVM pipeline, decoupling **what the code does** from **how it's built**:

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

### Module Responsibilities

- **`garganttua-aot-commons`** — interfaces for the AOT registry, reflection provider, annotation scanner; conventions and file formats for `META-INF/garganttua/aot/*`.
- **`garganttua-aot-reflection`** — pre-generated `IClass<T>` / `IMethod` / `IField` / `IConstructor` implementations for classes annotated `@Reflected`.
- **`garganttua-aot-annotation-scanner`** — pre-indexed `IAnnotationScanner` reading the AOT index files.
- **`garganttua-aot-annotation-processor`** — compile-time code generator that produces the descriptors and direct binders. Internally split into per-member source generators (`AOTConstructorSourceGenerator`, `AOTFieldSourceGenerator`, `AOTMethodSourceGenerator`) with shared helpers (`AOTNaming`, `TypeNames`, `MemberInclusion`).
- **`garganttua-aot-maven-plugin`** — Maven binding for the processor.

## Usage

```bash
mvn clean install -pl garganttua-aot
```

Annotate code that should be available without runtime reflection:

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

The annotation processor scans these at compile time and emits AOT descriptors under `target/generated-sources/`.

## Tips and best practices

- Co-design `@Reflected` annotations on records and concrete builders early — retrofitting after the fact requires re-running the processor and inspecting generated sources.
- Keep `@ReflectedBuilder` reports honest: anything reflected on at runtime that isn't reported won't be in the AOT descriptors and will fail at runtime.
- The AOT pipeline is **work in progress** — until `CompositeReflection` integration lands, runtime reflection (`garganttua-runtime-reflection`) is still required.

## License
This module is distributed under the MIT License.
