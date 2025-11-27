# Garganttua Commons

## Description

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-commons</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `javax.inject:javax.inject`
 - `jakarta.annotation:jakarta.annotation-api`

<!-- AUTO-GENERATED-END -->

## Core Concepts

Garganttua Commons centralizes the reusable building blocks that support the rest of the ecosystem:
 - Interfaces for standard contracts across modules
 - Enums for common status, types, or configuration flags
 - Annotations for metadata and runtime behavior
 - Exceptions for consistent error handling
 - Utility classes for common operations

## Packages Overview

 - **binding** : 
 - **condition** : 
 - **crypto** : 
 - **dsl** : 
 - **execution** : 
 - **injection** : 
 - **lifecycle** :
 - **mapper** : 
 - **native** : 
 - **reflection** : 
 - **runtime** : 
 - **supply** : 
 - **utils** : 

## Tips and best practices
 - Always use the common exceptions and annotations for consistency.
 - Leverage the utility classes to reduce boilerplate across modules.
 - Reuse enums and interfaces to maintain modular interoperability.
 - When creating new DSLs, extend the commons DSL package to maintain standard patterns.

## License
This module is distributed under the MIT License.