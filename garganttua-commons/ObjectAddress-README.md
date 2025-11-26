
# ObjectAddress

## Description
`ObjectAddress` represents a hierarchical path to a field or property within a Java object.  
It is used in Garganttua Mapper to navigate source and destination objects during object-to-object mapping.  

Addresses can include **nested fields, collections, and maps**, and are designed to be both readable and navigable.

## Structure of an Address
- Addresses are strings using `.` as a separator. Example: `"parent.child.field"`  
- Internally, fields are stored in a `String[] fields` array. Example: `["parent","child","field"]`

### Special Indicators for Maps
- `#key` → targets the key of a map  
- `#value` → targets the value of a map  

Example: `"map1.#value.inner"`  
- `"map1"` → map property in the object  
- `"#value"` → iterates over the values of the map  
- `"inner"` → field inside the value object

## Core Methods

### Construction
```java
ObjectAddress addr = new ObjectAddress("parent.child.field");
```
- Throws `IllegalArgumentException` if address is null, empty, or starts/ends with `.`  
- `detectLoops` (default `true`) verifies no field repeats in the path except `#key`/`#value`

### Accessing Fields
- `length()` → number of elements in the address  
- `getElement(int index)` → returns the field at the specified index  

### Sub-address
```java
ObjectAddress sub = addr.subAddress(1); // ["parent","child"]
```

### Adding an Element
```java
addr.addElement("field"); // becomes "parent.child.field"
```
- Validates new element is not null/empty  
- Optionally checks for loops  

### Cloning
```java
ObjectAddress clone = addr.clone();
```
- Creates a new instance with the same fields

## String Representation & Comparison
- `toString()` → reconstructs the address using `.` as a separator  
- `equals()` and `hashCode()` → compare based on the `fields` array

## Summary
- `ObjectAddress` is a **path representation** for object properties  
- Supports nested fields, collections, maps, and loop detection  
- Enables **automatic navigation and mapping** in Garganttua Mapper  
