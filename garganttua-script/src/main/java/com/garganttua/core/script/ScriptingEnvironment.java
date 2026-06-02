package com.garganttua.core.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Default {@link IScriptingEnvironment} implementation. Holds the references
 * the script layer needs to spawn fresh {@link IScript} instances on demand,
 * and contributes a "Script Engine" section to the Bootstrap startup summary.
 *
 * <p>Created by {@code ScriptsBuilder} once at build time. Every
 * {@link #newScript()} call returns an independent {@link IScript} configured
 * with the captured expression context, fresh-per-call runtimes-builder
 * factory, and class-loader manager.
 *
 * <p>The pre-compiled script registry (named scripts auto-detected from
 * {@code @ScriptDefinition}) is exposed via {@link #getRegistry()} and surfaced
 * in the Bootstrap summary.
 *
 * @since 2.0.0-ALPHA02
 */
public class ScriptingEnvironment implements IScriptingEnvironment, IBootstrapSummaryContributor {

    private final IExpressionContext expressionContext;
    private final Supplier<IRuntimesBuilder> runtimesBuilderFactory;
    private final IClassLoaderManager classLoaderManager;
    private final Map<String, IScript> registry;
    /** Number of {@link ICompiledScript} produced via {@link #precompile}.
     *  Long-lived runtimes baked at framework build time (auto-detected
     *  scripts + WorkflowBuilder.precompile(true) workflows). */
    private final AtomicInteger precompiledCount = new AtomicInteger();

    /**
     * @param expressionContext      expression context captured for every spawned script
     * @param runtimesBuilderFactory factory yielding a fresh {@link IRuntimesBuilder} per script
     * @param classLoaderManager     JAR hot-load manager, or {@code null} to disable hot-loading
     * @param registry               auto-detected named scripts, or {@code null} for an empty registry
     */
    public ScriptingEnvironment(IExpressionContext expressionContext,
                                Supplier<IRuntimesBuilder> runtimesBuilderFactory,
                                IClassLoaderManager classLoaderManager,
                                Map<String, IScript> registry) {
        this.expressionContext = Objects.requireNonNull(expressionContext, "expressionContext");
        this.runtimesBuilderFactory = Objects.requireNonNull(runtimesBuilderFactory, "runtimesBuilderFactory");
        this.classLoaderManager = classLoaderManager; // nullable — JAR hot-load disabled then
        this.registry = registry == null ? Collections.emptyMap() : Collections.unmodifiableMap(registry);
    }

    @Override
    public IScript newScript() {
        return new ScriptContext(this.expressionContext, this.runtimesBuilderFactory, this.classLoaderManager);
    }

    @Override
    public ICompiledScript precompile(String source, Map<String, Object> presetVariables)
            throws ScriptException {
        if (source == null || source.isBlank()) {
            throw new ScriptException("Cannot precompile: source is null or blank");
        }
        ScriptContext ctx = new ScriptContext(this.expressionContext,
                this.runtimesBuilderFactory, this.classLoaderManager);
        ctx.load(source);
        if (presetVariables != null) {
            for (Map.Entry<String, Object> e : presetVariables.entrySet()) {
                ctx.setVariable(e.getKey(), e.getValue());
            }
        }
        ctx.compile();
        ICompiledScript compiled = ctx.toCompiled();
        this.precompiledCount.incrementAndGet();
        return compiled;
    }

    /** @return total number of long-lived compiled scripts produced via
     *          {@link #precompile(String, Map)} since this environment was
     *          built. Surfaced in the Bootstrap summary as "Precompiled
     *          scripts". */
    public int getPrecompiledCount() {
        return this.precompiledCount.get();
    }

    /** @return immutable view of the auto-detected named script registry. */
    public Map<String, IScript> getRegistry() {
        return this.registry;
    }

    // --- IBootstrapSummaryContributor ---

    @Override
    public String getSummaryCategory() {
        return "Script Engine";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Scripts registered", String.valueOf(this.registry.size()));
        items.put("Precompiled scripts", String.valueOf(this.precompiledCount.get()));
        items.put("JAR hot-loading", this.classLoaderManager != null ? "enabled" : "disabled");
        if (!this.registry.isEmpty()) {
            String names = String.join(", ", this.registry.keySet());
            if (names.length() > 50) {
                names = names.substring(0, 47) + "...";
            }
            items.put("Script names", names);
        }
        return items;
    }
}
