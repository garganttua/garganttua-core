package com.garganttua.core.bootstrap.banner;

import java.util.Map;

/**
 * Interface for components that contribute information to the bootstrap summary.
 *
 * <p>
 * Built objects can implement this interface to provide summary information
 * that will be displayed after bootstrap completes.
 * </p>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class MyContext implements IBootstrapSummaryContributor {
 *     @Override
 *     public String getSummaryCategory() {
 *         return "My Context";
 *     }
 *
 *     @Override
 *     public Map<String, String> getSummaryItems() {
 *         return Map.of(
 *             "Items loaded", String.valueOf(items.size()),
 *             "Cache enabled", String.valueOf(cacheEnabled)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IBootstrapSummaryContributor {

    /**
     * Returns the category name for this contributor's summary items.
     *
     * @return the category name (e.g., "Injection Context", "Expression Engine")
     */
    String getSummaryCategory();

    /**
     * Returns the summary items to display.
     *
     * @return a map of item names to values
     */
    Map<String, String> getSummaryItems();

}
