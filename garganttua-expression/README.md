# Garganttua Expression

Module de langage de requ�tes (DSL) pour cr�er dynamiquement des `ISupplier` � partir de requ�tes textuelles.

## Vue d'ensemble

Le module `garganttua-query` fournit un langage de requ�tes bas� sur ANTLR4 permettant d'exprimer des appels de fonctions, des valeurs litt�rales, des tableaux typ�s et des objets JSON. Les requ�tes sont analys�es syntaxiquement, converties en AST (Abstract Syntax Tree), puis ex�cut�es pour produire des `ISupplier`.

## Architecture

```
Query String � ANTLR Parser � AST � Conversion � Java Values � ISupplier
```

### Composants principaux

- **Query.g4** : Grammaire ANTLR4 d�finissant la syntaxe du langage
- **Query.java** : Visiteur ANTLR qui convertit l'AST en valeurs Java
- **QueryMethodBinder** : Ex�cute les m�thodes statiques enregistr�es
- **QueryBuilder** : API fluide pour construire des requ�tes

---

## Grammaire Query.g4

### 1. Types de donn�es support�s

#### 1.1 Litt�raux primitifs

| Type | Syntaxe | Exemples |
|------|---------|----------|
| Entier | `INT` | `42`, `-123` |
| D�cimal | `FLOAT` | `3.14`, `-2.71` |
| Cha�ne | `STRING` | `"hello"`, `"world"` |
| Caract�re | `CHAR` | `'a'`, `'z'` |
| Bool�en | `BOOLEAN` | `true`, `false` |
| Null | `NULL` | `null` |

**Exemples :**
```
query.query("42")           // � 42L (Long)
query.query("3.14")         // � 3.14 (Double)
query.query("\"hello\"")    // � "hello" (String)
query.query("'c'")          // � "c" (String)
query.query("true")         // � true (Boolean)
query.query("null")         // � null
```

#### 1.2 Tableaux typ�s

Syntaxe : `type[valeur1, valeur2, ...]`

**Types primitifs support�s :**
- `int[...]` � `int[]`
- `long[...]` � `long[]`
- `double[...]` � `double[]`
- `float[...]` � `float[]`
- `boolean[...]` � `boolean[]`
- `char[...]` � `char[]`

**Exemples :**
```
query.query("int[1,2,3]")              // � int[] {1, 2, 3}
query.query("double[3.14,2.71]")       // � double[] {3.14, 2.71}
query.query("boolean[true,false]")     // � boolean[] {true, false}
query.query("char['a','b','c']")       // � char[] {'a', 'b', 'c'}
```

**Tableaux imbriqu�s :**
```
query.query("int[int[1,2], int[3,4]]") // � Object[] {int[]{1,2}, int[]{3,4}}
```

#### 1.3 Tableaux non typ�s

Syntaxe : `[valeur1, valeur2, ...]`

**Exemples :**
```
query.query("[1,2,3]")                 // � Object[] {1L, 2L, 3L}
query.query("[\"a\",\"b\",3.14]")      // � Object[] {"a", "b", 3.14}
query.query("[[1,2],[3,4]]")           // � Object[] {Object[]{1L,2L}, Object[]{3L,4L}}
```

#### 1.4 Objets (JSON-like)

Syntaxe : `{"cl�": valeur, ...}`

**Exemples :**
```
query.query("{\"name\":\"Alice\"}")
// � Map<String, Object> {"name" � "Alice"}

query.query("{\"age\":int[30]}")
// � Map<String, Object> {"age" � int[]{30}}

query.query("{\"coords\":float[1.0,2.0]}")
// � Map<String, Object> {"coords" � float[]{1.0, 2.0}}

query.query("{\"nested\":{\"x\":int[1],\"y\":int[2]}}")
// � Map<String, Object> {"nested" � Map{"x" � int[]{1}, "y" � int[]{2}}}
```

#### 1.5 Types (Class<?>)

**Types primitifs :**
```
query.query("int")       // � int.class
query.query("double")    // � double.class
query.query("boolean")   // � boolean.class
```

**Classes Java :**
```
query.query("java.lang.String")         // � String.class
query.query("java.util.List")           // � List.class
```

**Types g�n�riques :**
```
query.query("Class<String>")            // � Class.class
query.query("Class<?>")                 // � Class.class
query.query("java.util.List<String>")   // � Parsing support� (conversion non impl�ment�e)
```

---

### 2. Appels de fonction

Syntaxe : `nomFonction(arg1, arg2, ...)`

#### 2.1 Fonctions simples

**Exemple :**
```java
// Enregistrer une fonction
QueryBuilder qb = new QueryBuilder();
qb.withQuery(MyClass.class, String.class)
  .method("fixed", ISupplier.class, new Class<?>[] { String.class })
  .up();
IQuery query = qb.build();

// Ex�cuter
query.query("fixed(\"Hello world\")");
// � Appelle MyClass.fixed("Hello world") � ISupplier<String>
```

#### 2.2 Fonctions imbriqu�es

Les fonctions peuvent �tre imbriqu�es. Les fonctions internes sont ex�cut�es en premier, et leurs r�sultats (via `.supply()`) sont pass�s aux fonctions parentes.

**Exemple :**
```java
// Enregistrer les fonctions
qb.withQuery(MyClass.class, String.class)
  .method("fixed", ISupplier.class, new Class<?>[] { String.class })
  .up()
  .withQuery(TypeConverters.class, String.class)
  .method("string", ISupplier.class, new Class<?>[] { String.class })
  .up();

// Ex�cuter
query.query("fixed(string(\"Hello world\"))");

// Ex�cution :
// 1. string("Hello world") � ISupplier<String>
// 2. .supply() � Optional<"Hello world">
// 3. fixed("Hello world") � ISupplier<String>
```

#### 2.3 Arguments mixtes

Les fonctions peuvent recevoir diff�rents types d'arguments :

```java
query.query("process(int[1,2,3], \"text\", true, 3.14)");
// Arguments : int[]{1,2,3}, "text", true, 3.14

query.query("compute(Class<String>, int[42])");
// Arguments : String.class, int[]{42}

query.query("merge({\"a\":1}, {\"b\":2})");
// Arguments : Map{"a"�1L}, Map{"b"�2L}
```

---

### 3. R�gles de grammaire

#### 3.1 R�gle principale

```antlr4
query : expression EOF ;
```

Une requ�te est une expression unique suivie de la fin de fichier.

#### 3.2 Expressions

```antlr4
expression
    : functionCall
    | literal
    | type
    | IDENTIFIER
    ;
```

Une expression peut �tre :
- Un appel de fonction : `sum(1,2,3)`
- Un litt�ral : `42`, `"hello"`, `[1,2,3]`
- Un type : `int`, `Class<String>`
- Un identifiant : `myVar` (converti en cha�ne)

#### 3.3 Appels de fonction

```antlr4
functionCall
    : IDENTIFIER '(' arguments? ')'
    ;

arguments
    : expression (',' expression)*
    ;
```

Format : `nomFonction(arg1, arg2, ...)`

#### 3.4 Litt�raux

```antlr4
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
```

#### 3.5 Types

```antlr4
type
    : simpleType arrayDims?
    ;

simpleType
    : primitiveType
    | classType
    | classOfType
    ;

primitiveType
    : 'boolean' | 'byte' | 'short' | 'int'
    | 'long' | 'float' | 'double' | 'char'
    ;

arrayDims
    : ('[' (expression (',' expression)*)? ']')+
    ;

classType
    : className genericArguments?
    ;

className
    : IDENTIFIER ('.' IDENTIFIER)*
    ;

genericArguments
    : '<' type (',' type)* '>'
    ;

classOfType
    : 'Class' '<' type '>'
    | 'Class' '<' '?' '>'
    ;
```

#### 3.6 Tokens lexicaux

```antlr4
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]* ;
STRING     : '"' (~["\\] | '\\' .)* '"' ;
CHAR       : '\'' . '\'' ;
INT        : '-'? [0-9]+ ;
FLOAT      : '-'? [0-9]+ '.' [0-9]+ ;
BOOLEAN    : 'true' | 'false';
NULL       : 'null';
WS         : [ \t\r\n]+ -> skip ;
```

---

## Utilisation

### Exemple complet

```java
import com.garganttua.core.query.IQuery;
import com.garganttua.core.query.dsl.QueryBuilder;
import com.garganttua.core.supply.ISupplier;

public class Example {

    // Fonction � enregistrer (doit �tre statique)
    public static ISupplier<String> greet(String name) {
        return new FixedSupplierBuilder<>("Hello, " + name).build();
    }

    public static ISupplier<Integer> add(int a, int b) {
        return new FixedSupplierBuilder<>(a + b).build();
    }

    public static void main(String[] args) {
        // 1. Construire le QueryBuilder
        QueryBuilder qb = new QueryBuilder();

        // 2. Enregistrer les fonctions
        qb.withQuery(Example.class, String.class)
          .method("greet", ISupplier.class, new Class<?>[] { String.class })
          .up()
          .withQuery(Example.class, Integer.class)
          .method("add", ISupplier.class, new Class<?>[] { int.class, int.class })
          .up();

        // 3. Construire la Query
        IQuery query = qb.build();

        // 4. Ex�cuter des requ�tes
        Optional<ISupplier<?>> result1 = query.query("greet(\"Alice\")");
        if (result1.isPresent()) {
            Optional<String> value = (Optional<String>) result1.get().supply();
            System.out.println(value.get()); // "Hello, Alice"
        }

        Optional<ISupplier<?>> result2 = query.query("add(10, 32)");
        if (result2.isPresent()) {
            Optional<Integer> value = (Optional<Integer>) result2.get().supply();
            System.out.println(value.get()); // 42
        }
    }
}
```

### Fonctions de conversion de type

Le module fournit des fonctions de conversion dans `TypeConverters` :

```java
import com.garganttua.core.query.functions.TypeConverters;

// Fonctions disponibles :
TypeConverters.string("hello")      // � ISupplier<String>
TypeConverters.integer(42)          // � ISupplier<Integer>
TypeConverters.longValue(123L)      // � ISupplier<Long>
TypeConverters.doubleValue(3.14)    // � ISupplier<Double>
TypeConverters.floatValue(2.71f)    // � ISupplier<Float>
TypeConverters.booleanValue(true)   // � ISupplier<Boolean>
```

**Utilisation dans les requ�tes :**

```java
qb.withQuery(TypeConverters.class, String.class)
  .method("string", ISupplier.class, new Class<?>[] { String.class })
  .up();

query.query("string(\"Hello\")");
// � ISupplier<String> contenant "Hello"
```

---

## Comportement de conversion

### Valeurs litt�rales

| Expression | Type Java | Valeur |
|------------|-----------|--------|
| `42` | `Long` | `42L` |
| `3.14` | `Double` | `3.14` |
| `"hello"` | `String` | `"hello"` |
| `'c'` | `String` | `"c"` |
| `true` | `Boolean` | `true` |
| `null` | - | `null` � `Optional.empty()` |

### Tableaux typ�s

Les tableaux typ�s sont convertis en tableaux primitifs Java :

```java
int[1,2,3]       � int[] {1, 2, 3}
double[3.14]     � double[] {3.14}
boolean[true]    � boolean[] {true}
```

### Tableaux imbriqu�s

Les tableaux contenant d'autres tableaux sont convertis en `Object[]` :

```java
int[int[1,2], int[3,4]]  � Object[] {int[]{1,2}, int[]{3,4}}
```

### Appels de fonction

Les appels de fonction sont ex�cut�s r�cursivement :

1. Les arguments sont d'abord convertis (appels imbriqu�s ex�cut�s)
2. Pour les fonctions imbriqu�es, `.supply()` est appel� automatiquement
3. La fonction parente re�oit les valeurs d�j� extraites
4. Le r�sultat final est un `ISupplier`

**Exemple :**
```java
fixed(string("Hello"))

// �tapes :
// 1. string("Hello") � ISupplier<String>
// 2. .supply() � Optional.of("Hello")
// 3. fixed("Hello") � ISupplier<String>
```

---

## Gestion des erreurs

### Fonctions non enregistr�es

Si une fonction n'est pas enregistr�e, la requ�te retourne `Optional.empty()` :

```java
query.query("unknownFunction(1,2,3)")  // � Optional.empty()
```

### Valeurs null

Les valeurs `null` retournent `Optional.empty()` au lieu de cr�er un `FixedSupplier` :

```java
query.query("null")  // � Optional.empty()
```

### Erreurs d'ex�cution

Les erreurs lors de l'ex�cution des fonctions sont logg�es et retournent `Optional.empty()` :

```java
query.query("divide(10, 0)")  // � Optional.empty() + log d'erreur
```

---

## Limitations

1. **Mots-cl�s r�serv�s** : `boolean`, `byte`, `short`, `int`, `long`, `float`, `double`, `char`, `true`, `false`, `null`, `Class` ne peuvent pas �tre utilis�s comme noms de fonction ou variables

2. **M�thodes statiques uniquement** : Seules les m�thodes statiques peuvent �tre enregistr�es

3. **Types g�n�riques** : Le parsing des types g�n�riques est support� mais la conversion compl�te n'est pas encore impl�ment�e

4. **Cha�nes de caract�res** : Les �chappements support�s sont `\"` et `\\`

---

## AST (Abstract Syntax Tree)

Le module utilise les nSuds AST suivants :

| NSud | Description | Exemple |
|------|-------------|---------|
| `LiteralNode` | Valeur litt�rale | `42`, `"hello"` |
| `FunctionNode` | Appel de fonction | `sum(1,2,3)` |
| `IdentifierNode` | Identifiant | `myVar`, `true` |
| `ArrayLiteralNode` | Tableau non typ� | `[1,2,3]` |
| `TypedArrayNode` | Tableau typ� | `int[1,2,3]` |
| `ObjectLiteralNode` | Objet JSON | `{"key":"value"}` |
| `PrimitiveTypeNode` | Type primitif | `int`, `double` |
| `ClassTypeNode` | Type classe | `java.lang.String` |
| `ArrayTypeNode` | Type tableau | `int[]` |
| `ClassOfTypeNode` | Type Class | `Class<String>` |

---

## D�pendances

```xml
<dependency>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-runtime</artifactId>
    <version>4.13.0</version>
</dependency>
```

---

## Tests

Voir `QueryBuilderTest.java` pour des exemples d'utilisation :
- `testSimpleQuery()` : Test d'une requ�te avec fonctions imbriqu�es
- `testNonStaticQueryShouldThrowException()` : V�rification que seules les m�thodes statiques sont accept�es
- `dummyTest()` : Tests de parsing de diverses syntaxes
