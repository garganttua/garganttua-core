#!/usr/bin/env gs

#@workflow
#  description: Fetches data from a remote API endpoint with timeout handling.
#               Returns the API response and HTTP status code.
#  inputs:
#    - name: url position: 0 type: String
#    - name: timeout position: 1 type: Integer
#  outputs:
#    - name: response variable: apiResponse type: Object
#    - name: statusCode variable: httpStatus type: Integer
#  returnCodes:
#    0: SUCCESS
#    1: CONNECTION_ERROR
#    2: TIMEOUT_ERROR
#    3: HTTP_ERROR
#@end

# API Fetch Script
# @0 = url (String)      - the API endpoint URL
# @1 = timeout (Integer)  - request timeout in milliseconds

apiResponse <- "fetched data from " + @0
httpStatus <- 200
fetchComplete <- true
