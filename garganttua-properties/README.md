# Garganttua Properties

## Description

Property provider that loads `.properties` files into the injection context.

The `garganttua-properties` module bridges standard Java `.properties` files with the Garganttua dependency injection system. It implements `IPropertyProviderBuilder` to load properties from classpath resources (inside JARs) and filesystem files, making them available for `@Property` injection.

### Key Features

- **Classpath Loading**: Auto-detects `application.properties` in all JARs on the classpath
- **Filesystem Loading**: Loads properties from absolute or relative file paths
- **Multi-Source Merging**: Combines properties from multiple sources; later sources override earlier ones
- **JAR Aggregation**: When the same resource exists in multiple JARs, all are loaded and merged
- **Linked Builder**: Integrates seamlessly with `IInjectionContextBuilder` via the linked builder pattern
- **Auto-Detection**: When enabled, automatically discovers `application.properties` on the classpath

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-properties</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-reflections:test`
 - `com.garganttua.core:garganttua-runtime-reflection:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

## Usage

### Auto-Detect application.properties

```java
injectionContextBuilder
    .propertyProvider("config",
        PropertiesFileProviderBuilder.create(injectionContextBuilder)
            .autoDetect(true));
```

This discovers all `application.properties` files on the classpath (including inside JARs) and merges them.

### Load from Filesystem

```java
injectionContextBuilder
    .propertyProvider("config",
        PropertiesFileProviderBuilder.create(injectionContextBuilder)
            .file("/etc/myapp/config.properties"));
```

### Classpath + Filesystem Override

```java
injectionContextBuilder
    .propertyProvider("config",
        PropertiesFileProviderBuilder.create(injectionContextBuilder)
            .classpathResource("defaults.properties")
            .file("/etc/myapp/override.properties"));
```

Properties from the filesystem file override those from the classpath resource.

### Injecting Properties

Once the provider is registered, properties are injectable via `@Property`:

```java
public class DatabaseService {

    @Property("database.url")
    private String databaseUrl;

    @Property("database.pool.size")
    private int poolSize;
}
```

### Source Priority

Sources are loaded in declaration order. Later sources override earlier ones:

| Order | Source | Method |
|-------|--------|--------|
| 1 (lowest) | Auto-detected `application.properties` | `autoDetect(true)` |
| 2 | Explicit classpath resource | `classpathResource("x.properties")` |
| 3 (highest) | Filesystem file | `file("/path/to/x.properties")` |

### Testing

```bash
mvn test -pl garganttua-properties
```

Test coverage includes:
- Auto-detection of `application.properties` from classpath
- Explicit classpath resource loading
- Filesystem file loading
- Multi-source merging with override semantics
- Missing file handling (graceful skip)
- Empty builder produces empty provider
- Property key enumeration

## Tips and best practices

## License
This module is distributed under the MIT License.
