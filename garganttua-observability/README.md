# Garganttua Observability

## Description

Garganttua Observability provides **generic observer-pattern primitives** for instrumenting Garganttua components at runtime. It defines a sealed event hierarchy (`StartEvent`, `EndEvent`, `ErrorEvent`), a single-callback observer interface, and a thread-safe registry with built-in exception isolation. The module also ships a script-side `:observe(...)` expression function, so workflows and scripts can emit observability events from the generated script without leaking observer concerns into user code.

The module deliberately offers **only the primitives** — aggregators, metrics backends, and export adapters are left to consumers. Existing in-module patterns (`MapperMetrics`, `StatsObserver` in `garganttua-api`) remain valid examples to copy.

**Key Features:**
- **Sealed Event Hierarchy** - `StartEvent`, `EndEvent`, `ErrorEvent` with correlation IDs, timestamps, and source identifiers
- **Single-callback Observer** - `IObserver.onEvent(E)` with pattern-matching dispatch on the consumer side
- **Thread-safe Registry** - `ObservableRegistry` backed by `CopyOnWriteArrayList`, exception-isolated, with `hasObservers()` short-circuit
- **Script-side Instrumentation** - `:observe("start"|"end"|"error", source[, code])` expression function for scripts and workflows
- **ThreadLocal Context** - `ObservableContextHolder` for engines that fire events from generated scripts
- **Zero-cost When Disabled** - Empty registry path is non-allocating

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
 - `com.garganttua.core:garganttua-runtime-reflection:test`
 - `ch.qos.logback:logback-classic:test`

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

## Tips and best practices

- Keep observer logic fast and side-effect free — observers run on the caller thread.
- Use `hasObservers()` before constructing expensive event payloads.
- Correlate by `executionId` rather than parsing `source` strings.
- For aggregation/metrics, build a domain-specific observer (cf. `MapperMetrics`) — the registry itself is just plumbing.

## License
This module is distributed under the MIT License.
