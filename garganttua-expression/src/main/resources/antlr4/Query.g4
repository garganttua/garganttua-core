grammar Query;

options {
    language = Java;
}

@header {
    package com.garganttua.core.query.antlr4;
}

// ===============================
// RÈGLE PRINCIPALE
// ===============================
query
    : expression EOF
    ;

// ===============================
// EXPRESSIONS
// ===============================
expression
    : functionCall
    | literal
    | type           // Permet int[1,2,3], Class<String>, etc.
    | IDENTIFIER
    ;

// ===============================
// FONCTIONS
// ===============================
functionCall
    : IDENTIFIER '(' arguments? ')'
    ;

arguments
    : expression (',' expression)*
    ;

// ===============================
// LITTÉRAUX (VALEURS)
// ===============================
literal
    : STRING
    | CHAR
    | INT
    | FLOAT
    | BOOLEAN
    | NULL
    | arrayLiteral
    | objectLiteral
    ;

arrayLiteral
    : '[' (expression (',' expression)*)? ']'
    ;

objectLiteral
    : '{' (pair (',' pair)*)? '}'
    ;

pair
    : STRING ':' (literal | type | objectLiteral)
    ;

// ===============================
// TYPES (PRIMITIFS, CLASS, TABLEAUX)
// ===============================
type
    : simpleType arrayDims? // primitiveType ou classType ou Class<?>
    ;

simpleType
    : primitiveType
    | classType
    | classOfType
    ;

primitiveType
    : 'boolean'
    | 'byte'
    | 'short'
    | 'int'
    | 'long'
    | 'float'
    | 'double'
    | 'char'
    ;

// Dimensions pour les tableaux de type (peut contenir des valeurs)
arrayDims
    : ('[' (expression (',' expression)*)? ']')+
    ;

// Classe avec package
className
    : IDENTIFIER ('.' IDENTIFIER)*
    ;

// Classe avec génériques
classType
    : className genericArguments?
    ;

genericArguments
    : '<' type (',' type)* '>'
    ;

// Class<> ou Class<?> 
classOfType
    : 'Class' '<' type '>'
    | 'Class' '<' '?' '>'
    ;

// ===============================
// TOKENS
// ===============================
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]* ;

STRING     : '"' (~["\\] | '\\' .)* '"' ;
CHAR       : '\'' . '\'' ;

INT        : '-'? [0-9]+ ;
FLOAT      : '-'? [0-9]+ '.' [0-9]+ ;

BOOLEAN    : 'true' | 'false';
NULL       : 'null';

WS : [ \t\r\n]+ -> skip ;