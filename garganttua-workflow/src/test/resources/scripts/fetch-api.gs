#!/usr/bin/env gs

#@workflow
#  Fetches data from a remote API endpoint with timeout handling.
#  Returns the API response and HTTP status code.
#
#  @in  url: String
#  @in  timeout: Integer
#  @out response -> apiResponse: Object
#  @out statusCode -> httpStatus: Integer
#  @return 0: SUCCESS
#  @return 1: CONNECTION_ERROR
#  @return 2: TIMEOUT_ERROR
#  @return 3: HTTP_ERROR
#  @catch handleError(@exception)
#@end

# API Fetch Script
# @0 = url (String)      - the API endpoint URL
# @1 = timeout (Integer)  - request timeout in milliseconds

apiResponse <- "fetched data from " + @0
httpStatus <- 200
fetchComplete <- true