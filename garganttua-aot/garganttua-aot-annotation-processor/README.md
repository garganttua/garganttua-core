# Garganttua AOT Annotation Processor

## Description

Compile-time annotation processor for generating AOT class descriptors. This processor runs during compilation to produce metadata files that describe annotated classes, enabling ahead-of-time resolution without runtime classpath scanning.

**Note**: This module disables annotation processing (`-proc:none`) in its own build to avoid self-processing.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-annotation-processor</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

## Usage

```bash
mvn clean install -pl garganttua-aot/garganttua-aot-annotation-processor
```

## Tips and best practices

## License
This module is distributed under the MIT License.
