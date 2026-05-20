package com.garganttua.core.workflow.generator;

import com.garganttua.core.workflow.WorkflowTimingConfig;

/**
 * Options bundle passed to
 * {@link com.garganttua.core.workflow.generator.ScriptGenerator} controlling
 * optional code emission. Defaults preserve byte-for-byte compatibility with
 * the historical {@code generate(...)} overloads.
 *
 * @since 2.0.0-ALPHA02
 */
public record ScriptGenerationOptions(WorkflowTimingConfig timing) {

	public ScriptGenerationOptions {
		if (timing == null) {
			timing = WorkflowTimingConfig.disabled();
		}
	}

	public static ScriptGenerationOptions defaults() {
		return new ScriptGenerationOptions(WorkflowTimingConfig.disabled());
	}

	public static ScriptGenerationOptions withTiming(WorkflowTimingConfig timing) {
		return new ScriptGenerationOptions(timing);
	}
}
