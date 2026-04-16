# Garganttua Spring

## Description

Spring framework integration for Garganttua Core modules. This binding module bridges Garganttua's dependency injection, reflection, and annotation scanning abstractions with the Spring Framework, allowing Garganttua components to operate within a Spring application context.

Spring dependencies are declared with `provided` scope, so the consuming application must supply its own Spring runtime.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-spring</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-runtime-reflection`
 - `org.springframework:spring-core:provided`
 - `org.springframework:spring-context:provided`

<!-- AUTO-GENERATED-END -->

## Core Concepts

## Usage

```bash
mvn clean install -pl garganttua-bindings/garganttua-spring
```

## Tips and best practices

## License
This module is distributed under the MIT License.
