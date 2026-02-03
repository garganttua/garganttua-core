#!/usr/bin/env garganttua-script
################################################################################
# Garganttua Script Example
#
# This script demonstrates the Garganttua Script language features including
# variables, expressions, control flow (for loops), and functions.
#
# ------------------------------------------------------------------------------
# EXECUTION MODEL
# ------------------------------------------------------------------------------
# - Scripts execute statements sequentially from top to bottom
# - Each statement is an expression that can:
#   - Call functions: functionName(arg1, arg2, ...)
#   - Assign results to variables: varName = expression
#   - Assign results with arrow: varName <- expression
#   - Use variables in expressions: @variableName
#
# ------------------------------------------------------------------------------
# CONTROL FLOW: FOR LOOP
# ------------------------------------------------------------------------------
# for("varName", updateExpr, conditionExpr, bodyExpr)
#
# - varName       : string, name of the loop variable
# - updateExpr    : expression to update the variable each iteration
# - conditionExpr : boolean expression, loop continues while true
# - bodyExpr      : expression executed each iteration
#
# Example: Count from 0 to 4
#   counter = 0
#   for("counter", increment(@counter), lower(@counter, 5), @counter)
#
# ------------------------------------------------------------------------------
# COMPARISON FUNCTIONS
# ------------------------------------------------------------------------------
# - lower(a, b)           : returns true if a < b
# - greater(a, b)         : returns true if a > b
# - lowerOrEquals(a, b)   : returns true if a <= b
# - greaterOrEquals(a, b) : returns true if a >= b
#
# All comparison functions work with numbers (int, long, double) and Strings.
#
# ------------------------------------------------------------------------------
# ARITHMETIC FUNCTIONS
# ------------------------------------------------------------------------------
# - increment(value) : returns value + 1
# - decrement(value) : returns value - 1
#
# ------------------------------------------------------------------------------
# OUTPUT FUNCTIONS
# ------------------------------------------------------------------------------
# - print(value)     : prints value to stdout
# - println(value)   : prints value to stdout with newline
#
# ------------------------------------------------------------------------------
# SCRIPT MANAGEMENT
# ------------------------------------------------------------------------------
# - include(path)    : includes a JAR or another script (.gs)
# - call(name)       : calls an included script by its name
#
# ------------------------------------------------------------------------------
# PREDEFINED VARIABLES
# ------------------------------------------------------------------------------
# - input      : runtime input (if any)
# - output     : runtime output
# - code       : current return code
# - context    : execution context (read-only)
#
# ------------------------------------------------------------------------------
# NAMING RULES
# ------------------------------------------------------------------------------
# Variable names must follow kebab-case convention:
#   example: my-variable, default-value, counter-1
#
# ------------------------------------------------------------------------------
# SCRIPT METADATA
# ------------------------------------------------------------------------------
# File extension : .gs (garganttua-script)
# Shebang        : #!/usr/bin/env garganttua-script
#
################################################################################

# ------------------------------------------------------------------------------
# DEPENDENCIES
# ------------------------------------------------------------------------------
# Include custom JARs or other scripts
# include(./my-custom-functions.jar)
# include(./utils.gs)

# ------------------------------------------------------------------------------
# GLOBAL VARIABLES
# ------------------------------------------------------------------------------
threshold  <- 100
multiplier <- 2
output     <- "OK"
code       <- 0
counter    <- 0

# ------------------------------------------------------------------------------
# EXAMPLE: FOR LOOP
# ------------------------------------------------------------------------------
# Count from 0 to 4 using for loop
# for("counter", increment(@counter), lower(@counter, 5), @counter)

# ------------------------------------------------------------------------------
# EXAMPLE: COMPARISON OPERATIONS
# ------------------------------------------------------------------------------
# Check if threshold is greater than 50
# is-high <- greater(@threshold, 50)

# Check if counter is less than or equal to 10
# is-valid <- lowerOrEquals(@counter, 10)

# ------------------------------------------------------------------------------
# EXAMPLE: ARITHMETIC
# ------------------------------------------------------------------------------
# Increment and decrement operations
# next-value <- increment(@counter)
# prev-value <- decrement(@counter)

# ------------------------------------------------------------------------------
# EXAMPLE: STRING CONCATENATION
# ------------------------------------------------------------------------------
# Concatenate strings
# message <- concatenate("Result: ", @output)

# ------------------------------------------------------------------------------
# EXAMPLE: OUTPUT
# ------------------------------------------------------------------------------
# Print results
# print(@output)

# ------------------------------------------------------------------------------
# SCRIPT RESULT
# ------------------------------------------------------------------------------
# The script exits with the value of 'code' as the return code
result <- @code
