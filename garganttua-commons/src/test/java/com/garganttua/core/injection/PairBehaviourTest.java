package com.garganttua.core.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PairBehaviourTest {

    @Test
    void accessorsReturnComponents() {
        Pair<String, Integer> p = new Pair<>("a", 1);
        assertEquals("a", p.value1());
        assertEquals(1, p.value2());
    }

    @Test
    void nullComponentsAllowed() {
        Pair<String, String> p = new Pair<>(null, null);
        assertNull(p.value1());
        assertNull(p.value2());
    }

    @Test
    void valueEquality() {
        assertEquals(new Pair<>("x", 2), new Pair<>("x", 2));
        assertNotEquals(new Pair<>("x", 2), new Pair<>("x", 3));
        assertNotEquals(new Pair<>("x", 2), new Pair<>("y", 2));
    }

    @Test
    void hashCodeConsistentWithEquals() {
        assertEquals(new Pair<>("x", 2).hashCode(), new Pair<>("x", 2).hashCode());
    }
}
