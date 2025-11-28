# Garganttua Reflections

## Description

Garganttua Reflections is a **binding module that integrates the Reflections library** with Garganttua Core's annotation scanning infrastructure. It provides an implementation of `IAnnotationScanner` based on the popular [Reflections](https://github.com/ronmamo/reflections) library by ronmamo.

This module acts as a **bridge between Garganttua Core and Reflections**, enabling classpath scanning for annotated classes at runtime. Instead of manually searching for classes with specific annotations, you can use this module to automatically discover and retrieve all classes annotated with a particular annotation within specified packages.

**Key Features:**
- **IAnnotationScanner Implementation** - Implements Garganttua's standard annotation scanning interface
- **Reflections Library Integration** - Leverages the mature and widely-used Reflections library
- **Package-Based Scanning** - Scan specific packages for annotated classes
- **Annotation Discovery** - Find all classes annotated with a specific annotation at runtime
- **Inheritance Support** - Include subclasses and implementing classes in scan results
- **Performance Optimized** - Efficient scanning with Reflections' optimized indexing
- **Logging Support** - Comprehensive SLF4J logging for debugging and monitoring
- **Zero Configuration** - Works out-of-the-box with sensible defaults

## Installation

### Installation with Maven

```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-reflections</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
- `com.garganttua.core:garganttua-commons`
- `org.reflections:reflections`

## Core Concepts

### IAnnotationScanner Interface

The `IAnnotationScanner` interface is defined in Garganttua Commons and provides a standard contract for annotation scanning across different implementations.

**Interface Definition:**
```java
public interface IAnnotationScanner {
    List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation);
}
```

This interface allows Garganttua modules to discover annotated classes without being tightly coupled to a specific scanning library.

### ReflectionsAnnotationScanner

`ReflectionsAnnotationScanner` is the concrete implementation of `IAnnotationScanner` that uses the Reflections library under the hood.

**Key Characteristics:**
- Implements `IAnnotationScanner` interface
- Uses `org.reflections.Reflections` for classpath scanning
- Configured with `Scanners.TypesAnnotated` for efficient annotation scanning
- Includes annotated classes and their subclasses/implementations
- Returns results as a `List<Class<?>>` for easy iteration

**How It Works:**
1. **Initialization** - Creates a `Reflections` instance for the specified package
2. **Scanning** - Uses `getTypesAnnotatedWith()` to find all annotated classes
3. **Collection** - Converts the `Set<Class<?>>` result to a `List<Class<?>>`
4. **Return** - Returns the list of discovered classes to the caller

### Reflections Library

The underlying [Reflections library](https://github.com/ronmamo/reflections) provides:
- **Metadata scanning** - Indexes classpath at startup or runtime
- **Query capabilities** - Find types, methods, fields, resources by various criteria
- **Performance** - Uses indexing to avoid repeated classpath scanning
- **Flexibility** - Multiple scanner types (types, methods, fields, resources, etc.)

Garganttua Reflections specifically uses the `TypesAnnotated` scanner for annotation-based class discovery.

### Logging

The module uses SLF4J for comprehensive logging at multiple levels:
- **TRACE** - Entry/exit of methods with parameter details
- **DEBUG** - Initialization and scanning progress
- **INFO** - Summary of scan results (number of classes found)
- **ERROR** - Exceptional conditions (not typically used in this module)

**Log Output Example:**
```
[TRACE] Entering getClassesWithAnnotation(package=com.example.app, annotation=@Service)
[DEBUG] Initializing Reflections scanner for package 'com.example.app'
[DEBUG] Fetching annotated classes for annotation 'Service' in package com.example.app
[INFO]  Found 23 classes annotated with 'Service' in package com.example.app
[TRACE] Exiting getClassesWithAnnotation(package=com.example.app, annotation=@Service)
```

## Usage

### 1. Basic Annotation Scanning

Find all classes in a package annotated with a specific annotation:

```java
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import javax.inject.Singleton;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

// Find all @Singleton classes in package
List<Class<?>> singletonClasses = scanner.getClassesWithAnnotation(
    "com.example.services",
    Singleton.class
);

System.out.println("Found " + singletonClasses.size() + " singleton classes");

for (Class<?> clazz : singletonClasses) {
    System.out.println("  - " + clazz.getName());
}
```

**Output:**
```
Found 3 singleton classes
  - com.example.services.DatabaseService
  - com.example.services.CacheService
  - com.example.services.EmailService
```

### 2. Spring Component Scanning

Discover Spring components, services, controllers, and repositories:

```java
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

// Find all Spring components
List<Class<?>> components = scanner.getClassesWithAnnotation(
    "com.example.app",
    Component.class
);

// Find all Spring services
List<Class<?>> services = scanner.getClassesWithAnnotation(
    "com.example.app",
    Service.class
);

// Find all Spring controllers
List<Class<?>> controllers = scanner.getClassesWithAnnotation(
    "com.example.app.web",
    Controller.class
);

// Find all Spring repositories
List<Class<?>> repositories = scanner.getClassesWithAnnotation(
    "com.example.app.persistence",
    Repository.class
);

System.out.println("Components: " + components.size());
System.out.println("Services: " + services.size());
System.out.println("Controllers: " + controllers.size());
System.out.println("Repositories: " + repositories.size());
```

### 3. Custom Annotation Discovery

Find classes with your own custom annotations:

```java
// Define custom annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventHandler {
    String eventType();
}

// Annotated classes
@EventHandler(eventType = "UserRegistered")
public class UserRegistrationHandler { }

@EventHandler(eventType = "OrderCreated")
public class OrderCreationHandler { }

// Scan for event handlers
ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
List<Class<?>> handlers = scanner.getClassesWithAnnotation(
    "com.example.events",
    EventHandler.class
);

// Process discovered handlers
for (Class<?> handlerClass : handlers) {
    EventHandler annotation = handlerClass.getAnnotation(EventHandler.class);
    System.out.println("Handler for event: " + annotation.eventType());
}
```

**Output:**
```
Handler for event: UserRegistered
Handler for event: OrderCreated
```

### 4. JAX-RS Resource Discovery

Find JAX-RS REST API resources:

```java
import javax.ws.rs.Path;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

List<Class<?>> resources = scanner.getClassesWithAnnotation(
    "com.example.api",
    Path.class
);

for (Class<?> resourceClass : resources) {
    Path pathAnnotation = resourceClass.getAnnotation(Path.class);
    System.out.println("REST Resource: " + resourceClass.getSimpleName() +
                       " -> " + pathAnnotation.value());
}
```

**Output:**
```
REST Resource: UserResource -> /api/users
REST Resource: OrderResource -> /api/orders
REST Resource: ProductResource -> /api/products
```

### 5. JPA Entity Discovery

Find JPA entities for dynamic schema generation:

```java
import javax.persistence.Entity;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

List<Class<?>> entities = scanner.getClassesWithAnnotation(
    "com.example.model",
    Entity.class
);

System.out.println("Discovered " + entities.size() + " JPA entities");

for (Class<?> entityClass : entities) {
    Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
    String entityName = entityAnnotation.name().isEmpty()
        ? entityClass.getSimpleName()
        : entityAnnotation.name();

    System.out.println("  - Entity: " + entityName + " (" + entityClass.getName() + ")");
}
```

### 6. Multi-Package Scanning

Scan multiple packages by creating separate scanner instances:

```java
ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

// Scan different packages
List<Class<?>> coreServices = scanner.getClassesWithAnnotation(
    "com.example.core",
    Service.class
);

List<Class<?>> webServices = scanner.getClassesWithAnnotation(
    "com.example.web",
    Service.class
);

List<Class<?>> dataServices = scanner.getClassesWithAnnotation(
    "com.example.data",
    Service.class
);

// Combine results
List<Class<?>> allServices = new ArrayList<>();
allServices.addAll(coreServices);
allServices.addAll(webServices);
allServices.addAll(dataServices);

System.out.println("Total services across all packages: " + allServices.size());
```

### 7. Instantiation After Discovery

Discover and instantiate classes:

```java
import javax.inject.Inject;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    String name();
}

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
List<Class<?>> pluginClasses = scanner.getClassesWithAnnotation(
    "com.example.plugins",
    Plugin.class
);

List<Object> plugins = new ArrayList<>();

for (Class<?> pluginClass : pluginClasses) {
    try {
        // Instantiate plugin
        Object plugin = pluginClass.getDeclaredConstructor().newInstance();
        plugins.add(plugin);

        Plugin annotation = pluginClass.getAnnotation(Plugin.class);
        System.out.println("Loaded plugin: " + annotation.name());
    } catch (Exception e) {
        System.err.println("Failed to load plugin: " + pluginClass.getName());
    }
}

System.out.println("Total plugins loaded: " + plugins.size());
```

### 8. Integration with Garganttua Injection

Use with Garganttua's dependency injection system:

```java
import com.garganttua.core.di.DiContext;
import javax.inject.Singleton;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

// Find all singleton beans
List<Class<?>> singletons = scanner.getClassesWithAnnotation(
    "com.example.app",
    Singleton.class
);

// Register with DI container
DiContext diContext = new DiContext();

for (Class<?> singletonClass : singletons) {
    diContext.registerBean(singletonClass);
    System.out.println("Registered: " + singletonClass.getName());
}

// Now you can inject these beans anywhere
```

### 9. Filtering Scan Results

Filter discovered classes based on additional criteria:

```java
import java.lang.reflect.Modifier;

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

List<Class<?>> services = scanner.getClassesWithAnnotation(
    "com.example.services",
    Service.class
);

// Filter: only concrete classes (not abstract)
List<Class<?>> concreteServices = services.stream()
    .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
    .collect(Collectors.toList());

// Filter: only classes implementing specific interface
List<Class<?>> restServices = services.stream()
    .filter(RestService.class::isAssignableFrom)
    .collect(Collectors.toList());

// Filter: only classes with no-arg constructor
List<Class<?>> instantiableServices = services.stream()
    .filter(clazz -> {
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    })
    .collect(Collectors.toList());

System.out.println("Concrete services: " + concreteServices.size());
System.out.println("REST services: " + restServices.size());
System.out.println("Instantiable services: " + instantiableServices.size());
```

### 10. Hierarchical Package Scanning

Scan root package to include all subpackages:

```java
ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

// Scanning "com.example" includes:
// - com.example
// - com.example.services
// - com.example.services.impl
// - com.example.web
// - etc.
List<Class<?>> allComponents = scanner.getClassesWithAnnotation(
    "com.example",  // Root package
    Component.class
);

// Group by package
Map<String, List<Class<?>>> componentsByPackage = allComponents.stream()
    .collect(Collectors.groupingBy(clazz -> clazz.getPackage().getName()));

System.out.println("Components by package:");
componentsByPackage.forEach((pkg, classes) -> {
    System.out.println("  " + pkg + ": " + classes.size() + " components");
});
```

## Advanced Patterns

### Annotation Metadata Extraction

Extract and process annotation metadata:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scheduled {
    String cron();
    boolean enabled() default true;
}

ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
List<Class<?>> scheduledTasks = scanner.getClassesWithAnnotation(
    "com.example.tasks",
    Scheduled.class
);

for (Class<?> taskClass : scheduledTasks) {
    Scheduled annotation = taskClass.getAnnotation(Scheduled.class);

    if (annotation.enabled()) {
        System.out.println("Task: " + taskClass.getSimpleName());
        System.out.println("  Cron: " + annotation.cron());

        // Instantiate and schedule the task
        // ...
    }
}
```

### Plugin Architecture

Build a plugin system using annotation scanning:

```java
public interface IPlugin {
    void initialize();
    void execute();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    String name();
    int priority() default 100;
}

public class PluginManager {
    private List<IPlugin> plugins = new ArrayList<>();

    public void loadPlugins(String packageName) {
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
        List<Class<?>> pluginClasses = scanner.getClassesWithAnnotation(
            packageName,
            Plugin.class
        );

        // Sort by priority
        pluginClasses.sort((a, b) -> {
            int priorityA = a.getAnnotation(Plugin.class).priority();
            int priorityB = b.getAnnotation(Plugin.class).priority();
            return Integer.compare(priorityA, priorityB);
        });

        // Instantiate plugins
        for (Class<?> pluginClass : pluginClasses) {
            if (IPlugin.class.isAssignableFrom(pluginClass)) {
                try {
                    IPlugin plugin = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
                    plugin.initialize();
                    plugins.add(plugin);

                    Plugin annotation = pluginClass.getAnnotation(Plugin.class);
                    System.out.println("Loaded plugin: " + annotation.name());
                } catch (Exception e) {
                    System.err.println("Failed to load plugin: " + pluginClass.getName());
                }
            }
        }
    }

    public void executePlugins() {
        for (IPlugin plugin : plugins) {
            plugin.execute();
        }
    }
}
```

### Caching Scan Results

Cache scan results for better performance:

```java
public class CachedAnnotationScanner {
    private final ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
    private final Map<String, List<Class<?>>> cache = new ConcurrentHashMap<>();

    public List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotation) {
        String cacheKey = packageName + ":" + annotation.getName();

        return cache.computeIfAbsent(cacheKey, key -> {
            System.out.println("Scanning package " + packageName + " for @" + annotation.getSimpleName());
            return scanner.getClassesWithAnnotation(packageName, annotation);
        });
    }

    public void clearCache() {
        cache.clear();
    }
}
```

### Conditional Bean Registration

Dynamically register beans based on conditions:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionalBean {
    String property();
    String value();
}

public class ConditionalBeanRegistry {
    private Properties config;

    public void registerBeans(String packageName) {
        ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();
        List<Class<?>> conditionalBeans = scanner.getClassesWithAnnotation(
            packageName,
            ConditionalBean.class
        );

        for (Class<?> beanClass : conditionalBeans) {
            ConditionalBean condition = beanClass.getAnnotation(ConditionalBean.class);
            String configValue = config.getProperty(condition.property());

            if (condition.value().equals(configValue)) {
                // Register bean
                System.out.println("Registering conditional bean: " + beanClass.getName());
                // ... bean registration logic
            } else {
                System.out.println("Skipping bean (condition not met): " + beanClass.getName());
            }
        }
    }
}
```

## Performance

### Scanning Performance

The Reflections library uses indexing to optimize performance:

**First Scan (Cold Start):**
- Small package (10-50 classes): ~50-200ms
- Medium package (100-500 classes): ~200-800ms
- Large package (1000+ classes): ~1-3 seconds

**Subsequent Scans (Cached):**
- Negligible overhead (~1-5ms) if using cached `Reflections` instance

### Optimization Strategies

1. **Scan Narrow Packages** - Scan specific packages instead of entire classpath
2. **Cache Scanner Instances** - Reuse `ReflectionsAnnotationScanner` instances
3. **Lazy Loading** - Scan only when needed, not at startup
4. **Result Caching** - Cache scan results if package contents don't change
5. **Parallel Scanning** - Scan multiple packages in parallel with `CompletableFuture`

**Example: Parallel Scanning**
```java
ReflectionsAnnotationScanner scanner = new ReflectionsAnnotationScanner();

CompletableFuture<List<Class<?>>> services = CompletableFuture.supplyAsync(() ->
    scanner.getClassesWithAnnotation("com.example.services", Service.class)
);

CompletableFuture<List<Class<?>>> controllers = CompletableFuture.supplyAsync(() ->
    scanner.getClassesWithAnnotation("com.example.web", Controller.class)
);

CompletableFuture<List<Class<?>>> repositories = CompletableFuture.supplyAsync(() ->
    scanner.getClassesWithAnnotation("com.example.data", Repository.class)
);

// Wait for all scans to complete
CompletableFuture.allOf(services, controllers, repositories).join();

List<Class<?>> allServices = services.get();
List<Class<?>> allControllers = controllers.get();
List<Class<?>> allRepositories = repositories.get();
```

## Tips and best practices

### Scanning Strategy

1. **Narrow Package Scope** - Scan specific packages instead of entire classpath to reduce scan time
2. **Scan at Startup** - Perform scans during application initialization, not during request handling
3. **Cache Results** - Store scan results in memory if package contents don't change at runtime
4. **Use Specific Annotations** - Scan for specific annotations rather than broad markers
5. **Validate Results** - Check that discovered classes meet expected criteria (interfaces, superclasses, etc.)

### Annotation Design

6. **Runtime Retention** - Use `@Retention(RetentionPolicy.RUNTIME)` for annotations you want to scan
7. **Type Targeting** - Use `@Target(ElementType.TYPE)` for class-level annotations
8. **Meaningful Metadata** - Include useful metadata in annotation attributes
9. **Document Annotations** - Clearly document the purpose and usage of custom annotations
10. **Avoid Annotation Overuse** - Don't create annotations for every small use case

### Error Handling

11. **Handle Missing Classes** - Gracefully handle cases where scanned classes can't be loaded
12. **Check Instantiability** - Verify classes have accessible constructors before instantiation
13. **Log Scan Results** - Log the number of classes found for debugging and monitoring
14. **Validate Packages** - Ensure package names are correct to avoid empty scan results
15. **Handle Exceptions** - Wrap instantiation and method invocation in try-catch blocks

### Integration Patterns

16. **Dependency Injection** - Combine with DI frameworks for automatic bean registration
17. **Plugin Systems** - Use for dynamic plugin discovery and loading
18. **Auto-Configuration** - Implement Spring Boot-style auto-configuration
19. **Service Discovery** - Discover and register microservices or REST endpoints
20. **Event Handlers** - Find and register event handlers automatically

### Performance Optimization

21. **Reuse Scanner Instances** - Create `ReflectionsAnnotationScanner` once and reuse
22. **Limit Scan Depth** - Avoid scanning entire classpath; target specific packages
23. **Parallel Scanning** - Scan multiple independent packages concurrently
24. **Lazy Initialization** - Delay scanning until actually needed
25. **Monitor Scan Times** - Log scan durations to identify performance issues

### Testing

26. **Test Annotation Discovery** - Write unit tests to verify annotations are found
27. **Mock Scanners** - Use mock implementations in tests to avoid classpath scanning
28. **Test Edge Cases** - Test with empty packages, missing classes, etc.
29. **Integration Tests** - Verify scanning works with actual package structures
30. **Performance Tests** - Measure and benchmark scanning performance

### Debugging and Logging

31. **Enable Debug Logging** - Set `com.garganttua.core.reflections` to DEBUG/TRACE
32. **Log Package Names** - Verify you're scanning the correct packages
33. **Log Discovered Classes** - Output the list of found classes for verification
34. **Check Class Loader** - Ensure the correct class loader is being used
35. **Validate Classpath** - Confirm scanned packages are on the classpath

### Common Pitfalls to Avoid

36. **Don't Scan Entire Classpath** - Avoid scanning "" or root packages
37. **Don't Ignore Scan Results** - Always check if scan returned expected results
38. **Don't Scan on Every Request** - Cache results or scan at startup only
39. **Don't Forget Runtime Retention** - Annotations must be runtime-retained to be discoverable
40. **Don't Assume Order** - Scan results may not be in any particular order
41. **Classpath Issues** - Ensure annotated classes are compiled and on classpath
42. **Package Naming** - Use exact package names; partial names won't work

## License

This module is distributed under the MIT License.
