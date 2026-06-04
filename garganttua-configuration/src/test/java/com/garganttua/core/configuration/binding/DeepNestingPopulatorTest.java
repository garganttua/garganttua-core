package com.garganttua.core.configuration.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

/**
 * Proves the populator recurses to arbitrary depth (here depth 5) while mixing all four supported
 * method shapes — scalar setter, no-arg child-builder opener, keyed-child opener (String + IClass
 * keys), and keyed-scalar setter — and both ascent styles ({@code up()} and {@code and()}).
 * Every leaf at every level is asserted via {@link DeepNestingFixtures.RootBuilder}'s flat sink.
 */
@DisplayName("Populator drives builders at arbitrary depth, all method shapes")
class DeepNestingPopulatorTest {

    @BeforeEach
    void installReflection() throws Exception {
        // String -> IClass conversion for the entry(IClass) keyed-child needs the global IReflection.
        ReflectionBuilder.builder().withProvider(new JdkReflectionProvider()).build();
    }

    private static ConfigurationApplier applier() {
        return new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, true));
    }

    @Test
    void everyLeafAtEveryDepthLands() throws Exception {
        String json = """
                {
                  "$module": "deep",
                  "title": "root-title",
                  "verbose": true,
                  "group": {
                    "main": {
                      "label": "main-label",
                      "settings": {
                        "mode": "safe",
                        "option": { "x": "1", "y": "2" },
                        "advanced": {
                          "threshold": "7",
                          "flag": { "fast": "on" },
                          "tuning": {
                            "level": "high",
                            "param": { "alpha": "A", "beta": "B" }
                          }
                        }
                      },
                      "entry": {
                        "java.lang.String":  { "weight": "5" },
                        "java.lang.Integer": { "weight": "9" }
                      }
                    }
                  }
                }
                """;

        DeepNestingFixtures.RootBuilder root = applier()
                .apply(new DeepNestingFixtures.RootBuilder(), new StringConfigurationSource(json, "json"));
        Map<String, String> sink = root.build();

        // depth 0 scalar + 0-arg flag setter
        assertEquals("root-title", sink.get("title"));
        assertEquals("true", sink.get("verbose"));
        // depth 1 (keyed-child group) scalar
        assertEquals("main-label", sink.get("group.main.label"));
        // depth 2 (child-builder settings): scalar enum + keyed-scalar option
        assertEquals("safe", sink.get("group.main.settings.mode"));
        assertEquals("1", sink.get("group.main.settings.option.x"));
        assertEquals("2", sink.get("group.main.settings.option.y"));
        // depth 3 (child-builder advanced): scalar int + keyed-scalar flag
        assertEquals("7", sink.get("group.main.settings.advanced.threshold"));
        assertEquals("on", sink.get("group.main.settings.advanced.flag.fast"));
        // depth 4 (child-builder tuning, deepest): scalar + keyed-scalar param
        assertEquals("high", sink.get("group.main.settings.advanced.tuning.level"));
        assertEquals("A", sink.get("group.main.settings.advanced.tuning.param.alpha"));
        assertEquals("B", sink.get("group.main.settings.advanced.tuning.param.beta"));
        // parallel keyed-child branch via entry(IClass), ascending through and()
        assertEquals("5", sink.get("group.main.entry.java.lang.String.weight"));
        assertEquals("9", sink.get("group.main.entry.java.lang.Integer.weight"));

        // exactly the 13 leaves above — nothing dropped, nothing spurious
        assertEquals(13, sink.size(), () -> "unexpected sink contents: " + sink);
    }

    @Test
    @DisplayName("multiple sibling keyed children at the same depth are all traversed")
    void multipleSiblingGroupsTraversed() throws Exception {
        String json = """
                {
                  "$module": "deep",
                  "group": {
                    "a": { "settings": { "option": { "k": "va" } } },
                    "b": { "settings": { "option": { "k": "vb" } } }
                  }
                }
                """;

        DeepNestingFixtures.RootBuilder root = applier()
                .apply(new DeepNestingFixtures.RootBuilder(), new StringConfigurationSource(json, "json"));
        Map<String, String> sink = root.build();

        assertTrue(sink.containsKey("group.a.settings.option.k"));
        assertEquals("va", sink.get("group.a.settings.option.k"));
        assertEquals("vb", sink.get("group.b.settings.option.k"));
    }
}
