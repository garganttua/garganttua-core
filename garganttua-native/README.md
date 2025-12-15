# Garganttua Native

## Description

Garganttua Native is a **GraalVM Native Image configuration management library** that simplifies the creation and maintenance of reflection and resource configuration files required for building native executables with GraalVM.

When compiling Java applications to native images with GraalVM, reflection-based code and resources must be explicitly declared in configuration files (`reflect-config.json`, `resource-config.json`). Garganttua Native provides a fluent, programmatic API to generate and manage these configurations, eliminating manual JSON editing and reducing configuration errors.

**Key Features:**
- **Fluent Builder API** - Type-safe, chainable methods for creating reflection configurations
- **Automatic Configuration Management** - Load, modify, and save GraalVM configuration files programmatically
- **Annotation-based Registration** - Register fields and methods based on annotations
- **Reflection Metadata** - Full support for constructors, methods, fields, and classes
- **Resource Management** - Programmatically add/remove resources from native image bundles
- **JSON Serialization** - Automatic conversion to/from GraalVM's JSON configuration format
- **Deduplication** - Prevents duplicate entries in configuration files
- **Builder Pattern** - Incremental construction of complex reflection configurations

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons:provided`
 - `com.garganttua.core:garganttua-reflection:provided`
 - `com.garganttua.core:garganttua-reflections:provided`
 - `com.fasterxml.jackson.core:jackson-databind`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### GraalVM Native Image Configuration

GraalVM Native Image uses **closed-world assumption** - all classes, methods, and resources used at runtime must be known at build time. When using reflection, dynamic proxies, or resources, you must provide configuration files that declare them explicitly.

**Standard GraalVM Configuration Files:**
- `META-INF/native-image/reflect-config.json` - Reflection metadata (classes, methods, fields, constructors)
- `META-INF/native-image/resource-config.json` - Resources to include in native image (properties files, templates, etc.)

Garganttua Native provides APIs to generate and maintain these files programmatically.

### ReflectConfig

The `ReflectConfig` class represents the entire `reflect-config.json` file and manages a collection of `ReflectConfigEntry` objects. Each entry describes the reflection requirements for a single class.

**Capabilities:**
- **Load from file** - Parse existing `reflect-config.json` files
- **Add/Remove entries** - Manage individual class configurations
- **Update entries** - Modify existing reflection metadata
- **Save to file** - Write configuration back to JSON format
- **Find entries** - Query configuration by class name

**Example Structure:**
```json
[
  {
    "name": "com.example.MyClass",
    "allDeclaredFields": true,
    "allDeclaredMethods": true,
    "allDeclaredConstructors": true
  }
]
```

### ReflectConfigEntry

A `ReflectConfigEntry` represents the reflection configuration for a **single class**. It declares which constructors, methods, fields, and nested classes should be accessible via reflection at runtime.

**Configuration Options:**

| Property | Description |
|----------|-------------|
| `name` | Fully qualified class name (e.g., `com.example.User`) |
| `allDeclaredFields` | Allow reflection on all declared fields (public + private) |
| `allPublicFields` | Allow reflection on all public fields only |
| `allDeclaredMethods` | Allow reflection on all declared methods |
| `allDeclaredConstructors` | Allow reflection on all declared constructors |
| `allConstructors` | Allow instantiation via all constructors |
| `queryAllDeclaredConstructors` | Allow querying (not invoking) all declared constructors |
| `queryAllPublicConstructors` | Allow querying public constructors |
| `queryAllDeclaredMethods` | Allow querying all declared methods |
| `queryAllPublicMethods` | Allow querying public methods |
| `fields` | List of specific field names to enable for reflection |
| `methods` | List of specific methods with parameter types |

**Inner Classes:**
- `ReflectConfigEntry.Field` - Represents a single field with `name`
- `ReflectConfigEntry.Method` - Represents a method with `name` and `parameterTypes`

### ReflectConfigEntryBuilder

The `ReflectConfigEntryBuilder` provides a **fluent API** for constructing `ReflectConfigEntry` objects without manual JSON manipulation. It supports incremental configuration through method chaining.

**Builder Patterns:**
- **Broad access** - Enable all fields/methods/constructors at once
- **Selective access** - Register specific fields/methods/constructors by name
- **Annotation-based** - Register all members annotated with a specific annotation
- **Remove operations** - Unregister previously added members

**Example:**
```java
ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(MyClass.class)
    .allDeclaredFields(true)
    .method("getName")
    .method("setName", String.class)
    .constructor(String.class, int.class)
    .build();
```

### ResourceConfig

The `ResourceConfig` class manages the `resource-config.json` file, which declares resources (files, properties, etc.) to include in the native image.

**Capabilities:**
- **Add resources** - Register resource patterns for inclusion
- **Remove resources** - Unregister resource patterns
- **Pattern-based inclusion** - Uses regex patterns to match resources
- **Class-based registration** - Automatically compute resource path from Class object

**Example Structure:**
```json
{
  "resources": {
    "includes": [
      {"pattern": "\\Qconfig/application.properties\\E"},
      {"pattern": "\\Qcom/example/templates/template.html\\E"}
    ]
  }
}
```

### NativeImageConfig

The `NativeImageConfig` utility class provides helper methods to locate and create configuration files in the standard GraalVM directory structure (`META-INF/native-image/`).

**Methods:**
- `getReflectConfigFile(String baseDir)` - Returns `File` object for `reflect-config.json`
- `getResourceConfigFile(String baseDir)` - Returns `File` object for `resource-config.json`
- Automatically creates `META-INF/native-image/` directory if it doesn't exist

## Usage

### 1. Creating a Simple Reflection Configuration

Register a class with all declared fields and methods for reflection:

```java
import com.garganttua.core.nativve.image.config.reflection.*;

// Create an entry for MyClass with all declared fields and methods
ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(MyClass.class)
    .allDeclaredFields(true)
    .allDeclaredMethods(true)
    .allDeclaredConstructors(true)
    .build();

System.out.println(entry.getName()); // "com.example.MyClass"
System.out.println(entry.isAllDeclaredFields()); // true
```

### 2. Registering Specific Methods and Constructors

Precisely control which methods and constructors are accessible:

```java
class User {
    private String username;
    private String email;

    public User() {}
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
}

ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(User.class)
    .field("username")
    .field("email")
    .method("getUsername")  // No parameters
    .method("setUsername", String.class)  // With parameter type
    .method("getEmail")
    .constructor()  // No-arg constructor
    .constructor(String.class, String.class)  // Two-arg constructor
    .build();
```

### 3. Annotation-Based Field Registration

Register all fields annotated with a specific annotation:

```java
import javax.inject.Inject;

class ServiceClass {
    @Inject
    private DatabaseService dbService;

    @Inject
    private CacheService cacheService;

    private String internalState;  // Not annotated, won't be registered
}

ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(ServiceClass.class)
    .fieldsAnnotatedWith(Inject.class)  // Only dbService and cacheService
    .build();

// entry will contain fields: ["dbService", "cacheService"]
```

### 4. Annotation-Based Method Registration

Register methods based on annotations:

```java
import javax.annotation.PostConstruct;

class InitializableComponent {
    @PostConstruct
    public void initialize() {
        // Initialization logic
    }

    @PostConstruct
    public void setupResources() {
        // Setup logic
    }

    public void regularMethod() {
        // Regular method, not registered
    }
}

ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(InitializableComponent.class)
    .methodsAnnotatedWith(PostConstruct.class)
    .build();

// entry will contain methods: ["initialize", "setupResources"]
```

### 5. Loading and Modifying Existing Configuration

Load an existing `reflect-config.json`, modify it, and save:

```java
import com.garganttua.core.nativve.image.config.*;
import com.garganttua.core.nativve.image.config.reflection.*;
import java.io.File;

// Get the configuration file
File reflectConfigFile = NativeImageConfig.getReflectConfigFile("target/classes");

// Load existing configuration (or create empty if file doesn't exist)
ReflectConfig config = ReflectConfig.loadFromFile(reflectConfigFile);

// Add a new entry
ReflectConfigEntry newEntry = ReflectConfigEntryBuilder
    .builder(MyNewClass.class)
    .allDeclaredFields(true)
    .build();

config.addEntry(newEntry);

// Save back to file
config.saveToFile(reflectConfigFile);

System.out.println("Configuration saved to: " + reflectConfigFile.getAbsolutePath());
```

### 6. Updating an Existing Entry

Modify an existing class configuration:

```java
File reflectConfigFile = NativeImageConfig.getReflectConfigFile("target/classes");
ReflectConfig config = ReflectConfig.loadFromFile(reflectConfigFile);

// Find existing entry for UserService
Optional<ReflectConfigEntry> existingEntry = config.findEntryByName(UserService.class);

if (existingEntry.isPresent()) {
    // Modify the entry using the builder
    ReflectConfigEntry updated = ReflectConfigEntryBuilder
        .builder(existingEntry.get())  // Start from existing entry
        .method("newMethod", String.class, int.class)  // Add new method
        .field("newField")  // Add new field
        .build();

    // Update in configuration
    config.updateEntry(updated);
    config.saveToFile(reflectConfigFile);

    System.out.println("Entry updated for UserService");
} else {
    System.out.println("Entry not found for UserService");
}
```

### 7. Removing Configuration Entries

Remove a class from reflection configuration:

```java
File reflectConfigFile = NativeImageConfig.getReflectConfigFile("target/classes");
ReflectConfig config = ReflectConfig.loadFromFile(reflectConfigFile);

// Create an entry to remove (only name matters)
ReflectConfigEntry entryToRemove = new ReflectConfigEntry("com.example.ObsoleteClass");

config.removeEntry(entryToRemove);
config.saveToFile(reflectConfigFile);

System.out.println("Entry removed");
```

### 8. Removing Specific Fields and Methods

Remove specific members from a configuration entry:

```java
ReflectConfigEntry entry = ReflectConfigEntryBuilder
    .builder(MyClass.class)
    .field("field1")
    .field("field2")
    .field("field3")
    .method("method1")
    .method("method2")
    .removeField("field2")  // Remove specific field
    .removeMethod("method1")  // Remove specific method
    .build();

// entry now contains: fields ["field1", "field3"], methods ["method2"]
```

### 9. Managing Resource Configuration

Add resources to the native image:

```java
import com.garganttua.core.nativve.image.config.*;
import com.garganttua.core.nativve.image.config.resources.*;
import java.io.File;

// Get resource configuration file
File resourceConfigFile = NativeImageConfig.getResourceConfigFile("target/classes");

// Add a resource by class (automatically computes resource path)
ResourceConfig.addResource(resourceConfigFile, MyClass.class);
// Adds pattern: "\\Qcom/example/MyClass.class\\E"

// Add a resource by path
ResourceConfig.addResource(resourceConfigFile, "config/application.properties");
// Adds pattern: "\\Qconfig/application.properties\\E"

System.out.println("Resources registered for native image");
```

### 10. Removing Resources from Native Image

Remove resources from the configuration:

```java
File resourceConfigFile = NativeImageConfig.getResourceConfigFile("target/classes");

// Remove by class
ResourceConfig.removeResource(resourceConfigFile, ObsoleteClass.class);

// Remove by path
ResourceConfig.removeResource(resourceConfigFile, "old/config.properties");

System.out.println("Resources removed from native image configuration");
```

### 11. Complete Workflow: Build Configuration Programmatically

End-to-end example of building a complete native image configuration:

```java
import com.garganttua.core.nativve.image.config.*;
import com.garganttua.core.nativve.image.config.reflection.*;
import com.garganttua.core.nativve.image.config.resources.*;
import java.io.File;

public class NativeImageConfigGenerator {

    public static void main(String[] args) throws Exception {
        String baseDir = "target/classes";

        // 1. Setup reflection configuration
        File reflectConfigFile = NativeImageConfig.getReflectConfigFile(baseDir);
        ReflectConfig reflectConfig = ReflectConfig.loadFromFile(reflectConfigFile);

        // 2. Register domain classes
        ReflectConfigEntry userEntry = ReflectConfigEntryBuilder
            .builder(User.class)
            .allDeclaredFields(true)
            .allDeclaredConstructors(true)
            .build();
        reflectConfig.addEntry(userEntry);

        // 3. Register service classes with DI annotations
        ReflectConfigEntry serviceEntry = ReflectConfigEntryBuilder
            .builder(UserService.class)
            .fieldsAnnotatedWith(javax.inject.Inject.class)
            .methodsAnnotatedWith(javax.annotation.PostConstruct.class)
            .allDeclaredConstructors(true)
            .build();
        reflectConfig.addEntry(serviceEntry);

        // 4. Save reflection configuration
        reflectConfig.saveToFile(reflectConfigFile);
        System.out.println("Reflection configuration saved");

        // 5. Setup resource configuration
        File resourceConfigFile = NativeImageConfig.getResourceConfigFile(baseDir);
        ResourceConfig.addResource(resourceConfigFile, "application.properties");
        ResourceConfig.addResource(resourceConfigFile, "templates/email.html");
        ResourceConfig.addResource(resourceConfigFile, "db/schema.sql");

        System.out.println("Native image configuration complete");
        System.out.println("Reflection: " + reflectConfigFile.getAbsolutePath());
        System.out.println("Resources: " + resourceConfigFile.getAbsolutePath());
    }
}
```

### 12. Integration with Build Tools

Use Garganttua Native in your Maven build process:

```java
import org.apache.maven.plugin.AbstractMojo;
import com.garganttua.core.nativve.image.config.*;

public class GenerateNativeConfigMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException {
        try {
            String outputDir = project.getBuild().getOutputDirectory();

            File reflectConfigFile = NativeImageConfig.getReflectConfigFile(outputDir);
            ReflectConfig config = ReflectConfig.loadFromFile(reflectConfigFile);

            // Scan project classes and register them
            for (Class<?> clazz : scanProjectClasses()) {
                if (requiresReflection(clazz)) {
                    ReflectConfigEntry entry = ReflectConfigEntryBuilder
                        .builder(clazz)
                        .allDeclaredFields(true)
                        .build();
                    config.addEntry(entry);
                }
            }

            config.saveToFile(reflectConfigFile);
            getLog().info("Native image configuration generated");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate config", e);
        }
    }
}
```

## Advanced Patterns

### Conditional Configuration Based on Annotations

Register different reflection levels based on annotation presence:

```java
public class SmartConfigBuilder {

    public static ReflectConfigEntry buildEntry(Class<?> clazz) {
        ReflectConfigEntryBuilder builder = ReflectConfigEntryBuilder.builder(clazz);

        // Full reflection for @Entity classes
        if (clazz.isAnnotationPresent(Entity.class)) {
            builder.allDeclaredFields(true)
                   .allDeclaredMethods(true)
                   .allDeclaredConstructors(true);
        }

        // Limited reflection for @Service classes
        else if (clazz.isAnnotationPresent(Service.class)) {
            builder.fieldsAnnotatedWith(Inject.class)
                   .methodsAnnotatedWith(PostConstruct.class)
                   .allDeclaredConstructors(true);
        }

        // Minimal reflection for others
        else {
            builder.queryAllDeclaredConstructors(true);
        }

        return builder.build();
    }
}
```

### Bulk Registration with Package Scanning

Register all classes from specific packages:

```java
import org.reflections.Reflections;

public class PackageScanner {

    public static void registerPackage(ReflectConfig config, String packageName) {
        Reflections reflections = new Reflections(packageName);

        // Register all classes in package
        reflections.getSubTypesOf(Object.class).forEach(clazz -> {
            ReflectConfigEntry entry = ReflectConfigEntryBuilder
                .builder(clazz)
                .allDeclaredFields(true)
                .allDeclaredMethods(true)
                .build();
            config.addEntry(entry);
        });

        System.out.println("Registered package: " + packageName);
    }
}
```

### Configuration Merging

Merge configurations from multiple sources:

```java
public class ConfigMerger {

    public static ReflectConfig mergeConfigurations(File... configFiles)
            throws IOException {
        ReflectConfig merged = new ReflectConfig();
        merged.setEntries(new ArrayList<>());

        for (File file : configFiles) {
            ReflectConfig config = ReflectConfig.loadFromFile(file);
            for (ReflectConfigEntry entry : config.getEntries()) {
                merged.addEntry(entry);  // Deduplication handled automatically
            }
        }

        return merged;
    }
}
```

### Incremental Configuration Updates

Update configuration without overwriting existing entries:

```java
public class IncrementalUpdater {

    public static void addIfNotExists(ReflectConfig config, Class<?> clazz) {
        Optional<ReflectConfigEntry> existing = config.findEntryByName(clazz);

        if (existing.isEmpty()) {
            ReflectConfigEntry newEntry = ReflectConfigEntryBuilder
                .builder(clazz)
                .allDeclaredFields(true)
                .build();
            config.addEntry(newEntry);
            System.out.println("Added new entry: " + clazz.getName());
        } else {
            System.out.println("Entry already exists: " + clazz.getName());
        }
    }
}
```

## Performance

### Configuration File Loading

- **Lazy parsing** - JSON files are only parsed when `loadFromFile()` is called
- **In-memory representation** - Configurations are held in memory as Java objects
- **Cached ObjectMapper** - Jackson ObjectMapper is reused across operations

### Build-Time Considerations

Garganttua Native is intended for **build-time use**, not runtime:

1. **Development** - Generate configuration during development/testing
2. **CI/CD** - Run configuration generation as part of build pipeline
3. **Pre-compilation** - Configuration must exist before GraalVM native-image compilation

### Best Practices for Performance

1. **Batch operations** - Load, modify multiple entries, then save once
2. **Avoid redundant saves** - Save configuration file only after all modifications
3. **Use specific registration** - Prefer `field()` / `method()` over `allDeclaredFields` when possible (smaller config files = faster native image builds)
4. **Cache ReflectConfig instances** - Reuse loaded configurations across multiple operations

## Tips and Best Practices

### Reflection Configuration Strategy

1. **Minimal Configuration** - Only register classes, methods, and fields that are truly accessed via reflection at runtime
2. **Test Native Images** - Always test your native image to verify reflection configuration is complete
3. **Use GraalVM Agent** - Combine Garganttua Native with GraalVM's tracing agent to discover reflection usage
4. **Annotation-Based Registration** - Prefer `fieldsAnnotatedWith()` and `methodsAnnotatedWith()` for maintainability
5. **Avoid Over-Registration** - Don't use `allDeclaredMethods` unless necessary; large configs slow native image builds

### Resource Management

6. **Explicit Resource Paths** - Use exact resource paths instead of wildcards when possible
7. **Test Resource Availability** - Verify resources are accessible in native image with integration tests
8. **Separate Environments** - Maintain different configurations for dev/test/prod if resource requirements differ
9. **Version Control Configs** - Commit generated configuration files to version control for reproducibility

### Configuration Maintenance

10. **Incremental Updates** - Use `findEntryByName()` and `updateEntry()` to modify existing configurations
11. **Document Registration Reasons** - Add comments explaining why specific classes need reflection
12. **Automate Generation** - Integrate configuration generation into your build process
13. **Review Before Saving** - Inspect generated JSON files to ensure correctness
14. **Backup Configurations** - Keep backups of working configurations before major changes

### Build Integration

15. **Maven/Gradle Plugins** - Create custom plugins to auto-generate configurations
16. **CI/CD Integration** - Validate configuration files in CI pipeline
17. **Multi-Module Projects** - Generate separate configurations per module, then merge
18. **Profile-Based Configs** - Use Maven profiles to generate different configs for different environments

### Debugging Native Images

19. **Enable GraalVM Diagnostics** - Use `-H:+PrintAnalysisCallTree` to debug missing reflection config
20. **Incremental Testing** - Add reflection config incrementally and test after each addition
21. **Log Configuration Changes** - Log what entries are being added/removed during generation
22. **Compare Configurations** - Use JSON diff tools to track configuration changes over time

### JSON Format Compliance

23. **Match GraalVM Format** - Ensure generated JSON matches GraalVM's expected schema
24. **Pretty Print** - Use Jackson's pretty printer for human-readable configurations
25. **Validate JSON** - Validate generated JSON files before native image compilation
26. **Escape Patterns** - Resource patterns use `\\Q...\\E` for literal matching (handled automatically by Garganttua Native)

### Common Pitfalls to Avoid

27. **Don't Mix Manual and Programmatic Edits** - Choose one approach (manual JSON or Garganttua Native) to avoid conflicts
28. **Don't Register Internal JDK Classes** - Let GraalVM handle JDK reflection automatically
29. **Don't Ignore Build Warnings** - GraalVM warnings about missing reflection config indicate incomplete configuration
30. **Don't Duplicate Entries** - Use `addEntry()` which handles deduplication automatically
31. **Don't Forget Constructor Registration** - Many frameworks require constructor reflection for instantiation
32. **Version Compatibility** - Ensure Garganttua Native version is compatible with your GraalVM version

## GraalVM Integration

### Standard Workflow

1. **Write Java code** with reflection
2. **Generate configuration** using Garganttua Native
3. **Compile to native image** with GraalVM:
   ```bash
   native-image \
     -H:ReflectionConfigurationFiles=target/classes/META-INF/native-image/reflect-config.json \
     -H:ResourceConfigurationFiles=target/classes/META-INF/native-image/resource-config.json \
     -jar myapp.jar
   ```

### Using with GraalVM Tracing Agent

Combine automated discovery with programmatic management:

```bash
# 1. Run with tracing agent to discover reflection usage
java -agentlib:native-image-agent=config-output-dir=config/ -jar myapp.jar

# 2. Load discovered configuration with Garganttua Native
File discoveredConfig = new File("config/reflect-config.json");
ReflectConfig config = ReflectConfig.loadFromFile(discoveredConfig);

# 3. Add additional entries programmatically
config.addEntry(...);

# 4. Save to standard location
File targetConfig = NativeImageConfig.getReflectConfigFile("target/classes");
config.saveToFile(targetConfig);
```

### Spring Boot Native Integration

Example configuration for Spring Boot applications:

```java
ReflectConfigEntry controllerEntry = ReflectConfigEntryBuilder
    .builder(MyController.class)
    .methodsAnnotatedWith(RequestMapping.class)
    .methodsAnnotatedWith(GetMapping.class)
    .methodsAnnotatedWith(PostMapping.class)
    .allDeclaredConstructors(true)
    .build();

ReflectConfigEntry entityEntry = ReflectConfigEntryBuilder
    .builder(UserEntity.class)
    .fieldsAnnotatedWith(Column.class)
    .allDeclaredFields(true)
    .allDeclaredConstructors(true)
    .build();
```

## License

This module is distributed under the MIT License.
