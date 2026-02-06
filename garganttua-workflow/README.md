# Garganttua Workflow

## Description

The **garganttua-workflow** module provides a high-level orchestration DSL for composing multi-stage pipelines that generate and execute Garganttua Script code. It bridges the gap between the fluent Java builder API and the script execution engine, enabling modular, composable workflow definitions with automatic variable management, error handling, and runtime flexibility.

**Key Features:**
- **Fluent Hierarchical Builder** - Intuitive DSL with `up()` navigation for parent-child relationships
- **Dual Script Modes** - Include (runtime file loading) or Inline (embedded content) with auto-detection
- **Input/Output Mapping** - Named and positional variable references with expression support
- **Exception Handling** - Immediate (`!`), downstream (`*`), and stage-level catch clauses
- **Code Actions** - Handle exit codes with CONTINUE, ABORT, SKIP_STAGE, RETRY
- **Runtime Stage Filtering** - Execute partial workflows without rebuilding (startFrom, stopAfter, skipStages)
- **Preset Variables** - Global workflow-level variables injected into all scripts
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

<!-- AUTO-GENERATED-END -->

## Usage

### Building a Workflow

Workflows are constructed using a hierarchical fluent builder with `up()` navigation:

```java
import com.garganttua.core.workflow.dsl.WorkflowBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.WorkflowResult;
import com.garganttua.core.workflow.WorkflowInput;

IWorkflow workflow = WorkflowBuilder.create()
    .provide(injectionContextBuilder)
    .provide(expressionContextBuilder)
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
        .script(Path.of("scripts/validate.gs"))
            .name("validator")
            .input("data", "@rawData")
            .input("strict", "true")
            .output("validated", "validatedData")
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

### Executing a Workflow

```java
// Simple execution
WorkflowResult result = workflow.execute();

// With input payload
WorkflowResult result = workflow.execute(WorkflowInput.of(myPayload));

// With payload and parameters
WorkflowResult result = workflow.execute(
    WorkflowInput.of(myPayload, Map.of("env", "production"))
);

// Check results
if (result.isSuccess()) {
    Object output = result.getVariable("result", Object.class);
    Map<String, Object> stageOutputs = result.stageOutputs();
}
```

### Partial Execution with Options

Execute specific stages without rebuilding the workflow:

```java
import com.garganttua.core.workflow.WorkflowExecutionOptions;

WorkflowResult result = workflow.execute(
    WorkflowInput.of(payload),
    WorkflowExecutionOptions.builder()
        .startFrom("validate")
        .stopAfter("transform")
        .skipStage("cache")
        .build()
);
```

### Inline vs Include Modes

**Include mode** (default for file-based scripts) loads, compiles, and executes scripts at runtime:

```
_fetch_api_fetcher_ref <- include("/path/to/fetch.gs")
_fetch_api_fetcher_code <- execute_script(@_fetch_api_fetcher_ref, @url, @timeout)
rawData <- script_variable(@_fetch_api_fetcher_ref, "apiResponse")
```

**Inline mode** embeds script content directly, replacing positional variables (`@0`, `@1`) with named variables:

```
url <- @apiUrl
timeout <- @requestTimeout
apiResponse <- "fetched data from " + @url
httpStatus <- 200
rawData <- @apiResponse
```

Force all scripts to inline with `.inlineAll()` on the workflow builder.

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

## Context Setup

The workflow module requires both an injection context and an expression context:

```java
IInjectionContextBuilder injectionContextBuilder = InjectionContext.builder()
    .autoDetect(true)
    .withPackage("com.garganttua.core.runtime");

IExpressionContextBuilder expressionContextBuilder = ExpressionContextBuilder.builder()
    .withPackage("com.garganttua")
    .autoDetect(true)
    .provide(injectionContextBuilder);

injectionContextBuilder.build().onInit().onStart();
expressionContextBuilder.build();
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
| `WorkflowStageBuilder` | Builder for workflow stages |
| `WorkflowScriptBuilder` | Builder for scripts within stages |
| `ScriptGenerator` | Converts builder definitions into script source code |
| `ScriptHeaderParser` | Parses `#@workflow ... #@end` metadata blocks |
| `Workflow` | Executes pre-generated scripts using ScriptContext |

## Integration with Other Modules

### garganttua-script
- Workflows generate Garganttua Script code for execution
- Uses `ScriptContext` for script loading, compilation, and execution
- Leverages `include()`, `execute_script()`, `script_variable()` functions

### garganttua-expression
- Expressions in input mappings, catch clauses, and code actions
- Auto-detection of expression functions via `@Expression` annotations

### garganttua-injection
- Required dependency for the expression and runtime context initialization
- Bean resolution for workflow components

## License

This module is distributed under the MIT License.
