grammar Script;

options {
    language = Java;
}

@header {
    package com.garganttua.core.script.antlr4;
}

// ===============================
// SCRIPT (ENTRY POINT)
// ===============================
script
    : NL* statement (NL+ statement)* NL* EOF
    ;

// ===============================
// STATEMENT
// ===============================
statement
    : (IDENTIFIER LARROW)? expression (RARROW INT_LITERAL)? (NL+ catchClause)* (NL+ downstreamCatchClause)* (NL+ pipeClause)*     # resultAssignStatement
    | (IDENTIFIER '=')? expression (RARROW INT_LITERAL)? (NL+ catchClause)* (NL+ downstreamCatchClause)* (NL+ pipeClause)*        # expressionAssignStatement
    ;

// ===============================
// CATCH CLAUSE (! ... => handler)
// ===============================
catchClause
    : '!' exceptionList? '=>' catchHandler
    ;

downstreamCatchClause
    : '*' exceptionList? '=>' catchHandler
    ;

pipeClause
    : '|' expression? FAT_ARROW pipeHandler
    ;

pipeHandler
    : (IDENTIFIER LARROW)? expression (RARROW INT_LITERAL)?     # resultAssignPipeHandler
    | (IDENTIFIER '=')? expression (RARROW INT_LITERAL)?        # expressionAssignPipeHandler
    ;

exceptionList
    : exceptionType (',' exceptionType)*
    ;

exceptionType
    : IDENTIFIER ('.' IDENTIFIER)* '.' CLASS
    ;

catchHandler
    : (IDENTIFIER LARROW)? expression (RARROW INT_LITERAL)?     # resultAssignHandler
    | (IDENTIFIER '=')? expression (RARROW INT_LITERAL)?        # expressionAssignHandler
    ;

// ===============================
// EXPRESSION (opaque text delegated to garganttua-expression)
// ===============================
expression
    : expressionToken+
    ;

expressionToken
    : IDENTIFIER
    | STRING
    | CHAR
    | INT_LITERAL
    | FLOAT_LIT
    | BOOLEAN
    | NULL
    | '('
    | ')'
    | ','
    | ':'
    | '.'
    | '<'
    | '>'
    | '?'
    | '['
    | ']'
    | '{'
    | '}'
    | '@'
    | '$'
    | BOOLEAN_TYPE
    | BYTE_TYPE
    | SHORT_TYPE
    | INT_TYPE
    | LONG_TYPE
    | FLOAT_TYPE
    | DOUBLE_TYPE
    | CHAR_TYPE
    | CLASS
    ;

// ===============================
// TOKENS
// ===============================

// Script operators
LARROW       : '<-';
RARROW       : '->';
FAT_ARROW    : '=>';

// Type keywords
BOOLEAN_TYPE : 'boolean';
BYTE_TYPE    : 'byte';
SHORT_TYPE   : 'short';
INT_TYPE     : 'int';
LONG_TYPE    : 'long';
FLOAT_TYPE   : 'float';
DOUBLE_TYPE  : 'double';
CHAR_TYPE    : 'char';
CLASS        : 'Class';

// Literals
BOOLEAN      : 'true' | 'false';
NULL         : 'null';
STRING       : '"' (~["\\] | '\\' .)* '"' ;
CHAR         : '\'' . '\'' ;
INT_LITERAL  : '-'? [0-9]+ ;
FLOAT_LIT    : '-'? [0-9]+ '.' [0-9]+ ;

// Identifiers
IDENTIFIER   : [a-zA-Z_][a-zA-Z_0-9]* ;

// Newline (statement separator)
NL : '\r'? '\n' ;

WS : [ \t]+ -> skip ;

// Comments
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
HASH_COMMENT  : '#' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
