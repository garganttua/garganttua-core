package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class RuntimeStepOperationPositionBehaviourTest {

    @Test
    void afterFactorySetsAfterPositionAndElement() {
        IClass<?> type = JdkClass.of(String.class);
        RuntimeStepOperationPosition p = RuntimeStepOperationPosition.after(type);
        assertEquals(Position.AFTER, p.position());
        assertSame(type, p.element());
    }

    @Test
    void beforeFactorySetsBeforePosition() {
        IClass<?> type = JdkClass.of(Integer.class);
        RuntimeStepOperationPosition p = RuntimeStepOperationPosition.before(type);
        assertEquals(Position.BEFORE, p.position());
        assertSame(type, p.element());
    }

    @Test
    void equalitySamePositionSameElement() {
        IClass<?> a = JdkClass.of(String.class);
        assertEquals(RuntimeStepOperationPosition.after(a), RuntimeStepOperationPosition.after(JdkClass.of(String.class)));
    }

    @Test
    void differentPositionNotEqual() {
        IClass<?> a = JdkClass.of(String.class);
        assertNotEquals(RuntimeStepOperationPosition.after(a), RuntimeStepOperationPosition.before(JdkClass.of(String.class)));
    }
}
