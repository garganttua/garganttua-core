package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link RuntimeProcess}: defensive copy of the supplied
 * stage map, unmodifiability of the nested maps, and the {@link RuntimeProcess#print()}
 * rendering.
 */
class RuntimeProcessBehaviourTest {

    private Map<String, Map<String, String>> sampleStages() {
        Map<String, Map<String, String>> stages = new LinkedHashMap<>();
        Map<String, String> stage1 = new LinkedHashMap<>();
        stage1.put("step-1", "var-1");
        stage1.put("step-2", "var-2");
        Map<String, String> stage2 = new LinkedHashMap<>();
        stage2.put("step-3", "var-3");
        stages.put("stage-A", stage1);
        stages.put("stage-B", stage2);
        return stages;
    }

    @Test
    void constructor_defensivelyCopies_soLaterMutationsToSourceMapDoNotLeak() {
        Map<String, Map<String, String>> source = sampleStages();
        RuntimeProcess process = new RuntimeProcess(source);

        // Mutate the source after construction.
        source.clear();
        source.put("injected", new HashMap<>(Map.of("x", "y")));

        // The print output must still reflect the original two stages, proving
        // the constructor copied rather than referenced the source.
        String out = capturePrint(process);
        assertTrue(out.contains("Stage: stage-A"), "stage-A should survive source mutation");
        assertTrue(out.contains("Stage: stage-B"), "stage-B should survive source mutation");
        assertTrue(!out.contains("injected"), "injected stage must not appear");
    }

    @Test
    void print_rendersHeaderStagesStepsAndVariables_inInsertionOrder() {
        RuntimeProcess process = new RuntimeProcess(sampleStages());

        String out = capturePrint(process);

        assertTrue(out.contains("==== Runtime Process ===="), "must print header");
        assertTrue(out.contains("========================="), "must print footer");
        assertTrue(out.contains("Stage: stage-A"));
        assertTrue(out.contains("  Step: step-1 -> Variable: var-1"));
        assertTrue(out.contains("  Step: step-2 -> Variable: var-2"));
        assertTrue(out.contains("Stage: stage-B"));
        assertTrue(out.contains("  Step: step-3 -> Variable: var-3"));

        // Insertion order: stage-A must be printed before stage-B.
        assertTrue(out.indexOf("Stage: stage-A") < out.indexOf("Stage: stage-B"),
                "stages should render in insertion order");
        // step-1 must precede step-2 within stage-A.
        assertTrue(out.indexOf("step-1") < out.indexOf("step-2"),
                "steps should render in insertion order");
    }

    @Test
    void print_withEmptyStages_stillRendersHeaderAndFooter() {
        RuntimeProcess process = new RuntimeProcess(new LinkedHashMap<>());

        String out = capturePrint(process);

        assertTrue(out.contains("==== Runtime Process ===="));
        assertTrue(out.contains("========================="));
        assertTrue(!out.contains("Stage:"), "no stage should be rendered for empty map");
    }

    @Test
    void constructor_withNullStages_throwsNullPointerException() {
        // RuntimeProcess.<init> calls stages.forEach(...) on the argument.
        assertThrows(NullPointerException.class, () -> new RuntimeProcess(null));
    }

    private String capturePrint(RuntimeProcess process) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            process.print();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
