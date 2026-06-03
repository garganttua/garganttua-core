package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;
import com.garganttua.core.reflection.ObjectAddress;

class MappingRuleBehaviourTest {

    private MappingRule rule(String src, String dst) throws Exception {
        IClass<?> str = JdkClass.of(String.class);
        return new MappingRule(new ObjectAddress(src), new ObjectAddress(dst), str, null, null);
    }

    @Test
    void equalSameComponents() throws Exception {
        assertEquals(rule("a.b", "c.d"), rule("a.b", "c.d"));
    }

    @Test
    void notEqualDifferentDestination() throws Exception {
        assertNotEquals(rule("a.b", "c.d"), rule("a.b", "c.e"));
    }

    @Test
    void hashCodeConsistentWithEquals() throws Exception {
        assertEquals(rule("a.b", "c.d").hashCode(), rule("a.b", "c.d").hashCode());
    }

    @Test
    void toStringMentionsAddresses() throws Exception {
        String s = rule("user.email", "dto.addr").toString();
        assertTrue(s.contains("user.email"));
        assertTrue(s.contains("dto.addr"));
    }

    @Test
    void transformMethodsParticipateInEquality() throws Exception {
        IClass<?> str = JdkClass.of(String.class);
        MappingRule withTransform = new MappingRule(
                new ObjectAddress("a"), new ObjectAddress("b"), str,
                new ObjectAddress("Util.fwd"), null);
        MappingRule without = new MappingRule(
                new ObjectAddress("a"), new ObjectAddress("b"), str, null, null);
        assertNotEquals(withTransform, without);
    }
}
