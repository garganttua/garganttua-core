package com.garganttua.core.runtime;

/**
 * Marker interface for runtime execution engines.
 *
 * <p>
 * IRuntimeExecutor serves as a type marker for components responsible for
 * executing runtime workflows. It is used internally by the runtime framework
 * to identify and manage execution engines.
 * </p>
 *
 * <p>
 * Implementations handle the actual invocation of runtime stages and steps,
 * context management, and exception handling during workflow execution.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see com.garganttua.core.execution.IExecutor
 */
public interface IRuntimeExecutor {

}
