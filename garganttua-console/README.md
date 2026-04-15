# Garganttua Console

Interactive REPL console for Garganttua Script.

## Description

The `garganttua-console` module provides a terminal-based interactive console (REPL) for executing Garganttua Script expressions and statements. It is built on JLine for full terminal support including command history, tab completion, and multi-line input.

### Key Features

- **JLine Terminal**: Full terminal support with persistent command history (`~/.garganttua_script_history`)
- **Tab Completion**: Auto-complete for expression functions, session variables, and keywords
- **Multi-line Input**: Continuation with `..`, `\`, or implicit bracket matching
- **Session Variables**: Variables persist across statements within a session
- **ANSI Colors**: Auto-detection of color support with graceful fallback
- **Built-in Commands**: `help()`, `vars()`, `clear()`, `load()`, `man()`, `syntax()`, `exit()`

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-console</artifactId>
    <version>2.0.0-ALPHA02</version>
</dependency>
```

### Actual version
2.0.0-ALPHA02

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-script`
 - `com.garganttua.core:garganttua-expression`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-bootstrap`
 - `com.garganttua.core:garganttua-reflections`
 - `com.garganttua.core:garganttua-mutex`
 - `org.jline:jline:3.25.1`
 - `org.slf4j:slf4j-api:2.0.17:compile`
 - `ch.qos.logback:logback-classic:compile`
 - `com.garganttua.core:garganttua-runtime-reflection`
 - `com.garganttua.core:garganttua-aot-annotation-scanner`

<!-- AUTO-GENERATED-END -->

## Usage

### Running the Console

```bash
# Build the executable JAR
mvn clean package -pl garganttua-console

# Start the REPL
java -jar garganttua-console/target/garganttua-console-*-executable.jar
```

### Built-in Commands

| Command | Description |
|---------|-------------|
| `help()` | Display help with available commands and syntax |
| `vars()` | List all session variables with types and values |
| `clear()` | Clear all session variables |
| `load("file.gs")` | Load and execute a script file |
| `man()` | List all available expression functions (paginated) |
| `man("name")` | Show documentation for a specific function |
| `man(index)` | Show documentation by index |
| `syntax()` | Show script syntax reference (paginated) |
| `exit()` / `quit()` | Exit the console |

### Example Session

```
garganttua> x <- 42
x = 42
garganttua> y <- add(x, 8)
y = 50
garganttua> vars()
  x : Integer = 42
  y : Integer = 50
garganttua> exit()
```

## Architecture

```
garganttua-console/
└── src/main/java/com/garganttua/core/console/
    ├── ConsoleMain.java              # Fat JAR entry point
    ├── ScriptConsole.java            # Main REPL orchestrator
    ├── ConsoleExecutionContext.java   # ThreadLocal context holder
    ├── ConsoleFunctions.java         # Built-in console commands (@Expression)
    └── ScriptCompleter.java          # JLine tab completion
```

## License

This module is distributed under the MIT License.

---

**Version**: 2.0.0-ALPHA01
**Maintainer**: Garganttua Team
