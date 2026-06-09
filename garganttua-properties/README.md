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
- **Placeholder Resolution**: `${VAR:default}` syntax resolved against JVM system properties, environment variables, other loaded properties, and the optional default — multiple placeholders per value supported

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-properties</artifactId>
    <version>2.0.0-ALPHA03</version>
</dependency>
```

### Actual version
2.0.0-ALPHA03

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-reflections:test`
 - `com.garganttua.core:garganttua-runtime-reflection:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### PropertiesFileProviderBuilder

The single entry point for declaring property sources. Implements `IPropertyProviderBuilder` (so it plugs into the DI container) and is `@PropertyProviderAnnotation`-annotated, allowing auto-detection by `InjectionContextBuilder.autoDetect(true)`.

Sources can be added in three ways:
- `autoDetect(true)` — scan the classpath for every `application.properties` (including inside JARs)
- `classpathResource("name.properties")` — load a named resource from the classpath
- `file("/abs/path")` or `file("relative/path")` — load from the filesystem

### Source Merging

Sources are loaded in declaration order. Properties from later sources override earlier ones. When the same classpath resource exists in multiple JARs, all occurrences are loaded and merged.

### Placeholder Resolution

Values may contain one or more `${VAR}` or `${VAR:default}` placeholders. Resolution order (first hit wins):
1. JVM system property (`-Dkey=value`)
2. Environment variable
3. Another loaded property
4. The default value (if provided)

If none resolves and there's no default, the placeholder is left intact (no exception).

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

### Placeholder Resolution

```properties
# application.properties
database.host=${DB_HOST:localhost}
database.url=jdbc:postgresql://${database.host}:${DB_PORT:5432}/mydb
log.dir=${user.home}/logs
```

Resolution order: JVM system property → environment variable → other loaded property → default. Multiple placeholders per value are supported.

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

- Prefer `autoDetect(true)` in production — every JAR can ship its own `application.properties` for sensible defaults, then a filesystem override (`file("/etc/myapp/config.properties")`) carries the deployment-specific values.
- Use `${VAR:default}` for any value that depends on the environment — keeps `application.properties` deployable as-is across dev/staging/prod.
- Combine `garganttua-properties` with `@PropertyProviderAnnotation` auto-detection in `garganttua-injection`: declaring a `@PropertyProviderAnnotation("scope")` on a builder lets `DiContext.autoDetect(true)` register it without explicit wiring.
- Missing files are silently skipped — handy for optional override files, but verify your sources are loading by enabling DEBUG logs on the provider class if a property seems missing.
- Placeholders are resolved lazily at lookup time; you can override a placeholder by setting `-Dproperty.name=...` on the JVM command line without rebuilding the JAR.

## License
This module is distributed under the MIT License.
