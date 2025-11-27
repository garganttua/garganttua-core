# Garganttua DSL

## Description

The **garganttua-dsl** module provides a flexible and extensible
framework to build domain specific structures using builders.
It includes: 
 - **Automatic builders** that can auto-detect configuration
before building. 
 - **Linked builders** that maintain a parent/child
relationship. 
 - **Automatic linked builders**, combining both
behaviors. 
 - A utility **OrderedMapBuilder** for building collections of
typed sub-builders.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-dsl</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

### Automatic Builder

An `AbstractAutomaticBuilder` supports:
 - Optional auto-detection
(`autoDetect(true)`)
 - Delayed build with caching (`built` stored after
first build)
 - Customizable detection and build steps
(`doAutoDetection`, `doBuild`)

### Linked Builder

An `AbstractLinkedBuilder`:
 - Maintains a reference to a parent
(`up()`)
 - Is ideal for nested DSLs (`root.section().field().value()`)

### Automatic Linked Builder

`AbstractAutomaticLinkedBuilder`:
 - Extends auto-detection
 - Includes
parent linking
 - Used for hierarchical DSLs where children must validate
or auto-complete based on the parent context

### OrderedMapBuilder

Builds an `OrderedMap<K, B>` from builders `V extends IBuilder<B>`,
transforming each entry's builder into a built object.

## Usage

### Example : Simple Automatic Builder

``` java
public class PersonBuilder extends AbstractAutomaticBuilder<PersonBuilder, Person> {

    private String name;

    public PersonBuilder name(String n) {
        this.name = n;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.name == null) {
            this.name = "Unknown"; // fallback auto-detection
        }
    }

    @Override
    protected Person doBuild() throws DslException {
        return new Person(this.name);
    }
}
```

Usage:

``` java
Person p = new PersonBuilder()
    .autoDetect(true)
    .build();
```

### Example : Linked Builder

``` java
public class AddressBuilder extends AbstractLinkedBuilder<PersonBuilder, Address> {

    private String city;

    public AddressBuilder(PersonBuilder parent) {
        super(parent);
    }

    public AddressBuilder city(String c) {
        this.city = c;
        return this;
    }

    @Override
    public Address build() {
        return new Address(this.city);
    }
}
```

Usage:

``` java
PersonBuilder pb = new PersonBuilder();
AddressBuilder ab = new AddressBuilder(pb);

Address a = ab.city("Paris").build();
Person p = pb.name("John").build();
```

### Example : Automatic Linked Builder

``` java
public class FieldBuilder extends AbstractAutomaticLinkedBuilder< FieldBuilder, ObjectBuilder, Field > {

    private String name;

    public FieldBuilder(ObjectBuilder parent) {
        super(parent);
    }

    public FieldBuilder name(String n) {
        this.name = n;
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.name == null) {
            this.name = "default"; // auto-complete
        }
    }

    @Override
    protected Field doBuild() throws DslException {
        return new Field(this.name, this.up().getObjectType());
    }
}
```

### Example : OrderedMapBuilder

``` java
OrderedMapBuilder<String, FieldBuilder, Field> mapBuilder = new OrderedMapBuilder<>();

mapBuilder.put("id", new FieldBuilder(obj).name("id"));
mapBuilder.put("name", new FieldBuilder(obj).name("name"));

OrderedMap<String, Field> fields = mapBuilder.build();
```

## Tips and best practices

-   **Use Simple Builders** when no parent-child relationship or
    auto-detection is required.
-   **Use Automatic Builders** when the component requires optional
    inference or fallback values.
-   **Use Linked Builders** when building a hierarchical DSL where each
    child must refer to its parent.
-   **Use Automatic Linked Builders** when both inference and structural
    linking are needed.
-   Enable `autoDetect(true)` only when the builder can safely infer
    missing fields.
-   Keep `doAutoDetection()` lightweight and deterministic.
-   Avoid storing builders directly in your domain objects---store only
    built instances.
-   OrderedMapBuilder is ideal for DSLs describing lists of fields,
    steps, or properties.

## License
This module is distributed under the MIT License.