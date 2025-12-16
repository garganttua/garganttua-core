# 📜 Garganttua DSL

## Description

The **garganttua-dsl** module provides a comprehensive framework for building fluent, type-safe Domain-Specific Languages (DSLs) in Java. It offers a rich set of abstract builder classes that enable the creation of expressive, hierarchical, and self-configuring APIs with minimal boilerplate.

This module is the foundation for all DSL implementations across the Garganttua ecosystem, providing:

- **Fluent Builder Pattern** - Method chaining for readable, declarative code
- **Hierarchical Navigation** - Parent-child relationships with `up()` navigation
- **Automatic Configuration** - Optional auto-detection and inference of missing values
- **Build Caching** - Automatic memoization to prevent redundant builds
- **Type Safety** - Generic interfaces ensuring compile-time type checking
- **Ordered Collections** - Builder-aware ordered maps for maintaining insertion order

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-dsl</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Builder Pattern

The builder pattern separates object construction from representation, enabling:
- **Step-by-step configuration** - Set properties incrementally
- **Immutable results** - Built objects can be immutable
- **Validation** - Validate configuration before building
- **Reusability** - Share builder configurations

### Fluent Interface

All builders support method chaining:
```java
builder
    .property1(value1)
    .property2(value2)
    .property3(value3)
    .build();
```

### IBuilder Interface

The base interface defining the builder contract:

```java
public interface IBuilder<Built> {
    Built build() throws DslException;
}
```

All builders in the Garganttua ecosystem implement this interface.

## Builder Types

### 1. Abstract Automatic Builder

**Purpose**: Builders that support optional auto-detection and inference of missing configuration.

**Features**:
- `autoDetect(boolean)` - Enable/disable automatic configuration
- `doAutoDetection()` - Hook for inferring missing values
- `doBuild()` - Hook for constructing the final object
- Build caching - Built instance is cached after first build

**Generic Parameters**:
- `<Builder>` - The concrete builder type (for fluent returns)
- `<Built>` - The type being built

**Use Cases**:
- Configuration with sensible defaults
- Builders that can infer values from context
- Lazy initialization scenarios

### 2. Abstract Linked Builder

**Purpose**: Builders that maintain a parent-child relationship for hierarchical DSLs.

**Features**:
- `up()` - Navigate back to the parent builder
- `setUp(Link)` - Update the parent reference
- Mandatory parent in constructor

**Generic Parameters**:
- `<Link>` - The parent builder type
- `<Built>` - The type being built

**Use Cases**:
- Nested configuration (e.g., `config.section().field().value()`)
- Tree structures with back navigation
- Multi-level DSLs (workflow stages, steps, operations)

### 3. Abstract Automatic Linked Builder

**Purpose**: Combines automatic configuration with hierarchical navigation.

**Features**:
- All features of `AbstractAutomaticBuilder`
- All features of `AbstractLinkedBuilder`
- Ideal for complex, self-configuring hierarchical DSLs

**Generic Parameters**:
- `<Builder>` - The concrete builder type
- `<Link>` - The parent builder type
- `<Built>` - The type being built

**Use Cases**:
- Complex nested DSLs with defaults
- Hierarchical configurations that adapt to parent context
- Multi-level structures with smart defaults

### 4. OrderedMapBuilder

**Purpose**: Build an `OrderedMap<K, B>` from a collection of builders.

**Features**:
- Preserves insertion order
- Automatically builds all contained builders
- Type-safe key-value-builder associations
- Filters out null builders

**Generic Parameters**:
- `<K>` - The key type
- `<V extends IBuilder<B>>` - The builder type
- `<B>` - The built value type

**Use Cases**:
- Field definitions in object mappers
- Step collections in workflows
- Property registries
- Any ordered collection of configured items

## Usage

### Simple Automatic Builder

```java
public class PersonBuilder extends AbstractAutomaticBuilder<PersonBuilder, Person> {

    private String name;
    private Integer age;

    public PersonBuilder name(String n) {
        this.name = n;
        return this;
    }

    public PersonBuilder age(Integer a) {
        this.age = a;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // Auto-configure missing values
        if (this.name == null) {
            this.name = "Unknown";
        }
        if (this.age == null) {
            this.age = 0;
        }
    }

    @Override
    protected Person doBuild() throws DslException {
        // Validate before building
        if (this.age < 0) {
            throw new DslException("Age cannot be negative");
        }
        return new Person(this.name, this.age);
    }
}

// Usage without auto-detection
Person p1 = new PersonBuilder()
    .name("Alice")
    .age(30)
    .build();

// Usage with auto-detection (missing values filled)
Person p2 = new PersonBuilder()
    .autoDetect(true)
    .age(25)
    .build(); // name will be "Unknown"
```

### Linked Builder for Hierarchical DSL

```java
public class ConfigBuilder {
    private Map<String, SectionBuilder> sections = new HashMap<>();

    public SectionBuilder section(String name) {
        SectionBuilder builder = new SectionBuilder(this);
        sections.put(name, builder);
        return builder;
    }

    public Config build() {
        return new Config(sections.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().build()
            )));
    }
}

public class SectionBuilder extends AbstractLinkedBuilder<ConfigBuilder, Section> {

    private Map<String, String> properties = new HashMap<>();

    public SectionBuilder(ConfigBuilder parent) {
        super(parent);
    }

    public SectionBuilder property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public Section build() {
        return new Section(properties);
    }
}

// Usage with fluent hierarchical DSL
Config config = new ConfigBuilder()
    .section("database")
        .property("host", "localhost")
        .property("port", "5432")
        .up() // Navigate back to ConfigBuilder
    .section("cache")
        .property("ttl", "3600")
        .up()
    .build();
```

### Automatic Linked Builder

```java
public class WorkflowBuilder {
    private List<StageBuilder> stages = new ArrayList<>();

    public StageBuilder stage(String name) {
        StageBuilder builder = new StageBuilder(this, name);
        stages.add(builder);
        return builder;
    }

    public String getWorkflowName() {
        return "MyWorkflow";
    }

    public Workflow build() {
        return new Workflow(stages.stream()
            .map(StageBuilder::build)
            .collect(Collectors.toList()));
    }
}

public class StageBuilder extends AbstractAutomaticLinkedBuilder<StageBuilder, WorkflowBuilder, Stage> {

    private String name;
    private List<String> steps = new ArrayList<>();
    private Integer timeout;

    public StageBuilder(WorkflowBuilder parent, String name) {
        super(parent);
        this.name = name;
    }

    public StageBuilder step(String stepName) {
        this.steps.add(stepName);
        return this;
    }

    public StageBuilder timeout(Integer seconds) {
        this.timeout = seconds;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // Auto-configure timeout based on parent context
        if (this.timeout == null) {
            // Default timeout: 10 seconds per step
            this.timeout = this.steps.size() * 10;
        }

        // Add implicit step if none provided
        if (this.steps.isEmpty()) {
            this.steps.add("default_" + this.name);
        }
    }

    @Override
    protected Stage doBuild() throws DslException {
        String workflowName = this.up().getWorkflowName();
        return new Stage(workflowName, this.name, this.steps, this.timeout);
    }
}

// Usage with auto-detection
Workflow workflow = new WorkflowBuilder()
    .stage("validation")
        .step("validateInput")
        .step("checkPermissions")
        .autoDetect(true) // Timeout auto-calculated: 2 steps * 10s = 20s
        .up()
    .stage("processing")
        .autoDetect(true) // Empty steps list auto-populated: ["default_processing"]
        .up()
    .build();
```

### OrderedMapBuilder

```java
// Define field builders
public class FieldBuilder extends AbstractAutomaticBuilder<FieldBuilder, Field> {
    private String name;
    private String type;
    private boolean required;

    public FieldBuilder name(String n) {
        this.name = n;
        return this;
    }

    public FieldBuilder type(String t) {
        this.type = t;
        return this;
    }

    public FieldBuilder required(boolean r) {
        this.required = r;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.type == null) {
            this.type = "String"; // Default type
        }
    }

    @Override
    protected Field doBuild() throws DslException {
        return new Field(this.name, this.type, this.required);
    }
}

// Build ordered collection of fields
OrderedMapBuilder<String, FieldBuilder, Field> fieldsBuilder = new OrderedMapBuilder<>();

fieldsBuilder.put("id", new FieldBuilder()
    .name("id")
    .type("Long")
    .required(true));

fieldsBuilder.put("name", new FieldBuilder()
    .name("name")
    .autoDetect(true) // Type will be "String"
    .required(true));

fieldsBuilder.put("email", new FieldBuilder()
    .name("email")
    .type("String")
    .required(false));

// Build all fields maintaining order
OrderedMap<String, Field> fields = fieldsBuilder.build();

// Iterate in insertion order
for (Map.Entry<String, Field> entry : fields.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
// Output:
// id: Field(name=id, type=Long, required=true)
// name: Field(name=name, type=String, required=true)
// email: Field(name=email, type=String, required=false)
```

### Complex Real-World Example: API Definition DSL

```java
// Root builder
public class ApiBuilder {
    private String basePath;
    private OrderedMapBuilder<String, EndpointBuilder, Endpoint> endpoints = new OrderedMapBuilder<>();

    public ApiBuilder basePath(String path) {
        this.basePath = path;
        return this;
    }

    public EndpointBuilder endpoint(String path) {
        EndpointBuilder builder = new EndpointBuilder(this, path);
        endpoints.put(path, builder);
        return builder;
    }

    public String getBasePath() {
        return basePath;
    }

    public Api build() {
        return new Api(basePath, endpoints.build());
    }
}

// Endpoint builder (automatic + linked)
public class EndpointBuilder extends AbstractAutomaticLinkedBuilder<EndpointBuilder, ApiBuilder, Endpoint> {

    private String path;
    private String method;
    private List<String> params = new ArrayList<>();
    private Integer rateLimit;

    public EndpointBuilder(ApiBuilder parent, String path) {
        super(parent);
        this.path = path;
    }

    public EndpointBuilder method(String m) {
        this.method = m;
        return this;
    }

    public EndpointBuilder param(String p) {
        this.params.add(p);
        return this;
    }

    public EndpointBuilder rateLimit(Integer limit) {
        this.rateLimit = limit;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.method == null) {
            this.method = "GET"; // Default to GET
        }
        if (this.rateLimit == null) {
            // Stricter limits for mutations
            this.rateLimit = this.method.equals("GET") ? 1000 : 100;
        }
    }

    @Override
    protected Endpoint doBuild() throws DslException {
        String fullPath = this.up().getBasePath() + this.path;
        return new Endpoint(fullPath, this.method, this.params, this.rateLimit);
    }
}

// Usage: Build complete API definition
Api api = new ApiBuilder()
    .basePath("/api/v1")
    .endpoint("/users")
        .method("GET")
        .autoDetect(true) // rateLimit = 1000
        .up()
    .endpoint("/users")
        .method("POST")
        .param("name")
        .param("email")
        .autoDetect(true) // rateLimit = 100
        .up()
    .endpoint("/users/{id}")
        .autoDetect(true) // method = GET, rateLimit = 1000
        .up()
    .build();
```

## Advanced Patterns

### Build Caching

Automatic builders cache their results:

```java
PersonBuilder builder = new PersonBuilder()
    .name("Alice")
    .age(30);

Person p1 = builder.build(); // Builds instance
Person p2 = builder.build(); // Returns cached instance
assert p1 == p2; // Same object reference
```

### Custom Validation

Add validation in `doBuild()`:

```java
@Override
protected Person doBuild() throws DslException {
    if (this.age < 0) {
        throw new DslException("Age must be non-negative");
    }
    if (this.name == null || this.name.isEmpty()) {
        throw new DslException("Name is required");
    }
    if (this.age < 18 && this.requiresAdult) {
        throw new DslException("Person must be an adult");
    }
    return new Person(this.name, this.age);
}
```

### Context-Aware Auto-Detection

Use parent context in auto-detection:

```java
@Override
protected void doAutoDetection() throws DslException {
    WorkflowBuilder parent = this.up();

    // Inherit settings from parent
    if (this.timeout == null) {
        this.timeout = parent.getDefaultStageTimeout();
    }

    // Adapt based on parent state
    if (parent.isProductionMode() && this.retryCount == null) {
        this.retryCount = 3; // Prod: enable retries
    }
}
```

### Conditional Builder Chains

```java
ConfigBuilder builder = new ConfigBuilder();

if (isDevelopment()) {
    builder.section("debug")
        .property("level", "TRACE")
        .up();
}

builder.section("app")
    .property("name", "MyApp")
    .up();

Config config = builder.build();
```

## Tips and best practices

### General Principles

1. **Choose the right builder type** - Use the simplest builder that meets your needs:
   - Simple needs → `IBuilder` implementation
   - Need defaults → `AbstractAutomaticBuilder`
   - Need hierarchy → `AbstractLinkedBuilder`
   - Need both → `AbstractAutomaticLinkedBuilder`

2. **Enable auto-detect selectively** - Only use `autoDetect(true)` when the builder can safely infer missing values without ambiguity.

3. **Keep methods fluent** - All configuration methods should return `this` (or the builder type) to enable chaining.

4. **Use method chaining consistently** - Format chains for readability with indentation showing hierarchy.

### Auto-Detection Best Practices

5. **Make doAutoDetection() deterministic** - Auto-detection should always produce the same result for the same input state.

6. **Keep auto-detection lightweight** - Avoid expensive operations in `doAutoDetection()`. It's called on every build when enabled.

7. **Document auto-detection behavior** - Clearly document what values are auto-detected and how they're computed.

8. **Validate after auto-detection** - Perform validation in `doBuild()` after auto-detection completes.

### Linked Builder Best Practices

9. **Always call up() to return to parent** - Maintain the fluent chain by returning to the parent after configuring children.

10. **Use parent context wisely** - Access parent state via `up()` only when necessary for context-aware configuration.

11. **Avoid circular navigation** - Don't create cycles in the builder hierarchy (child → parent → child).

### Performance Optimization

12. **Leverage build caching** - Automatic builders cache results. Build once and reuse when possible.

13. **Avoid storing builders in domain objects** - Store only built instances, not builders. Builders are for construction only.

14. **Use OrderedMapBuilder for collections** - More efficient than manually building each item and collecting them.

### Code Organization

15. **Group related builders** - Keep builder classes near the types they build for discoverability.

16. **Extract complex validation** - Move complex validation logic to separate validator classes if `doBuild()` becomes large.

17. **Consider builder factories** - For complex initialization, provide factory methods that pre-configure common scenarios.

### Error Handling

18. **Throw DslException for build errors** - Use `DslException` consistently for all build-time errors.

19. **Provide clear error messages** - Include context about what failed and why in exception messages.

20. **Validate early when possible** - Validate configuration as it's set (in fluent methods) rather than waiting until `build()`.

## Design Patterns

### Composite Pattern

Use linked builders for tree structures:

```java
MenuBuilder menu = new MenuBuilder()
    .item("File")
        .submenu()
            .item("New")
            .item("Open")
            .up()
        .up()
    .item("Edit")
        .up()
    .build();
```

### Strategy Pattern

Use auto-detection to select strategies:

```java
@Override
protected void doAutoDetection() throws DslException {
    if (this.strategy == null) {
        // Select strategy based on context
        if (this.up().isHighThroughput()) {
            this.strategy = "async";
        } else {
            this.strategy = "sync";
        }
    }
}
```

### Template Method Pattern

`AbstractAutomaticBuilder` uses template method:

```java
public final Built build() {
    // Template method
    if (autoDetect) {
        doAutoDetection(); // Hook 1
    }
    return doBuild(); // Hook 2
}
```

## License

This module is distributed under the MIT License.
