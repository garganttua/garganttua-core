# Workflow: full-pipeline
# Generated: 2026-02-06T11:56:20.921601750Z

# Preset variables
apiUrl <- "https://api.example.com/data"
requestTimeout <- 30000
targetFormat <- "json"

# Stage: fetch
url <- @apiUrl
timeout <- @requestTimeout
_fetch_api_fetcher_ref <- include("/tmp/junit2166830200927408094/fetch-api.gs")
_fetch_api_fetcher_code <- execute_script(@_fetch_api_fetcher_ref, @url, @timeout)
    ! => handleFetchError(@exception)
    | equals(@_fetch_api_fetcher_code, 2) => retry(3, @_current_script)
    | equals(@_fetch_api_fetcher_code, 1) => abort()
rawData <- script_variable(@_fetch_api_fetcher_ref, "apiResponse")
httpCode <- script_variable(@_fetch_api_fetcher_ref, "httpStatus")


# Stage: validation
data <- @rawData
strict <- true
_validation_data_validator_ref <- include("/tmp/junit2166830200927408094/validate-data.gs")
_validation_data_validator_code <- execute_script(@_validation_data_validator_ref, @data, @strict)
    | equals(@_validation_data_validator_code, 2) => abort()
    | equals(@_validation_data_validator_code, 1) => abort()
validated <- script_variable(@_validation_data_validator_ref, "validatedData")
validationStatus <- script_variable(@_validation_data_validator_ref, "validationStatus")


# Stage: transform
inputData <- @validated
format <- @targetFormat
_transform_data_transformer_ref <- include("/tmp/junit2166830200927408094/transform-data.gs")
_transform_data_transformer_code <- execute_script(@_transform_data_transformer_ref, @inputData, @format)
transformed <- script_variable(@_transform_data_transformer_ref, "transformedData")


# Stage: statistics
values <- @transformed
_statistics_stats_calculator_ref <- include("/tmp/junit2166830200927408094/calculate-stats.gs")
_statistics_stats_calculator_code <- execute_script(@_statistics_stats_calculator_ref, @values)
sum <- script_variable(@_statistics_stats_calculator_ref, "total")
count <- script_variable(@_statistics_stats_calculator_ref, "itemCount")


# Stage: finalize
data <- @transformed
metadata <- "pipeline-v1"
_finalize_finalizer_ref <- include("/tmp/junit2166830200927408094/finalize-output.gs")
_finalize_finalizer_code <- execute_script(@_finalize_finalizer_ref, @data, @metadata)
result <- script_variable(@_finalize_finalizer_ref, "finalOutput")
finalStatus <- script_variable(@_finalize_finalizer_ref, "finalStatus")


# Output
