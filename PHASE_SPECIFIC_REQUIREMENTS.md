# Phase-Specific Dependency Requirements

## 🎯 New Feature: Different Requirements Per Phase

You can now specify that a dependency is **required** in one phase and **optional** in another!

### The Problem This Solves

Previously, you had to choose:
- **Required** everywhere (even if only needed in one phase)
- **Optional** everywhere (even if critical in one phase)

Now you can be precise:
- **Required for AUTO_DETECT**, optional for BUILD
- **Optional for AUTO_DETECT**, required for BUILD
- Any combination you need

## 📖 API

### DependencyRequirement Enum

```java
public enum DependencyRequirement {
    OPTIONAL,                    // Optional in all phases
    REQUIRED,                    // Required in all phases
    REQUIRED_FOR_AUTO_DETECT,    // Required in AUTO_DETECT, optional in BUILD
    REQUIRED_FOR_BUILD           // Optional in AUTO_DETECT, required in BUILD
}
```

### Fluent Builder API

```java
// Config REQUIRED for auto-detection, OPTIONAL for build
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()
    .useForBuild()

// InjectionContext OPTIONAL for auto-detection, REQUIRED for build
DependencySpec.of(IInjectionContextBuilder.class)
    .useForAutoDetect()
    .requireForBuild()
```

## 💡 Use Cases

### Use Case 1: Configuration Required Only During Auto-Detection

**Scenario**: Your builder reads a configuration file to auto-detect components, but once those components are discovered, the configuration is no longer needed during build.

**Before (All-or-Nothing)**:
```java
public class ServiceRegistryBuilder extends AbstractAutomaticDependentBuilder<...> {
    public ServiceRegistryBuilder() {
        super(Set.of(
            // Have to mark as REQUIRED even though only needed for auto-detection
            DependencySpec.require(IConfigBuilder.class, DependencyPhase.BOTH)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof IConfig config) {
            this.discoverServices(config);  // Config IS used here
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IConfig config) {
            // Config NOT used here, but still gets passed!
        }
    }
}
```

**After (Phase-Specific)**:
```java
public class ServiceRegistryBuilder extends AbstractAutomaticDependentBuilder<...> {
    public ServiceRegistryBuilder() {
        super(Set.of(
            // REQUIRED for auto-detection, OPTIONAL for build
            DependencySpec.of(IConfigBuilder.class)
                .requireForAutoDetect()
                .useForBuild()
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // Config IS passed here and MUST be ready
        if (dependency instanceof IConfig config) {
            this.discoverServices(config);
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Config might be passed here if available, but NOT required
        // This method only gets called with config if it happens to be ready
    }
}
```

**Benefits**:
- ✅ Auto-detection FAILS if config is missing (correct behavior)
- ✅ Build can proceed without config (correct behavior)
- ✅ Clear contract: config is mandatory for discovery, optional afterwards

---

### Use Case 2: InjectionContext Required Only During Build

**Scenario**: Your builder uses reflection for auto-detection (doesn't need DI), but requires the injection context to wire up dependencies during build.

**Before (All-or-Nothing)**:
```java
public class PluginManagerBuilder extends AbstractAutomaticDependentBuilder<...> {
    public PluginManagerBuilder() {
        super(Set.of(
            // Have to mark as REQUIRED for both phases
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BOTH)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // InjectionContext IS passed here even though not needed!
        if (dependency instanceof IInjectionContext context) {
            // Not used during auto-detection
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext context) {
            this.injectPlugins(context);  // Context IS used here
        }
    }
}
```

**After (Phase-Specific)**:
```java
public class PluginManagerBuilder extends AbstractAutomaticDependentBuilder<...> {
    public PluginManagerBuilder() {
        super(Set.of(
            // OPTIONAL for auto-detection, REQUIRED for build
            DependencySpec.of(IInjectionContextBuilder.class)
                .useForAutoDetect()
                .requireForBuild()
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // InjectionContext might be passed if available, but NOT required
        // Auto-detection uses reflection instead
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // InjectionContext IS passed here and MUST be ready
        if (dependency instanceof IInjectionContext context) {
            this.injectPlugins(context);
        }
    }
}
```

**Benefits**:
- ✅ Auto-detection works even if InjectionContext not ready yet
- ✅ Build FAILS if InjectionContext is missing (correct behavior)
- ✅ Clear contract: DI is optional for discovery, mandatory for wiring

---

### Use Case 3: Schema Registry with Different Requirements

**Scenario**: A schema registry can optionally validate schemas during auto-detection (nice-to-have), but MUST be available during build for runtime validation (critical).

```java
public class DataValidatorBuilder extends AbstractAutomaticDependentBuilder<...> {
    public DataValidatorBuilder() {
        super(Set.of(
            // OPTIONAL for auto-detection (validation is nice-to-have)
            // REQUIRED for build (validation is mandatory)
            DependencySpec.of(ISchemaRegistryBuilder.class)
                .useForAutoDetect()
                .requireForBuild()
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof ISchemaRegistry registry) {
            // If available, validate discovered schemas
            this.validateDiscoveredSchemas(registry);
        }
        // If not available, that's OK - discovery still works
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Schema registry MUST be ready here
        if (dependency instanceof ISchemaRegistry registry) {
            this.schemaRegistry = registry;  // Critical for runtime
        }
    }

    @Override
    protected IDataValidator doBuild() {
        // Will fail if schemaRegistry is null (as it should)
        return new DataValidator(this.schemaRegistry);
    }
}
```

**Benefits**:
- ✅ Auto-detection can use schema registry if available for early validation
- ✅ Auto-detection doesn't fail if schema registry not ready yet
- ✅ Build ensures schema registry is available before creating validator

---

## 📝 Validation Behavior

### Required Dependencies

When a dependency is **required** for a phase:
- The dependency **MUST** be ready when that phase executes
- If the dependency is not ready, a `DslException` is thrown
- The build/auto-detection fails immediately with a clear error message

### Optional Dependencies

When a dependency is **optional** for a phase:
- The dependency is processed **if ready**
- If the dependency is not ready, the phase continues normally
- No error is thrown

### Examples

```java
// Config required for auto-detection
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()   // MUST be ready during auto-detection
    .useForBuild()            // Processed if ready during build

// If config is not ready during auto-detection:
// → DslException thrown: "Required dependency IConfigBuilder is not ready during auto-detection phase"

// If config is not ready during build:
// → No error, build continues normally
```

```java
// InjectionContext required for build
DependencySpec.of(IInjectionContextBuilder.class)
    .useForAutoDetect()      // Processed if ready during auto-detection
    .requireForBuild()       // MUST be ready during build

// If InjectionContext is not ready during auto-detection:
// → No error, auto-detection continues normally

// If InjectionContext is not ready during build:
// → DslException thrown: "Required dependency IInjectionContextBuilder is not ready during pre-build phase"
```

## 🔄 Migration from Simple Required/Optional

### Old API (Still Supported)
```java
// Simple required for all phases
DependencySpec.require(IConfigBuilder.class, DependencyPhase.BOTH)

// Simple optional for all phases
DependencySpec.use(IConfigBuilder.class, DependencyPhase.BOTH)
```

### New API (Phase-Specific)
```java
// Required for auto-detection, optional for build
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()
    .useForBuild()

// Optional for auto-detection, required for build
DependencySpec.of(IInjectionContextBuilder.class)
    .useForAutoDetect()
    .requireForBuild()

// Required for both phases (equivalent to old .require(..., BOTH))
DependencySpec.of(ILoggerBuilder.class)
    .requireForAutoDetect()
    .requireForBuild()

// Optional for both phases (equivalent to old .use(..., BOTH))
DependencySpec.of(ICacheBuilder.class)
    .useForAutoDetect()
    .useForBuild()
```

## ⚠️ Common Pitfalls

### Pitfall 1: Making Everything Required

❌ **Bad**:
```java
// Over-constraining: making everything required everywhere
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()
    .requireForBuild()  // Do you really need config during build?
```

✅ **Good**:
```java
// Precise: config only needed for auto-detection
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()
    .useForBuild()  // Optional during build
```

### Pitfall 2: Making Everything Optional

❌ **Bad**:
```java
// Under-constraining: making everything optional
DependencySpec.of(IInjectionContextBuilder.class)
    .useForAutoDetect()
    .useForBuild()  // Build will silently fail if context is missing!
```

✅ **Good**:
```java
// Precise: context is critical for build
DependencySpec.of(IInjectionContextBuilder.class)
    .useForAutoDetect()
    .requireForBuild()  // Build fails fast if context is missing
```

### Pitfall 3: Wrong Phase Configuration

❌ **Bad**:
```java
// Config is required for auto-detection but marked for build!
DependencySpec.of(IConfigBuilder.class)
    .useForAutoDetect()       // Should be requireForAutoDetect
    .requireForBuild()        // Should be useForBuild
```

✅ **Good**:
```java
// Matches actual usage pattern
DependencySpec.of(IConfigBuilder.class)
    .requireForAutoDetect()   // Config IS used in auto-detection
    .useForBuild()            // Config NOT needed in build
```

## 📊 Comparison Table

| Dependency Spec | AUTO_DETECT | BUILD | Use Case |
|----------------|-------------|-------|----------|
| `.requireForAutoDetect().useForBuild()` | Required | Optional | Config files for discovery |
| `.useForAutoDetect().requireForBuild()` | Optional | Required | InjectionContext for wiring |
| `.requireForAutoDetect().requireForBuild()` | Required | Required | Logger needed everywhere |
| `.useForAutoDetect().useForBuild()` | Optional | Optional | Cache for optimization |

## 🎓 Best Practices

1. **Analyze actual usage**: Check which dependencies are actually used in each phase
2. **Fail fast**: Mark critical dependencies as required
3. **Be lenient**: Mark nice-to-have dependencies as optional
4. **Document intent**: The requirement level documents your builder's contract
5. **Test both phases**: Ensure both auto-detection and build work correctly

## 🔗 See Also

- [DependencyPhase.java](garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencyPhase.java) - Phase enum
- [DependencyRequirement.java](garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencyRequirement.java) - Requirement enum
- [DependencySpec.java](garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencySpec.java) - Specification record
- [DependencySpecBuilder.java](garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencySpecBuilder.java) - Fluent builder
