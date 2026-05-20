package com.garganttua.core.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configures the emission of observability timing markers in generated workflow scripts.
 * <p>
 * When applied to an {@link com.garganttua.core.workflow.dsl.IWorkflowBuilder},
 * the {@link com.garganttua.core.workflow.generator.ScriptGenerator} injects
 * {@code :observe("start"|"end", source)} calls around stages and scripts. The
 * runtime {@code Workflow} forwards the events to registered observers.
 * <p>
 * Defaults: timing markers disabled. Construct an enabled config via
 * {@link #of()} and toggle scopes through the fluent setters.
 *
 * @since 2.0.0-ALPHA02
 */
public final class WorkflowTimingConfig {

	private static final WorkflowTimingConfig DISABLED = new WorkflowTimingConfig(false, false, Set.of(), Set.of());

	private final boolean stages;
	private final boolean scripts;
	private final Set<String> disabledStages;
	private final Set<String> disabledScripts;

	private WorkflowTimingConfig(boolean stages, boolean scripts,
			Set<String> disabledStages, Set<String> disabledScripts) {
		this.stages = stages;
		this.scripts = scripts;
		this.disabledStages = Collections.unmodifiableSet(new HashSet<>(disabledStages));
		this.disabledScripts = Collections.unmodifiableSet(new HashSet<>(disabledScripts));
	}

	/**
	 * Globally disabled timing config — no markers emitted. Default value.
	 */
	public static WorkflowTimingConfig disabled() {
		return DISABLED;
	}

	/**
	 * Fluent constructor for an enabled config with both stage and script
	 * markers turned on. Use the {@code stages(false)} / {@code scripts(false)}
	 * setters to narrow the scope, or {@code disableStage} / {@code disableScript}
	 * to exclude specific names.
	 */
	public static WorkflowTimingConfig of() {
		return new WorkflowTimingConfig(true, true, Set.of(), Set.of());
	}

	public WorkflowTimingConfig stages(boolean enabled) {
		return new WorkflowTimingConfig(enabled, this.scripts, this.disabledStages, this.disabledScripts);
	}

	public WorkflowTimingConfig scripts(boolean enabled) {
		return new WorkflowTimingConfig(this.stages, enabled, this.disabledStages, this.disabledScripts);
	}

	public WorkflowTimingConfig disableStage(String stageName) {
		Set<String> next = new HashSet<>(this.disabledStages);
		next.add(stageName);
		return new WorkflowTimingConfig(this.stages, this.scripts, next, this.disabledScripts);
	}

	public WorkflowTimingConfig disableScript(String stageDotScript) {
		Set<String> next = new HashSet<>(this.disabledScripts);
		next.add(stageDotScript);
		return new WorkflowTimingConfig(this.stages, this.scripts, this.disabledStages, next);
	}

	public boolean stagesEnabled() {
		return this.stages;
	}

	public boolean scriptsEnabled() {
		return this.scripts;
	}

	/**
	 * @return {@code true} when the global config emits no markers at all
	 *         (used by callers to short-circuit byte-identical generation).
	 */
	public boolean isFullyDisabled() {
		return !this.stages && !this.scripts;
	}

	public boolean isStageEnabled(String stageName) {
		return this.stages && !this.disabledStages.contains(stageName);
	}

	public boolean isScriptEnabled(String stageName, String scriptName) {
		return this.scripts && !this.disabledScripts.contains(stageName + "." + scriptName);
	}
}
