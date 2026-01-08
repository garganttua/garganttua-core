package com.garganttua.core.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Test class for {@link MultiSourceCollector}.
 */
class MultiSourceCollectorTest {

    private MultiSourceCollector<String, String> collector;
    private Map<String, String> contextMap;
    private Map<String, String> manualMap;
    private Map<String, String> reflectionMap;

    @BeforeEach
    void setUp() {
        collector = new MultiSourceCollector<>();

        // Setup test data
        contextMap = new HashMap<>();
        contextMap.put("key1", "context-value1");
        contextMap.put("key2", "context-value2");

        manualMap = new HashMap<>();
        manualMap.put("key2", "manual-value2");
        manualMap.put("key3", "manual-value3");

        reflectionMap = new HashMap<>();
        reflectionMap.put("key3", "reflection-value3");
        reflectionMap.put("key4", "reflection-value4");
    }

    private ISupplier<Map<String, String>> supplier(Map<String, String> map) {
        return new ISupplier<Map<String, String>>() {
            @Override
            public Optional<Map<String, String>> supply() throws SupplyException {
                return Optional.of(map);
            }

            @Override
            public Type getSuppliedType() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
            }
        };
    }

    @Test
    void testBuildWithSingleSource() {
        collector.source(supplier(contextMap), 0, "context");

        Map<String, String> result = collector.build();

        assertEquals(2, result.size());
        assertEquals("context-value1", result.get("key1"));
        assertEquals("context-value2", result.get("key2"));
    }

    @Test
    void testBuildWithMultipleSources_PriorityRespected() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(reflectionMap), 2, "reflection");

        Map<String, String> result = collector.build();

        assertEquals(4, result.size());
        assertEquals("context-value1", result.get("key1"));
        assertEquals("context-value2", result.get("key2")); // Context wins over manual
        assertEquals("manual-value3", result.get("key3"));   // Manual wins over reflection
        assertEquals("reflection-value4", result.get("key4"));
    }

    @Test
    void testBuildWithReversePriority() {
        // Reflection has highest priority (0)
        collector.source(supplier(reflectionMap), 0, "reflection");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(contextMap), 2, "context");

        Map<String, String> result = collector.build();

        assertEquals(4, result.size());
        assertEquals("context-value1", result.get("key1"));
        assertEquals("manual-value2", result.get("key2"));
        assertEquals("reflection-value3", result.get("key3")); // Reflection wins
        assertEquals("reflection-value4", result.get("key4"));
    }

    @Test
    void testBuildWithSources_IncludeSpecificSources() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(reflectionMap), 2, "reflection");

        Map<String, String> result = collector.buildWithSources(Set.of("context", "manual"));

        assertEquals(3, result.size());
        assertEquals("context-value1", result.get("key1"));
        assertEquals("context-value2", result.get("key2"));
        assertEquals("manual-value3", result.get("key3"));
        assertFalse(result.containsKey("key4")); // Reflection excluded
    }

    @Test
    void testBuildExcludingSourceItems() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(reflectionMap), 2, "reflection");

        // Exclude keys from context source
        Map<String, String> result = collector.buildExcludingSourceItems(Set.of("context"));

        assertEquals(2, result.size());
        assertFalse(result.containsKey("key1")); // Excluded (in context)
        assertFalse(result.containsKey("key2")); // Excluded (in context)
        assertEquals("manual-value3", result.get("key3"));
        assertEquals("reflection-value4", result.get("key4"));
    }

    @Test
    void testSourceWithDuplicateName_ThrowsException() {
        collector.source(supplier(contextMap), 0, "context");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            collector.source(supplier(manualMap), 1, "context");
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testBuildWithSources_InvalidSourceName_ThrowsException() {
        collector.source(supplier(contextMap), 0, "context");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            collector.buildWithSources(Set.of("invalid-source"));
        });

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void testBuildExcludingSourceItems_InvalidSourceName_ThrowsException() {
        collector.source(supplier(contextMap), 0, "context");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            collector.buildExcludingSourceItems(Set.of("invalid-source"));
        });

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void testGetSourceCount() {
        assertEquals(0, collector.getSourceCount());

        collector.source(supplier(contextMap), 0, "context");
        assertEquals(1, collector.getSourceCount());

        collector.source(supplier(manualMap), 1, "manual");
        assertEquals(2, collector.getSourceCount());
    }

    @Test
    void testGetSourceNames() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(reflectionMap), 2, "reflection");

        var names = collector.getSourceNames();

        assertEquals(3, names.size());
        assertEquals("context", names.get(0));
        assertEquals("manual", names.get(1));
        assertEquals("reflection", names.get(2));
    }

    @Test
    void testGetSourcePriority() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 5, "manual");
        collector.source(supplier(reflectionMap), 10, "reflection");

        assertEquals(0, collector.getSourcePriority("context"));
        assertEquals(5, collector.getSourcePriority("manual"));
        assertEquals(10, collector.getSourcePriority("reflection"));
    }

    @Test
    void testGetSourcePriority_InvalidSource_ThrowsException() {
        collector.source(supplier(contextMap), 0, "context");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            collector.getSourcePriority("invalid-source");
        });

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void testEmptyCollector() {
        Map<String, String> result = collector.build();

        assertTrue(result.isEmpty());
    }

    @Test
    void testLazyEvaluation() {
        final boolean[] supplierCalled = {false};

        collector.source(new ISupplier<Map<String, String>>() {
            @Override
            public Optional<Map<String, String>> supply() throws SupplyException {
                supplierCalled[0] = true;
                return Optional.of(contextMap);
            }

            @Override
            public Type getSuppliedType() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
            }
        }, 0, "context");

        assertFalse(supplierCalled[0], "Supplier should not be called during registration");

        collector.build();

        assertTrue(supplierCalled[0], "Supplier should be called during build");
    }

    @Test
    void testMultipleBuildCalls() {
        final int[] callCount = {0};

        collector.source(new ISupplier<Map<String, String>>() {
            @Override
            public Optional<Map<String, String>> supply() throws SupplyException {
                callCount[0]++;
                return Optional.of(contextMap);
            }

            @Override
            public Type getSuppliedType() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
            }
        }, 0, "context");

        collector.build();
        assertEquals(1, callCount[0]);

        collector.build();
        assertEquals(2, callCount[0], "Supplier should be called for each build");
    }

    @Test
    void testComplexScenarioWithOverlappingKeys() {
        Map<String, String> source1 = new HashMap<>();
        source1.put("a", "s1-a");
        source1.put("b", "s1-b");
        source1.put("c", "s1-c");

        Map<String, String> source2 = new HashMap<>();
        source2.put("b", "s2-b");
        source2.put("c", "s2-c");
        source2.put("d", "s2-d");

        Map<String, String> source3 = new HashMap<>();
        source3.put("c", "s3-c");
        source3.put("d", "s3-d");
        source3.put("e", "s3-e");

        collector.source(supplier(source1), 0, "source1");
        collector.source(supplier(source2), 1, "source2");
        collector.source(supplier(source3), 2, "source3");

        Map<String, String> result = collector.build();

        assertEquals(5, result.size());
        assertEquals("s1-a", result.get("a"));
        assertEquals("s1-b", result.get("b")); // source1 wins
        assertEquals("s1-c", result.get("c")); // source1 wins
        assertEquals("s2-d", result.get("d")); // source2 wins over source3
        assertEquals("s3-e", result.get("e"));
    }

    @Test
    void testBuildExcludingMultipleSources() {
        collector.source(supplier(contextMap), 0, "context");
        collector.source(supplier(manualMap), 1, "manual");
        collector.source(supplier(reflectionMap), 2, "reflection");

        Map<String, String> result = collector.buildExcludingSourceItems(Set.of("context", "manual"));

        assertEquals(1, result.size());
        assertFalse(result.containsKey("key1"));
        assertFalse(result.containsKey("key2"));
        assertFalse(result.containsKey("key3"));
        assertEquals("reflection-value4", result.get("key4"));
    }

    @Test
    void testChainedMethodCalls() {
        Map<String, String> result = collector
                .source(supplier(contextMap), 0, "context")
                .source(supplier(manualMap), 1, "manual")
                .source(supplier(reflectionMap), 2, "reflection")
                .build();

        assertEquals(4, result.size());
    }
}
