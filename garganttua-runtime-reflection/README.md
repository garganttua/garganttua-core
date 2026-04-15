# Garganttua Runtime Reflection

## Description

Runtime reflection utilities for Garganttua Core. This module provides `RuntimeReflectionProvider`, the standard JVM runtime implementation of the `IReflectionProvider` interface defined in `garganttua-commons`. It uses `java.lang.reflect` to resolve classes, inspect fields, methods, constructors, and annotations at runtime.

This is the default reflection provider used throughout the framework. Alternative providers (e.g., AOT-based) can be composed alongside it via `ReflectionBuilder` with priority-based selection.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-runtime-reflection</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-runtime-reflection
```

## License
This module is distributed under the MIT License.
