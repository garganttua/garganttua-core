package com.garganttua.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.runtime.Position;
import com.garganttua.core.utils.OrderedMap;

public class OrderedMapTest {

    private OrderedMap<String, String> map;

    @BeforeEach
    public void setUp() {
        map = new OrderedMap<>();
    }

    @Test
    public void testPutAndGet() {
        map.put("a", "Alpha");
        map.put("b", "Bravo");

        assertEquals("Alpha", map.get("a"));
        assertEquals("Bravo", map.get("b"));
        assertEquals(2, map.size());
    }

    @Test
    public void testDuplicateKeyThrows() {
        map.put("a", "Alpha");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> map.put("a", "Another Alpha"));
        assertEquals("Key already exists: a", ex.getMessage());
    }

    @Test
    public void testPutAtBefore() {
        map.put("a", "Alpha");
        map.put("c", "Charlie");

        map.putAt("b", "Bravo", "c", Position.BEFORE);

        // Vérifie l'ordre
        Map<String, String> result = map.asMap();
        String[] keys = result.keySet().toArray(new String[0]);
        assertArrayEquals(new String[] { "a", "b", "c" }, keys);
    }

    @Test
    public void testPutAtAfter() {
        map.put("a", "Alpha");
        map.put("c", "Charlie");

        map.putAt("b", "Bravo", "a", Position.AFTER);

        Map<String, String> result = map.asMap();
        String[] keys = result.keySet().toArray(new String[0]);
        assertArrayEquals(new String[] { "a", "b", "c" }, keys);
    }

    @Test
    public void testPutAtReferenceNotFoundAddsToEnd() {
        map.put("a", "Alpha");
        map.put("b", "Bravo");

        map.putAt("c", "Charlie", "nonexistent", Position.BEFORE);

        Map<String, String> result = map.asMap();
        String[] keys = result.keySet().toArray(new String[0]);
        assertArrayEquals(new String[] { "a", "b", "c" }, keys);
    }

    @Test
    public void testRemoveAndContains() {
        map.put("a", "Alpha");
        map.put("b", "Bravo");

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue("Bravo"));

        String removed = map.remove("a");
        assertEquals("Alpha", removed);
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutAll() {
        OrderedMap<String, String> other = new OrderedMap<>();
        other.put("x", "X-ray");
        other.put("y", "Yankee");

        map.put("a", "Alpha");
        map.putAll(other);

        Map<String, String> result = map.asMap();
        assertEquals(3, result.size());
        assertEquals("X-ray", result.get("x"));
        assertEquals("Yankee", result.get("y"));
    }

    @Test
    public void testIsEmptyAndClear() {
        assertTrue(map.isEmpty());

        map.put("a", "Alpha");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
    }
}