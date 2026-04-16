# Workflow: full-pipeline
# Generated: 2026-04-16T20:31:43.272852256Z

# Preset variables
apiUrl <- "https://api.example.com/data"
requestTimeout <- 30000
targetFormat <- "json"

# Stage: fetch
url <- @apiUrl
timeout <- @requestTimeout
_fetch_api_fetcher_ref <- include("/tmp/junit5500116633915281239/fetch-api.gs")
_fetch_api_fetcher_code <- execute_script(@_fetch_api_fetcher_ref, @url, @timeout)
rawData <- script_variable(@_fetch_api_fetcher_ref, "apiResponse")
httpCode <- script_variable(@_fetch_api_fetcher_ref, "httpStatus")
response <- script_variable(@_fetch_api_fetcher_ref, "apiResponse")
statusCode <- script_variable(@_fetch_api_fetcher_ref, "httpStatus")


# Stage: validation
data <- @rawData
strict <- true
_validation_data_validator_ref <- include("/tmp/junit5500116633915281239/validate-data.gs")
_validation_data_validator_code <- execute_script(@_validation_data_validator_ref, @data, @strict)
validated <- script_variable(@_validation_data_validator_ref, "validatedData")
validationStatus <- script_variable(@_validation_data_validator_ref, "validationStatus")
status <- script_variable(@_validation_data_validator_ref, "validationStatus")


# Stage: transform
inputData <- @validated
format <- @targetFormat
_transform_data_transformer_ref <- include("/tmp/junit5500116633915281239/transform-data.gs")
_transform_data_transformer_code <- execute_script(@_transform_data_transformer_ref, @inputData, @format)
result <- script_variable(@_transform_data_transformer_ref, "transformedData")
outputFormat <- script_variable(@_transform_data_transformer_ref, "outputFormat")
transformed <- script_variable(@_transform_data_transformer_ref, "transformedData")


# Stage: statistics
values <- @transformed
_statistics_stats_calculator_ref <- include("/tmp/junit5500116633915281239/calculate-stats.gs")
_statistics_stats_calculator_code <- execute_script(@_statistics_stats_calculator_ref, @values)
count <- script_variable(@_statistics_stats_calculator_ref, "itemCount")
sum <- script_variable(@_statistics_stats_calculator_ref, "total")


# Stage: finalize
data <- @transformed
metadata <- "pipeline-v1"
_finalize_finalizer_ref <- include("/tmp/junit5500116633915281239/finalize-output.gs")
_finalize_finalizer_code <- execute_script(@_finalize_finalizer_ref, @data, @metadata)
result <- script_variable(@_finalize_finalizer_ref, "finalOutput")
finalStatus <- script_variable(@_finalize_finalizer_ref, "finalStatus")
finalResult <- script_variable(@_finalize_finalizer_ref, "finalOutput")
status <- script_variable(@_finalize_finalizer_ref, "finalStatus")


# Output
