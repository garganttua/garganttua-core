package com.garganttua.core.runtime;

/**
 * Immutable record specifying the relative position for inserting an operation within a runtime step.
 *
 * <p>
 * RuntimeStepOperationPosition allows precise control over operation ordering within a step
 * when building runtimes programmatically using the DSL. This is used for advanced scenarios
 * where multiple operations need to be executed in a specific order within a single step.
 * </p>
 *
 * <p>
 * Unlike {@link RuntimeStagePosition} and {@link RuntimeStepPosition} which use string names
 * for reference, RuntimeStepOperationPosition uses class types to identify the reference element.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Position an operation relative to another operation by class
 * RuntimeStepOperationPosition afterValidation =
 *     RuntimeStepOperationPosition.after(ValidationOperation.class);
 *
 * RuntimeStepOperationPosition beforeTransform =
 *     RuntimeStepOperationPosition.before(TransformOperation.class);
 * }</pre>
 *
 * @param position whether to insert before or after the reference element
 * @param element the class of the existing operation to position relative to
 * @since 2.0.0-ALPHA01
 * @see Position
 * @see RuntimeStagePosition
 * @see RuntimeStepPosition
 */
public record RuntimeStepOperationPosition(Position position,
        Class<?> element) {

    /**
     * Creates a position specifying insertion after the operation of the given class.
     *
     * @param element the class of the operation to insert after
     * @return a new RuntimeStepOperationPosition for after-insertion
     */
    public static RuntimeStepOperationPosition after(Class<?>  element) {
        return new RuntimeStepOperationPosition(Position.AFTER, element);
    }

    /**
     * Creates a position specifying insertion before the operation of the given class.
     *
     * @param element the class of the operation to insert before
     * @return a new RuntimeStepOperationPosition for before-insertion
     */
    public static RuntimeStepOperationPosition before(Class<?>  element) {
        return new RuntimeStepOperationPosition(Position.BEFORE, element);
    }

}