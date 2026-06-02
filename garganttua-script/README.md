# Garganttua Script

## Description

The **garganttua-script** module provides a scripting language engine for composing and executing runtime steps using a concise, expression-based syntax. It bridges the gap between the `garganttua-expression` language and the `garganttua-runtime` orchestration framework, enabling dynamic workflow definition through script files or inline strings.

**Key Features:**
- **Variable Assignment** - Store expression results or expressions themselves in named variables
- **Exit Codes** - Associate exit codes with statement execution
- **Exception Handling** - Immediate catch clauses (`!`) and downstream/fallback catch clauses (`*`)
- **Conditional Pipes** - Route execution flow based on conditions (`|`)
- **Statement Groups** - Group statements in parentheses with unified error handling and function scope isolation
- **User-Defined Functions** - Define reusable functions with parameters and scoped variables
- **Conditional Execution** - `if(condition, thenBlock, elseBlock)` with lazy block evaluation
- **Expression Integration** - Full support for the `garganttua-expression` language syntax
- **Script Inclusion** - Load, execute, and extract variables from external scripts or JAR files
- **Retry & Backoff** - Retry expressions with fixed delay or exponential backoff
- **Synchronization** - Mutex-based synchronization for concurrent access control
- **Time Measurement** - Measure execution time and convert time units
- **Comments** - Single-line (`//`, `#`) and multi-line (`/* */`) comment support
- **Bootstrap-discoverable** - `ScriptsBuilder` (`@Bootstrap`) auto-detects `@ScriptDefinition` beans, exposes an `IScriptingEnvironment` as a DI bean, and surfaces stats in the startup summary
- **Thread-safe pre-compilation** - `IScriptingEnvironment.precompile(source, presetVars)` returns an `ICompiledScript` that can be `execute()`-d concurrently across threads without re-parsing or re-building the runtime

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-script</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-aot-commons`
 - `com.garganttua.core:garganttua-runtime`
 - `com.garganttua.core:garganttua-expression`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-runtime-reflection:provided`
 - `com.garganttua.core:garganttua-bootstrap`
 - `com.garganttua.core:garganttua-classloader`
 - `com.garganttua.core:garganttua-reflections:provided`
 - `org.antlr:antlr4-runtime:4.13.0`
 - `com.garganttua.core:garganttua-condition`
 - `com.garganttua.core:garganttua-mutex`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Script Syntax

### Statement with Result Assignment

```
varName <- expression -> exitCode
```

Evaluates the expression, stores the result in `varName`, and sets the exit code.

### Statement with Expression Assignment

```
varName = expression -> exitCode
```

Stores the expression itself (not its result) in `varName`.

### Exception Handling

```
! ExceptionType1, ExceptionType2 => result <- handler
! => catchAllHandler
```

Immediate catch clauses handle exceptions thrown during the current statement.

### Downstream Exception Handling (Fallback)

```
* ExceptionType => result <- handler
```

Downstream catch clauses run during the fallback phase of the execution chain.

### Conditional Pipes

```
| condition => pipeHandler
| => defaultHandler
```

Route execution based on conditions evaluated after the main statement.

### Statement Groups

```
(
  print("step 1") -> 10
  data <- "result" -> 11
) -> 99
  ! ExceptionType => catchHandler
  | condition => handler
```

Groups apply catch clauses and pipe clauses to all enclosed statements. Functions defined inside a group are scoped to that group and do not leak to the outer scope.

### User-Defined Functions

```
myFunc = (param1, param2) => (
    joined <- concatenate(@param1, @param2)
    result <- concatenate("[", @joined, "]")
)
output <- myFunc("hello", " world")
```

Functions are defined with the `=` operator, a parameter list in parentheses, the `=>` arrow, and a body block. Parameters are bound as local variables during invocation and restored after the call returns. The function returns the last evaluated value in its body.

Functions can call other functions:

```
bracket = (x) => (
    result <- concatenate("[", @x, "]")
)
doubleBracket = (x) => (
    inner <- bracket(@x)
    result <- bracket(@inner)
)
output <- doubleBracket("ok")   // produces "[[ok]]"
```

### Conditional Execution

```
if(condition, thenBlock)
if(condition, thenBlock, elseBlock)
```

The `if()` function evaluates its condition and lazily executes the matching block. When a statement block `(...)` is passed as an argument, it is only executed if the condition matches.

```
result <- if(equals(@status, "ok"), (
    data <- processData(@input)
    result <- validate(@data)
), (
    result <- handleError(@status)
))
```

### Complete Example

```
// Assign result of expression to variable with exit code 200
result <- :processOrder(order) -> 200
  ! java.io.IOException => error <- :handleIOError()
  ! => error <- :handleGenericError()
  * java.lang.Exception => fallback <- :recoverOrder()
  | :isSuccess(result) => :logSuccess(result)
  | => :logFailure(result)
```

### Comments

```
// Single-line comment
# Hash-style comment
/* Multi-line
   comment */
```

## Usage

### Core API

### IScript (mutable, single-thread)

The main interface for loading, compiling, and executing scripts in a
single-threaded fashion. Each `IScript` instance carries mutable state
(last-output, last-exception, last-variables) so do NOT share an instance
across concurrent calls.

```java
IScript script = ...;

// Load from string
script.load("result <- :process(input) -> 200");

// Load from file
script.load(new File("workflow.gs"));

// Load from stream
script.load(inputStream);

// Compile the loaded script
script.compile();

// Execute with arguments
int exitCode = script.execute(args);

// Retrieve variables after execution
Optional<String> result = script.getVariable("result", String.class);
```

### IScriptingEnvironment (Bootstrap-injected factory)

`ScriptsBuilder` produces an `IScriptingEnvironment` and exposes it as a DI
bean. It encapsulates the dependencies every script needs (expression
context, runtimes-builder factory, class-loader manager), so transient
consumers (Workflow, REPL, CLI, tests) don't have to wire those by hand.

```java
@Inject
private IScriptingEnvironment env;

// Spawn a fresh, mutable IScript per call:
IScript s = env.newScript();
s.load(source); s.compile(); s.execute(args);

// OR — pre-compile once and execute concurrently many times:
ICompiledScript hot = env.precompile(source, Map.of("apiUrl", "https://..."));
```

### ICompiledScript (immutable, thread-safe)

Returned by `IScriptingEnvironment.precompile`. Wraps an already-built
`IRuntime`; every `execute()` call produces its own `IRuntimeContext` and
returns a fresh, immutable `IScriptExecutionResult`. Safe to share across
threads.

```java
ICompiledScript hot = env.precompile(source, Map.of());
IScriptExecutionResult res = hot.execute("arg0", "arg1");
res.code();                 // exit code
res.variables();            // immutable variable snapshot
res.output();               // Optional<Object>
res.exception();            // Optional<Throwable>
res.hasAborted();           // boolean
```

### @ScriptDefinition (declarative scripts auto-discovered)

Annotate a class with `@ScriptDefinition(name = "...")` and implement
`IScriptDefinition.source()` — `ScriptsBuilder.doAutoDetectionWithDependency`
picks it up from the `IInjectionContext`, compiles it, and registers the
resulting `IScript` as a DI bean qualified by name. Surfaced in the
Bootstrap "Script Engine" summary section.

```java
@ScriptDefinition(name = "myHotPath")
public class MyHotPathScript implements IScriptDefinition {
    @Override
    public String source() {
        return "result <- :process(@0) -> 0";
    }
}
```

### Built-in Functions

### Script Inclusion & Execution

| Function | Description |
|:--|:--|
| `include(path)` | Loads a `.gs` script file or JAR plugin into the current context. Returns the script name. |
| `execute_script(name, args...)` | Executes a previously included script with positional arguments. Returns the exit code. Supports 0 to 10 arguments. |
| `script_variable(name, varName)` | Extracts a variable value from a previously executed child script. |

```
ref <- include("helper.gs")
code <- execute_script(@ref, "arg1", 42)
result <- script_variable(@ref, "outputVar")
```

### Retry & Backoff

| Function | Description |
|:--|:--|
| `retry(maxAttempts, delayMs, expr)` | Retries the expression with a fixed delay between attempts. |
| `retryWithBackoff(maxAttempts, initialDelayMs, maxDelayMs, expr)` | Retries with exponential backoff (delay doubles each attempt, capped at maxDelayMs). |

```
result <- retry(3, seconds(10), riskyOperation())
result <- retryWithBackoff(5, seconds(1), seconds(30), fetchData())
```

### Synchronization

| Function | Description |
|:--|:--|
| `synchronized(mutexName, mutex, mode, timeoutMs, expr)` | Executes expression under a mutex lock. Mode: `"acquire"` (wait with timeout) or `"tryAcquire"` (immediate). |
| `sync(mutexName, mutex, expr)` | Simplified version that waits indefinitely for the lock. |

```
result <- synchronized("my-lock", $mutex, "acquire", seconds(30), processOrder())
result <- sync("order-lock", $mutex, myExpression())
```

### Control Flow

| Function | Description |
|:--|:--|
| `if(condition, thenBlock)` | Executes `thenBlock` if condition is true. Returns null otherwise. |
| `if(condition, thenBlock, elseBlock)` | Executes `thenBlock` if condition is true, `elseBlock` otherwise. |

Statement blocks `(...)` passed as arguments are lazily evaluated — only the matching branch executes.

```
result <- if(equals(@mode, "prod"), (
    data <- fetchFromProd()
), (
    data <- fetchFromDev()
))
```

### Time Functions

| Function | Description |
|:--|:--|
| `time(expr)` | Measures execution time of an expression. Returns elapsed milliseconds. |
| `timeWithResult(expr)` | Measures execution time and returns `[elapsedMs, result]` array. |
| `milliseconds(n)` | Identity: returns n (for readability). |
| `seconds(n)` | Converts seconds to milliseconds. |
| `minutes(n)` | Converts minutes to milliseconds. |
| `hours(n)` | Converts hours to milliseconds. |

```
elapsed <- time(print("hello"))
delay <- seconds(10)     // 10000
timeout <- minutes(5)    // 300000
```

### Command Line Usage

```bash
# Execute a script file
java -jar garganttua-script-*-executable.jar script.gs [args...]

# Show syntax reference
java -jar garganttua-script-*-executable.jar --syntax

# List or search function documentation
java -jar garganttua-script-*-executable.jar --man [function]

# Show version
java -jar garganttua-script-*-executable.jar --version

# Show help
java -jar garganttua-script-*-executable.jar --help
```

Scripts support shebang lines for direct execution:

```bash
#!/usr/bin/env garganttua-script
result <- print("Hello from script")
```

### Architecture

### Module Structure

```
garganttua-script/
├── src/main/
│   ├── java/com/garganttua/core/script/
│   │   ├── context/           # ScriptContext, ScriptExecutionContext, CompiledScript, CompiledScriptExecutionResult
│   │   ├── dsl/               # ScriptsBuilder (@Bootstrap), ScriptsBuilderFactory (SPI)
│   │   ├── functions/         # Built-in functions (include, retry, sync, time, control flow, ...)
│   │   ├── nodes/             # IScriptNode, StatementNode, StatementGroupNode, ScriptFunction, FunctionDefNode
│   │   ├── ScriptingEnvironment.java   # IScriptingEnvironment impl + IBootstrapSummaryContributor
│   │   └── Main.java          # CLI entry point
│   ├── resources/META-INF/
│   │   └── services/          # com.garganttua.core.dsl.IBootstrapBuilderFactory → ScriptsBuilderFactory
│   └── resources/antlr4/
│       └── Script.g4          # ANTLR4 grammar definition
└── src/test/                  # Test suite
```

> **Note:** The interactive REPL console has been extracted to the separate [`garganttua-console`](../garganttua-console/README.md) module.

### Integration with Other Modules

### garganttua-expression
- Scripts use the expression language for all value expressions
- Full support for function calls, method calls, constructors, and literals

### garganttua-runtime
- Scripts compile into runtime steps via `ScriptRuntimeStep`
- Exception handling maps to runtime catch/fallback mechanisms
- Variables are shared through the runtime context

### garganttua-workflow
- Workflow module generates script code from a fluent builder DSL
- `WorkflowsBuilder` depends on `IScriptsBuilder` so its workflows always
  receive a fully-wired `IScriptingEnvironment` — Workflow no longer pulls
  directly on Expression and Runtime layers
- Uses `include()`, `execute_script()`, `script_variable()` for inter-script communication
- Supports inline (embedded) and include (file-based) script modes
- `WorkflowBuilder.precompile(true)` delegates to
  `IScriptingEnvironment.precompile(...)` for thread-safe hot-path execution

### garganttua-classloader
- `ScriptContext` takes an `IClassLoaderManager` (not an `IBootstrap`) for
  JAR hot-loading via `include("foo.jar")`. The script module no longer
  owns `JarManifestReader` — it lives in `garganttua-classloader`.
- Bootstrap auto-wires itself as a rebuild hook on the manager, so
  `include("plugin.jar")` triggers `Bootstrap.withPackage + rebuild`
  transparently.

## Tips and best practices

- **Prefer `precompile()` for hot paths** — REST request handlers, batch
  loops, anything executed > 100×. Saves an ANTLR parse + a fresh
  `IRuntime` build on every call. Thread-safe out of the box.
- **Use `@ScriptDefinition` for static scripts** — anything you ship as
  part of the framework binary. They get auto-detected, auto-compiled,
  exposed as DI beans and listed in the startup summary.
- **Reach for `IScript` only when you need mutable state** — variable
  setters, last-output getters etc. don't exist on `ICompiledScript` by
  design; if you find yourself wanting them, you probably want a fresh
  `IScript` per call anyway.
- **Don't share an `IScript` instance across threads** — last-* fields are
  per-instance mutable state. Use `IScriptingEnvironment.newScript()` to
  spawn one per call, or `precompile()` for the thread-safe path.
- **Plugin JARs auto-discoverable** — declare `Garganttua-Packages` in your
  plugin JAR's `MANIFEST.MF` and `include("plugin.jar")` will trigger
  Bootstrap.rebuild automatically (via the classloader hook chain).

## License
This module is distributed under the MIT License.
