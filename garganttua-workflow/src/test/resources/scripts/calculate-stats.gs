#!/usr/bin/env gs

#@workflow
#  description: Calculates basic statistics on a list of numeric values.
#               Returns sum and count of items.
#  inputs:
#    - name: values position: 0 type: List
#  outputs:
#    - name: sum variable: total type: Number
#    - name: count variable: itemCount type: Integer
#  returnCodes:
#    0: SUCCESS
#    1: EMPTY_LIST_ERROR
#@end

# Statistics Calculation Script
# @0 = values (List) - the list of numeric values

total <- 0
itemCount <- 0
result <- @0
