package com.garganttua.core.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

/**
 * A generic collector that aggregates items from multiple prioritized sources.
 *
 * <p>
 * {@code MultiSourceCollector} manages items from an unlimited number of sources,
 * each with a configurable priority. When multiple sources provide items with the
 * same key, the source with the lowest priority number wins (0 = highest priority).
 * </p>
 *
 * <h2>Priority System</h2>
 * <p>
 * Sources are ordered by priority number (ascending):
 * <ul>
 *   <li>Priority 0 - Highest priority (e.g., dependency injection context)</li>
 *   <li>Priority 1 - High priority (e.g., manually registered items)</li>
 *   <li>Priority 2+ - Lower priorities (e.g., reflection-based discovery)</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MultiSourceCollector<Class<?>, Factory> collector = new MultiSourceCollector<>();
 *
 * // Add context source (highest priority)
 * collector.source(() -> contextFactories, 0, "context");
 *
 * // Add manual source (medium priority)
 * collector.source(() -> manualFactories, 1, "manual");
 *
 * // Add reflection source (lowest priority)
 * collector.source(() -> reflectionFactories, 2, "reflection");
 *
 * // Build final map with all sources
 * Map<Class<?>, Factory> result = collector.build();
 *
 * // Build with selected sources only
 * Map<Class<?>, Factory> filtered = collector.buildWithSources(Set.of("context", "manual"));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is not thread-safe. It should be used by a single thread or properly synchronized.
 * </p>
 *
 * @param <K> the type of keys in the collected maps
 * @param <V> the type of values in the collected maps
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class MultiSourceCollector<K, V> {

    private static final String ERROR_SOURCE_PREFIX = "Source '";
    private static final String ERROR_EMPTY_RESULT = "' returned empty result";
    private static final String LOG_FAILED_RETRIEVE = "Failed to retrieve items from source '{}': {}";

    /**
     * Represents a named source of items with a priority.
     */
    private static class Source<K, V> {
        private final ISupplier<Map<K, V>> supplier;
        private final int priority;
        private final String name;

        Source(ISupplier<Map<K, V>> supplier, int priority, String name) {
            this.supplier = supplier;
            this.priority = priority;
            this.name = name;
        }

        Map<K, V> supply() {
            try {
                return supplier.supply()
                        .orElseThrow(() -> new SupplyException(ERROR_SOURCE_PREFIX + name + ERROR_EMPTY_RESULT));
            } catch (SupplyException e) {
                log.atError().log(LOG_FAILED_RETRIEVE, name, e.getMessage());
                throw new IllegalStateException(ERROR_SOURCE_PREFIX + name + "' failed to supply items", e);
            }
        }
    }

    private final List<Source<K, V>> sources = new ArrayList<>();

    /**
     * Registers a new source of items with a specified priority and name.
     *
     * <p>
     * Sources are processed in priority order (lowest number = highest priority).
     * The supplier is called lazily when building the final map.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // Context source (highest priority)
     * collector.source(() -> contextItems, 0, "context");
     *
     * // Manual source (medium priority)
     * collector.source(() -> manualItems, 1, "manual");
     *
     * // Reflection source (lowest priority)
     * collector.source(() -> reflectionItems, 2, "reflection");
     * }</pre>
     *
     * @param supplier a supplier that provides the map of items from this source
     * @param priority the priority of this source (0 = highest, lower numbers win conflicts)
     * @param name a unique name for this source (used for filtering and logging)
     * @return this collector for method chaining
     * @throws IllegalArgumentException if a source with the same name already exists
     */
    public MultiSourceCollector<K, V> source(ISupplier<Map<K, V>> supplier, int priority, String name) {
        log.atTrace().log("Entering source() with priority={}, name={}", priority, name);

        // Check for duplicate names
        if (sources.stream().anyMatch(s -> s.name.equals(name))) {
            throw new IllegalArgumentException("Source with name '" + name + "' already exists");
        }

        sources.add(new Source<>(supplier, priority, name));
        log.atDebug().log("Registered source '{}' with priority {}", name, priority);

        log.atTrace().log("Exiting source()");
        return this;
    }

    /**
     * Builds the final map with all registered sources, respecting priority order.
     *
     * <p>
     * All sources are evaluated and merged according to their priority.
     * When multiple sources provide the same key, the source with the lowest priority number wins.
     * </p>
     *
     * @return a map containing all collected items with priority resolution
     */
    public Map<K, V> build() {
        log.atTrace().log("Entering build()");
        List<Source<K, V>> sortedSources = sortSourcesByPriority(sources);
        Map<K, V> result = collectFromSources(sortedSources);
        log.atInfo().log("Built final map with {} items from {} sources", result.size(), sources.size());
        log.atTrace().log("Exiting build()");
        return result;
    }

    /**
     * Builds a map using only the specified sources, filtering out items from excluded sources.
     *
     * <p>
     * This method allows selective inclusion of sources by name. Items from excluded sources
     * are completely ignored. Among included sources, priority order is still respected.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // Include only context and manual sources, exclude reflection
     * Map<Class<?>, Factory> filtered = collector.buildWithSources(
     *     Set.of("context", "manual")
     * );
     * }</pre>
     *
     * @param includedSourceNames the names of sources to include in the build
     * @return a map containing items only from the specified sources
     * @throws IllegalArgumentException if any specified source name does not exist
     */
    public Map<K, V> buildWithSources(Set<String> includedSourceNames) {
        log.atTrace().log("Entering buildWithSources() with {} source names", includedSourceNames.size());
        validateSourceNames(includedSourceNames);
        List<Source<K, V>> filteredSources = filterAndSortSources(includedSourceNames);
        Map<K, V> result = collectFromSources(filteredSources);
        log.atInfo().log("Built filtered map with {} items from {} sources (excluded {} sources)",
                result.size(), filteredSources.size(), sources.size() - filteredSources.size());
        log.atTrace().log("Exiting buildWithSources()");
        return result;
    }

    /**
     * Builds a map excluding items that would be provided by excluded sources.
     *
     * <p>
     * This method includes all sources except those specified, but also filters out
     * any keys that appear in the excluded sources. This is useful when you want to
     * know which items to add to a specific source without duplicating items already
     * present in that source.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // Get items to add to context, excluding items already in context
     * Map<Class<?>, Factory> toAddToContext = collector.buildExcludingSourceItems(
     *     Set.of("context")
     * );
     * }</pre>
     *
     * @param excludedSourceNames the names of sources whose items should be filtered out
     * @return a map with items from included sources, excluding keys present in excluded sources
     * @throws IllegalArgumentException if any specified source name does not exist
     */
    public Map<K, V> buildExcludingSourceItems(Set<String> excludedSourceNames) {
        log.atTrace().log("Entering buildExcludingSourceItems() with {} excluded sources", excludedSourceNames.size());
        validateSourceNames(excludedSourceNames);
        Set<K> excludedKeys = collectKeysFromSources(excludedSourceNames);
        List<Source<K, V>> includedSources = filterAndSortSourcesExcluding(excludedSourceNames);
        Map<K, V> result = collectFromSourcesExcludingKeys(includedSources, excludedKeys);
        log.atInfo().log("Built filtered map with {} items (excluded {} keys from {} sources)",
                result.size(), excludedKeys.size(), excludedSourceNames.size());
        log.atTrace().log("Exiting buildExcludingSourceItems()");
        return result;
    }

    /**
     * Returns the number of registered sources.
     *
     * @return the count of sources
     */
    public int getSourceCount() {
        return sources.size();
    }

    /**
     * Returns the names of all registered sources.
     *
     * @return a list of source names in registration order
     */
    public List<String> getSourceNames() {
        return sources.stream()
                .map(s -> s.name)
                .toList();
    }

    /**
     * Returns the priority of a specific source.
     *
     * @param sourceName the name of the source
     * @return the priority of the source
     * @throws IllegalArgumentException if the source does not exist
     */
    public int getSourcePriority(String sourceName) {
        return sources.stream()
                .filter(s -> s.name.equals(sourceName))
                .findFirst()
                .map(s -> s.priority)
                .orElseThrow(() -> new IllegalArgumentException("Source with name '" + sourceName + "' does not exist"));
    }

    // Private helper methods

    private void validateSourceNames(Set<String> sourceNames) {
        for (String sourceName : sourceNames) {
            if (sources.stream().noneMatch(s -> s.name.equals(sourceName))) {
                throw new IllegalArgumentException("Source with name '" + sourceName + "' does not exist");
            }
        }
    }

    private List<Source<K, V>> sortSourcesByPriority(List<Source<K, V>> sourcesToSort) {
        return sourcesToSort.stream()
                .sorted((s1, s2) -> Integer.compare(s2.priority, s1.priority))
                .toList();
    }

    private List<Source<K, V>> filterAndSortSources(Set<String> includedSourceNames) {
        return sources.stream()
                .filter(s -> includedSourceNames.contains(s.name))
                .sorted((s1, s2) -> Integer.compare(s2.priority, s1.priority))
                .toList();
    }

    private List<Source<K, V>> filterAndSortSourcesExcluding(Set<String> excludedSourceNames) {
        return sources.stream()
                .filter(s -> !excludedSourceNames.contains(s.name))
                .sorted((s1, s2) -> Integer.compare(s2.priority, s1.priority))
                .toList();
    }

    private Map<K, V> collectFromSources(List<Source<K, V>> sourcesToCollect) {
        log.atDebug().log("Processing {} sources in priority order", sourcesToCollect.size());
        Map<K, V> result = new HashMap<>();

        for (Source<K, V> source : sourcesToCollect) {
            Map<K, V> items = source.supply();
            int beforeSize = result.size();
            result.putAll(items);
            int addedCount = result.size() - beforeSize;

            log.atDebug().log("Processed source '{}' (priority {}): {} items, {} new keys added",
                    source.name, source.priority, items.size(), addedCount);
        }

        return result;
    }

    private Set<K> collectKeysFromSources(Set<String> sourceNames) {
        Set<K> collectedKeys = new HashSet<>();

        for (Source<K, V> source : sources) {
            if (sourceNames.contains(source.name)) {
                Map<K, V> items = source.supply();
                collectedKeys.addAll(items.keySet());
            }
        }

        log.atDebug().log("Found {} keys to exclude from {} sources", collectedKeys.size(), sourceNames.size());
        return collectedKeys;
    }

    private Map<K, V> collectFromSourcesExcludingKeys(List<Source<K, V>> sourcesToCollect, Set<K> keysToExclude) {
        log.atDebug().log("Processing {} included sources", sourcesToCollect.size());
        Map<K, V> result = new HashMap<>();

        for (Source<K, V> source : sourcesToCollect) {
            Map<K, V> items = source.supply();
            int beforeSize = result.size();

            items.entrySet().stream()
                    .filter(e -> !keysToExclude.contains(e.getKey()))
                    .forEach(e -> result.put(e.getKey(), e.getValue()));

            int addedCount = result.size() - beforeSize;

            log.atDebug().log("Processed source '{}' (priority {}): {} items, {} new keys added (filtered by excluded keys)",
                    source.name, source.priority, items.size(), addedCount);
        }

        return result;
    }
}
