# Garganttua ClassLoader

## Description

The **garganttua-classloader** module provides a Bootstrap-discoverable
runtime classpath manager that loads JARs into the thread context
classloader and fires registered hooks so downstream consumers (typically
`Bootstrap.rebuild()`) can react to the newly available classes.

It decouples runtime JAR hot-loading from the `garganttua-bootstrap` module
itself: the bootstrap is the build orchestrator, not the classpath manager.
With this module on the classpath, a script's `include("plugin.jar")` (or any
other JAR-aware consumer) only needs to know about an `IClassLoaderManager`
— never about `IBootstrap`.

**Key features:**

- **Bootstrap-discoverable** via the `IBootstrapBuilderFactory` SPI
  (`META-INF/services`). `Bootstrap.autoDetect(true).load()` picks the
  builder up automatically.
- **Auto-wiring with Bootstrap** — when Bootstrap finishes its build it
  registers itself as a rebuild hook on every freshly-built
  `IClassLoaderManager` it discovers, so `include("foo.jar")` triggers
  `Bootstrap.withPackage(pkg) + Bootstrap.rebuild()` transparently.
- **Hook-based extensibility** — anything that needs to react to JAR loads
  implements `IClassLoaderRebuildHook` and registers via
  `manager.addRebuildHook(...)`. Hooks run in registration order, exceptions
  are wrapped in `ClassLoaderException`.
- **Manifest-driven discovery** — reads the `Garganttua-Packages` attribute
  from each JAR's manifest, so plugin authors declare which packages should
  be scanned by downstream framework modules after loading.
- **Idempotent across rebuilds** — Bootstrap deduplicates hook registration
  via an `IdentityHashMap` so a `Bootstrap.rebuild()` cycle never piles up
  duplicate hooks on the same manager instance.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-classloader</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-bootstrap`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### IClassLoaderManager (commons-side interface)

Lives in `garganttua-commons` so the script layer can take it as a
constructor argument without depending on this module's implementation:

```java
public interface IClassLoaderManager {
    void loadJar(Path jar) throws ClassLoaderException;
    void addRebuildHook(IClassLoaderRebuildHook hook);
    List<IClassLoaderRebuildHook> getRebuildHooks();
}
```

### IClassLoaderRebuildHook

Functional interface fired by `loadJar()` with the list of packages declared
in the JAR's manifest. Bootstrap auto-registers a hook that does
`withPackage + rebuild`; user code can add its own for custom reactions.

```java
@FunctionalInterface
public interface IClassLoaderRebuildHook {
    void onJarLoaded(List<String> packages) throws Exception;
}
```

### ClassLoaderManager (implementation)

Default implementation in this module:

- Chains a fresh `URLClassLoader` onto the thread context classloader for
  each `loadJar` call.
- Reads the manifest via `JarManifestReader` (moved from
  `garganttua-script` so the script module no longer owns the loader
  concern).
- Iterates `CopyOnWriteArrayList<IClassLoaderRebuildHook>` to fire hooks —
  safe under concurrent callers.

### ClassLoaderManagerBuilder (Bootstrap surface)

Annotated `@Bootstrap` and listed in
`META-INF/services/com.garganttua.core.dsl.IBootstrapBuilderFactory`.
Builds cleanly with zero configuration — required so a Bootstrap that
discovers the manager but doesn't actually use JAR hot-loading doesn't
block the pipeline.

## Usage

### Out of the box — nothing to wire

With both `garganttua-classloader` and `garganttua-bootstrap` on the
classpath, the following just works:

```java
IBootstrap bootstrap = new Bootstrap();
bootstrap.autoDetect(true).withPackage("com.myapp").load();
bootstrap.build();
// IClassLoaderManager is now a bean. Bootstrap has registered itself as a
// rebuild hook on it.
```

Inside any script subsequently executed by the framework:

```bash
include("./plugins/my-plugin.jar")
# → JAR added to the thread classloader
# → Bootstrap.rebuild() fires, picks up @Bootstrap-annotated classes in the
#   JAR's declared Garganttua-Packages
```

### Manual usage outside Bootstrap

```java
IClassLoaderManager mgr = new ClassLoaderManager();
mgr.addRebuildHook(packages -> {
    System.out.println("JAR loaded with packages: " + packages);
    // ...trigger your own rebuild / cache invalidation here
});
mgr.loadJar(Path.of("/opt/plugins/foo.jar"));
```

### Declaring packages in a plugin JAR

In the plugin's `pom.xml` (or `MANIFEST.MF`):

```xml
<plugin>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifestEntries>
                <Garganttua-Packages>com.myplugin.expressions,com.myplugin.beans</Garganttua-Packages>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

## Tips and best practices

- **Keep the manager singleton-scoped** — chaining URLClassLoaders is cheap
  but each `loadJar` mutates the thread context classloader. Holding one
  shared manager per JVM and serialising loads via your own coordination
  layer prevents surprises in concurrent producers.
- **Manifest hygiene** — without the `Garganttua-Packages` attribute the
  JAR is added to the classpath but no hook fires (the manager logs a
  debug-level warning). Always set this attribute when shipping a plugin
  that wants to be auto-discovered by Bootstrap.
- **Hook ordering is contract-driven** — hooks are invoked in registration
  order. If you have a hook that depends on Bootstrap having rebuilt
  already, register it AFTER the Bootstrap auto-wire (i.e. after
  `bootstrap.build()`).
- **Don't reach for IBootstrap from a script context** — that coupling is
  what this module exists to remove. Wire your downstream rebuild logic as
  an `IClassLoaderRebuildHook` instead.

## License
This module is distributed under the MIT License.
