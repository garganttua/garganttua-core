#!/usr/bin/env gs
var <- "hello"
print(@0)
 ! => print(@exception) -> 500
print(@1)
 ! => print(@exception) -> 500
print(@2) -> 300
 ! => print(@exception) -> 500
 | equals(@0, @1) 
    ! => print(@exception) -> 500
    => print("les deux premiers arguments sont identiques") -> 200
     ! => print(@exception) -> 500
 | => print("les deux premiers arguments ne sont pas identiques") -> 201
