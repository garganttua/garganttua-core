# Migration Example: RuntimesBuilder

## Current Implementation (Deprecated API)

```java
public class RuntimesBuilder extends AbstractAutomaticDependentBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private RuntimesBuilder() {
        super(
                Set.of(),                                  // No optional dependencies
                Set.of(IInjectionContextBuilder.class));   // InjectionContext is REQUIRED

        // ...
    }
}
```

### Problem

Currently, `IInjectionContextBuilder` is declared as a required dependency, which means:
- It will be processed in **ALL** phases (auto-detection, pre-build, post-build)
- But looking at the code, it's **only actually used during BUILD phase**, not during auto-detection

## Recommended Migration

```java
public class RuntimesBuilder extends AbstractAutomaticDependentBuilder<IRuntimesBuilder, Map<String, IRuntime<?, ?>>>
        implements IRuntimesBuilder {

    private RuntimesBuilder() {
        super(Set.of(
                // InjectionContext is only needed during BUILD phase
                // Not needed during auto-detection because RuntimesBuilder uses reflection
                // to auto-detect runtimes, not dependency injection
                DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
        ));

        // ...
    }
}
```

## Benefits of This Migration

1. **Clarity**: Makes it explicit that InjectionContext is only needed for building, not auto-detection
2. **Performance**: Avoids unnecessary processing of InjectionContext during auto-detection phase
3. **Correctness**: Matches the actual usage pattern in the code

## Analysis

Looking at `RuntimesBuilder` code:

- **Auto-detection**: Uses reflection to scan for `@RuntimeDefinition` annotations
  - Does NOT need InjectionContext

- **Build phase**: Creates runtime objects and needs InjectionContext
  - DOES need InjectionContext

Therefore, `DependencyPhase.BUILD` is the correct specification.

## Alternative: If InjectionContext Was Needed in Both Phases

If in the future RuntimesBuilder needs InjectionContext during auto-detection (for example, to auto-detect runtimes from beans in the context), you would use:

```java
DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BOTH)
```

Or simply (equivalent shorthand):

```java
DependencySpec.require(IInjectionContextBuilder.class)
```
