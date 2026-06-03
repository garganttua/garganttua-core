package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class RuntimeStepPositionBehaviourTest {

    @Test
    void afterFactoryUsesAfterPosition() {
        RuntimeStepPosition p = RuntimeStepPosition.after("validation");
        assertEquals(Position.AFTER, p.position());
        assertEquals("validation", p.elementName());
    }

    @Test
    void beforeFactoryUsesBeforePosition() {
        RuntimeStepPosition p = RuntimeStepPosition.before("processing");
        assertEquals(Position.BEFORE, p.position());
        assertEquals("processing", p.elementName());
    }

    @Test
    void recordEqualityBasedOnComponents() {
        assertEquals(RuntimeStepPosition.after("x"), RuntimeStepPosition.after("x"));
        assertNotEquals(RuntimeStepPosition.after("x"), RuntimeStepPosition.before("x"));
        assertNotEquals(RuntimeStepPosition.after("x"), RuntimeStepPosition.after("y"));
    }

    @Test
    void operationPositionAfterFactory() {
        IClass<?> type = JdkClass.of(String.class);
        RuntimeStepOperationPosition p = RuntimeStepOperationPosition.after(type);
        assertEquals(Position.AFTER, p.position());
        assertSame(type, p.element());
    }

    @Test
    void operationPositionBeforeFactory() {
        IClass<?> type = JdkClass.of(Integer.class);
        RuntimeStepOperationPosition p = RuntimeStepOperationPosition.before(type);
        assertEquals(Position.BEFORE, p.position());
        assertSame(type, p.element());
    }

    @Test
    void positionEnumHasTwoValues() {
        assertEquals(2, Position.values().length);
        assertEquals(Position.AFTER, Position.valueOf("AFTER"));
        assertEquals(Position.BEFORE, Position.valueOf("BEFORE"));
    }
}
