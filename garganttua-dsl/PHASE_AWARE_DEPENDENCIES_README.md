# Phase-Aware Dependency Management

## 🎯 Quick Start

Your dependency management system now supports specifying **when** dependencies are needed!

```java
// Old way (still works, but deprecated)
super(Set.of(IConfigBuilder.class), Set.of(IInjectionContextBuilder.class));

// New way - specify exactly when each dependency is needed
super(Set.of(
    DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT),
    DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
));
```

## 📚 Documentation

### Core Documentation
1. **[PHASE_AWARE_DEPENDENCIES_SUMMARY.md](PHASE_AWARE_DEPENDENCIES_SUMMARY.md)**
   - Complete technical implementation summary
   - All new components and how they work
   - Migration path and backward compatibility

2. **[DEPENDENCY_PHASE_USAGE.md](DEPENDENCY_PHASE_USAGE.md)**
   - Detailed usage guide with examples
   - API reference
   - Migration guide from old API to new API

3. **[PHASE_AWARE_USE_CASES.md](PHASE_AWARE_USE_CASES.md)**
   - Real-world use case examples
   - Common patterns and anti-patterns
   - When to use AUTO_DETECT vs BUILD vs BOTH

### Migration Examples
4. **[MIGRATION_EXAMPLE_RuntimesBuilder.md](MIGRATION_EXAMPLE_RuntimesBuilder.md)**
   - Detailed migration example for RuntimesBuilder
   - Analysis of dependency usage
   - Recommended phase specifications

5. **[MIGRATION_EXAMPLE_RuntimeBuilder.md](MIGRATION_EXAMPLE_RuntimeBuilder.md)**
   - Detailed migration example for RuntimeBuilder
   - Analysis of dependency usage across lifecycle
   - Explanation of why BOTH phase is appropriate

## 🚀 What's New

### Three Lifecycle Phases

Your builders now have three distinct phases where dependencies can be specified:

| Phase | Description | When Dependencies Are Processed |
|-------|-------------|----------------------------------|
| `AUTO_DETECT` | Auto-detection phase | In `doAutoDetectionWithDependency()` |
| `BUILD` | Build phase | In `doPreBuildWithDependency()` and `doPostBuildWithDependency()` |
| `BOTH` | Both phases | In all dependency handling methods |

### New Components

1. **DependencyPhase** enum - Defines the three phases
2. **DependencySpec** record - Combines dependency class with phase information
3. **Enhanced BuilderDependency** - Now tracks phase information
4. **Enhanced DependentBuilderSupport** - Filters dependencies by phase

## 💡 Why Use This?

### Before (All dependencies processed everywhere)
```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<...> {
    public MyBuilder() {
        super(Set.of(), Set.of(IInjectionContextBuilder.class));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // InjectionContext is passed here even if not needed!
        if (dependency instanceof IInjectionContext ctx) {
            // Not actually used during auto-detection
        }
    }
}
```

### After (Dependencies only processed when needed)
```java
public class MyBuilder extends AbstractAutomaticDependentBuilder<...> {
    public MyBuilder() {
        super(Set.of(
            // Only needed during BUILD, not AUTO_DETECT
            DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
        ));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) {
        // InjectionContext is NOT passed here - not needed!
    }
}
```

## ✅ Benefits

1. **Performance**: Dependencies only processed in phases where needed
2. **Clarity**: Explicit declaration of dependency requirements
3. **Correctness**: Prevents dependencies from being used in wrong phases
4. **Documentation**: Self-documenting code
5. **Backward Compatible**: Old API still works (defaults to BOTH)

## 🔧 How to Use

### Step 1: Analyze Your Dependencies

Look at your builder and identify which dependencies are used where:

- Check `doAutoDetectionWithDependency()` - used during auto-detection?
- Check `doPreBuildWithDependency()` - used before build?
- Check `doPostBuildWithDependency()` - used after build?
- Check `doBuild()` - used during the actual build?

### Step 2: Specify Phases

Update your constructor:

```java
super(Set.of(
    // Config only for auto-detection
    DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT),

    // Context only for building
    DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD),

    // Logger needed throughout
    DependencySpec.require(ILoggerBuilder.class, DependencyPhase.BOTH),

    // Or use shorthand for BOTH:
    DependencySpec.require(ILoggerBuilder.class)  // Defaults to BOTH
));
```

### Step 3: Test

- Verify auto-detection works correctly
- Verify building works correctly
- Check that dependencies are only processed when expected

## 📖 Common Patterns

### Pattern 1: Config for Auto-Detection Only
```java
DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT)
```
**When**: Reading configuration to discover components, not needed during build.

### Pattern 2: Context for Build Only
```java
DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
```
**When**: Using injection context to resolve dependencies during build, not during auto-detection.

### Pattern 3: Resource Throughout
```java
DependencySpec.require(IResourceManager.class, DependencyPhase.BOTH)
```
**When**: Resource needed for discovering resources (auto-detection) AND accessing them (build).

## ⚠️ Important Notes

### Backward Compatibility

The old API is **deprecated but still works**:

```java
// This still works - all dependencies default to BOTH phases
super(
    Set.of(IOptionalDep.class),
    Set.of(IRequiredDep.class)
);
```

### Migration is Optional

You can migrate at your own pace. The old API will continue to work until a future version.

### Be Specific

Don't just mark everything as `BOTH` - analyze actual usage and be specific about phases.

## 📋 Checklist for Migrating a Builder

- [ ] Read `DEPENDENCY_PHASE_USAGE.md`
- [ ] Analyze dependency usage in your builder
- [ ] Update constructor to use `DependencySpec`
- [ ] Test auto-detection
- [ ] Test building
- [ ] Verify dependencies are processed in correct phases
- [ ] Update documentation if needed

## 🔗 See Also

- [Complete Summary](PHASE_AWARE_DEPENDENCIES_SUMMARY.md) - Full technical documentation
- [Usage Guide](DEPENDENCY_PHASE_USAGE.md) - How to use the new API
- [Use Cases](PHASE_AWARE_USE_CASES.md) - Real-world examples
- [RuntimesBuilder Migration](MIGRATION_EXAMPLE_RuntimesBuilder.md) - Specific example
- [RuntimeBuilder Migration](MIGRATION_EXAMPLE_RuntimeBuilder.md) - Specific example

## 🆘 Questions?

Check the use cases document for examples similar to your situation:
- [PHASE_AWARE_USE_CASES.md](PHASE_AWARE_USE_CASES.md)

Or review the migration examples:
- [RuntimesBuilder Example](MIGRATION_EXAMPLE_RuntimesBuilder.md)
- [RuntimeBuilder Example](MIGRATION_EXAMPLE_RuntimeBuilder.md)
