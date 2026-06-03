package com.garganttua.core.configuration.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

import com.garganttua.core.configuration.binding.KeyedChildFixtures.Item;
import com.garganttua.core.configuration.binding.KeyedChildFixtures.ParentBuilder;
import com.garganttua.core.configuration.binding.KeyedChildFixtures.Strategy;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;

/**
 * Verifies the populator can traverse keyed child-builders with arguments — the shape needed to
 * declare beans from config: {@code provider(String)} → {@code item(IClass)} → {@code strategy(enum)},
 * with String/IClass argument conversion and ascent via {@code up()} (provider) then {@code and()} (item).
 */
class KeyedChildBuilderTest {

    @BeforeEach
    void installReflection() throws Exception {
        // IClass.forName (String -> IClass argument conversion) needs the global IReflection.
        ReflectionBuilder.builder().withProvider(new JdkReflectionProvider()).build();
    }

    private static ConfigurationApplier applier() {
        return new ConfigurationApplier(
                new BuilderPopulator(List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false));
    }

    @Test
    void declaresKeyedChildrenWithArgsFromConfig() throws Exception {
        String json = """
                {
                  "$module": "demo",
                  "provider": {
                    "app": {
                      "item": {
                        "java.lang.String":  { "strategy": "singleton" },
                        "java.lang.Integer": { "strategy": "prototype" }
                      }
                    }
                  }
                }
                """;

        ParentBuilder parent = applier().apply(new ParentBuilder(), new StringConfigurationSource(json, "json"));

        assertEquals(
                List.of(
                        new Item("app", "java.lang.String", Strategy.singleton),
                        new Item("app", "java.lang.Integer", Strategy.prototype)),
                parent.items());
    }

    @Test
    void multipleProvidersEachWithItems() throws Exception {
        String json = """
                {
                  "$module": "demo",
                  "provider": {
                    "a": { "item": { "java.lang.String": { "strategy": "prototype" } } },
                    "b": { "item": { "java.lang.Long":   { "strategy": "singleton" } } }
                  }
                }
                """;

        ParentBuilder parent = applier().apply(new ParentBuilder(), new StringConfigurationSource(json, "json"));

        assertEquals(2, parent.providers.size());
        assertEquals(
                List.of(
                        new Item("a", "java.lang.String", Strategy.prototype),
                        new Item("b", "java.lang.Long", Strategy.singleton)),
                parent.items());
    }

}
