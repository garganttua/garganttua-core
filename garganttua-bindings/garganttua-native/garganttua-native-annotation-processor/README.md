# Garganttua Native Annotation Processor

## Description

Compile-time annotation processor for GraalVM native image configuration generation. This processor scans classes annotated with `@Reflected` and related annotations to automatically produce the reflection, resource, and proxy configuration files required by GraalVM's `native-image` tool.

**Note**: This module disables annotation processing (`-proc:none`) in its own build to avoid self-processing.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native-annotation-processor</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-bindings/garganttua-native/garganttua-native-annotation-processor
```

## License
This module is distributed under the MIT License.
