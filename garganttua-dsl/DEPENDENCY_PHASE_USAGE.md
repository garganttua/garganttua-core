# Phase-Aware Dependency Management

## Overview

The dependency management system now supports specifying **when** dependencies are needed during the builder lifecycle:

- **AUTO_DETECT**: Dependency needed only during auto-detection phase
- **BUILD**: Dependency needed only during build phase (pre-build and post-build)
- **BOTH**: Dependency needed in both phases

## Benefits

1. **Fine-grained control**: Specify exactly when each dependency is required
2. **Performance**: Avoid unnecessary dependency processing in phases where they're not needed
3. **Clarity**: Makes dependency requirements explicit and self-documenting

## API

### DependencyPhase Enum

```java
public enum DependencyPhase {
    AUTO_DETECT,  // For auto-detection phase only
    BUILD,        // For build phase only
    BOTH          // For both phases
}
```

### DependencySpec Record

```java
// Create dependency specifications
DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT)
DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
DependencySpec.use(ICacheBuilder.class, DependencyPhase.BOTH)

// Shorthand for BOTH phases
DependencySpec.use(ISomeBuilder.class)      // equivalent to .use(..., BOTH)
DependencySpec.require(ISomeBuilder.class)  // equivalent to .require(..., BOTH)
```

## Migration Guide

### Before (Deprecated API)

```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<IMyBuilder, MyObject> {

    public MyBuilder() {
        super(
            Set.of(IConfigBuilder.class),              // use dependencies
            Set.of(IInjectionContextBuilder.class)     // require dependencies
        );
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // This is called for ALL dependencies, even if not needed for auto-detection!
        if (dependency instanceof IInjectionContext ctx) {
            // But IInjectionContext is not actually needed here...
        }
    }
}
```

### After (New Phase-Aware API)

```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<IMyBuilder, MyObject> {

    public MyBuilder() {
        super(Set.of(
            // Config is only needed during auto-detection
            DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT),

            // InjectionContext is only needed during build
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // Now only receives dependencies marked with AUTO_DETECT or BOTH
        if (dependency instanceof IConfig config) {
            // Use config for auto-detection
            this.autoDetectFromConfig(config);
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Now only receives dependencies marked with BUILD or BOTH
        if (dependency instanceof IInjectionContext ctx) {
            // Use context for building
            this.context = ctx;
        }
    }
}
```

## Real-World Example: RuntimeBuilder

```java
public class RuntimeBuilder extends AbstractAutomaticLinkedDependentBuilder<...> {

    public RuntimeBuilder(RuntimesBuilder parent, String name, ...) {
        super(
            parent,
            Set.of(),  // No optional dependencies
            Set.of(
                // InjectionContext is needed:
                // - During AUTO_DETECT: to set up stage builders
                // - During BUILD: to construct the runtime
                DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BOTH)
            )
        );
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext injectionContext) {
            // Called during auto-detection because spec says AUTO_DETECT or BOTH
            this.handle(injectionContext);
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Also receives IInjectionContext here because spec says BUILD or BOTH
        // (But in this case, RuntimeBuilder doesn't need it in pre-build)
    }
}
```

## Use Cases

### 1. Configuration Only Needed for Auto-Detection

```java
Set.of(
    DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT)
)
```

Use case: A builder that reads configuration files to auto-detect beans, but doesn't need the config during actual building.

### 2. Context Only Needed for Building

```java
Set.of(
    DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
)
```

Use case: A builder that needs the injection context to resolve dependencies during build, but not during auto-detection.

### 3. Dependency Needed Throughout

```java
Set.of(
    DependencySpec.require(IResourceManager.class, DependencyPhase.BOTH)
)
```

Use case: A resource manager that's needed both for discovering resources during auto-detection and for accessing them during build.

## Implementation Details

### How It Works

1. **Declaration**: Dependencies are declared with `DependencySpec` in the builder constructor
2. **Storage**: `DependentBuilderSupport` stores dependencies with their phase information
3. **Filtering**: During each phase, only relevant dependencies are processed:
   - `processAutoDetectionWithDependencies()`: Filters for `AUTO_DETECT` or `BOTH`
   - `processPreBuildDependencies()`: Filters for `BUILD` or `BOTH`
   - `processPostBuildDependencies()`: Filters for `BUILD` or `BOTH`

### Backward Compatibility

The old API (separate `useDependencies` and `requireDependencies` Sets) is still supported but deprecated. It defaults all dependencies to `DependencyPhase.BOTH` to maintain existing behavior.

## Testing Recommendations

When migrating to phase-aware dependencies:

1. **Identify actual usage**: Review `doAutoDetectionWithDependency()`, `doPreBuildWithDependency()`, and `doPostBuildWithDependency()` to see which dependencies are actually used in each phase

2. **Update specs accordingly**: Change dependency declarations to reflect actual usage patterns

3. **Test auto-detection**: Ensure auto-detection still works when dependencies are marked as `AUTO_DETECT`

4. **Test building**: Ensure building works when dependencies are marked as `BUILD`

5. **Remove dead code**: If a dependency was previously processed in a phase but never used, you can now safely exclude it from that phase
