# Garganttua Script

## Description

The **garganttua-script** module provides a scripting language engine for composing and executing runtime steps using a concise, expression-based syntax. It bridges the gap between the `garganttua-expression` language and the `garganttua-runtime` orchestration framework, enabling dynamic workflow definition through script files or inline strings.

**Key Features:**
- **Variable Assignment** - Store expression results or expressions themselves in named variables
- **Exit Codes** - Associate exit codes with statement execution
- **Exception Handling** - Immediate catch clauses (`!`) and downstream/fallback catch clauses (`*`)
- **Conditional Pipes** - Route execution flow based on conditions (`|`)
- **Expression Integration** - Full support for the `garganttua-expression` language syntax
- **Script Inclusion** - Load and execute external scripts or JAR files via `include()` and `call()`
- **Comments** - Single-line (`//`, `#`) and multi-line (`/* */`) comment support

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-script</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-runtime`
 - `com.garganttua.core:garganttua-expression`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-reflections`
 - `com.garganttua.core:garganttua-bootstrap`
 - `org.antlr:antlr4-runtime:4.13.0`
 - `org.slf4j:slf4j-api:2.0.17:compile`
 - `ch.qos.logback:logback-classic:compile`
 - `com.garganttua.core:garganttua-condition`

<!-- AUTO-GENERATED-END -->

## Script Syntax

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

## Core API

### IScript

The main interface for loading, compiling, and executing scripts:

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

## Built-in Functions

### include(path)

Loads external JAR files or `.gs` script files into the current script context.

### call(name)

Executes a previously included script by name.

## Architecture

### Module Structure

```
garganttua-script/
├── src/main/
│   ├── java/com/garganttua/core/script/
│   │   ├── context/           # ScriptContext, ScriptExecutionContext, ScriptRuntimeStep
│   │   ├── functions/         # Built-in functions (include, call)
│   │   └── nodes/             # IScriptNode, StatementNode, CatchClause, PipeClause
│   └── resources/antlr4/
│       └── Script.g4          # ANTLR4 grammar definition
└── src/test/                  # Test suite
```

## Integration with Other Modules

### garganttua-expression
- Scripts use the expression language for all value expressions
- Full support for function calls, method calls, constructors, and literals

### garganttua-runtime
- Scripts compile into runtime steps via `ScriptRuntimeStep`
- Exception handling maps to runtime catch/fallback mechanisms
- Variables are shared through the runtime context

## License

This module is distributed under the MIT License.
