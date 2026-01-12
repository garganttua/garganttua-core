# Migration Example: RuntimeBuilder

## Current Implementation (Deprecated API)

```java
public class RuntimeBuilder<InputType, OutputType>
        extends AbstractAutomaticLinkedDependentBuilder<...> {

    public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, ...) {
        super(
            Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"),
            Set.of(),                                  // No optional dependencies
            Set.of(IInjectionContextBuilder.class)     // InjectionContext is REQUIRED
        );
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (dependency instanceof IInjectionContext injectionContext) {
            // InjectionContext IS used during auto-detection!
            this.handle(injectionContext);
        }
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // No pre-build operations needed with dependencies for RuntimeBuilder
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build operations needed with dependencies for RuntimeBuilder
    }
}
```

## Analysis

Looking at RuntimeBuilder's usage of IInjectionContext:

1. **Auto-detection phase**:
   - ✅ USED in `doAutoDetectionWithDependency()` to handle the injection context
   - Passes context to stage builders during auto-detection

2. **Build phase**:
   - ✅ USED in `doBuild()` - context is required for constructing Runtime object
   - ❌ NOT used in `doPreBuildWithDependency()` (empty method)
   - ❌ NOT used in `doPostBuildWithDependency()` (empty method)

## Recommended Migration

Since InjectionContext is used in **both** auto-detection and build phases:

```java
public class RuntimeBuilder<InputType, OutputType>
        extends AbstractAutomaticLinkedDependentBuilder<...> {

    public RuntimeBuilder(RuntimesBuilder runtimesBuilder, String name, ...) {
        super(
            Objects.requireNonNull(runtimesBuilder, "RuntimesBuilder cannot be null"),
            Set.of(
                // InjectionContext is needed in BOTH phases:
                // - AUTO_DETECT: to setup stage builders
                // - BUILD: to construct the Runtime object
                DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BOTH)
            )
        );
    }
}
```

### Equivalent Shorthand

Since `BOTH` is the default, you can also write:

```java
Set.of(
    DependencySpec.require(IInjectionContextBuilder.class)  // Defaults to BOTH
)
```

## Benefits of This Migration

1. **Documentation**: Makes it explicit that InjectionContext is needed throughout the lifecycle
2. **Correctness**: Accurately reflects the actual dependency usage
3. **Maintainability**: Future developers will understand the dependency requirements

## Code Flow

### During Auto-Detection (with InjectionContext)

```
RuntimeBuilder.build()
  → doAutoDetection()
  → doAutoDetectionWithDependency(IInjectionContext) ✅ USED
      → this.handle(injectionContext)
      → stages.values().forEach(s -> s.handle(context))
```

### During Build (with InjectionContext)

```
RuntimeBuilder.build()
  → doPreBuildWithDependency(IInjectionContext) ❌ NOT USED (empty)
  → doBuild() ✅ USES this.context
      → new Runtime<>(..., this.context, ...)
  → doPostBuildWithDependency(IInjectionContext) ❌ NOT USED (empty)
```

## Conclusion

For RuntimeBuilder, `DependencyPhase.BOTH` is correct because:
- Auto-detection needs it to configure stage builders
- Build needs it to construct the Runtime object

Even though pre-build and post-build methods don't use it, the `doBuild()` method does, which makes it required for the BUILD phase.
