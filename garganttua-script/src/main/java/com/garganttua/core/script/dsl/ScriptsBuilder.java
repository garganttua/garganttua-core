package com.garganttua.core.script.dsl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.classloader.IClassLoaderManager;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.expression.context.IExpressionContext;
import com.garganttua.core.expression.dsl.IExpressionContextBuilder;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.garganttua.core.script.IScript;
import com.garganttua.core.script.IScriptingEnvironment;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.ScriptingEnvironment;
import com.garganttua.core.script.annotations.IScriptDefinition;
import com.garganttua.core.script.annotations.ScriptDefinition;
import com.garganttua.core.script.context.ScriptContext;

/**
 * Bootstrap-discoverable plural builder for the script layer. Produces an
 * {@link IScriptingEnvironment} that downstream consumers (Workflow, REPL,
 * CLI, tests) use to spawn fresh {@link IScript} instances without wiring
 * Expression / Runtimes / ClassLoader by hand.
 *
 * <p>Auto-detects classes annotated with {@code @ScriptDefinition} from the
 * injection context, compiles each one's source into an {@link IScript}, and
 * registers the resulting instances as DI beans named after the annotation.
 *
 * <p>Builds cleanly with zero registered scripts — a Bootstrap that
 * auto-detects this builder but doesn't actually use scripts must not block.
 *
 * @since 2.0.0-ALPHA02
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("scripts")
public class ScriptsBuilder
        extends AbstractAutomaticDependentBuilder<IScriptsBuilder, IScriptingEnvironment>
        implements IScriptsBuilder {

    private static final Logger log = Logger.getLogger(ScriptsBuilder.class);

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            DependencySpec.require(IClass.getClass(IInjectionContextBuilder.class)),
            DependencySpec.require(IClass.getClass(IExpressionContextBuilder.class)),
            DependencySpec.requireBuilder(IClass.getClass(IRuntimesBuilder.class)));

    private final Set<String> packages = new HashSet<>();
    private final Map<String, IScript> registry = new LinkedHashMap<>();

    private IInjectionContextBuilder injectionContextBuilder;
    private IExpressionContextBuilder expressionContextBuilder;
    private IExpressionContext expressionContext;
    private IRuntimesBuilder runtimesBuilder;
    private IClassLoaderManager classLoaderManager;

    private ScriptsBuilder() {
        super(DEPENDENCIES);
        log.trace("ScriptsBuilder created");
    }

    /**
     * @return a new, unconfigured {@link ScriptsBuilder}
     */
    public static IScriptsBuilder builder() {
        return new ScriptsBuilder();
    }

    /**
     * Hook to inject a class-loader manager. Not declared as a DependencySpec
     * because the manager isn't required for the script layer to function — it
     * only enables JAR hot-loading via {@code include("foo.jar")}. Wired post-
     * build by whoever cares (Bootstrap post-build phase or manual).
     */
    public ScriptsBuilder withClassLoaderManager(IClassLoaderManager mgr) {
        this.classLoaderManager = mgr;
        return this;
    }

    @Override
    public IScriptsBuilder observer(IBuilderObserver<IScriptsBuilder, IScriptingEnvironment> observer) {
        return this;
    }

    @Override
    public IScriptsBuilder withPackage(String packageName) {
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IScriptsBuilder withPackages(String[] packageNames) {
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String p : packageNames) {
            this.withPackage(p);
        }
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    public IScriptsBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder b) {
            this.injectionContextBuilder = b;
        }
        if (dependency instanceof IExpressionContextBuilder b) {
            this.expressionContextBuilder = b;
        }
        if (dependency instanceof IRuntimesBuilder b) {
            this.runtimesBuilder = b;
        }
        return super.provide(dependency);
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IExpressionContext ctx) {
            this.expressionContext = ctx;
        }
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // No-op without a dependency.
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (!(dependency instanceof IInjectionContext context)) {
            return;
        }
        List<?> definitions = context.queryBeans(
                new BeanReference<>(null, Optional.empty(), Optional.empty(),
                        Set.of(IClass.getClass(ScriptDefinition.class))));
        if (definitions.isEmpty()) {
            return;
        }
        log.debug("Auto-detecting {} script definition(s) from InjectionContext",
                definitions.size());
        for (Object def : definitions) {
            registerAutoDetectedScript(def);
        }
    }

    private void registerAutoDetectedScript(Object definitionBean) {
        ScriptDefinition ann = definitionBean.getClass().getAnnotation(ScriptDefinition.class);
        String name = ann != null && !ann.name().isBlank()
                ? ann.name()
                : Optional.ofNullable(definitionBean.getClass().getAnnotation(Named.class))
                        .map(Named::value)
                        .orElse(definitionBean.getClass().getSimpleName());
        if (!(definitionBean instanceof IScriptDefinition def)) {
            log.warn("@ScriptDefinition class {} does not implement IScriptDefinition; skipping",
                    definitionBean.getClass().getName());
            return;
        }
        try {
            IScript compiled = compileScript(def.source());
            this.registry.put(name, compiled);
            log.debug("Auto-detected script '{}' compiled and registered", name);
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to compile auto-detected script '" + name + "'", e);
        }
    }

    private IScript compileScript(String source) throws ScriptException {
        IScript s = createScript();
        s.load(source);
        s.compile();
        return s;
    }

    private IScript createScript() {
        final IInjectionContextBuilder injCtx = this.injectionContextBuilder;
        return new ScriptContext(
                this.expressionContext,
                // Fresh RuntimesBuilder per compile — same invariant as the
                // workflow layer enforces (see project_scriptcontext_fresh_runtimes_builder).
                () -> RuntimesBuilder.builder().provide(injCtx),
                this.classLoaderManager);
    }

    @Override
    protected IScriptingEnvironment doBuild() throws DslException {
        log.debug("Building IScriptingEnvironment with {} registered script(s)", this.registry.size());
        final IInjectionContextBuilder injCtx = this.injectionContextBuilder;
        // Fresh RuntimesBuilder per compile — see project_scriptcontext_fresh_runtimes_builder.
        return new ScriptingEnvironment(
                this.expressionContext,
                () -> RuntimesBuilder.builder().provide(injCtx),
                this.classLoaderManager,
                this.registry);
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext ctx) {
            registerBeans(ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerBeans(IInjectionContext context) {
        String providerName = Predefined.BeanProviders.garganttua.toString();

        // 1. Expose the scripting environment itself
        BeanReference<IScriptingEnvironment> envRef = new BeanReference<>(
                IClass.getClass(IScriptingEnvironment.class),
                Optional.of(BeanStrategy.singleton),
                Optional.of("scriptingEnvironment"),
                Set.of());
        context.addBean(providerName, envRef, this.built);

        // 2. Expose each auto-detected named script
        for (Map.Entry<String, IScript> e : this.registry.entrySet()) {
            BeanReference<IScript> beanRef = new BeanReference<>(
                    IClass.getClass(IScript.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.of(e.getKey()),
                    Set.of(IClass.getClass(ScriptDefinition.class)));
            context.addBean(providerName, beanRef, e.getValue());
        }

        // 3. Expose the registry map itself
        if (!this.registry.isEmpty()) {
            BeanReference<Map<String, IScript>> mapRef = new BeanReference<>(
                    (IClass<Map<String, IScript>>) (IClass<?>) IClass.getClass(Map.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.of("Scripts"),
                    Set.of());
            context.addBean(providerName, mapRef, this.registry);
        }
        log.debug("Registered scripting environment + {} script bean(s)", this.registry.size());
    }
}
