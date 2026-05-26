# Garganttua Observability

## Description

Garganttua Observability provides the **script-side instrumentation bridge** (`:observe(...)` expression function) over the observer primitives that now live in `garganttua-commons` (so any module — including foundation layers like `injection` — can be made observable without creating a dependency cycle).

The primitives themselves — sealed event hierarchy (`StartEvent`, `EndEvent`, `ErrorEvent`), `IObservable<E>`, `IObserver<E>`, `ObservableRegistry`, `ObservableContextHolder`, `ObservabilityEmitter` — are in `garganttua-commons` under package `com.garganttua.core.observability`. Code that only needs to emit events depends on `commons`; only this module is needed when scripts must emit `:observe(...)` events.

**Engines instrumented out of the box** (as of 2.0.0-ALPHA02): `Workflow`, `Runtime` (per-execution + per-step), `ScriptContext` (compile/execute), `Mapper`, `Bootstrap` (per phase + per builder), `InterruptibleLeaseMutex`, `BeanFactory` (bean creation). An observer attached to a `Workflow` sees correlated events from every nested engine — they all share the same `executionId` thanks to `ObservableContextHolder` stack-based propagation.

The module deliberately offers **only the primitives** — aggregators, metrics backends, and export adapters are left to consumers. Existing in-module patterns (`MapperMetrics`, `StatsObserver` in `garganttua-api`) remain valid examples to copy.

**Key Features:**
- **Sealed Event Hierarchy** - `StartEvent`, `EndEvent`, `ErrorEvent` with correlation IDs, timestamps, and source identifiers (in `commons`)
- **Single-callback Observer** - `IObserver.onEvent(E)` with pattern-matching dispatch on the consumer side (in `commons`)
- **Thread-safe Registry** - `ObservableRegistry` backed by `CopyOnWriteArrayList`, exception-isolated, with `hasObservers()` short-circuit (in `commons`)
- **Stackable Session Holder** - `ObservableContextHolder.push()` returns the previous session, `pop(previous)` restores it — nested engine invocations share the same `executionId` (in `commons`)
- **Engine Emission Helper** - `ObservabilityEmitter.open(...)` / `joinCurrent()` bundle push/pop + start/end/error fire so engines instrument with a single `try-with-resources` block (in `commons`)
- **Script-side Instrumentation** - `:observe("start"|"end"|"error", source[, code])` expression function for scripts and workflows (in this module)
- **Zero-cost When Disabled** - Empty registry path is non-allocating; `hasObservers()` short-circuit skips event allocation

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-observability</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-expression`
 - `com.garganttua.core:garganttua-condition`
 - `com.garganttua.core:garganttua-supply`
 - `com.garganttua.core:garganttua-runtime-reflection:test`
 - `com.garganttua.core:garganttua-reflections:test`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Observable Event

An `ObservableEvent` is a sealed interface implemented by `StartEvent`, `EndEvent`, and `ErrorEvent`. Each event carries:
- `executionId` — a `UUID` correlating all events of a single execution
- `timestamp` — when the event was emitted
- `source` — a stable string identifier (e.g. `"workflow:users:update"`, `"stage:verify_auth"`)

### Observer

`IObserver<E>` exposes a single `onEvent(E)` callback. Implementations use `switch` pattern matching on the sealed event hierarchy to dispatch.

### Registry

`ObservableRegistry<E>` collects observers via `addObserver`/`removeObserver`, iterates over them in a thread-safe manner via `CopyOnWriteArrayList`, and catches exceptions thrown by observers so a single broken observer cannot break a workflow.

### Script Instrumentation

The `:observe(eventType, source[, code])` expression function reads the current registry from `ObservableContextHolder` (a `ThreadLocal`) and fires the corresponding event. Workflows push their registry on the holder before executing the generated script and pop it in `finally`.

### Stack-based Session Propagation

`ObservableContextHolder.push()` returns the previously bound `Session` (or `null`); `pop(previous)` restores it. This allows a workflow that invokes a script that invokes a runtime to share the same registry/executionId at every level — a single observer at the workflow level sees every nested event with consistent correlation. Engines that wrap their own units of work use `ObservabilityEmitter.open()` to push automatically, or `ObservabilityEmitter.joinCurrent()` to passively piggy-back on the parent session.

### Source Naming Conventions

Engines emit events whose `source` follows a hierarchical, colon-separated convention so observers can dispatch with simple prefix matching:

| Engine | Sources |
|---|---|
| Workflow | `stage:<name>`, `script:<stage>.<scriptName>` (generated `:observe`) |
| ScriptContext | `scriptcontext:compile`, `scriptcontext:execute` |
| Runtime | `runtime:<name>`, `runtime:<name>:step:<stepName>`, `runtime:<name>:step:<stepName>:fallback` |
| Mapper | `mapper:<src>-><dst>` |
| Bootstrap | `bootstrap:build`, `bootstrap:phase:resolve`, `bootstrap:builder:<className>` |
| Mutex | `mutex:<mutexName>` |
| Injection | `injection:bean:<beanRef>` (fired during instantiation; propagates to parent session) |

## Usage

### Defining an observer

```java
IObserver<ObservableEvent> observer = event -> {
    switch (event) {
        case StartEvent s -> log.info("start {}", s.source());
        case EndEvent e -> log.info("end {} took {}", e.source(), e.duration());
        case ErrorEvent err -> log.warn("error {}: {}", err.source(), err.failure().getMessage());
    }
};
```

### Wiring on a workflow

```java
Workflow wf = WorkflowBuilder.create()
    .name("users:update")
    .stage(...)
    .timing(WorkflowTimingConfig.of().stages(true).scripts(true))
    .build();

wf.addObserver(observer);
WorkflowResult result = wf.execute(input);
```

### Multi-observable DSL with filters

For wiring one observer to several observables in one expression — with
optional per-subscription filters — use the `ObservabilityBuilder` DSL.
Filters compose with AND when called multiple times. `build()` returns an
`AutoCloseable` binding that detaches every observer when closed.

```java
import static com.garganttua.core.condition.Conditions.*;
import com.garganttua.core.observability.dsl.ObservabilityBuilder;

try (var binding = ObservabilityBuilder.create()
        .observe(workflow, mapper, runtime)            // default source set
        .observer(loggingObserver)                     // attached to all three
        .up()
        .observer(alertObserver)
            // garganttua-condition DSL — same vocabulary as RuntimeStepMethodBuilder.condition(...)
            .when(events -> and(
                custom(events, ObservableEvent::source,
                    src -> src != null && src.startsWith("workflow:critical:")),
                custom(events, e -> e instanceof EndEvent ee
                    && ee.code() != null && ee.code() >= 400)))
        .up()
        .observer(errorReporter)
            .onlyEvents(ErrorEvent.class)              // sugar
            .matchingSource("workflow:*")              // glob — only * is special
        .up()
        .observer(timingObserver)
            .where(e -> e instanceof EndEvent ee       // Predicate escape hatch
                && ee.duration().toMillis() > 1000)
            .toObservable(runtime)                     // narrow this observer to runtime only
        .up()
        .build()) {

    workflow.execute(input);
    // ... binding.count() registrations live until close()
}   // binding.close() detaches every wrapper
```

The condition lambda receives an `EventHolderSupplierBuilder` named
`events` by convention. The framework refreshes the holder with the current
event before each `fullEvaluate()`, so user code reads the event through
`custom(events, ...)` without dealing with the supplier mutation directly.

### Log observers (file / console)

Two ready-to-use sinks ship in `com.garganttua.core.observability.log`. Both
are pure JDK — no external dependency. Pair them with `IEventFormatter`:
- `PlainTextEventFormatter` — human single-line, ideal for console / dev tail.
- `JsonLineEventFormatter` — NDJSON, ingestible by Elasticsearch / Loki /
  Filebeat / Splunk without transformation.

```java
import com.garganttua.core.observability.log.*;

// Console — dev mode
ConsoleLogObserver console = ConsoleLogObserver.builder()
        .formatter(PlainTextEventFormatter.INSTANCE)
        .build();

// File — production mode, NDJSON for log shippers
try (FileLogObserver file = FileLogObserver.builder()
        .path(Path.of("/var/log/garganttua/events.ndjson"))
        .formatter(JsonLineEventFormatter.INSTANCE)
        .append(true)
        .build();
     var binding = ObservabilityBuilder.create()
            .observe(workflow, mapper, runtime, scriptContext, bootstrap)
            .observer(console).up()
            .observer(file).up()
            .build()) {

    workflow.execute(input);
}   // file flushed and closed; binding detaches both observers
```

**Custom sinks (Elasticsearch, Loki, custom HTTP, etc.)** belong in dedicated
binding modules to keep this module dependency-free. Pattern: implement
`IObserver<ObservableEvent>` (optionally `AutoCloseable`), reuse one of the
ship-with formatters or write a custom `IEventFormatter`. Example layout for
a future binding:

```
garganttua-bindings/
  garganttua-observability-elasticsearch/
    pom.xml (depends on observability + ES client)
    src/main/java/.../ElasticsearchLogObserver.java
```

## Tips and best practices

- Keep observer logic fast and side-effect free — observers run on the caller thread.
- Use `hasObservers()` before constructing expensive event payloads.
- Correlate by `executionId` rather than parsing `source` strings.
- For aggregation/metrics, build a domain-specific observer (cf. `MapperMetrics`) — the registry itself is just plumbing.

## License
This module is distributed under the MIT License.
