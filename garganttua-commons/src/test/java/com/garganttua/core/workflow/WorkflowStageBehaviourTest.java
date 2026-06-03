package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.workflow.WorkflowScript.ScriptSource;

class WorkflowStageBehaviourTest {

    private WorkflowScript script(String name) {
        return WorkflowScript.builder().name(name).source(ScriptSource.of("x")).build();
    }

    @Test
    void nullScriptsBecomeEmpty() {
        WorkflowStage s = new WorkflowStage("st", null, null, null, null, null);
        assertTrue(s.scripts().isEmpty());
    }

    @Test
    void scriptsAreDefensivelyCopiedAndUnmodifiable() {
        List<WorkflowScript> list = new ArrayList<>();
        list.add(script("a"));
        WorkflowStage s = new WorkflowStage("st", list, null, null, null, null);
        list.add(script("b")); // mutate after construction
        assertEquals(1, s.scripts().size());
        assertThrows(UnsupportedOperationException.class, () -> s.scripts().add(script("c")));
    }

    @Test
    void hasWrapReflectsNonEmptyWrapExpression() {
        assertFalse(new WorkflowStage("st", null, null, null, null, null).hasWrap());
        assertFalse(new WorkflowStage("st", null, "", null, null, null).hasWrap());
        assertTrue(new WorkflowStage("st", null, "retry(3,@0)", null, null, null).hasWrap());
    }

    @Test
    void hasCatchTrueForEitherCatchKind() {
        assertFalse(new WorkflowStage("st", null, null, null, null, null).hasCatch());
        assertTrue(new WorkflowStage("st", null, null, "imm()", null, null).hasCatch());
        assertTrue(new WorkflowStage("st", null, null, null, "down()", null).hasCatch());
        assertFalse(new WorkflowStage("st", null, null, "", "", null).hasCatch());
    }

    @Test
    void ofListFactoryHasNoWrapNoCatch() {
        WorkflowStage s = WorkflowStage.of("st", List.of(script("a")));
        assertEquals(1, s.scripts().size());
        assertFalse(s.hasWrap());
        assertFalse(s.hasCatch());
    }

    @Test
    void ofVarargsFactoryPreservesOrder() {
        WorkflowStage s = WorkflowStage.of("st", script("a"), script("b"));
        assertEquals(2, s.scripts().size());
        assertEquals("a", s.scripts().get(0).getName());
        assertEquals("b", s.scripts().get(1).getName());
    }
}
