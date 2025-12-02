package com.garganttua.core.runtime;

/**
 * Immutable record specifying the relative position for inserting a step within a runtime stage.
 *
 * <p>
 * RuntimeStepPosition allows precise control over step ordering when building runtimes
 * programmatically using the DSL. Instead of always appending steps at the end of a stage,
 * you can insert them before or after existing steps by name.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * runtimeBuilder
 *     .stage("validation")
 *         .step("validateFormat", () -> new FormatValidator(), Void.class)
 *             .method().name("validate").end()
 *             .end()
 *         .step("validateBusiness", () -> new BusinessValidator(), Void.class)
 *             .method().name("validate").end()
 *             .end()
 *         // Insert a new step between the two existing steps
 *         .step("sanitizeInput", RuntimeStepPosition.after("validateFormat"),
 *               () -> new InputSanitizer(), Void.class)
 *             .method().name("sanitize").end()
 *             .end()
 *         // Insert a step at the beginning
 *         .step("logInput", RuntimeStepPosition.before("validateFormat"),
 *               () -> new InputLogger(), Void.class)
 *             .method().name("log").end()
 *             .end()
 *         .end();
 * }</pre>
 *
 * @param position whether to insert before or after the reference element
 * @param elementName the name of the existing step to position relative to
 * @since 2.0.0-ALPHA01
 * @see Position
 * @see RuntimeStagePosition
 * @see com.garganttua.core.runtime.dsl.IRuntimeStageBuilder#step(String, com.garganttua.core.utils.OrderedMapPosition, com.garganttua.core.supply.dsl.IObjectSupplierBuilder, Class)
 */
public record RuntimeStepPosition (Position position,
        String elementName) {

    /**
     * Creates a position specifying insertion after the named step.
     *
     * @param elementName the name of the step to insert after
     * @return a new RuntimeStepPosition for after-insertion
     */
    public static RuntimeStepPosition after(String elementName) {
        return new RuntimeStepPosition(Position.AFTER, elementName);
    }

    /**
     * Creates a position specifying insertion before the named step.
     *
     * @param elementName the name of the step to insert before
     * @return a new RuntimeStepPosition for before-insertion
     */
    public static RuntimeStepPosition before(String elementName) {
        return new RuntimeStepPosition(Position.BEFORE, elementName);
    }

}