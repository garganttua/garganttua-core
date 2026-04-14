# Garganttua AOT Maven Plugin

## Description

Maven plugin for Garganttua AOT processing. This plugin integrates AOT descriptor generation into the Maven build lifecycle, allowing projects to automatically produce ahead-of-time metadata during compilation.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-maven-plugin</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `org.apache.maven:maven-plugin-api`
 - `org.apache.maven:maven-core`
 - `org.apache.maven.plugin-tools:maven-plugin-annotations`

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-aot/garganttua-aot-maven-plugin
```

## License
This module is distributed under the MIT License.
