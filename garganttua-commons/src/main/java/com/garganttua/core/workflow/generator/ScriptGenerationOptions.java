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

	/**
	 * @return options with timing disabled (byte-identical legacy generation)
	 */
	public static ScriptGenerationOptions defaults() {
		return new ScriptGenerationOptions(WorkflowTimingConfig.disabled());
	}

	/**
	 * @param timing the timing config to apply
	 * @return options carrying the given timing config
	 */
	public static ScriptGenerationOptions withTiming(WorkflowTimingConfig timing) {
		return new ScriptGenerationOptions(timing);
	}
}
