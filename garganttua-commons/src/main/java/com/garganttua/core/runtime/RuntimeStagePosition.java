package com.garganttua.core.runtime;

/**
 * Immutable record specifying the relative position for inserting a stage within a runtime workflow.
 *
 * <p>
 * RuntimeStagePosition allows precise control over stage ordering when building runtimes
 * programmatically using the DSL. Instead of always appending stages at the end, you can
 * insert them before or after existing stages by name.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IRuntimeBuilder<Order, OrderResult> builder = runtimesBuilder
 *     .runtime("orderProcessing", Order.class, OrderResult.class)
 *         .stage("validation")
 *             // ... steps ...
 *             .end()
 *         .stage("processing")
 *             // ... steps ...
 *             .end()
 *         // Insert a new stage before "processing"
 *         .stage("enrichment", RuntimeStagePosition.before("processing"))
 *             .step("enrichData", () -> new DataEnricher(), Void.class)
 *                 .method().name("enrich").end()
 *                 .end()
 *             .end()
 *         // Insert a new stage after "validation"
 *         .stage("authorization", RuntimeStagePosition.after("validation"))
 *             .step("checkPermissions", () -> new AuthChecker(), Void.class)
 *                 .method().name("check").end()
 *                 .end()
 *             .end();
 * }</pre>
 *
 * @param position whether to insert before or after the reference element
 * @param elementName the name of the existing stage to position relative to
 * @since 2.0.0-ALPHA01
 * @see Position
 * @see RuntimeStepPosition
 * @see com.garganttua.core.runtime.dsl.IRuntimeBuilder#stage(String, com.garganttua.core.utils.OrderedMapPosition)
 */
public record RuntimeStagePosition(Position position,
        String elementName) {

    /**
     * Creates a position specifying insertion after the named stage.
     *
     * @param elementName the name of the stage to insert after
     * @return a new RuntimeStagePosition for after-insertion
     */
    public static RuntimeStagePosition after(String elementName) {
        return new RuntimeStagePosition(Position.AFTER, elementName);
    }

    /**
     * Creates a position specifying insertion before the named stage.
     *
     * @param elementName the name of the stage to insert before
     * @return a new RuntimeStagePosition for before-insertion
     */
    public static RuntimeStagePosition before(String elementName) {
        return new RuntimeStagePosition(Position.BEFORE, elementName);
    }

}
