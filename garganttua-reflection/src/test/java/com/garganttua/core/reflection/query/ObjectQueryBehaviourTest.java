package com.garganttua.core.reflection.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Behaviour tests for {@link ObjectQuery} resolution: nested field paths,
 * inheritance, collection / array / map element traversal, both-field-and-method
 * conflicts and not-found error reporting.
 */
public class ObjectQueryBehaviourTest {

    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUpReflection() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    // --- Domain ---

    public static class Leaf {
        public String value;

        public String compute() {
            return value;
        }
    }

    public static class Node {
        public Leaf leaf;
    }

    public static class Parent {
        public String parentField;
    }

    public static class Child extends Parent {
        public String childField;

        public String childMethod() {
            return "x";
        }
    }

    public static class CollectionContainer {
        public List<Leaf> leaves;
    }

    public static class MapContainer {
        public Map<Leaf, Leaf> map;
    }

    public static class Conflict {
        public String thing;

        public String thing() {
            return "method";
        }
    }

    private static IObjectQuery<?> query(Class<?> clazz) throws ReflectionException {
        return new ObjectQuery<>(RuntimeClass.of(clazz), PROVIDER);
    }

    // ========================================================================
    // Constructor guard
    // ========================================================================

    @Test
    public void constructorRejectsNullClass() {
        assertThrows(ReflectionException.class, () -> new ObjectQuery<>(null, PROVIDER));
    }

    // ========================================================================
    // Nested field resolution via deep search
    // ========================================================================

    @Test
    public void addressResolvesNestedLeafFieldThroughNode() throws ReflectionException {
        ObjectAddress addr = query(Node.class).address("value");
        assertNotNull(addr);
        assertEquals("leaf.value", addr.toString());
    }

    @Test
    public void findReturnsFullPathForNestedField() throws ReflectionException {
        List<Object> path = query(Node.class).find(new ObjectAddress("leaf.value", false));
        assertEquals(2, path.size());
        assertInstanceOf(IField.class, path.get(0));
        assertInstanceOf(IField.class, path.get(1));
        assertEquals("leaf", ((IField) path.get(0)).getName());
        assertEquals("value", ((IField) path.get(1)).getName());
    }

    @Test
    public void findResolvesNestedMethodAsLeaf() throws ReflectionException {
        List<Object> path = query(Node.class).find(new ObjectAddress("leaf.compute", false));
        assertEquals(2, path.size());
        assertInstanceOf(IField.class, path.get(0));
        assertInstanceOf(IMethod.class, path.get(1));
        assertEquals("compute", ((IMethod) path.get(1)).getName());
    }

    // ========================================================================
    // Inheritance traversal
    // ========================================================================

    @Test
    public void addressFindsInheritedField() throws ReflectionException {
        ObjectAddress addr = query(Child.class).address("parentField");
        assertEquals("parentField", addr.toString());
    }

    @Test
    public void findFindsInheritedFieldViaSuperclassRecursion() throws ReflectionException {
        List<Object> path = query(Child.class).find("parentField");
        assertEquals(1, path.size());
        assertEquals("parentField", ((IField) path.getLast()).getName());
    }

    @Test
    public void findFindsMethodDeclaredOnSubclass() throws ReflectionException {
        List<Object> path = query(Child.class).find("childMethod");
        assertInstanceOf(IMethod.class, path.getLast());
        assertEquals("childMethod", ((IMethod) path.getLast()).getName());
    }

    // ========================================================================
    // Collection / array / map element traversal
    //
    // NOTE: deep address resolution does NOT descend into a collection/array/map
    // field's element type. scanFieldsForAddress() only recurses through fields
    // whose own type passes Fields.isNotPrimitiveOrInternal(...), and List / arrays /
    // Map live in java.util (treated as "internal"), so the collection branches are
    // never reached for an element name living only inside the element type.
    // See the explicit-address test below for the supported way to reach such elements.
    // ========================================================================

    @Test
    public void deepSearchDoesNotDescendIntoCollectionElementType() throws ReflectionException {
        // 'value' lives only on Leaf, the element type of List<Leaf>; deep search cannot find it.
        assertNull(query(CollectionContainer.class).address("value"));
    }

    @Test
    public void explicitCollectionElementAddressStillResolvesViaFind() throws ReflectionException {
        // When the full address is provided, find() does traverse the collection element type.
        List<Object> path = query(CollectionContainer.class).find(new ObjectAddress("leaves.value", false));
        assertEquals(2, path.size());
        assertEquals("leaves", ((IField) path.get(0)).getName());
        assertEquals("value", ((IField) path.get(1)).getName());
    }

    @Test
    public void explicitMapValueElementAddressResolvesViaFind() throws ReflectionException {
        List<Object> path = query(MapContainer.class)
                .find(new ObjectAddress("map." + ObjectAddress.MAP_VALUE_INDICATOR + ".value", false));
        assertEquals("value", ((IField) path.getLast()).getName());
    }

    // ========================================================================
    // Conflict + not found
    // ========================================================================

    @Test
    public void whenFieldAndMethodShareNameTheFieldWinsInFind() throws ReflectionException {
        // find() looks up the field first; a same-named method is only consulted when no field exists,
        // so the field shadows the method rather than producing a conflict.
        List<Object> path = query(Conflict.class).find("thing");
        assertInstanceOf(IField.class, path.getLast());
        assertEquals("thing", ((IField) path.getLast()).getName());
    }

    @Test
    public void addressReturnsNullForUnknownElement() throws ReflectionException {
        assertNull(query(Leaf.class).address("doesNotExist"));
    }

    @Test
    public void findThrowsForUnknownElement() throws ReflectionException {
        IObjectQuery<?> q = query(Leaf.class);
        assertThrows(ReflectionException.class, () -> q.find("doesNotExist"));
    }

    @Test
    public void addressesReturnsEmptyForUnknownElement() throws ReflectionException {
        assertTrue(query(Leaf.class).addresses("doesNotExist").isEmpty());
    }
}
