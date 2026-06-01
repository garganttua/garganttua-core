package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkflowExecutionOptionsTest {

    @Test
    void noneHasNoExecutionIdAndNoFiltering() {
        WorkflowExecutionOptions none = WorkflowExecutionOptions.none();
        assertTrue(none.executionId().isEmpty());
        assertFalse(none.hasFiltering());
    }

    @Test
    void pinnedExecutionIdIsCarriedButDoesNotCountAsFiltering() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        WorkflowExecutionOptions opts = WorkflowExecutionOptions.builder()
                .executionId(id)
                .build();

        assertEquals(id, opts.executionId().orElseThrow());
        // Critical: pinning an id must NOT engage the filtering path, otherwise
        // Workflow.execute would re-generate the script and bypass the
        // precompiled cache on every correlated call.
        assertFalse(opts.hasFiltering());
    }

    @Test
    void filteringStillDetectedIndependentlyOfExecutionId() {
        WorkflowExecutionOptions opts = WorkflowExecutionOptions.builder()
                .startFrom("s1")
                .executionId(UUID.randomUUID())
                .build();
        assertTrue(opts.hasFiltering());
    }

    @Test
    void nullExecutionIdNormalisesToEmpty() {
        WorkflowExecutionOptions opts = new WorkflowExecutionOptions(null, null, null, null);
        assertTrue(opts.executionId().isEmpty());
    }
}
