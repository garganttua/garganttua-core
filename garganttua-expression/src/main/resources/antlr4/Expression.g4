grammar Expression;

options {
    language = Java;
}

@header {
    package com.garganttua.core.expression.antlr4;
}

// ===============================
// RÈGLE PRINCIPALE
// ===============================
root
    : expression EOF
    ;

// ===============================
// EXPRESSIONS
// ===============================
expression
    : functionCall
    | literal
    | variableReference  // Référence à une variable du contexte: @myVar
    | IDENTIFIER     // Identifiants simples traités comme strings
    | type           // Types (int, boolean, java.lang.String, Class<?>, etc.)
    ;

// ===============================
// VARIABLE REFERENCE
// ===============================
variableReference
    : '@' IDENTIFIER          // Variable reference: @myVar
    | '@' INT_LITERAL         // Argument reference: @0, @1, @2...
    ;

// ===============================
// FONCTIONS
// ===============================
functionCall
    : IDENTIFIER '(' arguments? ')'                      // Fonction classique: concatenate("a", "b")
    | ':' IDENTIFIER '(' arguments ')'                   // Méthode d'instance ou statique: :equals(obj, "test") ou :copyValueOf(String.class, "")
    | ':' '(' arguments ')'                              // Constructeur: :(String.class, "toto")
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
    | INT_LITERAL
    | FLOAT_LIT
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
    : BOOLEAN_TYPE
    | BYTE_TYPE
    | SHORT_TYPE
    | INT_TYPE
    | LONG_TYPE
    | FLOAT_TYPE
    | DOUBLE_TYPE
    | CHAR_TYPE
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
    : CLASS '<' type '>'
    | CLASS '<' '?' '>'
    ;

// ===============================
// TOKENS
// ===============================

// Type keywords (must come before IDENTIFIER to have priority)
BOOLEAN_TYPE : 'boolean';
BYTE_TYPE    : 'byte';
SHORT_TYPE   : 'short';
INT_TYPE     : 'int';
LONG_TYPE    : 'long';
FLOAT_TYPE   : 'float';
DOUBLE_TYPE  : 'double';
CHAR_TYPE    : 'char';
CLASS        : 'Class';

// Boolean literals (must come before IDENTIFIER)
BOOLEAN      : 'true' | 'false';
NULL         : 'null';

// Literals
STRING       : '"' (~["\\] | '\\' .)* '"' ;
CHAR         : '\'' . '\'' ;
INT_LITERAL  : '-'? [0-9]+ ;
FLOAT_LIT    : '-'? [0-9]+ '.' [0-9]+ ;

// Identifiers (must come after keywords)
IDENTIFIER   : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\r\n]+ -> skip ;