# Garganttua Workflow

## Description

The **garganttua-workflow** module provides a high-level orchestration DSL for composing multi-stage pipelines that generate and execute Garganttua Script code. It bridges the gap between the fluent Java builder API and the script execution engine, enabling modular, composable workflow definitions with automatic variable management, error handling, and runtime flexibility.

A **workflow** is a pipeline composed of **stages** executed sequentially. Each stage contains one or more **scripts** — either `.gs` files loaded at runtime (include mode) or embedded content (inline mode). The workflow does not execute Java directly — it **generates Garganttua Script code** from the builder definition, then executes that script. The `ScriptGenerator` is the component that translates stages and scripts into `.gs` code.

**Key Features:**
- **Fluent Hierarchical Builder** - Intuitive DSL with `up()` navigation for parent-child relationships
- **Dual Script Modes** - Include (runtime file loading) or Inline (embedded content) with auto-detection
- **Conditional Execution** - `when()` on stages and scripts, using `if()` blocks with lazy evaluation
- **Input/Output Mapping** - Named and positional variable references with expression support
- **Exception Handling** - Immediate (`!`), downstream (`*`), and stage-level catch clauses
- **Code Actions** - Handle exit codes with CONTINUE, ABORT, SKIP_STAGE, RETRY
- **Runtime Stage Filtering** - Execute partial workflows without rebuilding (startFrom, stopAfter, skipStages)
- **Preset Variables** - Global workflow-level variables injected into all scripts
- **Function Scope Isolation** - Inline scripts are wrapped in `(...)` groups to prevent name collisions between stages
- **Script Headers** - Metadata format (`#@workflow ... #@end`) for documentation and introspection
- **ASCII Visualization** - `describeWorkflow()` renders a workflow cartography

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-workflow</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`
 - `com.garganttua.core:garganttua-script`
 - `com.garganttua.core:garganttua-dsl`
 - `com.garganttua.core:garganttua-expression`
 - `com.garganttua.core:garganttua-injection`
 - `com.garganttua.core:garganttua-reflections:test`
 - `ch.qos.logback:logback-classic:test`

<!-- AUTO-GENERATED-END -->

## Context Setup

The workflow module requires both an injection context and an expression context:

```java
IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
    .provide(reflectionBuilder)
    .autoDetect(true)
    .withPackage("com.garganttua.core.runtime");

IExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder()
    .withPackage("com.garganttua")
    .autoDetect(true)
    .provide(injectionContextBuilder);

injectionContextBuilder.build().onInit().onStart();
expressionContextBuilder.build();
```

## Usage

### Building a Workflow

Workflows are constructed using a hierarchical fluent builder with `up()` navigation:

```java
IWorkflow workflow = WorkflowBuilder.create()
    .provide(injectionContextBuilder)     // Required: injection context
    .provide(expressionContextBuilder)    // Required: expression context
    .name("order-pipeline")
    .variable("apiUrl", "https://api.example.com")
    .variable("timeout", 30000)
    .stage("fetch")
        .script(Path.of("scripts/fetch-data.gs"))
            .name("api-fetcher")
            .input("url", "@apiUrl")
            .input("timeout", "@timeout")
            .output("rawData", "apiResponse")
            .output("httpCode", "httpStatus")
            .catch_("handleError(@exception)")
            .onCode(1, CodeAction.RETRY)
            .up()
        .up()
    .stage("validate")
        .when("equals(@env, \"prod\")")
        .script(Path.of("scripts/validate.gs"))
            .name("validator")
            .input("data", "@rawData")
            .input("strict", "true")
            .output("validated", "validatedData")
            .onCode(1, CodeAction.ABORT)
            .up()
        .up()
    .stage("transform")
        .script(Path.of("scripts/transform.gs"))
            .name("transformer")
            .input("inputData", "@validated")
            .output("result", "transformedData")
            .inline()  // Force inline mode for this script
            .up()
        .up()
    .build();
```

**Key points:**
- `.provide()` supplies the required contexts (injection + expression). Without them, the build fails.
- `.variable(name, value)` declares variables accessible to all scripts via `@name`.
- `.up()` navigates back to the parent builder (script -> stage -> workflow). This is the Hierarchical Builder pattern.
- `.name()` on a script is used to name the generated internal variables (`_stageName_scriptName_code`, etc.).

### Script Modes

#### Include mode (`.gs` files)

Default mode for file-based scripts. The script is loaded and executed in an **isolated child scope**:

```java
.stage("fetch")
    .script(Path.of("scripts/fetch-data.gs"))
        .name("api-fetcher")
        .input("url", "@apiUrl")           // @url available in the script
        .input("timeout", "@requestTimeout")
        .output("rawData", "apiResponse")  // apiResponse from script -> rawData in workflow
        .up()
    .up()
```

**Generated code:**
```
url <- @apiUrl
timeout <- @requestTimeout
_fetch_api_fetcher_ref <- include("scripts/fetch-data.gs")
_fetch_api_fetcher_code <- execute_script(@_fetch_api_fetcher_ref, @url, @timeout)
rawData <- script_variable(@_fetch_api_fetcher_ref, "apiResponse")
```

The script receives its inputs as positional arguments (`@0` = `url`, `@1` = `timeout`). Outputs are extracted via `script_variable()`.

#### Inline mode (embedded content)

For string scripts or when `.inline()` is explicitly called. The content is injected directly into the generated script, wrapped in a `(...)` group for isolation:

```java
.stage("transform")
    .script("processed <- concatenate(\"[\", @inputData, \"]\")")
        .name("transformer")
        .input("inputData", "@rawData")
        .output("result", "processed")
        .up()
    .up()
```

**Generated code:**
```
inputData <- @rawData
(
    processed <- concatenate("[", @inputData, "]")
    result <- @processed
)
```

The `(...)` group ensures that functions defined in an inline script do not pollute other stages. Positional variables (`@0`, `@1`) in the content are automatically replaced with input names.

#### Forcing inline mode

```java
// On a specific script
.script(Path.of("scripts/small.gs"))
    .inline()    // Embeds file content instead of using include()
    .up()

// On the entire workflow
WorkflowBuilder.create()
    .inlineAll()   // All scripts are inlined
    ...
```

### Input and Output Mappings

**Inputs** inject workflow variables into the script:
```java
.input("scriptVar", "@workflowVar")    // The script sees @scriptVar
.input("config", "\"hardcoded\"")      // Literal value
.input("flag", "true")                 // Boolean
```

**Outputs** extract variables from the script into the workflow:
```java
.output("workflowVar", "scriptVar")    // scriptVar from script -> workflowVar in workflow
```

For include mode, outputs use `script_variable()`. For inline mode, they become an assignment `workflowVar <- @scriptVar` inside the group.

## Conditional Execution with `when()`

### Condition on a script

```java
.stage("deploy")
    .script("deployed <- \"yes\"")
        .name("deployer")
        .when("equals(@env, \"prod\")")
        .output("deployResult", "deployed")
        .up()
    .up()
```

**Generated code:**
```
_deploy_deployer_cond <- equals(@env, "prod")
if(@_deploy_deployer_cond, (
    deployed <- "yes"
    deployResult <- @deployed
), 0)
```

The script is executed **only if the condition is true**. The `0` is the else branch (no-op). The `if()` function uses `(...)` blocks that are lazily evaluated — the content is only executed if the branch is taken.

### Condition on an entire stage

```java
.stage("deploy")
    .when("equals(@env, \"prod\")")
    .script("deployed <- deploy(@artifact)")
        .name("deployer")
        .output("deployResult", "deployed")
        .up()
    .script("notify <- sendSlack(@deployResult)")
        .name("notifier")
        .up()
    .up()
```

The stage condition applies to **all scripts** in the stage. The generator emits a condition variable at stage level:

```
_deploy_cond <- equals(@env, "prod")
```

Each script then uses this condition. If a script **also** has its own condition, both are combined with `and()`:

```java
.stage("deploy")
    .when("equals(@env, \"prod\")")          // Stage condition
    .script("deployed <- deploy(@artifact)")
        .name("deployer")
        .when("equals(@region, \"eu\")")     // Script condition
        .up()
    .up()
```

**Generated code:**
```
_deploy_cond <- equals(@env, "prod")
_deploy_deployer_cond <- and(@_deploy_cond, equals(@region, "eu"))
if(@_deploy_deployer_cond, (
    deployed <- deploy(@artifact)
), 0)
```

### Condition + include mode (files)

For file-based scripts with a condition, `include()` is **always executed** (it is a lightweight operation). Only `execute_script()` is conditional:

```
_deploy_deployer_cond <- equals(@env, "prod")
_deploy_deployer_ref <- include("scripts/deploy.gs")
_deploy_deployer_code <- if(@_deploy_deployer_cond, execute_script(@_deploy_deployer_ref), 0)
if(@_deploy_deployer_cond, (
    deployResult <- script_variable(@_deploy_deployer_ref, "deployed")
), 0)
```

### Available expressions for `when()`

All expressions from the `garganttua-expression` language are available:

```java
// Equality
.when("equals(@env, \"prod\")")

// Direct boolean (variable containing true/false)
.when("@shouldDeploy")

// Logical combinations
.when("and(equals(@env, \"prod\"), @featureEnabled)")
.when("or(equals(@mode, \"full\"), equals(@mode, \"partial\"))")
.when("not(equals(@status, \"disabled\"))")

// Numeric comparison
.when("greater(@retryCount, 0)")
.when("lowerOrEquals(@errorRate, 5)")

// Null / empty checks
.when("not(is_null(@inputData))")
.when("not(is_empty(@inputData))")
```

## Error Handling

### Catch on a script

```java
.script(Path.of("scripts/risky.gs"))
    .name("risky")
    .catch_("log(concatenate(\"Error: \", @exception))")
    .catchDownstream("cleanup()")
    .up()
```

- `.catch_(expr)` : `!` clause — catches exceptions thrown directly by this script
- `.catchDownstream(expr)` : `*` clause — catches exceptions propagated from nested calls (fallback)

### Catch on a stage

```java
.stage("processing")
    .catch_("log(\"Stage error\")")
    .catchDownstream("rollback()")
    .script(...)
    .up()
```

When a stage has catch clauses, its content is grouped and the clauses are applied to the entire group.

### Code Actions

Handle specific exit codes from scripts:

```java
.script(Path.of("scripts/validate.gs"))
    .name("validator")
    .onCode(1, CodeAction.ABORT)        // Code 1 -> abort workflow
    .onCode(2, CodeAction.SKIP_STAGE)   // Code 2 -> skip to next stage
    .onCode(3, CodeAction.RETRY)        // Code 3 -> retry the script
    .up()
```

`CodeAction` values:
- `CONTINUE` — continue normally (default, no code generated)
- `ABORT` — calls `abort()` to stop the workflow
- `SKIP_STAGE` — calls `skip()` to jump to the next stage
- `RETRY` — calls `retry(3, ...)` to retry the script

For conditional scripts, code actions are wrapped in `if()`:
```
if(and(@_stage_script_cond, equals(@_stage_script_code, 1)), (abort()), 0)
```

### Stage Wrapping

Wraps the entire stage content in an expression, using `@0` as a placeholder:

```java
.stage("risky-stage")
    .wrap("retry(3, seconds(5), @0)")   // Retry 3 times with 5s delay
    .script(...)
    .up()
```

## Execution

### Simple execution

```java
WorkflowResult result = workflow.execute();
```

### With an input payload

```java
WorkflowResult result = workflow.execute(WorkflowInput.of("my-data"));
// The payload is accessible via @0 in the script
```

### With named parameters

```java
WorkflowResult result = workflow.execute(
    WorkflowInput.of("payload", Map.of("env", "prod", "region", "eu"))
);
```

### Partial execution (stage filtering)

Execute specific stages without rebuilding the workflow:

```java
WorkflowResult result = workflow.execute(
    WorkflowInput.of(data),
    WorkflowExecutionOptions.builder()
        .startFrom("transform")           // Start at this stage
        .stopAfter("statistics")          // Stop after this stage
        .skipStage("cache")               // Skip this stage
        .build()
);
```

Stage filtering **regenerates** the script with only the selected stages.

### Handling the result

```java
WorkflowResult result = workflow.execute();

// Success?
result.isSuccess();        // true if code == 0 and no exception
result.code();             // Exit code (Integer)
result.hasAborted();       // true if an exception stopped execution

// Variables
result.variables();        // Map<String, Object> of all variables
result.getVariable("result", IClass.getClass(String.class));  // Type-safe

// Stage outputs
result.stageOutputs();     // Map<String, Object> of mapped outputs
result.getStageOutput("transform", "data", IClass.getClass(String.class));

// Errors
result.exception();        // Optional<Throwable>
result.exceptionMessage(); // Optional<String> root cause message

// Timing
result.duration();         // Duration between start and stop
result.start();            // Start Instant
result.stop();             // Stop Instant
```

## Introspection

```java
// Generated script (useful for debugging)
String script = workflow.getGeneratedScript();

// Textual workflow description (ASCII cartography)
String description = workflow.describeWorkflow();

// Structured descriptor
WorkflowDescriptor descriptor = workflow.getDescriptor();
descriptor.stages();       // List of StageDescriptor
descriptor.presetVariables();
```

## Script Headers

Scripts can include metadata headers for documentation and introspection:

```
#@workflow
#  description: Validates input data and ensures data quality.
#  inputs:
#    - name: data position: 0 type: Object
#    - name: strict position: 1 type: Boolean
#  outputs:
#    - name: validated variable: validatedData type: Object
#    - name: status variable: validationStatus type: String
#  returnCodes:
#    0: SUCCESS
#    1: VALIDATION_ERROR
#    2: NULL_DATA_ERROR
#@end

# Script code below
validationStatus <- "pending"
validatedData <- @0
validationStatus <- "completed"
```

Parse headers programmatically with `ScriptHeaderParser.parse(scriptContent)`.

## Complete Example

```java
IWorkflow workflow = WorkflowBuilder.create()
    .provide(injectionContextBuilder)
    .provide(expressionContextBuilder)
    .name("data-pipeline")
    .variable("apiUrl", "https://api.example.com/data")
    .variable("timeout", 30000)
    .variable("env", "prod")

    // Stage 1: Fetch data (always executed)
    .stage("fetch")
        .script(Path.of("scripts/fetch-data.gs"))
            .name("api-fetcher")
            .input("url", "@apiUrl")
            .input("timeout", "@timeout")
            .output("rawData", "apiResponse")
            .output("httpCode", "httpStatus")
            .catch_("log(concatenate(\"Fetch error: \", @exception))")
            .onCode(1, CodeAction.RETRY)
            .up()
        .up()

    // Stage 2: Validation (prod only)
    .stage("validation")
        .when("equals(@env, \"prod\")")
        .script(Path.of("scripts/validate.gs"))
            .name("validator")
            .input("data", "@rawData")
            .input("strict", "true")
            .output("validated", "validatedData")
            .onCode(1, CodeAction.ABORT)
            .up()
        .up()

    // Stage 3: Transform (always, with retry)
    .stage("transform")
        .wrap("retry(3, seconds(5), @0)")
        .script(Path.of("scripts/transform.gs"))
            .name("transformer")
            .input("inputData", "@validated")
            .output("result", "transformedData")
            .up()
        .up()

    // Stage 4: Notification (prod only + feature flag)
    .stage("notify")
        .when("equals(@env, \"prod\")")
        .script("notified <- sendNotification(@result)")
            .name("notifier")
            .when("@notificationsEnabled")   // Combined with stage condition
            .up()
        .up()

    .build();

// Execute
WorkflowResult result = workflow.execute(WorkflowInput.of(inputPayload));

if (result.isSuccess()) {
    Optional<String> output = result.getStageOutput(
        "transform", "result", IClass.getClass(String.class));
    System.out.println("Result: " + output.orElse("N/A"));
} else {
    result.exceptionMessage().ifPresent(msg ->
        System.err.println("Error: " + msg));
}
```

## Architecture

### Module Structure

```
garganttua-workflow/
├── src/main/java/com/garganttua/core/workflow/
│   ├── dsl/                  # Fluent builders
│   │   ├── WorkflowBuilder.java
│   │   ├── WorkflowStageBuilder.java
│   │   └── WorkflowScriptBuilder.java
│   ├── generator/            # Script generation
│   │   ├── ScriptGenerator.java
│   │   └── ScriptTemplate.java
│   ├── header/               # Script header parsing
│   │   ├── ScriptHeaderParser.java
│   │   └── ScriptHeader.java
│   └── Workflow.java         # Execution engine
└── src/test/
    ├── java/                 # Test suite
    └── resources/scripts/    # Test scripts (.gs)
```

### Key Classes

| Class | Purpose |
|:--|:--|
| `WorkflowBuilder` | Top-level fluent builder for workflow definition |
| `WorkflowStageBuilder` | Builder for stages — supports `when()`, `wrap()`, `catch_()` |
| `WorkflowScriptBuilder` | Builder for scripts — supports `when()`, `inline()`, `input()`, `output()`, `catch_()`, `onCode()` |
| `ScriptGenerator` | Converts builder definitions into script source code. Uses `if()` blocks for conditional execution and `(...)` groups for inline script isolation |
| `ScriptHeaderParser` | Parses `#@workflow ... #@end` metadata blocks |
| `Workflow` | Executes pre-generated scripts using ScriptContext |
| `WorkflowResult` | Execution result with variables, outputs, timing, and error info |
| `WorkflowInput` | Execution input with payload and named parameters |
| `WorkflowExecutionOptions` | Stage filtering (startFrom, stopAfter, skipStages) |
| `CodeAction` | Exit code handling: CONTINUE, ABORT, SKIP_STAGE, RETRY |

## Script Generation Rules

| Situation | Generated code |
|:--|:--|
| Unconditional inline script | `(script content)` (isolated group) |
| Conditional inline script | `if(@cond, (content), 0)` |
| Unconditional file script | `include()` + `execute_script()` + `script_variable()` |
| Conditional file script | `include()` + `if(@cond, execute_script(...), 0)` + `if(@cond, (outputs), 0)` |
| Stage condition + script condition | `and(@stageCond, scriptCond)` |
| Code action (unconditional) | Pipe clause: `\| equals(@code, N) => action()` |
| Code action (conditional) | `if(and(@cond, equals(@code, N)), (action()), 0)` |
| Catch (unconditional) | Clause: `! => handler` |
| Stage with wrap | `_stage_result <- wrapExpr(@0 replaced by (content))` |

## Integration with Other Modules

### garganttua-script
- Workflows generate Garganttua Script code for execution
- Uses `ScriptContext` for script loading, compilation, and execution
- Leverages `include()`, `execute_script()`, `script_variable()` functions
- Inline scripts are wrapped in `(...)` groups for function scope isolation

### garganttua-expression
- Expressions in `when()` conditions, input mappings, catch clauses, and code actions
- Auto-detection of expression functions via `@Expression` annotations
- `if()` function with lazy `StatementBlock` evaluation for conditional execution

### garganttua-injection
- Required dependency for the expression and runtime context initialization
- Bean resolution for workflow components

## License

This module is distributed under the MIT License.
