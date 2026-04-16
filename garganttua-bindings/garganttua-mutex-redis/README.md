# Garganttua Mutex Redis

## Description

Distributed mutex implementation over Redis for Garganttua Core. This binding module provides a Redis-backed locking mechanism that extends the `garganttua-mutex` abstractions, enabling distributed mutual exclusion across multiple JVM instances.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-mutex-redis</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.github.siahsang:red-utils`
 - `com.garganttua.core:garganttua-mutex`
 - `org.junit.jupiter:junit-jupiter-engine:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

## Usage

```bash
mvn clean install -pl garganttua-bindings/garganttua-mutex-redis
```

## Tips and best practices

## License
This module is distributed under the MIT License.
