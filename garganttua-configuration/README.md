# :gear: Garganttua Configuration

Multi-format configuration loading and builder population for Garganttua DSLs.

## Description

The `garganttua-configuration` module provides a unified API for loading configuration from multiple sources (files, classpath, environment variables, inline strings) in multiple formats (JSON, YAML, XML, TOML, Properties), and automatically populating Garganttua builder objects via intelligent method mapping.

### Key Features

- **Multi-Format Support**: JSON (built-in), YAML, XML, TOML, Properties (optional, auto-detected on classpath)
- **Multiple Sources**: File system, classpath, input stream, inline string, environment variables
- **Smart Method Mapping**: Automatic binding of configuration keys to builder methods with support for camelCase, kebab-case, snake_case, and `@ConfigProperty` annotations
- **Recursive Builder Population**: Handles nested `IBuilder`/`ILinkedBuilder` hierarchies with automatic `up()` navigation
- **Type Conversion**: 20+ target types including primitives, temporal types, URI/URL, UUID, enums, and `Class<?>`
- **Strict/Lax Modes**: Strict mode fails on unknown keys; lax mode logs warnings
- **DI Integration**: `ConfigurationPropertyProvider` adapts configuration trees as `IPropertyProvider` for the injection module
- **Fluent DSL**: Type-safe builder API following the Garganttua hierarchical builder pattern

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-configuration</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-reflection`
 - `com.fasterxml.jackson.core:jackson-databind`
 - `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:provided`
 - `com.fasterxml.jackson.dataformat:jackson-dataformat-xml:provided`
 - `com.fasterxml.jackson.dataformat:jackson-dataformat-properties:provided`
 - `com.fasterxml.jackson.dataformat:jackson-dataformat-toml:provided`
 - `com.garganttua.core:garganttua-injection:provided`
 - `org.slf4j:slf4j-simple:test`

<!-- AUTO-GENERATED-END -->

### Optional Format Libraries

Format support is **conditional** on classpath availability. Add the corresponding Jackson dataformat dependency to enable each format:

| Format | Dependency | Extensions |
|---|---|---|
| JSON | `jackson-databind` (included) | `.json` |
| YAML | `jackson-dataformat-yaml` | `.yaml`, `.yml` |
| XML | `jackson-dataformat-xml` | `.xml` |
| TOML | `jackson-dataformat-toml` | `.toml` |
| Properties | `jackson-dataformat-properties` | `.properties` |

## Usage

### Quick Start

```java
import com.garganttua.core.configuration.dsl.ConfigurationBuilder;
import com.garganttua.core.configuration.source.StringConfigurationSource;

// 1. Build a populator (auto-detects available formats)
var populator = ConfigurationBuilder.builder().build();

// 2. Define a builder
public class ServerBuilder implements IBuilder<Server> {
    private String name;
    private int port;
    private boolean debug;

    public ServerBuilder name(String name) { this.name = name; return this; }
    public ServerBuilder withPort(int port) { this.port = port; return this; }
    public ServerBuilder debug(boolean debug) { this.debug = debug; return this; }

    @Override
    public Server build() { return new Server(name, port, debug); }
}

// 3. Populate the builder from JSON
var json = """
        {"name": "myApp", "port": 8080, "debug": true}
        """;
var source = new StringConfigurationSource(json, "json");
var builder = new ServerBuilder();

populator.populate(builder, source);
// builder.name = "myApp", builder.port = 8080, builder.debug = true
```

### DSL Builder Configuration

```java
var populator = ConfigurationBuilder.builder()
        .withFormat(new JsonConfigurationFormat())   // explicit format
        .withMappingStrategy("SMART")                // SMART, DIRECT, CAMEL_CASE, KEBAB_CASE
        .strict(true)                                // fail on unknown keys
        .source()
            .classpath("config/app.yaml")
            .up()
        .build();
```

### Configuration Sources

```java
import com.garganttua.core.configuration.source.*;

// From file system
var fileSource = new FileConfigurationSource("/etc/app/config.yaml");
var fileSource2 = new FileConfigurationSource(Path.of("config.json"));

// From classpath
var cpSource = new ClasspathConfigurationSource("config/app.json");

// From inline string
var inlineSource = new StringConfigurationSource("{\"port\": 8080}", "json");

// From input stream
var streamSource = new InputStreamConfigurationSource(inputStream, "yaml");

// From environment variables
var envSource = new EnvironmentConfigurationSource();             // all env vars
var envSource2 = new EnvironmentConfigurationSource("APP_");      // filter by prefix
```

Environment variables are normalized: keys are lowercased and `_` is replaced with `.` (e.g., `APP_DATABASE_HOST` becomes `database.host` with prefix `APP_`).

### Nested Builder Population

The populator automatically detects child builders (`IBuilder` / `ILinkedBuilder`) and recurses:

```java
public class AppBuilder implements IBuilder<App> {
    private String name;

    public AppBuilder name(String name) { this.name = name; return this; }
    public DatabaseBuilder database() { return new DatabaseBuilder(this); }

    @Override
    public App build() { ... }
}

public class DatabaseBuilder extends AbstractLinkedBuilder<AppBuilder, Void> {
    private String host;
    private int port;

    public DatabaseBuilder host(String host) { this.host = host; return this; }
    public DatabaseBuilder port(int port) { this.port = port; return this; }
}
```

```json
{
    "name": "myApp",
    "database": {
        "host": "localhost",
        "port": 5432
    }
}
```

The populator calls `database()` to obtain the child builder, populates it recursively, then calls `up()` to return to the parent.

### Array Handling

Arrays are handled in three ways depending on the target method signature:

```java
// 1. Repeated calls: method(String) called once per element
public MyBuilder tag(String tag) { this.tags.add(tag); return this; }

// 2. List parameter: method(List) called once with full list
public MyBuilder tags(List<String> tags) { this.tags = tags; return this; }

// 3. Array parameter: method(String[]) called once with array
public MyBuilder tags(String[] tags) { this.tags = Arrays.asList(tags); return this; }
```

```json
{"tags": ["web", "api", "rest"]}
```

Arrays of objects are supported too: each element creates a child builder via repeated method calls.

## Method Mapping

The populator resolves configuration keys to builder methods using a priority chain:

| Priority | Strategy | Example Key | Resolves To |
|---|---|---|---|
| 1 | `@ConfigProperty` annotation | `"custom-key"` | `@ConfigProperty("custom-key") customMethod(...)` |
| 2 | Direct name match | `"name"` | `name(...)` |
| 3 | `with` prefix | `"port"` | `withPort(...)` |
| 4 | camelCase conversion | `"max_retries"` or `"max.retries"` | `maxRetries(...)` |
| 5 | camelCase + `with` prefix | `"max_retries"` | `withMaxRetries(...)` |
| 6 | kebab-case conversion | `"connection-timeout"` | `connectionTimeout(...)` |
| 7 | kebab-case + `with` prefix | `"connection-timeout"` | `withConnectionTimeout(...)` |

Strategies 4-7 are only available with `SMART` mapping strategy (the default). `DIRECT` only uses strategies 1-3.

### Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@ConfigProperty("key")` | Method | Explicitly maps a config key to a builder method |
| `@ConfigIgnore` | Method | Excludes a method from configuration mapping |
| `@Configurable` | Type | Marks a class as configurable (indexed at compile-time via `@Indexed`) |
| `@ConfigurationFormat` | Type | Marks a format implementation for runtime discovery |

## Type Conversion

String configuration values are automatically converted to the target method parameter type:

| Category | Supported Types |
|---|---|
| Primitives & Wrappers | `int`, `long`, `double`, `float`, `boolean`, `byte`, `short`, `char` |
| Big Numbers | `BigDecimal`, `BigInteger` |
| Temporal | `Duration`, `Period`, `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime` |
| IO / Net | `Path`, `URI`, `URL` |
| Misc | `UUID`, `Class<?>`, any `Enum` (case-insensitive) |

## DI Integration

`ConfigurationPropertyProvider` adapts a parsed configuration tree into an `IPropertyProvider` for the injection module:

```java
import com.garganttua.core.configuration.integration.ConfigurationPropertyProvider;

var node = new JsonConfigurationFormat().parse(inputStream);
var provider = new ConfigurationPropertyProvider(node);

// Flattened dot-notation access
provider.getProperty("database.host", String.class);   // Optional<"localhost">
provider.getProperty("database.port", Integer.class);   // Optional<5432>
provider.getProperty("tags[0]", String.class);           // Optional<"web">

// Metadata
provider.keys();        // Set<"database.host", "database.port", "tags[0]", ...>
provider.isMutable();   // false (immutable)
provider.copy();        // independent copy
```

Nested objects are flattened with `.` separators, arrays with `[index]` notation.

## Architecture

### Module Structure

```
garganttua-configuration/
├── src/main/java/com/garganttua/core/configuration/
│   ├── annotations/            # @Configurable, @ConfigProperty, @ConfigIgnore, @ConfigurationFormat
│   ├── dsl/                    # ConfigurationBuilder, ConfigurationSourceBuilder
│   ├── format/                 # AbstractConfigurationFormat, Json/Yaml/Xml/Toml/Properties formats
│   ├── integration/            # ConfigurationPropertyProvider (DI bridge)
│   ├── node/                   # ConfigurationNode (Jackson JsonNode adapter)
│   ├── populator/              # BuilderPopulator, MethodMapping, TypeConverter, PopulationContext
│   └── source/                 # File, Classpath, String, InputStream, Environment sources
└── src/test/
```

### Key Interfaces (in garganttua-commons)

| Interface | Purpose |
|---|---|
| `IConfigurationFormat` | Parses input streams into `IConfigurationNode` trees |
| `IConfigurationNode` | Tree representation of parsed configuration (OBJECT, ARRAY, VALUE, NULL) |
| `IConfigurationSource` | Provides an `InputStream` and format hint for a configuration source |
| `IConfigurationPopulator` | Populates `IBuilder<?>` instances from configuration data |

### Design Patterns

- **Hierarchical Builder**: `ConfigurationBuilder` / `ConfigurationSourceBuilder` with `up()` navigation
- **Adapter**: `ConfigurationNode` wraps Jackson's `JsonNode` behind `IConfigurationNode`
- **Strategy**: `MethodMappingStrategy` controls method resolution behavior
- **Auto-Detection**: Format classes use `isClassAvailable()` to check optional dependencies at runtime

## Testing

```bash
# Run all configuration tests
mvn test -pl garganttua-configuration

# Run specific test class
mvn test -pl garganttua-configuration -Dtest=BuilderPopulatorTest
```

Test coverage includes:
- `BuilderPopulatorTest` - flat/nested population, arrays, strict/lax modes
- `ConfigurationBuilderTest` - DSL builder, defaults, custom formats, strategies
- `ConfigurationNodeTest` - node types, children, arrays, paths, type conversion
- `ConfigurationPropertyProviderTest` - flattening, nesting, array indexing, type conversion
- `JsonConfigurationFormatTest` - format discovery, parsing, media types
- `MethodMappingTest` - annotation matching, naming conversions, strategies
- `TypeConverterTest` - all type conversions (temporal, URI, UUID, enums, etc.)

## License

This module is distributed under the MIT License.

---

**Version**: 2.0.0-ALPHA01
**Maintainer**: Garganttua Team
