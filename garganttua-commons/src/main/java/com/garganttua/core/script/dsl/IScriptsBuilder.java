package com.garganttua.core.script.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.script.IScriptingEnvironment;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Bootstrap-discoverable builder that produces the script layer's public
 * artefacts:
 * <ul>
 *   <li>An {@link IScriptingEnvironment} exposed as a DI bean so transient
 *       consumers (Workflow, REPL, CLI, tests) can spawn new scripts without
 *       wiring the lower layers (Expression, Runtimes, ClassLoader) by hand.
 *   <li>A registry of named scripts auto-detected from
 *       {@code @ScriptDefinition} annotations — also exposed as DI beans.
 * </ul>
 *
 * <p>Sits between {@code WorkflowsBuilder} and {@code RuntimesBuilder} /
 * {@code ExpressionContextBuilder} so the dependency graph reflects the
 * execution chain: {@code Workflows → Scripts → {Expression, Runtimes,
 * ClassLoader}}.
 *
 * <p>Builds cleanly with zero scripts declared.
 *
 * @since 2.0.0-ALPHA02
 */
@Reflected
public interface IScriptsBuilder
        extends IAutomaticBuilder<IScriptsBuilder, IScriptingEnvironment>,
                IObservableBuilder<IScriptsBuilder, IScriptingEnvironment>,
                IPackageableBuilder<IScriptsBuilder, IScriptingEnvironment>,
                IDependentBuilder<IScriptsBuilder, IScriptingEnvironment> {
}
