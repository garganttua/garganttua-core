# Workflow: full-pipeline
# Generated: 2026-02-06T11:56:20.913635363Z

# Preset variables
apiUrl <- "https://api.example.com/data"
requestTimeout <- 30000
targetFormat <- "json"

# Stage: fetch
url <- @apiUrl
timeout <- @requestTimeout
apiResponse <- "fetched data from " + @url
httpStatus <- 200
fetchComplete <- true
rawData <- @apiResponse
httpCode <- @httpStatus
@code == 2 | retry(3, @_current_script)
@code == 1 | abort()


# Stage: validation
data <- @rawData
strict <- true
validationStatus <- "pending"
@data == null | (
    validationStatus <- "error"
    code <- 2
)
isValid <- true
validatedData <- @data
validationStatus <- "completed"
validated <- @validatedData
@code == 2 | abort()
@code == 1 | abort()


# Stage: transform
inputData <- @validated
format <- @targetFormat
processingStep <- "transform"
transformedData <- @inputData
outputFormat <- @format
result <- @transformedData
transformed <- @transformedData


# Stage: statistics
values <- @transformed
total <- 0
itemCount <- 0
result <- @values
sum <- @total
count <- @itemCount


# Stage: finalize
data <- @transformed
metadata <- "pipeline-v1"
finalOutput <- @data
finalStatus <- "success"
output <- @finalOutput
result <- @finalOutput


# Output
