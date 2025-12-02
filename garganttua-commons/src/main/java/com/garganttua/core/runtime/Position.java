package com.garganttua.core.runtime;

/**
 * Enumeration defining relative positioning options for runtime elements.
 *
 * <p>
 * Position is used in conjunction with position records ({@link RuntimeStagePosition},
 * {@link RuntimeStepPosition}, {@link RuntimeStepOperationPosition}) to specify where
 * new elements should be inserted relative to existing elements in a runtime workflow.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Insert a stage before another stage
 * RuntimeStagePosition position = RuntimeStagePosition.before("validation");
 * runtimeBuilder.stage("preValidation", position);
 *
 * // Insert a stage after another stage
 * RuntimeStagePosition afterPos = RuntimeStagePosition.after("processing");
 * runtimeBuilder.stage("postProcessing", afterPos);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see RuntimeStagePosition
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
