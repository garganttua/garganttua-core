#!/bin/garganttua-script.jar
################################################################################
# Garganttua Script Runtime
#
# This script defines a complete runtime execution flow composed of stages,
# steps and expressions. It is designed to be deterministic, composable and
# explicit regarding error handling and propagation.
#
# ------------------------------------------------------------------------------
# EXECUTION MODEL
# ------------------------------------------------------------------------------
# - A runtime is composed of one or more stages
# - A stage is composed of ordered steps
# - A step executes an operation expressed as a Garganttua expression
#
# PARALLELISM MODEL:
# - Parallelism operates ONLY at the step level via parallel() blocks
# - Moving to the next step implies an implicit join() on all parallel operations
# - This prevents side effects and ensures deterministic execution
# - Example:
#     step(step-1, parallel(op1, op2, op3))  # Parallel execution
#     step(step-2, op4)                      # Implicit join: waits for step-1
#
# - Use explicit join() when you need to synchronize within a single step:
#     step(step-1,
#         join(parallel(op1, op2)),  # Explicit join
#         op3                        # Executes after parallel completion
#     )
#
# EXCEPTION HANDLING:
# If an exception is thrown during a step execution:
# - Catch clauses are resolved from the closest scope to the farthest one
#   (step → previous steps → stage → runtime)
# - Only ONE catch clause is executed, even if multiple match the exception type
# - This avoids generic catch clauses overriding more specific ones
#
# ------------------------------------------------------------------------------
# CATCH CLAUSE RESOLUTION
# ------------------------------------------------------------------------------
# catch(type, operation)
# catch(type, operation, propagation)
#
# - type         : the exception class to catch (Any matches all)
# - operation    : expression executed when the exception is caught
# - propagation  : optional, defines how far the exception propagates
#                  values: step | stage | runtime
#
# Default propagation is step.
#
# ------------------------------------------------------------------------------
# EXPRESSIONS & OPERATORS
# ------------------------------------------------------------------------------
# All operations are expressions provided by garganttua-expression.
#
# Operators:
# - "->" defines the return code of an operation
#        if omitted, the default code is preserved
#
# - "=" assigns the result of an expression to a variable
#
# - "@" injects a variable into an expression parameter
#
# - "( )" groups multiple operations into a compound operation
#        syntax: (op1, op2, op3)
#        operations are executed sequentially within the group
#
# - "parallel( )" executes operations in parallel
#        syntax: parallel(op1, op2, op3) -> code
#        operations are executed concurrently
#
# - "join( )" waits for all parallel operations to complete
#        syntax: join(parallel(op1, op2))
#        blocks until all parallel operations finish
#
# ------------------------------------------------------------------------------
# PREDEFINED VARIABLES
# ------------------------------------------------------------------------------
# - input              : runtime input
# - output             : runtime output
# - code               : current return code
# - context            : execution context (variables, exception stack, metadata)
# - exception          : last thrown exception
# - exception-message  : message of the last thrown exception
# context cannot be reasigned : context = 12 makes the script uncompilable
#
# ------------------------------------------------------------------------------
# NAMING RULES
# ------------------------------------------------------------------------------
# Variable names must follow the kebab-case convention:
#   example: nullable-variable, default-category
# Step and stage names must follow the kebab-case convention:
#   example: map-to-dto-1
#
# ------------------------------------------------------------------------------
# RUNTIME METADATA
# ------------------------------------------------------------------------------
# Runtime extension : .gs (garganttua-script)
# Runtime name      : derived from the script file name
#
################################################################################

# ------------------------------------------------------------------------------
# DEPENDENCIES
# ------------------------------------------------------------------------------
# Includes can be used when executing scripts through garganttua-scripts
include(./my-package.jar) # Custom package that may includes custom expressions
include(garganttua-rest-client.jar)   # Standard Garganttua package defining
#   some standard expressions
include(./another-runtime-script.gs) # Import another runtime script ran before
#   this one, the included script and current contexts are merged

# ------------------------------------------------------------------------------
# GLOBAL VARIABLES
# ------------------------------------------------------------------------------
threshold  = int("100")
multiplier = double("1.5")
output     = "OK"
code       = 200
counter    = 0
list       = string[1,2,3]

# ------------------------------------------------------------------------------
# STAGE: PROTOCOL
# ------------------------------------------------------------------------------

build(
    
)



stage(protocol,

    step("extract-format",
        ( # this is an example of one operation that integrate many others
            format = extractFormat(@input),
            variable = 12,
        )
    ),

    step("example-many-parallel-operations",
        join(
            parallel( # this is an example of one operation that integrate many others
                format = extractFormat(@input) -> 200,
                variable = 12 -> 201,
            ) -> 202
        ) -> 203
    ),

    step(
        "example-nullable-return",
        while(null(@nullable-variable), #This is an infinite loop
            nullable-variable = nullable(null)
        )
    ),

    step(
        "serialize",
        for(@counter, increment(@counter), lower(12),
            else(
                return(Invalid format) -> 405,

                elif(
                    equals(@format, xml),
                    body = serialize(
                        @input,
                        bean(garganttua::com.garganttua.core.MyXmlParser!singleton#XmlParser)
                    ) -> 200,

                    if(
                        and(equals(@format, json), null(@nullable-variable)),
                        body = serialize(
                            @input,
                            bean(garganttua::com.garganttua.core.MyJsonParser!singleton#JsonParser)
                        ) -> 200
                    )
                )
            ) -> 200,
        )

        # Parser-specific error handling with contextual analysis
        catch(
            ParserException.class,
            output = parseParserException(@exception, @context) -> 400
        ),

        # Fallback catch: must be used carefully
        catch(
            Any,
            output = Unknown exception -> 500
        )
    )
)

# ------------------------------------------------------------------------------
# STAGE: BUSINESS
# ------------------------------------------------------------------------------
stage("business-checks",

    step(
        "validate",
        validate(@body) -> 200,
        catch(ValidationException.class, output = @exceptionMessage) -> 400
    ),

    step(
        "map-to-dto-1",
        dto1 = map(@body, com.garganttua.core.example.Dto1) -> 200,
        catch(MapperException.class, output = processMapperException(@exception, @context)),
        catch(Any, output = Unknown exception -> 500)
    ),

    step(
        "map-to-dto-2",
        dto2 = map(@body, com.garganttua.core.example.Dto2) -> 200,
        catch(MapperException.class, output = processMapperException(@exception, @context)),
        catch(Any, output = Unknown exception -> 500)
    )
)

# ------------------------------------------------------------------------------
# STAGE: UPDATE IN DATABASE
# ------------------------------------------------------------------------------
stage("update-in-db",

    step(
        "update-dto-1",
        retry(
            3, 10, seconds,
            synchronized(
                mutex-name,
                bean(garganttua::com.garganttua.core.RedisMutex),
                acquire/tryAcquire,
                300, millis,
                store(@dto1, bean(garganttua::com.garganttua.core.Repository1))
            )
        ) -> 200,

        catch(
            RepositoryException.class,
            revertUpdate(
                @exception,
                @dto1,
                bean(garganttua::com.garganttua.core.Repository1)
            ) -> 500
        )
    ),

    step(
        "update-dto-2",
        retry(
            3, 10, seconds,
            synchronized(
                mutex-name,
                bean(garganttua::com.garganttua.core.RedisMutex),
                acquire/tryAcquire,
                300, millis,
                store(@dto2, bean(garganttua::com.garganttua.core.Repository2))
            )
        ) -> 200,

        catch(
            RepositoryException.class,
            processRepositoryException(
                @exception,
                @dto2,
                bean(garganttua::com.garganttua.core.Repository2)
            ) -> 500
        )
    )
)

# ------------------------------------------------------------------------------
# STAGE: PRODUCE ANSWER
# ------------------------------------------------------------------------------
stage(produce-answer,

    step(
        "set-output",
        for(@list,
            output = @input -> code @for-element
        )
    )
)

include(./another-runtime-script.gs) # Import another runtime script ran after
#   this one, the included script and current contexts are merged
# The runtime final return code is the last effective value of `code`