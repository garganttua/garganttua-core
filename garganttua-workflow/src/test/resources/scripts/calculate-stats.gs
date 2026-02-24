#!/usr/bin/env gs

#@workflow
#  Calculates basic statistics on a list of numeric values.
#  Returns sum and count of items.
#
#  @in  values: List
#  @out sum -> total: Number
#  @out count -> itemCount: Integer
#  @return 0: SUCCESS
#  @return 1: EMPTY_LIST_ERROR
#@end

# Statistics Calculation Script
# @0 = values (List) - the list of numeric values

total <- 0
itemCount <- 0
result <- @0