# Phase-Aware Dependency Management - Implementation Summary

## What Was Implemented

A fine-grained dependency management system that allows specifying **when** dependencies are needed during the builder lifecycle.

## New Components

### 1. DependencyPhase Enum
Location: `garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencyPhase.java`

```java
public enum DependencyPhase {
    AUTO_DETECT,  // Dependency needed only during auto-detection
    BUILD,        // Dependency needed only during build (pre/post)
    BOTH          // Dependency needed in both phases
}
```

**Key Methods:**
- `includesAutoDetect()`: Returns true if phase is AUTO_DETECT or BOTH
- `includesBuild()`: Returns true if phase is BUILD or BOTH

### 2. DependencySpec Record
Location: `garganttua-commons/src/main/java/com/garganttua/core/dsl/DependencySpec.java`

```java
public record DependencySpec(
    Class<? extends IObservableBuilder<?, ?>> dependencyClass,
    DependencyPhase phase,
    boolean required
)
```

**Factory Methods:**
- `DependencySpec.use(Class, DependencyPhase)`: Creates optional dependency
- `DependencySpec.require(Class, DependencyPhase)`: Creates required dependency
- `DependencySpec.use(Class)`: Creates optional dependency for BOTH phases
- `DependencySpec.require(Class)`: Creates required dependency for BOTH phases

**Helper Methods:**
- `isNeededForAutoDetect()`: Checks if needed during auto-detection
- `isNeededForBuild()`: Checks if needed during build

### 3. Updated BuilderDependency
Location: `garganttua-dsl/src/main/java/com/garganttua/core/dsl/BuilderDependency.java`

**New Constructor:**
```java
public BuilderDependency(
    Class<? extends IObservableBuilder<?, ?>> dependencyClass,
    DependencySpec spec
)
```

**New Methods:**
- `getSpec()`: Gets the dependency specification
- `isNeededForAutoDetect()`: Checks if dependency is needed for auto-detection
- `isNeededForBuild()`: Checks if dependency is needed for build

**Deprecated Constructor:**
```java
@Deprecated
public BuilderDependency(Class<? extends IObservableBuilder<?, ?>> dependencyClass)
```
(Defaults to DependencyPhase.BOTH for backward compatibility)

### 4. Updated DependentBuilderSupport
Location: `garganttua-dsl/src/main/java/com/garganttua/core/dsl/DependentBuilderSupport.java`

**New Constructor:**
```java
public DependentBuilderSupport(Set<DependencySpec> dependencySpecs)
```

**Updated Processing Methods:**
All now filter dependencies based on their phase:

- `processAutoDetectionWithDependencies()`: Filters for AUTO_DETECT or BOTH
- `processPreBuildDependencies()`: Filters for BUILD or BOTH
- `processPostBuildDependencies()`: Filters for BUILD or BOTH

**Deprecated Constructor:**
```java
@Deprecated
public DependentBuilderSupport(
    Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
    Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies
)
```
(Still supported for backward compatibility)

## How It Works

### Before (Old API)

```java
super(
    Set.of(IConfigBuilder.class),           // use
    Set.of(IInjectionContextBuilder.class)  // require
);
```

All dependencies are processed in all phases, regardless of whether they're actually needed.

### After (New API)

```java
super(Set.of(
    DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT),
    DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
));
```

Dependencies are only processed in the phases where they're actually needed.

## Filtering Mechanism

### During Auto-Detection
```java
useDependencies.stream()
    .filter(d -> d instanceof BuilderDependency<?, ?> bd && bd.isNeededForAutoDetect())
    .forEach(d -> processIfReady(d, autoDetectHandler));
```

Only dependencies with phase AUTO_DETECT or BOTH are processed.

### During Build (Pre/Post)
```java
useDependencies.stream()
    .filter(d -> d instanceof BuilderDependency<?, ?> bd && bd.isNeededForBuild())
    .forEach(d -> processIfReady(d, preBuildHandler));
```

Only dependencies with phase BUILD or BOTH are processed.

## Benefits

1. **Performance**: Avoid unnecessary dependency processing
2. **Clarity**: Explicit declaration of when dependencies are needed
3. **Correctness**: Ensures dependencies are only used in appropriate phases
4. **Maintainability**: Self-documenting code
5. **Backward Compatibility**: Old API still works (defaults to BOTH)

## Migration Path

### Step 1: Identify Usage
Review your builder's dependency usage:
- Check `doAutoDetectionWithDependency()` - which dependencies are used?
- Check `doPreBuildWithDependency()` - which dependencies are used?
- Check `doPostBuildWithDependency()` - which dependencies are used?
- Check `doBuild()` - which dependencies are used?

### Step 2: Update Constructor
Replace old constructor calls:

```java
// Old
super(useDeps, requireDeps);

// New
super(Set.of(
    DependencySpec.use(Dep1.class, DependencyPhase.AUTO_DETECT),
    DependencySpec.require(Dep2.class, DependencyPhase.BUILD),
    DependencySpec.require(Dep3.class, DependencyPhase.BOTH)
));
```

### Step 3: Test
- Verify auto-detection still works
- Verify building still works
- Check that dependencies are only processed when needed

## Examples

See detailed examples in:
- `DEPENDENCY_PHASE_USAGE.md`: Complete usage guide
- `MIGRATION_EXAMPLE_RuntimesBuilder.md`: RuntimesBuilder migration example
- `MIGRATION_EXAMPLE_RuntimeBuilder.md`: RuntimeBuilder migration example

## Common Patterns

### Config for Auto-Detection Only
```java
DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT)
```
Use when: Builder reads config to auto-detect components, but doesn't need config during build.

### Context for Build Only
```java
DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
```
Use when: Builder needs injection context to resolve dependencies during build, but not during auto-detection.

### Resource Manager Throughout
```java
DependencySpec.require(IResourceManager.class, DependencyPhase.BOTH)
// Or shorthand:
DependencySpec.require(IResourceManager.class)
```
Use when: Resource is needed for both discovering resources (auto-detection) and accessing them (build).

## Affected Classes

The following abstract builder classes now support phase-aware dependencies:
- `AbstractDependentBuilder`
- `AbstractAutomaticDependentBuilder`
- `AbstractLinkedDependentBuilder`
- `AbstractAutomaticLinkedDependentBuilder`

All concrete builders extending these classes can now use the new API.

## Breaking Changes

**None** - The old API is deprecated but still fully functional. All existing code continues to work with the same behavior (all dependencies default to BOTH phases).

## Deprecation Timeline

- Current version: Both old and new APIs available
- Recommended: Migrate to new API at your convenience
- Future version: Old API will be removed (marked with `forRemoval = true`)
