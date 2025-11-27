# Garganttua Mapper

## Description
Garganttua Mapper is a flexible, declarative object-to-object mapping engine for Java.  
It allows mapping data between objects, supporting simple fields, collections, maps, inheritance, and custom conversion methods.  
The mapper is **reversible**, meaning you can map from source to destination or from destination back to source.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-mapper</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-reflection`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### FieldMappingRule

Annotation to define how a field in the source object maps to a field in the destination object.

**Parameters:**

- `sourceFieldAddress` (String, required): Path to the field in the source object.
- `fromSourceMethod` (String, optional): Name of a method in the destination to convert data from the source.
- `toSourceMethod` (String, optional): Name of a method in the destination to convert data back to the source.

**Example:**
```java
class Source {
    public int field;
}

class Destination {
    @FieldMappingRule(sourceFieldAddress = "field")
    private String field;
}
```
### Addressing fields 
Each `MappingRule` uses an `ObjectAddress` to know **which property in the source maps to which property in the destination**.

Example simple mapping:
```java
@FieldMappingRule(sourceFieldAddress = "parent.child.field")
private String field;
```
- `"parent.child.field"` becomes an `ObjectAddress` and guides the mapper to the correct source field.

Example mapping with map values:
```java
@FieldMappingRule(sourceFieldAddress = "map1.#value.inner")
private Inner inner;
```
- Iterates over all map values to map the `inner` field

More infos [**here**](../garganttua-commons/ObjectAddress-README.md) 

### ObjectMappingRule
Annotation to define custom conversion methods for an entire object.

**Parameters:**
- `fromSourceMethod` (String, required): Method to map data from source to this object.
- `toSourceMethod` (String, required): Method to map data from this object back to source.

**Example:**
```java
@ObjectMappingRule(fromSourceMethod = "from", toSourceMethod = "to")
class Dto {
    @FieldMappingRule(sourceFieldAddress = "innerField")
    private String innerField;

    private void from(Source inner) {
        this.innerField = inner.getInnerField();
    }

    private void to(Source inner) {
        inner.setInnerField(this.innerField);
    }
}
```

## Usage

### 1. Mapping simple fields
```java
Source source = new Source();
source.field = 123;

Destination dest = new Mapper()
    .configure(MapperConfigurationItem.FAIL_ON_ERROR, false)
    .map(source, Destination.class);

System.out.println(dest.getField()); // "123"
```

### 2. Mapping collections
```java
class SourceListContainer {
    public List<SourceItem> items = new ArrayList<>();
}
class SourceItem { public int value; }

class DestListContainer {
    @FieldMappingRule(sourceFieldAddress = "items")
    public List<DestItem> items = new ArrayList<>();
}
class DestItem {
    @FieldMappingRule(sourceFieldAddress = "value")
    public int value;
}

SourceListContainer source = new SourceListContainer();
source.items.add(new SourceItem(){ { value = 1; } });

DestListContainer dest = new Mapper()
    .configure(MapperConfigurationItem.FAIL_ON_ERROR, true)
    .map(source, DestListContainer.class);

System.out.println(dest.items.get(0).value); // 1
```
### 3. Mapping with inheritance and nested objects
```java
class Parent {
    public String parentField;
}

class Child extends Parent {
    @FieldMappingRule(sourceFieldAddress = "childField")
    private String childField;

    private String fromChildField(int value) { return String.valueOf(value); }
    private int toChildField(String value) { return Integer.parseInt(value); }
}
```
### 4. Reversible mapping
The same `Mapper` instance can be used to map **from source to destination** or **from destination back to source**:

```java
GenericDto dto = mapper.map(entity, GenericDto.class);
GenericEntity entity2 = mapper.map(dto, GenericEntity.class);
```

## Error handling
- `MapperException` is thrown when mapping rules are invalid, e.g.,:
  - Source field does not exist.
  - Method signatures do not match the field types.
- You can configure the mapper to **fail or ignore errors**:
``` java 
Mapper mapper = new Mapper()
    .configure(MapperConfigurationItem.FAIL_ON_ERROR, true);
```

## Performance
**Mapping pre-recording**

To improve performance, mapping configurations between source and destination classes can be recorded in the Mapper.
This avoids repeatedly parsing annotations and creating mapping rules at runtime.
```java 
Mapper mapper = new Mapper();
MappingConfiguration config = mapper.recordMappingConfiguration(Source.class, Destination.class);
```
Once recorded, the mapper will reuse the configuration for all subsequent mappings between the same classes, significantly reducing mapping overhead.

**Mapping post-recording**

Once a mapping is done, the configuration is recorded in the Mapper, that reuses previously done mapping rules.

## Tips and Best Practices
- It is recommended to use a single Mapper within the hole client program to improve performances by reused the mapping rules already done.
- Always annotate fields with `@FieldMappingRule` to ensure correct mapping.
- Use `@ObjectMappingRule` when a class requires custom conversion logic for the whole object.
- The mapper supports **collections, sets, maps, and nested objects** automatically.
- Keep your `from`/`to` methods **private** unless you need them elsewhere.
- Enable `FAIL_ON_ERROR` during development to catch misconfigurations early.

## License
This module is distributed under the MIT License.
