# Phase-Aware Dependencies - Use Cases

## Use Case 1: Configuration-Driven Auto-Detection

### Scenario
A builder that reads a configuration file to auto-detect beans, but doesn't need the configuration during building.

### Implementation
```java
public class ServiceRegistryBuilder extends AbstractAutomaticDependentBuilder<...> {

    public ServiceRegistryBuilder() {
        super(Set.of(
            // Configuration is only needed during auto-detection
            DependencySpec.use(IConfigurationBuilder.class, DependencyPhase.AUTO_DETECT)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof IConfiguration config) {
            // Read service definitions from config
            List<String> serviceClasses = config.getStringList("services.classes");
            serviceClasses.forEach(this::registerService);
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Configuration not needed here - dependency won't be passed
    }
}
```

### Benefit
Configuration is not processed during build phase, saving time and memory.

---

## Use Case 2: Injection Context for Building Only

### Scenario
A builder that uses reflection for auto-detection but needs the injection context to resolve dependencies during building.

### Implementation
```java
public class PluginManagerBuilder extends AbstractAutomaticDependentBuilder<...> {

    public PluginManagerBuilder() {
        super(Set.of(
            // InjectionContext only needed during build, not auto-detection
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // No dependencies here - uses reflection instead
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext context) {
            // Initialize plugins with dependency injection
            this.plugins.forEach(plugin -> context.inject(plugin));
        }
    }

    @Override
    protected void doAutoDetection() {
        // Scan classpath for @Plugin annotations
        this.plugins = ClasspathScanner.findAnnotated(Plugin.class);
    }
}
```

### Benefit
InjectionContext is not processed during auto-detection phase, which happens before the context is fully built.

---

## Use Case 3: Schema Registry Needed Throughout

### Scenario
A builder that needs a schema registry both for validating discovered schemas (auto-detection) and for runtime schema resolution (build).

### Implementation
```java
public class DataValidatorBuilder extends AbstractAutomaticDependentBuilder<...> {

    public DataValidatorBuilder() {
        super(Set.of(
            // Schema registry needed in both phases
            DependencySpec.require(ISchemaRegistryBuilder.class, DependencyPhase.BOTH)
            // Or shorthand:
            // DependencySpec.require(ISchemaRegistryBuilder.class)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof ISchemaRegistry registry) {
            // Validate auto-detected schemas against registry
            this.discoveredSchemas.forEach(schema -> {
                if (!registry.isValid(schema)) {
                    log.warn("Invalid schema: {}", schema);
                }
            });
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof ISchemaRegistry registry) {
            // Use registry for runtime validation
            this.schemaRegistry = registry;
        }
    }

    @Override
    protected IDataValidator doBuild() {
        return new DataValidator(this.schemaRegistry);
    }
}
```

### Benefit
Explicit documentation that schema registry is needed throughout the lifecycle.

---

## Use Case 4: Multiple Dependencies with Different Phases

### Scenario
A complex builder with multiple dependencies needed at different phases.

### Implementation
```java
public class ApplicationBuilder extends AbstractAutomaticDependentBuilder<...> {

    public ApplicationBuilder() {
        super(Set.of(
            // Config file for auto-detecting components
            DependencySpec.use(IConfigurationBuilder.class, DependencyPhase.AUTO_DETECT),

            // Injection context for building components
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD),

            // Logger needed throughout
            DependencySpec.require(ILoggerBuilder.class, DependencyPhase.BOTH),

            // Metrics for build phase only
            DependencySpec.use(IMetricsBuilder.class, DependencyPhase.BUILD)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // Only receives: IConfiguration, ILogger
        if (dependency instanceof IConfiguration config) {
            this.autoDetectModules(config);
        }
        if (dependency instanceof ILogger logger) {
            logger.info("Starting auto-detection");
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Only receives: IInjectionContext, ILogger, IMetrics
        if (dependency instanceof IInjectionContext context) {
            this.context = context;
        }
        if (dependency instanceof ILogger logger) {
            logger.info("Starting build");
        }
        if (dependency instanceof IMetrics metrics) {
            metrics.increment("builds.started");
        }
    }
}
```

### Benefit
Each dependency is only processed in the phases where it's actually needed, improving performance and clarity.

---

## Use Case 5: Optional Caching During Auto-Detection

### Scenario
A builder that can use a cache to speed up auto-detection if available, but doesn't require it.

### Implementation
```java
public class EntityScannerBuilder extends AbstractAutomaticDependentBuilder<...> {

    public EntityScannerBuilder() {
        super(Set.of(
            // Cache is optional and only useful during auto-detection
            DependencySpec.use(ICacheBuilder.class, DependencyPhase.AUTO_DETECT)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof ICache cache) {
            // Check cache for previously scanned entities
            this.entities = cache.get("scanned.entities");
            if (this.entities != null) {
                log.info("Using cached entity scan results");
                return;
            }
        }

        // Cache miss or not available - do full scan
        this.entities = this.scanForEntities();

        if (dependency instanceof ICache cache) {
            cache.put("scanned.entities", this.entities);
        }
    }
}
```

### Benefit
Cache dependency is optional and only used during auto-detection, where it provides value.

---

## Use Case 6: Resource Manager with Phase-Specific Behavior

### Scenario
A builder that uses a resource manager differently in auto-detection vs. build phases.

### Implementation
```java
public class TemplateEngineBuilder extends AbstractAutomaticDependentBuilder<...> {

    public TemplateEngineBuilder() {
        super(Set.of(
            // Resource manager needed in both phases but used differently
            DependencySpec.require(IResourceManagerBuilder.class, DependencyPhase.BOTH)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof IResourceManager resources) {
            // During auto-detection: scan for template files
            this.templatePaths = resources.findResources("templates/**/*.html");
            log.info("Found {} templates", this.templatePaths.size());
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IResourceManager resources) {
            // During build: load actual template content
            this.templates = this.templatePaths.stream()
                .collect(Collectors.toMap(
                    path -> path,
                    path -> resources.loadResource(path)
                ));
        }
    }
}
```

### Benefit
Same dependency used in both phases, but with different responsibilities in each phase.

---

## Use Case 7: Validation Before Build

### Scenario
A builder that needs a validator only before building, not during auto-detection or after build.

### Implementation
```java
public class ConfigurableServiceBuilder extends AbstractAutomaticDependentBuilder<...> {

    public ConfigurableServiceBuilder() {
        super(Set.of(
            DependencySpec.use(IValidatorBuilder.class, DependencyPhase.BUILD)
        ));
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IValidator validator) {
            // Validate configuration before building
            ValidationResult result = validator.validate(this.config);
            if (!result.isValid()) {
                throw new DslException("Invalid configuration: " + result.getErrors());
            }
        }
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // Validator not needed after build
    }
}
```

### Benefit
Validator is only processed during build phase, and specifically only in pre-build.

---

## Anti-Pattern: Marking Everything as BOTH

### ❌ Bad Practice
```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<...> {
    public MyBuilder() {
        super(Set.of(
            // Marking everything as BOTH defeats the purpose!
            DependencySpec.require(IDep1.class, DependencyPhase.BOTH),
            DependencySpec.require(IDep2.class, DependencyPhase.BOTH),
            DependencySpec.require(IDep3.class, DependencyPhase.BOTH)
        ));
    }
}
```

### ✅ Good Practice
```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<...> {
    public MyBuilder() {
        super(Set.of(
            // Only mark as needed for specific phases
            DependencySpec.require(IDep1.class, DependencyPhase.AUTO_DETECT),
            DependencySpec.require(IDep2.class, DependencyPhase.BUILD),
            DependencySpec.require(IDep3.class, DependencyPhase.BOTH)
        ));
    }
}
```

### Rule of Thumb
Review your actual dependency usage in:
- `doAutoDetectionWithDependency()`
- `doPreBuildWithDependency()`
- `doPostBuildWithDependency()`
- `doBuild()` (for dependencies used in the actual build method)

Then set the phase accordingly based on **actual usage**, not just to be safe.

---

## Summary

| Phase | When to Use |
|-------|-------------|
| `AUTO_DETECT` | Dependency only needed for discovering/configuring components before build |
| `BUILD` | Dependency only needed for constructing the final object |
| `BOTH` | Dependency genuinely needed in both auto-detection and build phases |

Remember: The goal is **precision**, not **permissiveness**. Be specific about when dependencies are actually needed.
