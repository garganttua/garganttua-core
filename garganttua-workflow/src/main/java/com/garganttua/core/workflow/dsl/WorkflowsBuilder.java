package com.garganttua.core.workflow.dsl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.script.IScriptingEnvironment;
import com.garganttua.core.script.dsl.IScriptsBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.WorkflowsRegistry;
import com.garganttua.core.workflow.annotations.IWorkflowDefinition;
import com.garganttua.core.workflow.annotations.WorkflowDefinition;

/**
 * Plural builder that produces a registry of named {@link IWorkflow workflows}.
 *
 * <p>Bootstrap-discoverable via {@link WorkflowsBuilderFactory}. The
 * {@code @Bootstrap} annotation has moved here from the singular
 * {@link WorkflowBuilder} — the singular is now an internal building block
 * only reachable via {@link #workflow(String)}.
 *
 * <p>Dependency chain reflects the execution chain:
 * {@code Workflows → Scripts → {Expression, Runtimes, ClassLoader}}. This
 * builder requires only {@link IInjectionContextBuilder} (for bean exposure)
 * and {@link IScriptsBuilder} (whose built {@link IScriptingEnvironment} is
 * what every child {@link WorkflowBuilder} needs to compile and execute its
 * generated script). No direct dependency on Expression / Runtime layers.
 *
 * <p>Builds cleanly with zero workflows registered.
 *
 * @since 2.0.0-ALPHA02
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("workflows")
public class WorkflowsBuilder
        extends AbstractAutomaticDependentBuilder<IWorkflowsBuilder, Map<String, IWorkflow>>
        implements IWorkflowsBuilder {

    private static final Logger log = Logger.getLogger(WorkflowsBuilder.class);

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            DependencySpec.require(IClass.getClass(IInjectionContextBuilder.class)),
            DependencySpec.require(IClass.getClass(IScriptsBuilder.class)),
            DependencySpec.use(IClass.getClass(IObservabilityBuilder.class)));

    private final Map<String, WorkflowBuilder> workflowBuilders = new LinkedHashMap<>();
    private final Set<String> packages = new HashSet<>();

    private IInjectionContextBuilder injectionContextBuilder;
    private IScriptsBuilder scriptsBuilder;
    private IScriptingEnvironment scriptingEnvironment;
    private IObservabilityBuilder observabilityBuilder;

    private WorkflowsBuilder() {
        super(DEPENDENCIES);
        log.trace("WorkflowsBuilder created");
    }

    /** {@return a fresh {@link WorkflowsBuilder} with no workflows declared} */
    public static IWorkflowsBuilder builder() {
        return new WorkflowsBuilder();
    }

    @Override
    public IWorkflowsBuilder observer(com.garganttua.core.dsl.IBuilderObserver<IWorkflowsBuilder, Map<String, IWorkflow>> observer) {
        return this;
    }

    @Override
    public IWorkflowsBuilder withPackage(String packageName) {
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IWorkflowsBuilder withPackages(String[] packageNames) {
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
    public IWorkflowBuilder workflow(String name) {
        Objects.requireNonNull(name, "Workflow name cannot be null");
        WorkflowBuilder child = this.workflowBuilders.get(name);
        if (child == null) {
            child = WorkflowBuilder.createChild(this, name);
            applyDepsTo(child);
            this.workflowBuilders.put(name, child);
            log.debug("Opened new child workflow builder '{}'", name);
        } else {
            log.debug("Reusing existing child workflow builder '{}'", name);
        }
        return child;
    }

    private void applyDepsTo(WorkflowBuilder child) {
        if (this.injectionContextBuilder != null) {
            child.provide((IObservableBuilder<?, ?>) this.injectionContextBuilder);
        }
        if (this.observabilityBuilder != null) {
            child.provide((IObservableBuilder<?, ?>) this.observabilityBuilder);
        }
        // Push the scripting environment if it is ALREADY materialised
        // (PRE_BUILD has fired, or a direct-DSL caller built it earlier).
        // Otherwise defer — the child will pull it lazily via
        // requireScriptingEnvironment() at its own build time. This avoids
        // building the IScriptsBuilder during CONFIGURATION, before its
        // transitive deps (IExpressionContextBuilder, …) are ready.
        if (this.scriptingEnvironment != null) {
            child.acceptScriptingEnvironment(this.scriptingEnvironment);
        }
    }

    /**
     * Materialise the {@link IScriptingEnvironment} on demand. Called by:
     * <ul>
     *   <li>child {@code WorkflowBuilder.doBuild()} when its own build kicks
     *       in but no env has been delivered via the bootstrap pre-build path
     *       (direct-DSL caller flow);</li>
     *   <li>internally by {@link #doBuild()} before generating the children
     *       so the same env is propagated.</li>
     * </ul>
     * Returns {@code null} when no {@link IScriptsBuilder} has been provided
     * at all. Idempotent — caches the env after first successful build.
     */
    IScriptingEnvironment requireScriptingEnvironment() {
        if (this.scriptingEnvironment != null) {
            return this.scriptingEnvironment;
        }
        if (this.scriptsBuilder == null) {
            return null;
        }
        try {
            this.scriptingEnvironment = this.scriptsBuilder.build();
            log.debug("Materialised IScriptingEnvironment from provided IScriptsBuilder");
            return this.scriptingEnvironment;
        } catch (DslException e) {
            throw new IllegalStateException("Failed to build IScriptsBuilder while materialising scripting environment", e);
        }
    }

    @Override
    public IWorkflowsBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IInjectionContextBuilder b) {
            this.injectionContextBuilder = b;
        }
        if (dependency instanceof IScriptsBuilder b) {
            this.scriptsBuilder = b;
        }
        if (dependency instanceof IObservabilityBuilder b) {
            this.observabilityBuilder = b;
        }
        // Propagate only builders the child WorkflowBuilder declares; the
        // IScriptingEnvironment is pushed via acceptScriptingEnvironment()
        // through materializeScriptingEnvironment(), not via provide().
        for (WorkflowBuilder child : this.workflowBuilders.values()) {
            if (dependency instanceof IInjectionContextBuilder
                    || dependency instanceof IObservabilityBuilder) {
                child.provide(dependency);
            }
        }
        // Note: when an IScriptsBuilder is provided during CONFIGURATION (the
        // typical bootstrap flow), do NOT try to materialise it here — its own
        // transitive deps (IExpressionContextBuilder, …) may not be built yet.
        // The env is delivered either via doPreBuildWithDependency() once
        // PRE_BUILD fires, or pulled lazily by a child at its own build time
        // via requireScriptingEnvironment().
        return super.provide(dependency);
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        if (dependency instanceof IScriptingEnvironment env) {
            this.scriptingEnvironment = env;
            for (WorkflowBuilder child : this.workflowBuilders.values()) {
                child.acceptScriptingEnvironment(env);
            }
        }
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        if (dependency instanceof IInjectionContext ctx) {
            registerBuiltObjectInContext(ctx, this.built);
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
                        Set.of(IClass.getClass(WorkflowDefinition.class))));
        if (definitions.isEmpty()) {
            return;
        }
        log.debug("Auto-detecting {} workflow definition(s) from InjectionContext",
                definitions.size());
        for (Object def : definitions) {
            registerAutoDetectedWorkflow(def);
        }
    }

    private void registerAutoDetectedWorkflow(Object definitionBean) {
        WorkflowDefinition ann = definitionBean.getClass().getAnnotation(WorkflowDefinition.class);
        String name = ann != null && !ann.name().isBlank()
                ? ann.name()
                : Optional.ofNullable(definitionBean.getClass().getAnnotation(Named.class))
                        .map(Named::value)
                        .orElse(definitionBean.getClass().getSimpleName());
        IWorkflowBuilder child = workflow(name).name(name);
        if (definitionBean instanceof IWorkflowDefinition def) {
            def.define(child);
        } else {
            log.warn("@WorkflowDefinition class {} does not implement IWorkflowDefinition; skipping define()",
                    definitionBean.getClass().getName());
        }
    }

    @Override
    protected Map<String, IWorkflow> doBuild() throws DslException {
        Map<String, IWorkflow> result;
        if (this.workflowBuilders.isEmpty()) {
            log.debug("WorkflowsBuilder: no workflows declared, returning empty registry");
            result = Collections.emptyMap();
        } else {
            result = new LinkedHashMap<>();
            for (Map.Entry<String, WorkflowBuilder> e : this.workflowBuilders.entrySet()) {
                log.debug("Building workflow '{}'", e.getKey());
                result.put(e.getKey(), e.getValue().build());
            }
            log.debug("Built {} workflow(s)", result.size());
        }
        // Wrap so Bootstrap.printSummary surfaces "Workflow Engine" stats via
        // IBootstrapSummaryContributor.
        return new WorkflowsRegistry(result);
    }

    @SuppressWarnings("unchecked")
    private void registerBuiltObjectInContext(IInjectionContext context, Map<String, IWorkflow> result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        String providerName = Predefined.BeanProviders.garganttua.toString();

        BeanReference<Map<String, IWorkflow>> mapBeanRef = new BeanReference<>(
                (IClass<Map<String, IWorkflow>>) (IClass<?>) IClass.getClass(Map.class),
                Optional.of(BeanStrategy.singleton),
                Optional.of("Workflows"),
                Set.of());
        context.addBean(providerName, mapBeanRef, result);

        result.forEach((name, workflow) -> {
            BeanReference<IWorkflow> beanRef = new BeanReference<>(
                    IClass.getClass(IWorkflow.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.of(name),
                    Set.of(IClass.getClass(WorkflowDefinition.class)));
            context.addBean(providerName, beanRef, workflow);
        });
        log.debug("Registered {} workflow(s) as beans", result.size());
    }
}
