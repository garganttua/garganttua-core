# Garganttua AOT Annotation Scanner

## Description

Compile-time annotation scanner for AOT class descriptor generation. This module scans annotated classes at build time and produces descriptor metadata that can be used at runtime without classpath scanning, improving startup performance in AOT-compiled applications.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-aot-annotation-scanner</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-runtime-reflection`

<!-- AUTO-GENERATED-END -->

## Build

```bash
mvn clean install -pl garganttua-aot/garganttua-aot-annotation-scanner
```

## License
This module is distributed under the MIT License.
