# Workflow: full-pipeline
# Generated: 2026-02-06T12:26:30.094557646Z

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
httpCode <- @httpStatus
rawData <- @apiResponse


# Stage: validation
data <- @rawData
strict <- true
validationStatus <- "pending"
isValid <- true
validatedData <- @data
validationStatus <- "completed"
validated <- @validatedData


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
count <- @itemCount
sum <- @total


# Stage: finalize
data <- @transformed
metadata <- "pipeline-v1"
finalOutput <- @data
finalStatus <- "success"
output <- @finalOutput
result <- @finalOutput


# Output
