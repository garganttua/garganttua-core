package com.garganttua.core.reflection.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Additional behaviour tests for {@link ObjectQuery} targeting branches not covered by
 * {@code ObjectQueryBehaviourTest}: the {@code findAll} variants (overloaded methods,
 * nested paths, map-key traversal), the both-field-and-method conflict thrown by
 * {@code findAllRecursively}, multiple-address resolution and map-key element addressing.
 */
public class ObjectQueryMoreBehaviourTest {

    private static final RuntimeReflectionProvider PROVIDER = new RuntimeReflectionProvider();

    @BeforeAll
    static void setUp() throws DslException {
        IReflection reflection = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider()).build();
        IClass.setReflection(reflection);
    }

    @AfterAll
    static void tearDown() {
        IClass.setReflection(null);
    }

    public static class Leaf {
        public String value;
    }

    public static class Overloaded {
        public String run() {
            return "0";
        }

        public String run(String a) {
            return a;
        }

        public String run(String a, String b) {
            return a + b;
        }
    }

    public static class Node {
        public Leaf leaf;
    }

    public static class Conflict {
        public String thing;

        public String thing() {
            return "method";
        }
    }

    public static class KeyContainer {
        public Map<Leaf, String> map;
    }

    public static class TwoMethods {
        public String alpha() {
            return "a";
        }
    }

    private static IObjectQuery<?> query(Class<?> clazz) throws ReflectionException {
        return new ObjectQuery<>(RuntimeClass.of(clazz), PROVIDER);
    }

    // ===== findAll on overloaded methods returns one path per overload =====

    @Test
    public void findAllReturnsAllOverloads() throws ReflectionException {
        List<List<Object>> paths = query(Overloaded.class).findAll("run");
        assertEquals(3, paths.size());
        for (List<Object> path : paths) {
            assertInstanceOf(IMethod.class, path.getLast());
            assertEquals("run", ((IMethod) path.getLast()).getName());
        }
    }

    // ===== findAll on a single field returns one path =====

    @Test
    public void findAllReturnsSingleFieldPath() throws ReflectionException {
        List<List<Object>> paths = query(Leaf.class).findAll("value");
        assertEquals(1, paths.size());
        assertInstanceOf(IField.class, paths.get(0).getLast());
        assertEquals("value", ((IField) paths.get(0).getLast()).getName());
    }

    // ===== findAll traverses nested field path =====

    @Test
    public void findAllTraversesNestedFieldPath() throws ReflectionException {
        List<List<Object>> paths = query(Node.class).findAll(new ObjectAddress("leaf.value", false));
        assertEquals(1, paths.size());
        List<Object> path = paths.get(0);
        assertEquals(2, path.size());
        assertEquals("leaf", ((IField) path.get(0)).getName());
        assertEquals("value", ((IField) path.get(1)).getName());
    }

    // ===== findAll map-key element traversal =====

    @Test
    public void findAllTraversesMapKeyElement() throws ReflectionException {
        List<List<Object>> paths = query(KeyContainer.class)
                .findAll(new ObjectAddress("map." + ObjectAddress.MAP_KEY_INDICATOR + ".value", false));
        assertEquals(1, paths.size());
        assertEquals("value", ((IField) paths.get(0).getLast()).getName());
    }

    @Test
    public void findMapKeyElementViaSinglePath() throws ReflectionException {
        List<Object> path = query(KeyContainer.class)
                .find(new ObjectAddress("map." + ObjectAddress.MAP_KEY_INDICATOR + ".value", false));
        assertEquals("value", ((IField) path.getLast()).getName());
    }

    // ===== field shadows same-named method in findAll =====

    @Test
    public void findAllFieldShadowsSameNamedMethod() throws ReflectionException {
        // findAllRecursively only consults methods when no field exists, so a same-named
        // field shadows the method (the both-field-and-method conflict branch is unreachable
        // here) and findAll returns a single field path.
        List<List<Object>> paths = query(Conflict.class).findAll("thing");
        assertEquals(1, paths.size());
        assertInstanceOf(IField.class, paths.get(0).getLast());
        assertEquals("thing", ((IField) paths.get(0).getLast()).getName());
    }

    // ===== map with invalid (non key/value) indicator throws =====

    @Test
    public void findMapWithoutKeyValueIndicatorThrows() throws ReflectionException {
        IObjectQuery<?> q = query(KeyContainer.class);
        // 'map.value' — 'value' is neither MAP_KEY nor MAP_VALUE indicator after a map field
        assertThrows(ReflectionException.class,
                () -> q.find(new ObjectAddress("map.value", false)));
    }

    // ===== addresses returns a list (one entry per resolution) =====

    @Test
    public void addressesReturnsSingleEntryForField() throws ReflectionException {
        List<ObjectAddress> addresses = query(Leaf.class).addresses("value");
        assertEquals(1, addresses.size());
        assertEquals("value", addresses.get(0).toString());
    }

    @Test
    public void addressesReturnsEntryPerOverloadedMethod() throws ReflectionException {
        // overloaded methods produce one address per overload
        List<ObjectAddress> addresses = query(Overloaded.class).addresses("run");
        assertEquals(3, addresses.size());
        assertTrue(addresses.stream().allMatch(a -> a.toString().equals("run")));
    }

    @Test
    public void addressResolvesPlainMethod() throws ReflectionException {
        ObjectAddress addr = query(TwoMethods.class).address("alpha");
        assertEquals("alpha", addr.toString());
    }

    // ===== findAll throws for unknown element =====

    @Test
    public void findAllThrowsForUnknownElement() throws ReflectionException {
        IObjectQuery<?> q = query(Leaf.class);
        assertThrows(ReflectionException.class, () -> q.findAll("ghost"));
    }

    // ===== find(String) wraps in an addressed lookup =====

    @Test
    public void findByStringDelegatesToAddress() throws ReflectionException {
        List<Object> path = query(Leaf.class).find("value");
        assertEquals(1, path.size());
        assertEquals("value", ((IField) path.getLast()).getName());
        assertFalse(path.isEmpty());
    }
}
