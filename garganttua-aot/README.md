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
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-aot
```

## License
This module is distributed under the MIT License.
