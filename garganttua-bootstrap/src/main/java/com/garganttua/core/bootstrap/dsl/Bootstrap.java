package com.garganttua.core.bootstrap.dsl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.banner.BannerMode;
import com.garganttua.core.bootstrap.banner.DependencyGraphRenderer;
import com.garganttua.core.bootstrap.banner.StageTimings;
import com.garganttua.core.bootstrap.banner.FileBanner;
import com.garganttua.core.bootstrap.banner.GarganttuaBanner;
import com.garganttua.core.bootstrap.banner.IBanner;
import com.garganttua.core.bootstrap.dsl.IBootstrapConfigurationContributor;
import com.garganttua.core.bootstrap.dsl.IBootstrapStageListener;
import com.garganttua.core.bootstrap.dsl.IBootstrapStageListener.Stage;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.dsl.IObservabilityBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBootstrapBuilderFactory;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.IRebuildableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityEmitter;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.ObservableRegistry;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

import jakarta.annotation.Priority;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Builder for bootstrapping Garganttua applications.
 *
 * <p>
 * {@code Bootstrap} provides a fluent API for configuring and initializing a
 * Garganttua application. It manages the lifecycle of various builders
 * (injection context, runtime, expression context, etc.) and orchestrates their
 * initialization in the correct order. Obtain an instance via {@link #builder()}.
 * </p>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * IBuiltRegistry app = Bootstrap.builder()
 *         .withPackage("com.myapp")
 *         .withBuilder(injectionContextBuilder)
 *         .withBuilder(runtimeBuilder)
 *         .autoDetect(true)
 *         .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Automatic package scanning configuration</li>
 * <li>Builder lifecycle management</li>
 * <li>Auto-detection support</li>
 * <li>Dependency resolution between builders</li>
 * </ul>
 *
 * <p><b>Size note:</b> this orchestrator exceeds the 500-line code-size gate on
 * purpose. The separable concerns have been extracted ({@link BootstrapConsoleReporter},
 * {@link BootstrapStageNotifier}, {@link BootstrapSpiLoader}, {@link BuilderDependencyResolver});
 * the residual is the framework's startup orchestration / {@code IBootstrap} +
 * lifecycle implementation, which is inherently cohesive. Treated as a documented
 * exception, like the {@code IClass}/{@code IRuntimeContext} mirror implementations
 * and {@code InjectionContext}.
 *
 * @since 2.0.0-ALPHA01
 */
public class Bootstrap extends AbstractAutomaticDependentBuilder<IBootstrap, IBuiltRegistry>
        implements IBootstrap, IObservable {
    private static final Logger log = Logger.getLogger(Bootstrap.class);

    private static final String DEFAULT_VERSION = com.garganttua.core.bootstrap.GarganttuaVersion.getVersion();
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";


    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, IBuilder<?>> manualBuilders = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, IBuilder<?>> autoDetectedBuilders = Collections.synchronizedMap(new HashMap<>());
    private final MultiSourceCollector<String, IBuilder<?>> builderCollector;
    private int manualBuilderSeq = 0;
    private int autoDetectedBuilderSeq = 0;
    private final Map<IClass<?>, Object> builtObjectsRegistry = Collections.synchronizedMap(new HashMap<>());
    /** Tracks {@link IClassLoaderManager} instances we've already wired a rebuild hook on, so a Bootstrap.rebuild() doesn't pile up duplicate hooks. */
    private final java.util.Set<com.garganttua.core.classloader.IClassLoaderManager> classLoaderManagersWired = java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
    private final List<IObservableBuilder<?, ?>> providedBuilders = new ArrayList<>();
    private final Set<IBuilder<?>> spiAutoLoadedBuilders =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private final ObservableRegistry observers = new ObservableRegistry();
    private boolean reflectionBuilderProvided = false;
    private IObservabilityBuilder observabilityBuilder;
    /** Per-stage timings for the most recent build / rebuild; used by the banner summary. */
    private volatile StageTimings lastBuildTimings;
    /** Notifies high-level Bootstrap stage listeners (manual + ServiceLoader-discovered). */
    private final BootstrapStageNotifier stageNotifier = new BootstrapStageNotifier();
    /** When {@code true}, print an ASCII dep graph after Phase 2 (topo sort). */
    private boolean printDependencyGraph = false;
    private boolean spiFallbackEnabled = true;
    /** Guards {@link #load()} against re-loading the same modules. */
    private boolean spiModulesLoaded = false;

    @Override
    public void addObserver(IObserver<ObservableEvent> observer) {
        this.observers.addObserver(observer);
    }

    @Override
    public void removeObserver(IObserver<ObservableEvent> observer) {
        this.observers.removeObserver(observer);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ISupplier<Map<K, V>> mapSupplier(Map<K, V> map) {
        return new ISupplier<>() {
            @Override
            public Optional<Map<K, V>> supply() throws SupplyException {
                return Optional.of(map);
            }

            @Override
            public Type getSuppliedType() {
                return Map.class;
            }

            @Override
            public IClass<Map<K, V>> getSuppliedClass() {
                return (IClass<Map<K, V>>) (IClass<?>) IClass.getClass(Map.class);
            }
        };
    }

    // Banner configuration
    private IBanner banner;
    private BannerMode bannerMode = BannerMode.CONSOLE;
    private String applicationName = "Garganttua";
    private String applicationVersion = DEFAULT_VERSION;
    private boolean bannerPrinted = false;

    /**
     * Creates a new BootstrapBuilder instance.
     *
     * @return a new BootstrapBuilder
     */
    public static IBootstrap builder() {
        log.trace("Creating new Bootstrap instance");
        return new Bootstrap();
    }

    /**
     * Creates a new Bootstrap, wiring its manual and auto-detected builder
     * sources and triggering the {@link BootstrapSpiLoader} cold-start fallback
     * so {@code new Bootstrap()} is usable on a cold JVM with only provider JARs
     * on the classpath.
     */
    public Bootstrap() {
        super(buildDependencies());

        this.builderCollector = new MultiSourceCollector<>();
        builderCollector.source(mapSupplier(manualBuilders), 0, SOURCE_MANUAL);
        builderCollector.source(mapSupplier(autoDetectedBuilders), 1, SOURCE_AUTO_DETECTED);

        log.debug("Bootstrap initialized");
    }

    /**
     * Builds the dependency spec set passed to the parent constructor. Because
     * the spec uses {@link IClass#getClass(Class)} which requires a configured
     * {@link IReflection}, we trigger the ServiceLoader fallback here when no
     * reflection has been installed yet. This makes {@code new Bootstrap()}
     * usable on a cold JVM with only provider JARs on the classpath.
     */
    private static Set<DependencySpec> buildDependencies() {
        BootstrapSpiLoader.ensureReflectionAvailable();
        return Set.of(
                DependencySpec.require(IClass.getClass(IReflectionBuilder.class), DependencyPhase.AUTO_DETECT),
                DependencySpec.use(IClass.getClass(IObservabilityBuilder.class)));
    }


    @Override
    public IBootstrap withBanner(IBanner banner) {
        log.trace("Setting custom banner");
        this.banner = banner;
        return this;
    }

    @Override
    public IBootstrap withBannerMode(BannerMode mode) {
        log.trace("Setting banner mode: {}", mode);
        this.bannerMode = Objects.requireNonNull(mode, "Banner mode cannot be null");
        return this;
    }

    @Override
    public IBootstrap withApplicationName(String name) {
        log.trace("Setting application name: {}", name);
        this.applicationName = name != null ? name : "Garganttua";
        return this;
    }

    @Override
    public IBootstrap withApplicationVersion(String version) {
        log.trace("Setting application version: {}", version);
        this.applicationVersion = version != null ? version : DEFAULT_VERSION;
        return this;
    }

    /**
     * Prints the banner according to the configured mode.
     */
    private void printBanner() {
        if (bannerPrinted || bannerMode == BannerMode.OFF) {
            return;
        }

        IBanner bannerToPrint = resolveBanner();
        if (bannerToPrint == null || bannerToPrint == IBanner.OFF) {
            return;
        }

        switch (bannerMode) {
            case CONSOLE:
                bannerToPrint.print(BootstrapConsoleReporter.CONSOLE_OUT);
                break;
            case LOG:
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bannerToPrint.print(new PrintStream(baos, true, StandardCharsets.UTF_8));
                String bannerText = baos.toString(StandardCharsets.UTF_8);
                for (String line : bannerText.split("\n")) {
                    log.info(line);
                }
                break;
            case OFF:
                // Do nothing
                break;
        }

        bannerPrinted = true;
    }

    /**
     * Resolves the banner to use.
     * Priority: custom banner > classpath banner.txt > default Garganttua banner
     */
    private IBanner resolveBanner() {
        if (banner != null) {
            return banner;
        }

        // Try to load banner.txt from classpath
        FileBanner fileBanner = FileBanner.fromClasspath(
                FileBanner.DEFAULT_BANNER_LOCATION,
                applicationVersion,
                applicationName);
        if (fileBanner != null) {
            log.debug("Using banner from classpath: {}", FileBanner.DEFAULT_BANNER_LOCATION);
            return fileBanner;
        }

        // Use default Garganttua banner
        return new GarganttuaBanner(applicationVersion, true);
    }

    @Override
    public IBootstrap withPackage(String packageName) {
        log.trace("Adding package: {}", packageName);
        Objects.requireNonNull(packageName, "Package name cannot be null");
        this.packages.add(packageName);

        // Propagate package to all IPackageableBuilder instances
        propagatePackageToBuilders(packageName, manualBuilders);
        propagatePackageToBuilders(packageName, autoDetectedBuilders);

        log.debug("Package added: {}", packageName);
        return this;
    }

    @Override
    public IBootstrap withPackages(String[] packageNames) {
        log.trace("Adding {} packages", packageNames != null ? packageNames.length : 0);
        Objects.requireNonNull(packageNames, "Package names array cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        log.debug("All packages added");
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IBootstrap provide(IObservableBuilder<?, ?> dependency) throws DslException {
        this.providedBuilders.add(dependency);
        if (dependency instanceof IReflectionBuilder) {
            this.reflectionBuilderProvided = true;
        }
        if (dependency instanceof IObservabilityBuilder obs) {
            this.observabilityBuilder = obs;
        }
        return (IBootstrap) super.provide(dependency);
    }

    /**
     * Disables the ServiceLoader-based bootstrap of {@link IReflectionBuilder}.
     * <p>
     * By default, when {@code autoDetect(true)} is requested and no
     * {@code IReflectionBuilder} has been {@link #provide(IObservableBuilder)
     * provided}, Bootstrap discovers {@link IReflectionProvider} and
     * {@link IAnnotationScanner} implementations via JDK
     * {@link java.util.ServiceLoader} (reading
     * {@code META-INF/services/com.garganttua.core.reflection.*}) and builds a
     * default reflection. Call this method to opt out — typically only useful
     * for tests that want to assert the absence of a fallback.
     *
     * @return this builder for method chaining
     */
    public IBootstrap disableSpiFallback() {
        this.spiFallbackEnabled = false;
        return this;
    }

    @Override
    public IBootstrap withBuilder(IBuilder<?> builder) {
        log.trace("Adding builder: {}", builder != null ? builder.getClass().getSimpleName() : "null");
        Objects.requireNonNull(builder, "Builder cannot be null");
        this.manualBuilders.put(builder.getClass().getName() + "#" + (manualBuilderSeq++), builder);

        propagatePackagesToBuilder(builder);

        log.debug("Builder added: {}", builder.getClass().getSimpleName());
        return this;
    }

    @Override
    public String[] getPackages() {
        log.trace("Getting packages, count: {}", packages.size());
        return packages.toArray(new String[0]);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // SPI discovery is no longer auto-triggered from build() / Phase 1.
        // Callers must invoke {@link #load()} explicitly — see
        // its Javadoc for the rationale (lets users inspect / amend the
        // registered builders between SPI discovery and build).
        log.trace("doAutoDetection() — no-op, SPI is now driven by load()");
    }

    /**
     * Discover and register every framework module published through the
     * {@link IBootstrapBuilderFactory} SPI — plus the {@link IReflectionBuilder}
     * fallback when no reflection builder has been provided explicitly.
     *
     * <p>This method is the intermediate step between configuring the
     * Bootstrap ({@link #autoDetect(boolean)}, {@link #withPackage(String)},
     * {@link #provide(IObservableBuilder)}, …) and calling {@link #build()}.
     * It lets callers inspect / amend the registered builders after SPI
     * has populated them but before any dependency resolution runs.
     *
     * <p>Gated by {@link #autoDetect(boolean) autoDetect} being true and
     * the SPI fallback not being disabled via {@link #disableSpiFallback()}.
     * Idempotent — calling it twice is a no-op (a guard flag tracks whether
     * SPI has already been swept). {@link #build()} no longer drives this
     * step automatically.
     *
     * <p>Typical use:
     * <pre>{@code
     * Bootstrap bootstrap = new Bootstrap()
     *         .autoDetect(true)
     *         .withPackage("com.myapp");
     *
     * bootstrap.load();
     * // ... inspect bootstrap.getBuilders(), add more via withBuilder(...), etc.
     * bootstrap.build();
     * }</pre>
     *
     * @return this builder for method chaining
     * @throws DslException propagated from SPI factory construction failures
     */
    @Override
    public IBootstrap load() throws DslException {
        if (this.spiModulesLoaded) {
            log.trace("load() called twice — no-op");
            return this;
        }
        if (!this.autoDetect.booleanValue() || !this.spiFallbackEnabled) {
            log.debug("load() skipped — autoDetect={}, spiFallback={}",
                    this.autoDetect.booleanValue(), this.spiFallbackEnabled);
            return this;
        }

        // SPI fallback (Phase 2): even when the global IClass.reflection was
        // installed at constructor time, we still need to feed the bootstrap's
        // own dependency chain with an IReflectionBuilder so downstream
        // builders receive it via provide(). Skipped if a reflection builder
        // was already provided explicitly.
        if (!this.reflectionBuilderProvided) {
            IReflectionBuilder spiBuilder = BootstrapSpiLoader.buildReflectionBuilderFromSpi();
            if (spiBuilder != null) {
                log.debug("Registering SPI-bootstrapped IReflectionBuilder on this Bootstrap");
                this.provide(spiBuilder);
                this.withBuilder(spiBuilder);
                this.spiAutoLoadedBuilders.add(spiBuilder);
            }
        }

        // SPI fallback for standard builders (InjectionContextBuilder,
        // ExpressionContextBuilder, ObservabilityBuilder, WorkflowBuilder, …).
        // Each garganttua-core module that exposes a bootstrap-discoverable
        // builder ships an IBootstrapBuilderFactory via META-INF/services.
        loadBootstrapBuildersFromSpi();

        this.spiModulesLoaded = true;
        log.debug("SPI module discovery complete");
        return this;
    }

    /** Tracks whether the SPI builders summary has been emitted at INFO. */
    private static final AtomicBoolean SPI_BUILDERS_LOGGED = new AtomicBoolean(false);

    private void loadBootstrapBuildersFromSpi() {
        Set<String> registeredClasses = new HashSet<>();
        for (IBuilder<?> existing : this.manualBuilders.values()) {
            registeredClasses.add(existing.getClass().getName());
        }

        List<String> added = new ArrayList<>();
        for (IBootstrapBuilderFactory factory : ServiceLoader.load(IBootstrapBuilderFactory.class)) {
            try {
                IBuilder<?> builder = factory.create();
                if (builder == null) {
                    continue;
                }
                String className = builder.getClass().getName();
                if (registeredClasses.contains(className)) {
                    log.debug("SPI: skipping {} — already registered explicitly", className);
                    continue;
                }
                log.debug("SPI: registering bootstrap builder {} via factory {}",
                        className, factory.getClass().getName());
                // withBuilder() so the builder is visible to downstream dep
                // resolution AND has its own deps satisfied during Phase 1.
                // Tracked in spiAutoLoadedBuilders so Phase 3 SKIPS onInit/
                // onStart on its built result — the top-level consumer
                // (ApiBuilder, etc.) owns the lifecycle of these sub-contexts.
                this.withBuilder(builder);
                // Propagate Bootstrap.autoDetect to SPI-loaded builders that
                // honour it (IAutomaticBuilder). Without this, e.g. an
                // SPI-loaded InjectionContextBuilder stays in autoDetect=false
                // mode and never scans @Qualifier annotations / @Singleton
                // beans — silently breaking the auto-discovery contract users
                // expect when they set bootstrap.autoDetect(true).
                if (builder instanceof com.garganttua.core.dsl.IAutomaticBuilder<?, ?> autoBuilder
                        && this.autoDetect.booleanValue()) {
                    autoBuilder.autoDetect(true);
                }
                this.spiAutoLoadedBuilders.add(builder);
                registeredClasses.add(className);
                added.add(builder.getClass().getSimpleName());
            } catch (Exception e) {
                log.warn("SPI factory {} failed to create builder: {}",
                        factory.getClass().getName(), e.getMessage());
            }
        }

        if (!added.isEmpty()) {
            String summary = "SPI bootstrap: builders=" + added;
            if (SPI_BUILDERS_LOGGED.compareAndSet(false, true)) {
                log.info(summary);
            } else {
                log.debug(summary);
            }
        }
    }


    @Override
    protected IBuiltRegistry doBuild() throws DslException {
        log.trace("Entering doBuild()");
        Instant startTime = Instant.now();

        try (ObservabilityEmitter.Scope outer = ObservabilityEmitter.open(this.observers, UUID.randomUUID())) {
            outer.fireStart("bootstrap:build");
            try {
                IBuiltRegistry registry = doBuildInternal(startTime);
                outer.fireEnd("bootstrap:build");
                return registry;
            } catch (RuntimeException e) {
                outer.fireError("bootstrap:build", e);
                throw e;
            }
        }
    }

    /**
     * Toggle the ASCII dependency-graph dump printed to the banner after
     * topological sort. Off by default — useful for cold-start debugging.
     *
     * @param enabled whether to print the dependency graph after topological sort
     * @return this builder for method chaining
     */
    public Bootstrap printDependencyGraph(boolean enabled) {
        this.printDependencyGraph = enabled;
        return this;
    }

    /**
     * Register a {@link IBootstrapStageListener} to be notified of high-level
     * Bootstrap stages (REGISTRATION / RESOLVE / CONFIGURATION / BUILD).
     * Listeners are also auto-discovered via {@link java.util.ServiceLoader}
     * — manual registrations fire first.
     *
     * @param listener the stage listener to notify of Bootstrap stages
     * @return this builder for method chaining
     */
    public Bootstrap withStageListener(IBootstrapStageListener listener) {
        this.stageNotifier.add(listener);
        return this;
    }


    private IBuiltRegistry doBuildInternal(Instant startTime) throws DslException {
        StageTimings timings = new StageTimings();
        this.lastBuildTimings = timings;
        stageNotifier.ensureLoaded();
        // Print banner at the start of build
        printBanner();

        // Self-attach to the user's ObservabilityBinding (if any). Bootstrap is
        // itself an IObservable so its bootstrap:* events flow through the same
        // binding as the engines it builds.
        if (this.observabilityBuilder != null) {
            ObservabilityBinding binding = this.observabilityBuilder.getBinding();
            if (binding != null) {
                binding.attachSource(this);
                log.trace("Bootstrap attached to ObservabilityBinding");
            }
        }

        List<IBuilder<?>> allBuilders = getBuilders();

        if (allBuilders.isEmpty()) {
            log.warn("No builders registered, returning null");
            return null;
        }

        BootstrapConsoleReporter.printPhase(bannerMode, 1, "Resolving dependencies", allBuilders.size() + " builders");

        // Phase 1: Resolve dependencies between builders
        Instant resolveStart = Instant.now();
        stageNotifier.fireStageStart(Stage.RESOLVE);
        try (ObservabilityEmitter.Scope phase = ObservabilityEmitter.joinCurrent()) {
            phase.fireStart("bootstrap:phase:resolve");
            try {
                new BuilderDependencyResolver(getBuilders(), this.providedBuilders).resolveDependencies();
                phase.fireEnd("bootstrap:phase:resolve");
                stageNotifier.fireStageEnd(Stage.RESOLVE);
            } catch (RuntimeException e) {
                phase.fireError("bootstrap:phase:resolve", e);
                stageNotifier.fireStageError(Stage.RESOLVE, e);
                throw e;
            } finally {
                timings.record("resolve", Duration.between(resolveStart, Instant.now()));
            }
        }

        // Phase 1.5: CONFIGURATION stage. Run BEFORE any builder.build() so
        // every consumer's doConfigureWithDependencyBuilder hook gets a
        // chance to declare configuration on an upstream builder (e.g.
        // injCtxBuilder.withQualifier(...)) while every builder is still
        // mutable. Each (consumer, dep, CONFIGURATION) tuple fires at most
        // once per Bootstrap lifetime — idempotent across rebuild().
        Instant configureStart = Instant.now();
        stageNotifier.fireStageStart(Stage.CONFIGURATION);
        try (ObservabilityEmitter.Scope phase = ObservabilityEmitter.joinCurrent()) {
            phase.fireStart("bootstrap:phase:configure");
            try {
                runGlobalConfigurationPhase();
                phase.fireEnd("bootstrap:phase:configure");
                stageNotifier.fireStageEnd(Stage.CONFIGURATION);
            } catch (RuntimeException e) {
                phase.fireError("bootstrap:phase:configure", e);
                stageNotifier.fireStageError(Stage.CONFIGURATION, e);
                throw e;
            } finally {
                timings.record("configure", Duration.between(configureStart, Instant.now()));
            }
        }

        // Phase 2: Sort builders by dependency order (topological sort)
        List<IBuilder<?>> sortedBuilders = new BuilderDependencyResolver(getBuilders(), this.providedBuilders).sortBuildersByDependencies();
        log.debug("Builders sorted by dependency order: {}",
                sortedBuilders.stream()
                        .map(b -> b.getClass().getSimpleName())
                        .toList());

        if (this.printDependencyGraph) {
            for (String line : DependencyGraphRenderer.render(sortedBuilders)) {
                if (bannerMode == BannerMode.CONSOLE) {
                    BootstrapConsoleReporter.CONSOLE_OUT.println("  " + line);
                } else {
                    log.info(line);
                }
            }
        }

        BootstrapConsoleReporter.printPhase(bannerMode, 2, "Building components", sortedBuilders.size() + " builders");

        // Phase 3: Build all builders in dependency order, initialize lifecycle immediately
        stageNotifier.fireStageStart(Stage.BUILD);
        List<Object> builtObjects = new ArrayList<>();
        for (IBuilder<?> builder : sortedBuilders) {
            BootstrapConsoleReporter.printBuilderStart(bannerMode, builder.getClass().getSimpleName());
            String builderSource = "bootstrap:builder:" + builder.getClass().getSimpleName();
            Object built;
            Instant buildStart = Instant.now();
            try (ObservabilityEmitter.Scope buildScope = ObservabilityEmitter.joinCurrent()) {
                buildScope.fireStart(builderSource);
                try {
                    built = builder.build();
                    buildScope.fireEnd(builderSource);
                } catch (RuntimeException e) {
                    buildScope.fireError(builderSource, e);
                    throw e;
                } finally {
                    Duration elapsed = Duration.between(buildStart, Instant.now());
                    timings.record("build", elapsed);
                    timings.record("build:" + builder.getClass().getSimpleName(), elapsed);
                }
            }
            builtObjects.add(built);

            // Register the built object by its class
            if (built != null) {
                builtObjectsRegistry.put(IClass.getClass(built.getClass()), built);
            }

            // Initialize and start lifecycle objects immediately so downstream
            // builders can use them during their own build/auto-detection
            // phase. onInit() and onStart() are now idempotent at the
            // AbstractLifecycle level (silent no-op when already in the
            // target state) so we can safely init every built object —
            // including SPI auto-loaded ones — without fearing a double-init
            // exception from a top-level consumer that owns the lifecycle
            // and runs its own init later.
            if (built instanceof ILifecycle lifecycleObject) {
                try {
                    lifecycleObject.onInit();
                } catch (LifecycleException e) {
                    log.debug("onInit failed for {}: {}",
                            built.getClass().getSimpleName(), e.getMessage());
                }
                try {
                    lifecycleObject.onStart();
                } catch (LifecycleException e) {
                    log.debug("onStart failed for {}: {}",
                            built.getClass().getSimpleName(), e.getMessage());
                }
            }

            BootstrapConsoleReporter.printBuilderComplete(bannerMode, builder.getClass().getSimpleName());
        }
        stageNotifier.fireStageEnd(Stage.BUILD);

        // Wire ourselves as an IClassLoaderRebuildHook on every freshly-built
        // IClassLoaderManager so that a script's include("foo.jar") triggers
        // withPackage(...) + rebuild() on this Bootstrap without callers having
        // to plumb the link manually. Idempotent across rebuild() — see
        // classLoaderManagersWired.
        wireClassLoaderManagerHooks(builtObjects);

        Duration startupTime = Duration.between(startTime, Instant.now());

        // Print summary
        BootstrapConsoleReporter.printSummary(bannerMode, applicationName, applicationVersion, getBuilders().size(), builtObjects, startupTime, lastBuildTimings);

        log.trace("Exiting doBuild()");

        return new BuiltRegistry(builtObjectsRegistry);
    }

    /**
     * Wire this Bootstrap as a rebuild-hook on every freshly-built
     * {@link com.garganttua.core.classloader.IClassLoaderManager}. The hook
     * registers the JAR-declared packages on this Bootstrap and calls
     * {@link #rebuild()} so freshly-loaded annotated classes are picked up.
     *
     * <p>Idempotent across multiple {@link #rebuild()} cycles via
     * {@code classLoaderManagersWired} — a manager is wired at most once.
     */
    private void wireClassLoaderManagerHooks(List<Object> builtObjects) {
        for (Object o : builtObjects) {
            if (!(o instanceof com.garganttua.core.classloader.IClassLoaderManager mgr)) {
                continue;
            }
            if (!this.classLoaderManagersWired.add(mgr)) {
                log.debug("IClassLoaderManager {} already wired, skipping",
                        mgr.getClass().getSimpleName());
                continue;
            }
            mgr.addRebuildHook(packages -> {
                synchronized (this.packages) {
                    for (String pkg : packages) {
                        this.packages.add(pkg);
                    }
                }
                log.debug("ClassLoader rebuild hook fired with {} package(s); calling Bootstrap.rebuild()",
                        packages.size());
                this.rebuild();
            });
            log.debug("Registered Bootstrap as rebuild hook on {}", mgr.getClass().getSimpleName());
        }
    }


    /**
     * Rebuilds all managed builders, integrating any new packages or components.
     *
     * <p>
     * This method overrides the default rebuild behavior to provide coordinated
     * lifecycle management across all built objects. The rebuild process:
     * </p>
     * <ol>
     * <li>Validates that initial build() has been called</li>
     * <li>Stops all lifecycle-managed objects in reverse order</li>
     * <li>Re-runs auto-detection to discover new @Bootstrap builders</li>
     * <li>Rebuilds each builder in dependency order</li>
     * <li>Re-initializes and starts all lifecycle-managed objects</li>
     * </ol>
     *
     * @return the updated built registry
     * @throws DslException if rebuild fails or if called before initial build()
     */
    @Override
    public IBuiltRegistry rebuild() throws DslException {
        log.trace("Entering rebuild()");

        if (this.built == null) {
            log.error("Cannot rebuild before initial build()");
            throw new DslException("Cannot rebuild before initial build() has been called");
        }

        // Phase 1: Stop lifecycle objects (reverse order)
        log.debug("Phase 1: Stopping lifecycle objects in reverse order");
        List<Object> builtObjectsList = new ArrayList<>(builtObjectsRegistry.values());
        java.util.Collections.reverse(builtObjectsList);
        for (Object obj : builtObjectsList) {
            if (obj instanceof ILifecycle lifecycleObject) {
                try {
                    log.debug("Stopping lifecycle object: {}", obj.getClass().getSimpleName());
                    lifecycleObject.onStop();
                    log.debug("Stopped: {}", obj.getClass().getSimpleName());
                } catch (LifecycleException e) {
                    log.warn("Failed to stop lifecycle object: {} - continuing with rebuild",
                            obj.getClass().getSimpleName(), e);
                }
            }
        }

        // Phase 2: Re-run auto-detection for new @Bootstrap builders
        if (this.autoDetect.booleanValue()) {
            log.debug("Phase 2: Re-running auto-detection for new builders");
            this.doAutoDetection();
            log.debug("Auto-detection completed during rebuild");
        }

        // Phase 3: Rebuild each builder in dependency order
        log.debug("Phase 3: Rebuilding {} builders in dependency order", getBuilders().size());
        List<IBuilder<?>> sortedBuilders = new BuilderDependencyResolver(getBuilders(), this.providedBuilders).sortBuildersByDependencies();
        List<Object> newBuiltObjects = new ArrayList<>();

        for (IBuilder<?> builder : sortedBuilders) {
            log.debug("Rebuilding: {}", builder.getClass().getSimpleName());
            Object rebuilt;

            if (builder instanceof IRebuildableBuilder<?, ?> rebuildable) {
                rebuilt = rebuildable.rebuild();
                log.debug("Used rebuild() for builder: {}", builder.getClass().getSimpleName());
            } else {
                // For non-rebuildable builders, just call build() which returns cached instance
                rebuilt = builder.build();
                log.debug("Used build() (cached) for builder: {}", builder.getClass().getSimpleName());
            }

            if (rebuilt != null) {
                builtObjectsRegistry.put(IClass.getClass(rebuilt.getClass()), rebuilt);
                newBuiltObjects.add(rebuilt);
                log.debug("Registered rebuilt object of type: {}", rebuilt.getClass().getName());
            }

            // Initialize and start lifecycle objects immediately. onInit /
            // onStart are idempotent at the AbstractLifecycle level — see
            // doBuildInternal for the rationale.
            if (rebuilt instanceof ILifecycle lifecycleObject) {
                try {
                    lifecycleObject.onInit();
                } catch (LifecycleException e) {
                    log.debug("onInit failed during rebuild for {}: {}",
                            rebuilt.getClass().getSimpleName(), e.getMessage());
                }
                try {
                    lifecycleObject.onStart();
                } catch (LifecycleException e) {
                    log.debug("onStart failed during rebuild for {}: {}",
                            rebuilt.getClass().getSimpleName(), e.getMessage());
                }
                log.debug("Ready: {}", rebuilt.getClass().getSimpleName());
            }

            log.debug("Successfully rebuilt: {}", builder.getClass().getSimpleName());
        }

        this.built = new BuiltRegistry(builtObjectsRegistry);
        log.debug("Rebuild completed successfully with {} objects", builtObjectsRegistry.size());
        log.trace("Exiting rebuild()");

        return this.built;
    }

    /**
     * Sorts builders by their dependencies using topological sort (Kahn's
     * algorithm).
     * Builders with no dependencies are built first, then builders that depend on
     * them, etc.
     *
     * @return list of builders sorted by dependency order
     * @throws DslException if there is a circular dependency
     */


    /**
     * Drive the {@link com.garganttua.core.dsl.dependency.DependencyStage#CONFIGURATION
     * CONFIGURATION} stage globally: fire
     * {@code doConfigureWithDependencyBuilder} on every dependent builder.
     * Called once per Bootstrap lifetime — the per-dep idempotency tracking
     * in {@code BuilderDependency} guarantees each (consumer, dep) tuple
     * fires at most once even across {@code rebuild()}.
     *
     * <p>Named with a {@code Phase} suffix to avoid shadowing
     * {@link IDependentBuilder#runConfigurationStage()} which Bootstrap
     * itself inherits as a dependent builder.
     */
    private void runGlobalConfigurationPhase() throws DslException {
        log.trace("Entering runGlobalConfigurationPhase()");
        StageTimings timings = this.lastBuildTimings;
        for (IBuilder<?> builder : getBuilders()) {
            if (builder instanceof IDependentBuilder<?, ?> dep) {
                Instant t0 = Instant.now();
                try {
                    dep.runConfigurationStage();
                } finally {
                    if (timings != null) {
                        timings.record("configure:" + builder.getClass().getSimpleName(),
                                Duration.between(t0, Instant.now()));
                    }
                }
            }
        }
        runConfigurationContributors();
        log.trace("Exiting runGlobalConfigurationPhase()");
    }

    /**
     * Invoke any {@link IBootstrapConfigurationContributor} discovered via
     * {@link java.util.ServiceLoader} (e.g. the optional {@code garganttua-configuration}
     * module) with every registered builder, so external configuration files can be
     * applied to matching {@code @ConfigurableBuilder} instances before they build.
     * No-op when no contributor is on the classpath.
     */
    private void runConfigurationContributors() {
        List<IBuilder<?>> builders = getBuilders();
        for (IBootstrapConfigurationContributor contributor :
                java.util.ServiceLoader.load(IBootstrapConfigurationContributor.class)) {
            try {
                log.debug("Applying configuration contributor {}", contributor.getClass().getName());
                contributor.contribute(builders);
            } catch (RuntimeException e) {
                log.error("Configuration contributor {} failed: {}",
                        contributor.getClass().getName(), e.getMessage(), e);
            }
        }
    }


    /**
     * Gets the list of registered builders from all sources, merged by priority.
     *
     * @return unmodifiable list of builders
     */
    protected List<IBuilder<?>> getBuilders() {
        return List.copyOf(this.builderCollector.build().values());
    }

    private void propagatePackageToBuilders(String packageName, Map<String, IBuilder<?>> builderMap) {
        synchronized (builderMap) {
            for (IBuilder<?> builder : builderMap.values()) {
                if (builder instanceof IPackageableBuilder) {
                    IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
                    packageableBuilder.withPackage(packageName);
                    log.debug("Package '{}' propagated to builder: {}",
                            packageName, builder.getClass().getSimpleName());
                }
            }
        }
    }

    private void propagatePackagesToBuilder(IBuilder<?> builder) {
        if (builder instanceof IPackageableBuilder && !this.packages.isEmpty()) {
            IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
            synchronized (this.packages) {
                for (String packageName : this.packages) {
                    packageableBuilder.withPackage(packageName);
                }
            }
            log.debug("Propagated {} packages to builder: {}",
                    this.packages.size(), builder.getClass().getSimpleName());
        }
    }

    /**
     * Gets the set of configured packages.
     *
     * @return unmodifiable set of packages
     */
    protected Set<String> getConfiguredPackages() {
        return Set.copyOf(packages);
    }

    @Override
    protected String[] getPackagesForScanning() {
        return packages.toArray(new String[0]);
    }

    /**
     * Retrieves a built object from the registry by its class type.
     *
     * <p>
     * This method allows querying the registry of built objects after the bootstrap
     * process has completed. It returns an Optional containing the built object if
     * found, or an empty Optional if no object of the specified type was built.
     * </p>
     *
     * @param <T>   the type of the object to retrieve
     * @param clazz the class of the object to retrieve
     * @return an Optional containing the built object, or empty if not found
     * @throws IllegalStateException if called before build() has been executed
     */
    public <T> Optional<T> getBuiltObject(Class<T> clazz) {
        if (this.built == null) {
            throw new IllegalStateException("Cannot query registry before build() has been called");
        }

        IClass<T> iClass = IClass.getClass(clazz);
        Object obj = builtObjectsRegistry.get(iClass);
        if (obj != null && iClass.isInstance(obj)) {
            return Optional.of(iClass.cast(obj));
        }
        return Optional.empty();
    }

    /**
     * Gets all built objects from the registry.
     *
     * <p>
     * Returns an unmodifiable map of all objects that were built during the
     * bootstrap
     * process, keyed by their runtime class.
     * </p>
     *
     * @return unmodifiable map of built objects
     * @throws IllegalStateException if called before build() has been executed
     */
    public Map<IClass<?>, Object> getAllBuiltObjects() {
        if (this.built == null) {
            throw new IllegalStateException("Cannot query registry before build() has been called");
        }
        return Map.copyOf(builtObjectsRegistry);
    }

    /**
     * Implementation of IBuiltRegistry that wraps the built objects registry.
     */
    private static class BuiltRegistry implements IBuiltRegistry {

        private final Map<IClass<?>, Object> registry;

        public BuiltRegistry(Map<IClass<?>, Object> registry) {
            this.registry = Map.copyOf(registry);
        }

        @Override
        public Integer size() {
            return registry.size();
        }

        @Override
        public List<Object> toList() {
            return new ArrayList<>(registry.values());
        }

        @Override
        public <T> Optional<T> request(IClass<T> clazz) {
            Object obj = registry.get(clazz);
            if (obj != null && clazz.isInstance(obj)) {
                return Optional.of(clazz.cast(obj));
            }
            return Optional.empty();
        }
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        // No-op. Bootstrap-discoverable builders are loaded purely via
        // ServiceLoader (see {@link #loadBootstrapBuildersFromSpi()} called from
        // {@link #doAutoDetection()}); the previous reflection-based scan of
        // {@code @Bootstrap}-annotated classes was retired in 2026-05-26 to
        // remove the chicken-and-egg between Bootstrap and IReflection, and to
        // make the framework GraalVM-native-friendly.
        log.trace("doAutoDetectionWithDependency({}) — no-op (SPI handles discovery)",
                dependency.getClass().getSimpleName());
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // Nothing to do
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // Nothing to do
    }
}
