# Garganttua Native

## Description

Parent module for Garganttua Native support. This POM aggregates submodules that provide GraalVM native image configuration generation, annotation processing, and build tooling for producing native executables from Garganttua applications.

### Submodules

- `garganttua-native-commons` - Shared native image interfaces and types
- `garganttua-native-annotation-processor` - Compile-time processor for native image configuration
- `garganttua-native-image-maven-plugin` - Maven plugin for native image builds

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-bindings/garganttua-native
```

## License
This module is distributed under the MIT License.
