package com.garganttua.core.workflow.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as a declarative workflow definition.
 *
 * <p>Annotated classes implement {@link IWorkflowDefinition} and contribute a
 * workflow to the enclosing {@code WorkflowsBuilder} when it auto-detects them
 * from the injection context. The annotation also acts as a {@code @Qualifier}
 * so the DI container picks the class up and {@code WorkflowsBuilder} can
 * resolve it via a bean query — same mechanism as {@code @RuntimeDefinition}
 * for runtimes.
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Qualifier
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkflowDefinition {

    /** Logical name of the workflow exposed to the registry. */
    String name();
}
