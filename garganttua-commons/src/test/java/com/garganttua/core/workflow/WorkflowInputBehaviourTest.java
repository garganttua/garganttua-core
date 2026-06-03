package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkflowInputBehaviourTest {

    @Test
    void nullParametersNormalisedToEmpty() {
        WorkflowInput in = new WorkflowInput("payload", null);
        assertEquals("payload", in.payload());
        assertTrue(in.parameters().isEmpty());
    }

    @Test
    void providedParametersAreUnmodifiable() {
        Map<String, Object> p = new HashMap<>();
        p.put("k", "v");
        WorkflowInput in = new WorkflowInput(null, p);
        assertEquals("v", in.parameters().get("k"));
        assertThrows(UnsupportedOperationException.class, () -> in.parameters().put("x", 1));
    }

    @Test
    void ofPayloadOnlyHasNoParameters() {
        WorkflowInput in = WorkflowInput.of("p");
        assertEquals("p", in.payload());
        assertTrue(in.parameters().isEmpty());
    }

    @Test
    void ofPayloadAndParamsRetainsEntries() {
        WorkflowInput in = WorkflowInput.of("p", Map.of("a", 1, "b", 2));
        assertEquals(2, in.parameters().size());
        assertEquals(1, in.parameters().get("a"));
    }

    @Test
    void emptyHasNoPayloadNoParams() {
        WorkflowInput in = WorkflowInput.empty();
        assertNull(in.payload());
        assertTrue(in.parameters().isEmpty());
    }

    @Test
    void ofWithNullParamsMapNormalisesToEmpty() {
        WorkflowInput in = WorkflowInput.of("p", null);
        assertTrue(in.parameters().isEmpty());
    }
}
