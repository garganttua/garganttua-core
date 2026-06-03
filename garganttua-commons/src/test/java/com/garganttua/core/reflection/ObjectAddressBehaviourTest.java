package com.garganttua.core.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ObjectAddressBehaviourTest {

    @Test
    void parsesDotSeparatedPath() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user.profile.email");
        assertEquals(3, a.length());
        assertEquals("user", a.getElement(0));
        assertEquals("profile", a.getElement(1));
        assertEquals("email", a.getElement(2));
        assertEquals("email", a.getLastElement());
    }

    @Test
    void singleElementPath() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user");
        assertEquals(1, a.length());
        assertEquals("user", a.getElement(0));
        assertEquals("user", a.getLastElement());
    }

    @Test
    void toStringReassemblesPath() throws ReflectionException {
        assertEquals("a.b.c", new ObjectAddress("a.b.c").toString());
    }

    // --- invalid addresses ---

    @Test
    void nullAddressRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(null));
    }

    @Test
    void emptyAddressRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(""));
    }

    @Test
    void leadingDotRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress(".user"));
    }

    @Test
    void trailingDotRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ObjectAddress("user."));
    }

    // --- loop detection ---

    @Test
    void loopDetectedWhenDuplicateField() {
        assertThrows(ReflectionException.class, () -> new ObjectAddress("a.b.a"));
    }

    @Test
    void loopDetectionDisabledAllowsDuplicates() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b.a", false);
        assertEquals(3, a.length());
    }

    @Test
    void mapIndicatorsAreNotCountedAsLoops() throws ReflectionException {
        // #key and #value are excluded from loop detection, so they may repeat
        ObjectAddress a = new ObjectAddress("map.#key.other.#key");
        assertEquals(4, a.length());
    }

    // --- getElement bounds ---

    @Test
    void getElementOutOfBoundsRejected() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b");
        assertThrows(IllegalArgumentException.class, () -> a.getElement(2));
        assertThrows(IllegalArgumentException.class, () -> a.getElement(-1));
    }

    // --- subAddress ---

    @Test
    void subAddressReturnsPrefixInclusive() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user.profile.email");
        assertEquals("user", a.subAddress(0).toString());
        assertEquals("user.profile", a.subAddress(1).toString());
        assertEquals("user.profile.email", a.subAddress(2).toString());
    }

    @Test
    void subAddressOutOfBoundsRejected() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b");
        assertThrows(IllegalArgumentException.class, () -> a.subAddress(-1));
        assertThrows(IllegalArgumentException.class, () -> a.subAddress(2));
    }

    // --- addElement immutability ---

    @Test
    void addElementReturnsNewInstanceLeavingOriginalUnchanged() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user.profile");
        ObjectAddress extended = a.addElement("email");
        assertEquals("user.profile", a.toString());
        assertEquals("user.profile.email", extended.toString());
        assertEquals(3, extended.length());
    }

    @Test
    void addElementNullOrEmptyRejected() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user");
        assertThrows(IllegalArgumentException.class, () -> a.addElement(null));
        assertThrows(IllegalArgumentException.class, () -> a.addElement(""));
    }

    @Test
    void addElementCreatingLoopRejected() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user.profile");
        assertThrows(ReflectionException.class, () -> a.addElement("user"));
    }

    @Test
    void addElementCreatingLoopAllowedWhenDetectionOff() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("user.profile", false);
        ObjectAddress extended = a.addElement("user");
        assertEquals("user.profile.user", extended.toString());
    }

    // --- equals / hashCode ---

    @Test
    void equalsAndHashCodeByFields() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b.c");
        ObjectAddress b = new ObjectAddress("a.b.c");
        ObjectAddress c = new ObjectAddress("a.b.d");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "a.b.c");
    }

    @Test
    void equalsReflexive() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b");
        assertEquals(a, a);
    }

    // --- clone returns this (immutable) ---

    @Test
    void cloneReturnsSameInstance() throws ReflectionException {
        ObjectAddress a = new ObjectAddress("a.b");
        assertSame(a, a.clone());
    }

    @Test
    void constantsHaveExpectedValues() {
        assertEquals("#key", ObjectAddress.MAP_KEY_INDICATOR);
        assertEquals("#value", ObjectAddress.MAP_VALUE_INDICATOR);
        assertEquals(".", ObjectAddress.ELEMENT_SEPARATOR);
        assertTrue(true);
    }
}
