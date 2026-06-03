package com.garganttua.core.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;
import com.garganttua.core.reflection.ObjectAddress;

class MappingConfigurationBehaviourTest {

    private MappingRule rule() throws Exception {
        return new MappingRule(new ObjectAddress("a"), new ObjectAddress("b"),
                JdkClass.of(String.class), null, null);
    }

    @Test
    void equalityIgnoresRulesAndDirection() throws Exception {
        IClass<?> src = JdkClass.of(String.class);
        IClass<?> dst = JdkClass.of(Integer.class);
        MappingConfiguration a = new MappingConfiguration(src, dst,
                List.of(rule()), List.of(), MappingDirection.REGULAR);
        MappingConfiguration b = new MappingConfiguration(src, dst,
                List.of(), List.of(rule()), MappingDirection.REVERSE);
        // equals is intentionally only on source+destination
        assertEquals(a, b);
    }

    @Test
    void notEqualDifferentSource() throws Exception {
        IClass<?> dst = JdkClass.of(Integer.class);
        MappingConfiguration a = new MappingConfiguration(JdkClass.of(String.class), dst,
                List.of(), List.of(), MappingDirection.REGULAR);
        MappingConfiguration b = new MappingConfiguration(JdkClass.of(Long.class), dst,
                List.of(), List.of(), MappingDirection.REGULAR);
        assertNotEquals(a, b);
    }

    @Test
    void toStringContainsDirectionAndClasses() throws Exception {
        MappingConfiguration a = new MappingConfiguration(JdkClass.of(String.class),
                JdkClass.of(Integer.class), List.of(), List.of(), MappingDirection.REVERSE);
        String s = a.toString();
        assertTrue(s.contains("REVERSE"));
        assertTrue(s.contains("MappingConfiguration"));
    }

    @Test
    void accessorsExposeComponents() throws Exception {
        IClass<?> src = JdkClass.of(String.class);
        MappingConfiguration a = new MappingConfiguration(src, JdkClass.of(Integer.class),
                List.of(rule()), List.of(), MappingDirection.REGULAR);
        assertEquals(src, a.source());
        assertEquals(1, a.sourceRules().size());
        assertEquals(MappingDirection.REGULAR, a.mappingDirection());
    }
}
