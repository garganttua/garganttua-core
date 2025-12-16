# 🧩 Garganttua Injection

## Description

The **garganttua-injection** module provides a lightweight, high-performance **Dependency Injection (DI)** container for Java applications. This module is **fully compatible with JSR-330** (Dependency Injection for Java), supporting all standard annotations while providing additional advanced features for enterprise-grade applications.

**JSR-330 Compliance**: This implementation fully supports the `javax.inject` specification, including:
- `@Inject` - Constructor, field, and method injection
- `@Named` - Named qualifier for bean identification
- `@Singleton` - Singleton scope management
- `@Qualifier` - Custom qualifier annotations

Beyond JSR-330, this module offers:
- **Fluent DSL** - Expressive, type-safe bean and context configuration
- **Multiple Scopes** - Singleton, prototype, and custom scopes
- **Property Injection** - External configuration binding via `@Property`
- **Lifecycle Management** - Post-construct hooks and initialization callbacks
- **Child Contexts** - Isolated hierarchical dependency graphs
- **Auto-Detection** - Automatic bean discovery with classpath scanning
- **Cyclic Dependency Detection** - Build-time validation to prevent circular references
- **Thread-Safe** - Concurrent bean initialization and retrieval
- **GraalVM Native** - Native image compilation ready

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-injection</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-lifecycle`
 - `com.garganttua.core:garganttua-supply`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-reflection`
 - `com.garganttua.core:garganttua-reflections:provided`
 - `com.garganttua.core:garganttua-native:provided`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### DiContext

The `DiContext` is the central component of the injection framework. It manages:
- **Bean Providers** - Collections of beans organized by scope
- **Property Providers** - External configuration sources
- **Element Resolvers** - Annotation-based injection strategies
- **Child Context Factories** - Hierarchical context creation

DiContext follows the Inversion of Control (IoC) pattern, taking responsibility for object creation and lifecycle management.

### JSR-330 Annotations

This module fully implements the JSR-330 specification:

**`@Inject`** - Marks injection points (constructors, fields, methods)
```java
public class UserService {
    @Inject
    private UserRepository repository;

    @Inject
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

**`@Named`** - Identifies beans by name
```java
@Named("primary")
public class PrimaryDatabase implements Database { }

@Inject
@Named("primary")
private Database database;
```

**`@Singleton`** - Singleton scope (one instance per context)
```java
@Singleton
public class CacheManager { }
```

**`@Qualifier`** - Create custom qualifiers
```java
@Qualifier
@Retention(RUNTIME)
public @interface Production { }

@Inject
@Production
private DataSource dataSource;
```

### Bean Scopes

**Singleton (Default)** - Single instance shared across the context:
```java
@Singleton
public class ApplicationConfig { }
```

**Prototype** - New instance created on every request:
```java
@Prototype
public class RequestHandler { }
```

### Property Injection

Inject external configuration values using `@Property`:
```java
public class DatabaseConfig {
    @Property("db.host")
    private String host;

    @Property("db.port")
    private Integer port;
}
```

### Lifecycle Callbacks

Execute initialization logic after dependency injection:
```java
public class ServiceInitializer {
    @Inject
    private DataSource dataSource;

    @PostConstruct
    public void initialize() {
        // Called after all dependencies are injected
        dataSource.connect();
    }
}
```

### Child Contexts

Create isolated dependency graphs with parent-child relationships:
```java
IDiContext parentContext = DiContext.builder()
    .withPackage("com.app")
    .build();

IDiContext childContext = parentContext.newChildContext(
    CustomChildContext.class,
    "arg1", "arg2"
);
```

Child contexts inherit bean providers but maintain separate singleton instances.

### Bean Queries

Query beans from the context using fluent DSL:
```java
// Query by type
Optional<UserService> service = Beans.bean(UserService.class)
    .build()
    .supply();

// Query by name and type
Optional<Database> db = Beans.bean(Database.class)
    .name("primary")
    .build()
    .supply();

// Query multiple beans
List<Plugin> plugins = Beans.beans(Plugin.class)
    .build()
    .supplyAll();
```

## Usage

### Basic JSR-330 Example

Based on real test code from `DiContextTest.java`:

```java
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.beans.Beans;
import jakarta.annotation.PostConstruct;

// Define a singleton bean with dependencies
@Singleton
@Named("dummyBeanForTest")
public class DummyBean {
    private String value = "default";
    private boolean postConstructCalled = false;
    private AnotherDummyBean anotherBean;
    private DummyOtherBean otherBean;

    @Inject
    public DummyBean(
        @Property("com.garganttua.dummyPropertyInConstructor") String value,
        @Prototype @Named("AnotherDummyBeanForTest") AnotherDummyBean anotherBean,
        @Singleton @Named("emailService") DummyOtherBean otherBean
    ) {
        this.value = value;
        this.anotherBean = anotherBean;
    }

    @PostConstruct
    public void markPostConstruct() {
        this.postConstructCalled = true;
    }

    public String getValue() { return value; }
    public boolean isPostConstructCalled() { return postConstructCalled; }
}

// Prototype bean - new instance every time
@Prototype
@Named("AnotherDummyBeanForTest")
public class AnotherDummyBean {
    private String randomValue;

    public AnotherDummyBean() {
        this.randomValue = UUID.randomUUID().toString();
    }
}

// Another singleton bean
@Named("emailService")
@Singleton
public class DummyOtherBean {
}

// Initialize context and retrieve beans
public class Application {
    public static void main(String[] args) throws Exception {
        // Create and start context with property injection
        DiContext.builder()
            .withPackage("com.garganttua")
            .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
                .up()
            .autoDetect(true)
            .build()
            .onInit()
            .onStart();

        // Query bean by type
        Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();

        if (bean.isPresent()) {
            System.out.println("Value: " + bean.get().getValue()); // "propertyValue"
            System.out.println("Post-construct called: " + bean.get().isPostConstructCalled()); // true
        }
    }
}
```

### Named Beans with @Named

Based on real test code from `BeanQueryTest.java`:

```java
import javax.inject.Named;
import javax.inject.Singleton;
import com.garganttua.core.injection.context.beans.BeanQuery;

// Define a named bean
@Singleton
@Named("dummyBeanForTest")
public class DummyBean {
    private String value = "default";

    public String getValue() {
        return value;
    }
}

// Query bean by name
IBeanQueryBuilder<DummyBean> builder = BeanQuery.builder();
Optional<DummyBean> bean = builder
    .name("dummyBeanForTest")
    .build()
    .execute();

assertTrue(bean.isPresent());

// Query by type and name together
IBeanQueryBuilder<DummyBean> builder2 = BeanQuery.builder();
Optional<DummyBean> bean2 = builder2
    .type(DummyBean.class)
    .name("dummyBeanForTest")
    .build()
    .execute();

assertTrue(bean2.isPresent());

// Query with non-existent name returns empty
IBeanQueryBuilder<DummyBean> builder3 = BeanQuery.builder();
Optional<DummyBean> bean3 = builder3
    .name("nonExistent")
    .build()
    .execute();

assertFalse(bean3.isPresent());
```

### Custom Qualifiers

Based on real test code from the dummy beans:

```java
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Define custom qualifier (from DummyBeanQualifier.java)
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface DummyBeanQualifier {
}

// Apply qualifier to a bean (from DummyOtherBean.java)
@Named("emailService")
@Singleton
@DummyBeanQualifier
public class DummyOtherBean {
}

// Use qualifier in dependency injection (from DummyBean.java)
@Singleton
@Named("dummyBeanForTest")
public class DummyBean {
    @Provider("garganttua")
    @Named("emailService")
    @DummyBeanQualifier
    @Nullable
    private DummyOtherBean otherBean;
}

// Register qualifier in context when building
IDiContext context = DiContext.builder()
    .withPackage("com.garganttua")
    .withQualifier(DummyBeanQualifier.class)
    .autoDetect(true)
    .build();
```

### Property Injection

Based on real test code from `DiContextTest.java` and `InjectableElementResolverTest.java`:

```java
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.context.properties.Properties;

// Define bean with property injection in constructor (from DummyBean.java)
@Singleton
@Named("dummyBeanForTest")
public class DummyBean {
    private String value;

    @Inject
    public DummyBean(
        @Provider("garganttua")
        @Property("com.garganttua.dummyPropertyInConstructor") String value
    ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

// Configure properties in context (from DiContextTest.java)
String propertyValue = UUID.randomUUID().toString();

DiContext.builder()
    .withPackage("com.garganttua")
    .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
        .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", propertyValue)
        .up()
    .autoDetect(true)
    .build()
    .onInit()
    .onStart();

// Retrieve property values directly (from DiContextTest.java)
Optional<String> property = Properties.property(String.class)
    .key("com.garganttua.dummyPropertyInConstructor")
    .build()
    .supply();

assertNotNull(property);
assertTrue(property.isPresent());
assertEquals(propertyValue, property.get());

// Verify bean received the property (from DiContextTest.java)
Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
assertEquals(propertyValue, bean.get().getValue());
```

### Prototype Scope

Based on real test code from `AnotherDummyBean.java` and `BeanFactoryTest.java`:

```java
import com.garganttua.core.injection.annotations.Prototype;
import java.util.UUID;

// Prototype bean creates new instance each time (from AnotherDummyBean.java)
@Prototype
@Named("AnotherDummyBeanForTest")
public class AnotherDummyBean {
    private String randomValue;

    public AnotherDummyBean() {
        this.randomValue = UUID.randomUUID().toString();
    }

    public String getRandomValue() {
        return randomValue;
    }
}

// Test showing prototype vs singleton behavior (from BeanFactoryTest.java)
// Singleton strategy returns same instance
BeanFactory<DummyBean> singletonFactory = new BeanFactory<>(
    new BeanDefinition<>(
        new BeanReference<>(DummyBean.class, Optional.of(BeanStrategy.singleton), Optional.empty(), null),
        Optional.empty(), Set.of(), Set.of()
    )
);

Optional<DummyBean> bean1 = singletonFactory.supply();
Optional<DummyBean> bean2 = singletonFactory.supply();
assertSame(bean1.get(), bean2.get()); // Same instance

// Prototype strategy returns different instances
BeanFactory<DummyBean> prototypeFactory = new BeanFactory<>(
    new BeanDefinition<>(
        new BeanReference<>(DummyBean.class, Optional.of(BeanStrategy.prototype), Optional.empty(), null),
        Optional.empty(), Set.of(), Set.of()
    )
);

Optional<DummyBean> bean3 = prototypeFactory.supply();
Optional<DummyBean> bean4 = prototypeFactory.supply();
assertNotSame(bean3.get(), bean4.get()); // Different instances
```

### Programmatic Bean Registration

Based on real test code from `BeanFactoryBuilderTest.java`:

```java
import com.garganttua.core.injection.context.dsl.BeanFactoryBuilder;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

// Programmatically configure a bean with full control
String randomValue = UUID.randomUUID().toString();
IBeanFactoryBuilder<DummyBean> builder = new BeanFactoryBuilder<>(DummyBean.class);

IBeanSupplier<DummyBean> beanSupplier = builder
    // Set strategy and metadata
    .strategy(BeanStrategy.singleton)
    .name("aBean")
    .qualifier(DummyBeanQualifier.class)

    // Configure field injection
    .field(String.class).field("anotherValue")
        .withValue(FixedSupplierBuilder.of(randomValue))
        .up()

    // Configure constructor injection
    .constructor()
        .withParam(FixedSupplierBuilder.of("constructedWithParameter"))
        .up()

    // Configure post-construct method
    .postConstruction()
        .method("markPostConstruct")
        .withReturn(Void.class)
        .up()

    .build();

// Supply the configured bean
Optional<DummyBean> bean = beanSupplier.supply();

assertNotNull(bean);
assertTrue(bean.isPresent());
assertEquals("constructedWithParameter", bean.get().getValue());
assertTrue(bean.get().isPostConstructCalled());
assertEquals(randomValue, bean.get().getAnotherValue());

// Check dependencies
assertEquals(1, beanSupplier.dependencies().size());
```

### Post-Construct Callbacks

Based on real test code from `DummyBean.java` and `DiContextTest.java`:

```java
import jakarta.annotation.PostConstruct;

// Define bean with post-construct callback (from DummyBean.java)
@Singleton
@Named("dummyBeanForTest")
public class DummyBean {
    private String value;
    private boolean postConstructCalled = false;
    private AnotherDummyBean anotherBean;

    @Inject
    public DummyBean(
        @Property("com.garganttua.dummyPropertyInConstructor") String value,
        @Prototype @Named("AnotherDummyBeanForTest") AnotherDummyBean anotherBean
    ) {
        this.value = value;
        this.anotherBean = anotherBean;
    }

    @PostConstruct
    public void markPostConstruct() {
        // Called after all dependencies are injected
        this.postConstructCalled = true;
    }

    public boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}

// Test verifying post-construct is called (from DiContextTest.java)
DiContext.builder()
    .withPackage("com.garganttua")
    .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
        .withProperty(String.class, "com.garganttua.dummyPropertyInConstructor", "propertyValue")
        .up()
    .autoDetect(true)
    .build()
    .onInit()
    .onStart();

Optional<DummyBean> bean = Beans.bean(DummyBean.class).build().supply();
assertNotNull(bean);
assertTrue(bean.isPresent());
assertTrue(bean.get().isPostConstructCalled()); // Post-construct was executed
assertNotNull(bean.get().getAnotherBean()); // Dependencies were injected first
```

### Child Contexts

Note: The test files don't contain complete child context examples. The framework supports child contexts through the `IChildContextFactory` interface, but detailed working examples are not present in the current test suite. The basic pattern is:

```java
// Parent context with shared beans
IDiContext parentContext = DiContext.builder()
    .withPackage("com.example.shared")
    .build()
    .onInit()
    .onStart();

// Register child context factory
parentContext.registerChildContextFactory(new CustomChildContextFactory());

// Create child context with isolated beans
IDiContext childContext = parentContext.newChildContext(
    CustomChildContext.class,
    "module1"
);

// Child inherits parent beans but maintains separate singleton instances
```

### Querying Multiple Beans

Note: The test files don't contain examples of querying multiple beans at once. The framework supports this through the `Beans.beans()` method, but working examples are not present in the current test suite. The basic pattern would be:

```java
// Define multiple implementations
@Named("json")
public class JsonSerializer implements Serializer { }

@Named("xml")
public class XmlSerializer implements Serializer { }

// Query all implementations
List<Serializer> serializers = Beans.beans(Serializer.class)
    .build()
    .supplyAll();
```

### Dependency Validation

Based on real test code from `DependencyCycleDetectorTest.java`:

```java
import com.garganttua.core.injection.context.validation.DependencyCycleDetector;
import com.garganttua.core.injection.context.validation.DependencyGraph;

// Test classes
static class A { }
static class B { }
static class C { }

// Create dependency graph
DependencyGraph graph = new DependencyGraph();
DependencyCycleDetector detector = new DependencyCycleDetector();

// No cycle - validation passes
graph.addDependency(A.class, B.class);
graph.addDependency(B.class, C.class);
assertDoesNotThrow(() -> detector.detectCycles(graph));

// Simple cycle A -> B -> A
DependencyGraph cyclicGraph = new DependencyGraph();
cyclicGraph.addDependency(A.class, B.class);
cyclicGraph.addDependency(B.class, A.class);

DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(cyclicGraph));
assertTrue(ex.getMessage().contains("Circular dependency detected"));
assertTrue(ex.getMessage().contains("A") && ex.getMessage().contains("B"));

// Complex cycle A -> B -> C -> A
DependencyGraph complexCycle = new DependencyGraph();
complexCycle.addDependency(A.class, B.class);
complexCycle.addDependency(B.class, C.class);
complexCycle.addDependency(C.class, A.class);

DiException ex2 = assertThrows(DiException.class, () -> detector.detectCycles(complexCycle));
assertTrue(ex2.getMessage().contains("Circular dependency detected"));
assertTrue(ex2.getMessage().contains("A -> B -> C -> A"));

// Self-dependency A -> A
DependencyGraph selfCycle = new DependencyGraph();
selfCycle.addDependency(A.class, A.class);

DiException ex3 = assertThrows(DiException.class, () -> detector.detectCycles(selfCycle));
assertTrue(ex3.getMessage().contains("A"));
```

## Advanced Integration

### Creating a Custom BeanProvider

You can create custom `IBeanProvider` implementations to integrate external bean sources (e.g., Spring beans, CDI containers, remote services) into the Garganttua DI context.

**Step 1: Implement IBeanProvider**

```java
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

import java.util.*;

public class CustomBeanProvider extends AbstractLifecycle implements IBeanProvider {

    private final Map<Class<?>, Object> externalBeans = new HashMap<>();

    public CustomBeanProvider() {
        // Initialize with external bean sources
        // Example: integrate with another DI framework
    }

    @Override
    public <T> Optional<T> getBean(Class<T> type) throws DiException {
        // Ensure the provider is initialized and started
        ensureInitializedAndStarted();

        // Retrieve bean from external source
        Object bean = externalBeans.get(type);
        if (bean != null && type.isInstance(bean)) {
            return Optional.of(type.cast(bean));
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();

        // Implement named bean lookup
        // Example: lookup by name in external container
        return Optional.empty();
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(Class<T> interfaceType,
                                                      boolean includePrototypes) {
        // Find all beans implementing the given interface
        return externalBeans.values().stream()
            .filter(bean -> interfaceType.isInstance(bean))
            .map(interfaceType::cast)
            .toList();
    }

    @Override
    public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
        ensureInitializedAndStarted();

        // Query using BeanDefinition criteria
        Class<T> type = definition.type();
        Optional<String> name = definition.name();

        if (name.isPresent()) {
            return getBean(name.get(), type);
        }
        return getBean(type);
    }

    @Override
    public <T> List<T> queryBeans(BeanDefinition<T> definition) throws DiException {
        ensureInitializedAndStarted();

        // Return all beans matching the definition
        Class<T> type = definition.type();
        return externalBeans.values().stream()
            .filter(bean -> type.isInstance(bean))
            .map(type::cast)
            .toList();
    }

    @Override
    public boolean isMutable() {
        // Return true if beans can be registered dynamically
        return true;
    }

    @Override
    public int size() {
        return externalBeans.size();
    }

    // Lifecycle methods
    @Override
    protected ILifecycle doInit() throws LifecycleException {
        // Initialize external bean sources
        // Example: connect to remote service, scan classpath, etc.
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Start the provider (e.g., establish connections)
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        // Clear cached beans
        externalBeans.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        // Stop the provider (e.g., close connections)
        return this;
    }

    @Override
    public IBeanProvider copy() throws CopyException {
        // Create a copy of this provider
        CustomBeanProvider copy = new CustomBeanProvider();
        copy.externalBeans.putAll(this.externalBeans);
        return copy;
    }

    // Custom methods for your use case
    public void registerExternalBean(Class<?> type, Object bean) {
        externalBeans.put(type, bean);
    }
}
```

**Step 2: Register Custom BeanProvider in DiContext**

```java
// Create your custom bean provider
CustomBeanProvider customProvider = new CustomBeanProvider();
customProvider.registerExternalBean(ExternalService.class, new ExternalService());

// Option 1: Programmatic registration
IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .beanProvider("custom", new BeanProviderBuilder(null) {
        @Override
        protected IBeanProvider doBuild() throws DslException {
            return customProvider;
        }

        @Override
        protected void doAutoDetection() throws DslException {
            // No auto-detection needed
        }
    })
    .build()
    .onInit()
    .onStart();

// Option 2: Using builder pattern (more common)
IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .beanProvider("custom", new CustomBeanProviderBuilder(customProvider))
    .build()
    .onInit()
    .onStart();

// Query beans from custom provider
Optional<ExternalService> service = context.queryBean(
    "custom",
    Beans.bean(ExternalService.class).build()
);
```

**Step 3: Create a Builder for Your Custom Provider (Optional)**

```java
public class CustomBeanProviderBuilder
    extends AbstractAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider>
    implements IBeanProviderBuilder {

    private final CustomBeanProvider customProvider;

    public CustomBeanProviderBuilder(IDiContextBuilder link, CustomBeanProvider provider) {
        super(link);
        this.customProvider = provider;
    }

    @Override
    protected IBeanProvider doBuild() throws DslException {
        return customProvider;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // Implement auto-detection logic if needed
        // Example: scan external sources for beans
    }

    @Override
    public IBeanProviderBuilder withPackages(String[] packageNames) {
        // Handle package configuration if applicable
        return this;
    }

    @Override
    public IBeanProviderBuilder withPackage(String packageName) {
        // Handle package configuration if applicable
        return this;
    }

    @Override
    public <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType)
        throws DslException {
        // Implement bean registration if needed
        throw new UnsupportedOperationException("Use registerExternalBean instead");
    }
}
```

**Real-World Example: Spring Integration**

```java
public class SpringBeanProvider extends AbstractLifecycle implements IBeanProvider {

    private ApplicationContext springContext;

    public SpringBeanProvider(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    @Override
    public <T> Optional<T> getBean(Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        try {
            T bean = springContext.getBean(type);
            return Optional.ofNullable(bean);
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        try {
            T bean = springContext.getBean(name, type);
            return Optional.ofNullable(bean);
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    // ... implement other methods
}

// Usage
ApplicationContext springContext = new AnnotationConfigApplicationContext(SpringConfig.class);

IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .beanProvider("spring", new BeanProviderBuilder(null) {
        @Override
        protected IBeanProvider doBuild() {
            return new SpringBeanProvider(springContext);
        }
    })
    .build()
    .onInit()
    .onStart();
```

### Creating a Custom PropertyProvider

Custom `IPropertyProvider` implementations allow you to integrate external configuration sources (e.g., databases, remote config servers, encrypted vaults) into the DI context.

**Step 1: Implement IPropertyProvider**

```java
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabasePropertyProvider extends AbstractLifecycle implements IPropertyProvider {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final String connectionUrl;
    private Connection dbConnection;

    public DatabasePropertyProvider(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();

        // Check cache first
        if (cache.containsKey(key)) {
            return convertValue(cache.get(key), type);
        }

        // Load from database
        Object value = loadPropertyFromDatabase(key);
        if (value != null) {
            cache.put(key, value);
            return convertValue(value, type);
        }

        return Optional.empty();
    }

    @Override
    public void setProperty(String key, Object value) throws DiException {
        ensureInitializedAndStarted();

        if (!isMutable()) {
            throw new DiException("PropertyProvider is not mutable");
        }

        // Update database
        savePropertyToDatabase(key, value);

        // Update cache
        cache.put(key, value);
    }

    @Override
    public boolean isMutable() {
        // Allow runtime updates
        return true;
    }

    @Override
    public Set<String> keys() {
        ensureInitializedAndStarted();
        return loadAllKeysFromDatabase();
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        try {
            // Initialize database connection
            dbConnection = DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            throw new LifecycleException("Failed to connect to database", e);
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Pre-load frequently accessed properties into cache
        preloadCache();
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        // Clear cache
        cache.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            throw new LifecycleException("Failed to close database connection", e);
        }
        return this;
    }

    @Override
    public IPropertyProvider copy() throws CopyException {
        DatabasePropertyProvider copy = new DatabasePropertyProvider(connectionUrl);
        copy.cache.putAll(this.cache);
        return copy;
    }

    // Helper methods
    private Object loadPropertyFromDatabase(String key) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT value FROM properties WHERE key = ?")) {
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getObject("value");
            }
        } catch (SQLException e) {
            // Log error
        }
        return null;
    }

    private void savePropertyToDatabase(String key, Object value) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO properties (key, value) VALUES (?, ?) " +
                "ON CONFLICT (key) DO UPDATE SET value = ?")) {
            stmt.setString(1, key);
            stmt.setObject(2, value);
            stmt.setObject(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log error
        }
    }

    private Set<String> loadAllKeysFromDatabase() {
        Set<String> keys = new HashSet<>();
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT key FROM properties")) {
            while (rs.next()) {
                keys.add(rs.getString("key"));
            }
        } catch (SQLException e) {
            // Log error
        }
        return keys;
    }

    private void preloadCache() {
        // Load commonly accessed properties
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT key, value FROM properties")) {
            while (rs.next()) {
                cache.put(rs.getString("key"), rs.getObject("value"));
            }
        } catch (SQLException e) {
            // Log error
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> convertValue(Object value, Class<T> type) {
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }

        // Type conversion logic
        try {
            if (type.equals(String.class)) {
                return Optional.of(type.cast(value.toString()));
            } else if (type.equals(Integer.class)) {
                return Optional.of(type.cast(Integer.parseInt(value.toString())));
            } else if (type.equals(Long.class)) {
                return Optional.of(type.cast(Long.parseLong(value.toString())));
            } else if (type.equals(Double.class)) {
                return Optional.of(type.cast(Double.parseDouble(value.toString())));
            } else if (type.equals(Boolean.class)) {
                return Optional.of(type.cast(Boolean.parseBoolean(value.toString())));
            }
        } catch (Exception e) {
            // Log conversion error
        }

        return Optional.empty();
    }
}
```

**Step 2: Register Custom PropertyProvider in DiContext**

```java
// Create your custom property provider
DatabasePropertyProvider dbProperties = new DatabasePropertyProvider(
    "jdbc:postgresql://localhost:5432/config"
);

// Option 1: Direct registration
IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .propertyProvider("database", new PropertyProviderBuilder(null) {
        @Override
        protected IPropertyProvider doBuild() throws DslException {
            return dbProperties;
        }

        @Override
        protected void doAutoDetection() throws DslException {
            // No auto-detection needed
        }
    })
    .build()
    .onInit()
    .onStart();

// Option 2: Using custom builder
IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .propertyProvider("database", new DatabasePropertyProviderBuilder(dbProperties))
    .build()
    .onInit()
    .onStart();

// Query properties from custom provider
Optional<String> dbHost = context.getProperty("database", "db.host", String.class);
```

**Step 3: Create a Builder for Your Custom Provider (Optional)**

```java
public class DatabasePropertyProviderBuilder
    extends AbstractAutomaticLinkedBuilder<IPropertyProviderBuilder, IDiContextBuilder, IPropertyProvider>
    implements IPropertyProviderBuilder {

    private final DatabasePropertyProvider provider;

    public DatabasePropertyProviderBuilder(IDiContextBuilder link,
                                           DatabasePropertyProvider provider) {
        super(link);
        this.provider = provider;
    }

    @Override
    protected IPropertyProvider doBuild() throws DslException {
        return provider;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // No auto-detection needed for database properties
    }

    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(
            Class<PropertyType> propertyType,
            String key,
            PropertyType property) throws DslException {
        // Optionally allow programmatic property registration
        try {
            provider.setProperty(key, property);
        } catch (DiException e) {
            throw new DslException("Failed to set property", e);
        }
        return this;
    }
}
```

**Real-World Example: Encrypted Vault Provider**

```java
public class VaultPropertyProvider extends AbstractLifecycle implements IPropertyProvider {

    private final VaultClient vaultClient;
    private final String secretPath;
    private final Map<String, Object> decryptedCache = new ConcurrentHashMap<>();

    public VaultPropertyProvider(String vaultUrl, String token, String secretPath) {
        this.vaultClient = new VaultClient(vaultUrl, token);
        this.secretPath = secretPath;
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();

        // Check cache
        if (decryptedCache.containsKey(key)) {
            return convertAndCast(decryptedCache.get(key), type);
        }

        // Fetch and decrypt from vault
        try {
            String encryptedValue = vaultClient.read(secretPath + "/" + key);
            Object decryptedValue = decrypt(encryptedValue);
            decryptedCache.put(key, decryptedValue);
            return convertAndCast(decryptedValue, type);
        } catch (VaultException e) {
            throw new DiException("Failed to read from vault: " + key, e);
        }
    }

    @Override
    public void setProperty(String key, Object value) throws DiException {
        ensureInitializedAndStarted();

        try {
            String encryptedValue = encrypt(value);
            vaultClient.write(secretPath + "/" + key, encryptedValue);
            decryptedCache.put(key, value);
        } catch (VaultException e) {
            throw new DiException("Failed to write to vault: " + key, e);
        }
    }

    @Override
    public boolean isMutable() {
        return true; // Vault supports updates
    }

    @Override
    public Set<String> keys() {
        try {
            return vaultClient.list(secretPath);
        } catch (VaultException e) {
            return Collections.emptySet();
        }
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        try {
            vaultClient.authenticate();
        } catch (VaultException e) {
            throw new LifecycleException("Vault authentication failed", e);
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        // Pre-load critical secrets
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        decryptedCache.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        vaultClient.close();
        return this;
    }

    @Override
    public IPropertyProvider copy() throws CopyException {
        VaultPropertyProvider copy = new VaultPropertyProvider(
            vaultClient.getUrl(),
            vaultClient.getToken(),
            secretPath
        );
        copy.decryptedCache.putAll(this.decryptedCache);
        return copy;
    }

    private Object decrypt(String encrypted) {
        // Implement decryption logic
        return encrypted; // Placeholder
    }

    private String encrypt(Object value) {
        // Implement encryption logic
        return value.toString(); // Placeholder
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> convertAndCast(Object value, Class<T> type) {
        // Type conversion logic
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        // Add conversion logic as needed
        return Optional.empty();
    }
}

// Usage
IDiContext context = DiContext.builder()
    .withPackage("com.example")
    .propertyProvider("vault", new PropertyProviderBuilder(null) {
        @Override
        protected IPropertyProvider doBuild() {
            return new VaultPropertyProvider(
                "https://vault.example.com",
                System.getenv("VAULT_TOKEN"),
                "secret/myapp"
            );
        }
    })
    .build()
    .onInit()
    .onStart();

// Inject encrypted properties
public class ApiClient {
    @Property("api.key")
    private String apiKey; // Automatically decrypted from vault
}
```

### Building a native application

More info [here](../garganttua-native/README.md)

Your beans must be in the reflect configuration in order to be loaded at native runtime. 
There are two ways of doind this : 

#### Dynamic configuration 
Based on a real context loaded

### Static configuration 
Based on static code analysis

### Manual configuration 



## Tips and best practices

### General Principles

1. **Prefer constructor injection over field injection** - Constructor injection makes dependencies explicit and enables immutability. Use `@Inject` on constructors instead of fields when possible.

2. **Use JSR-330 annotations for portability** - Prefer standard `@Inject`, `@Named`, `@Singleton`, and `@Qualifier` annotations over framework-specific alternatives for better portability.

3. **Enable auto-detection for convenience** - Use `.autoDetect(true)` to automatically discover annotated beans and qualifiers in specified packages.

4. **Initialize context at startup** - Always call `.onInit().onStart()` after building the context to ensure proper lifecycle initialization.

5. **Favor singleton scope for stateless beans** - Use `@Singleton` (or default scope) for services, repositories, and stateless components to minimize memory overhead.

### Scope Management

6. **Use prototype scope for stateful objects** - Mark beans with `@Prototype` when each consumer needs an independent instance (e.g., request handlers, mutable DTOs).

7. **Avoid mixing scopes incorrectly** - Don't inject prototype beans into singleton beans. The singleton will hold the same prototype instance forever.

8. **Understand scope inheritance in child contexts** - Child contexts inherit bean definitions but maintain separate singleton instances.

### Dependency Design

9. **Keep dependency graphs shallow** - Avoid deeply nested dependency chains (more than 3-4 levels). Complex graphs are harder to understand and test.

10. **Avoid circular dependencies** - Design beans to have unidirectional dependencies. The context will detect and reject circular dependencies at build time.

11. **Use qualifiers for disambiguation** - When multiple beans of the same type exist, use `@Named` or custom `@Qualifier` annotations to specify which implementation to inject.

12. **Don't inject the DiContext itself** - Avoid injecting `IDiContext` into beans. Use direct dependency injection instead of service locator pattern.

### Property Management

13. **Externalize configuration with properties** - Use `@Property` annotations to inject configuration values from external sources instead of hardcoding.

14. **Provide default property values** - Configure default values for optional properties to avoid `Optional.empty()` results.

15. **Validate injected properties** - Add validation logic in `@PostConstruct` methods to ensure injected property values meet requirements.

### Lifecycle and Initialization

16. **Use @PostConstruct for initialization logic** - Place initialization code that depends on injected dependencies in `@PostConstruct` methods, not constructors.

17. **Clean up resources properly** - Implement lifecycle interfaces to release resources (connections, threads) when the context stops.

18. **Start contexts in the correct order** - For multi-context applications, ensure parent contexts are initialized before child contexts.

### Performance Optimization

19. **Limit package scanning scope** - Use specific package names in `.withPackage()` to reduce classpath scanning overhead during auto-detection.

20. **Cache bean queries for repeated access** - Store frequently accessed beans in local variables instead of querying the context repeatedly.

21. **Use lazy initialization when appropriate** - Defer bean creation until first access by querying beans on-demand rather than at startup.

### Testing and Debugging

22. **Use child contexts for test isolation** - Create child contexts in tests to isolate bean instances and avoid test pollution.

23. **Override beans for testing** - Register mock implementations in test contexts to replace production beans.

24. **Enable logging for troubleshooting** - The injection module uses SLF4J. Enable DEBUG or TRACE logging to diagnose dependency resolution issues.

### Code Organization

25. **Group related beans in packages** - Organize beans by functional module and use package-based scanning for clean context configuration.

26. **Avoid constructor ambiguity** - If a class has multiple constructors, mark the injection constructor explicitly with `@Inject`.

27. **Document custom qualifiers** - Add Javadoc to custom `@Qualifier` annotations explaining their purpose and usage.

## License
This module is distributed under the MIT License.
