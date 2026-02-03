package com.garganttua.core.runtime;

/**
 * Enumeration defining relative positioning options for runtime elements.
 *
 * <p>
 * Position is used in conjunction with position records ({@link RuntimeStepPosition},
 * {@link RuntimeStepOperationPosition}) to specify where new elements should be inserted
 * relative to existing elements in a runtime workflow.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Insert a step before another step
 * RuntimeStepPosition position = RuntimeStepPosition.before("validation");
 * runtimeBuilder.step("preValidation", position);
 *
 * // Insert a step after another step
 * RuntimeStepPosition afterPos = RuntimeStepPosition.after("processing");
 * runtimeBuilder.step("postProcessing", afterPos);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeStepPosition
 * @see RuntimeStepOperationPosition
 */
public enum Position {
    /**
     * Indicates that an element should be positioned after the reference element.
     */
    AFTER,

    /**
     * Indicates that an element should be positioned before the reference element.
     */
    BEFORE

}
