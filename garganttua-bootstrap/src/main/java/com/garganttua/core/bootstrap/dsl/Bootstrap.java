package com.garganttua.core.bootstrap.dsl;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Optional;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.bootstrap.banner.BannerMode;
import com.garganttua.core.bootstrap.banner.BootstrapSummary;
import com.garganttua.core.bootstrap.banner.FileBanner;
import com.garganttua.core.bootstrap.banner.GarganttuaBanner;
import com.garganttua.core.bootstrap.banner.IBanner;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
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
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

import jakarta.annotation.Priority;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Builder for bootstrapping Garganttua applications.
 *
 * <p>
 * The {@code BootstrapBuilder} provides a fluent API for configuring and
 * initializing
 * a Garganttua application. It manages the lifecycle of various builders
 * (injection context,
 * runtime, expression context, etc.) and orchestrates their initialization in
 * the correct order.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * Object app = new BootstrapBuilder()
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
 * @since 2.0.0-ALPHA01
 */
public class Bootstrap extends AbstractAutomaticDependentBuilder<IBoostrap, IBuiltRegistry>
        implements IBoostrap, IObservable<ObservableEvent> {
    private static final IDiagnostic log = Diagnostics.of(Bootstrap.class);

    private static final String DEFAULT_VERSION = com.garganttua.core.bootstrap.GarganttuaVersion.getVersion();
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    /**
     * UTF-8 console stream for printing banner art and progress glyphs (▶, ○, ✓, →).
     * On JVMs where the default {@code System.out} charset is not UTF-8 (e.g.
     * ANSI_X3.4-1968 on locale-less Linux containers), Unicode glyphs are
     * replaced by '?' when written through {@code System.out}. This stream
     * writes UTF-8 bytes directly to {@link FileDescriptor#out}, bypassing the
     * default {@code PrintStream} encoding.
     */
    private static final PrintStream CONSOLE_OUT = createUtf8ConsoleStream();

    private static PrintStream createUtf8ConsoleStream() {
        try {
            if (StandardCharsets.UTF_8.equals(System.out.charset())) {
                return System.out;
            }
        } catch (Throwable ignored) {
            // PrintStream.charset() is Java 18+; pre-21 fall through to wrap.
        }
        try {
            return new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            return System.out;
        }
    }

    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, IBuilder<?>> manualBuilders = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, IBuilder<?>> autoDetectedBuilders = Collections.synchronizedMap(new HashMap<>());
    private final MultiSourceCollector<String, IBuilder<?>> builderCollector;
    private int manualBuilderSeq = 0;
    private int autoDetectedBuilderSeq = 0;
    private final Map<IClass<?>, Object> builtObjectsRegistry = Collections.synchronizedMap(new HashMap<>());
    private final List<IObservableBuilder<?, ?>> providedBuilders = new ArrayList<>();
    private final Set<IBuilder<?>> spiAutoLoadedBuilders =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private final ObservableRegistry<ObservableEvent> observers = new ObservableRegistry<>();
    private boolean reflectionBuilderProvided = false;
    private boolean spiFallbackEnabled = true;

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
    public static IBoostrap builder() {
        log.trace("Creating new Bootstrap instance");
        return new Bootstrap();
    }

    /**
     * Default constructor.
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
        ensureReflectionAvailable();
        return Set.of(DependencySpec.require(IClass.getClass(IReflectionBuilder.class), DependencyPhase.AUTO_DETECT));
    }

    /**
     * Bootstraps a default {@link IReflection} via {@link ServiceLoader} when
     * none has been installed yet. No-op when one is already present, and also
     * no-op when no provider JAR is on the classpath (in which case the
     * downstream {@code IClass.getClass(...)} call will surface a clear error).
     */
    private static void ensureReflectionAvailable() {
        try {
            IClass.getReflection();
            return; // already configured by user code
        } catch (IllegalStateException ignored) {
            // No reflection bound — fall through to SPI bootstrap.
        }
        IReflectionBuilder rb = buildReflectionBuilderFromSpi();
        if (rb != null) {
            try {
                rb.build();
                // ReflectionBuilder.doBuild() calls IClass.setReflection() internally,
                // so the global facade is now configured for subsequent IClass.getClass()
                // calls in this Bootstrap's constructor.
            } catch (DslException e) {
                log.warn("SPI-built ReflectionBuilder failed to build: {}", e.getMessage());
            }
        }
    }

    @Override
    public IBoostrap withBanner(IBanner banner) {
        log.trace("Setting custom banner");
        this.banner = banner;
        return this;
    }

    @Override
    public IBoostrap withBannerMode(BannerMode mode) {
        log.trace("Setting banner mode: {}", mode);
        this.bannerMode = Objects.requireNonNull(mode, "Banner mode cannot be null");
        return this;
    }

    @Override
    public IBoostrap withApplicationName(String name) {
        log.trace("Setting application name: {}", name);
        this.applicationName = name != null ? name : "Garganttua";
        return this;
    }

    @Override
    public IBoostrap withApplicationVersion(String version) {
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
                bannerToPrint.print(CONSOLE_OUT);
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
    public IBoostrap withPackage(String packageName) {
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
    public IBoostrap withPackages(String[] packageNames) {
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
    public IBoostrap provide(IObservableBuilder<?, ?> dependency) throws DslException {
        this.providedBuilders.add(dependency);
        if (dependency instanceof IReflectionBuilder) {
            this.reflectionBuilderProvided = true;
        }
        return (IBoostrap) super.provide(dependency);
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
     */
    public IBoostrap disableSpiFallback() {
        this.spiFallbackEnabled = false;
        return this;
    }

    @Override
    public IBoostrap withBuilder(IBuilder<?> builder) {
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
        log.trace("Entering doAutoDetection()");

        // SPI fallback (Phase 2): even when the global IClass.reflection was
        // installed at constructor time, we still need to feed the bootstrap's
        // own dependency chain with an IReflectionBuilder so downstream
        // builders receive it via provide(). Skipped if a reflection builder
        // was already provided explicitly or if the user opted out.
        //
        // The builder is BOTH provide()'d (to satisfy Bootstrap's own
        // require(IReflectionBuilder) contract) and withBuilder()'d (so it
        // appears in getBuilders() for downstream builders' dependency
        // resolution). It is tracked in spiAutoLoadedBuilders so Phase 3 will
        // SKIP its onInit/onStart — its lifecycle belongs to whichever
        // top-level builder (e.g. ApiBuilder) actually consumes the produced
        // IReflection, not to Bootstrap.
        if (this.autoDetect.booleanValue() && this.spiFallbackEnabled && !this.reflectionBuilderProvided) {
            IReflectionBuilder spiBuilder = buildReflectionBuilderFromSpi();
            if (spiBuilder != null) {
                log.debug("Registering SPI-bootstrapped IReflectionBuilder on this Bootstrap");
                this.provide(spiBuilder);
                this.withBuilder(spiBuilder);
                this.spiAutoLoadedBuilders.add(spiBuilder);
            }
        }

        // SPI fallback for standard builders (InjectionContextBuilder,
        // ExpressionContextBuilder, …). Each garganttua-core module that
        // exposes a bootstrap-discoverable builder ships an
        // IBootstrapBuilderFactory via META-INF/services. Loaded only when
        // autoDetect is enabled and the SPI fallback is not opted out.
        // Existing classes (already registered via withBuilder) are skipped.
        if (this.autoDetect.booleanValue() && this.spiFallbackEnabled) {
            loadBootstrapBuildersFromSpi();
        }

        log.debug("Auto-detection completed for {} packages", packages.size());
        log.trace("Exiting doAutoDetection()");
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

    /**
     * Discovers {@link IReflectionProvider} and {@link IAnnotationScanner}
     * implementations via {@link ServiceLoader}, populates a fresh
     * {@link ReflectionBuilder} with them ordered by descending
     * {@link Priority}, and returns it.
     *
     * @return the populated builder, or {@code null} if no provider was found
     *         on the classpath (in which case the existing
     *         {@code DependencySpec.require(IReflectionBuilder)} contract will
     *         take over and produce a clear "dependency not provided" error)
     */
    /** Tracks whether the SPI summary line has already been emitted at INFO. */
    private static final AtomicBoolean SPI_SUMMARY_LOGGED = new AtomicBoolean(false);

    private static IReflectionBuilder buildReflectionBuilderFromSpi() {
        log.trace("Attempting SPI bootstrap of IReflectionBuilder");
        IReflectionBuilder rb = ReflectionBuilder.builder();
        List<String> providerLabels = new ArrayList<>();
        List<String> scannerLabels = new ArrayList<>();

        for (IReflectionProvider provider : sortedByPriority(ServiceLoader.load(IReflectionProvider.class))) {
            int priority = readPriority(provider);
            log.debug("SPI: registering reflection provider {} with priority {}",
                    provider.getClass().getName(), priority);
            rb.withProvider(provider, priority);
            providerLabels.add(provider.getClass().getSimpleName() + "@" + priority);
        }

        for (IAnnotationScanner scanner : sortedByPriority(ServiceLoader.load(IAnnotationScanner.class))) {
            int priority = readPriority(scanner);
            log.debug("SPI: registering annotation scanner {} with priority {}",
                    scanner.getClass().getName(), priority);
            rb.withScanner(scanner, priority);
            scannerLabels.add(scanner.getClass().getSimpleName() + "@" + priority);
        }

        if (providerLabels.isEmpty() && scannerLabels.isEmpty()) {
            log.debug("SPI bootstrap found no IReflectionProvider/IAnnotationScanner on the classpath");
            return null;
        }

        // First invocation logs at INFO (visible by default); subsequent ones at
        // DEBUG to avoid noisy duplicates when Bootstrap re-runs the loader from
        // both the constructor (ensureReflectionAvailable) and the build phase
        // (doAutoDetection).
        String summary = String.format("SPI bootstrap: providers=%s, scanners=%s",
                providerLabels, scannerLabels);
        if (SPI_SUMMARY_LOGGED.compareAndSet(false, true)) {
            log.info(summary);
        } else {
            log.debug(summary);
        }
        return rb;
    }

    private static <T> List<T> sortedByPriority(ServiceLoader<T> loader) {
        List<T> list = new ArrayList<>();
        for (T svc : loader) {
            list.add(svc);
        }
        list.sort(Comparator.comparingInt(Bootstrap::readPriority).reversed());
        return list;
    }

    private static int readPriority(Object svc) {
        Priority p = svc.getClass().getAnnotation(Priority.class);
        return p == null ? 0 : p.value();
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

    private IBuiltRegistry doBuildInternal(Instant startTime) throws DslException {
        // Print banner at the start of build
        printBanner();

        List<IBuilder<?>> allBuilders = getBuilders();

        if (allBuilders.isEmpty()) {
            log.warn("No builders registered, returning null");
            return null;
        }

        printPhase(1, "Resolving dependencies", allBuilders.size() + " builders");

        // Phase 1: Resolve dependencies between builders
        try (ObservabilityEmitter.Scope phase = ObservabilityEmitter.joinCurrent()) {
            phase.fireStart("bootstrap:phase:resolve");
            try {
                resolveDependencies();
                phase.fireEnd("bootstrap:phase:resolve");
            } catch (RuntimeException e) {
                phase.fireError("bootstrap:phase:resolve", e);
                throw e;
            }
        }

        // Phase 2: Sort builders by dependency order (topological sort)
        List<IBuilder<?>> sortedBuilders = sortBuildersByDependencies();
        log.debug("Builders sorted by dependency order: {}",
                sortedBuilders.stream()
                        .map(b -> b.getClass().getSimpleName())
                        .toList());

        printPhase(2, "Building components", sortedBuilders.size() + " builders");

        // Phase 3: Build all builders in dependency order, initialize lifecycle immediately
        List<Object> builtObjects = new ArrayList<>();
        for (IBuilder<?> builder : sortedBuilders) {
            printBuilderStart(builder.getClass().getSimpleName());
            String builderSource = "bootstrap:builder:" + builder.getClass().getSimpleName();
            Object built;
            try (ObservabilityEmitter.Scope buildScope = ObservabilityEmitter.joinCurrent()) {
                buildScope.fireStart(builderSource);
                try {
                    built = builder.build();
                    buildScope.fireEnd(builderSource);
                } catch (RuntimeException e) {
                    buildScope.fireError(builderSource, e);
                    throw e;
                }
            }
            builtObjects.add(built);

            // Register the built object by its class
            if (built != null) {
                builtObjectsRegistry.put(IClass.getClass(built.getClass()), built);
            }

            // Initialize and start lifecycle objects immediately so downstream
            // builders can use them during their own build/auto-detection phase.
            // Some builders already init/start the object during build() — tolerate that.
            //
            // SPI auto-loaded auxiliaries (e.g. InjectionContextBuilder /
            // ExpressionContextBuilder registered via IBootstrapBuilderFactory)
            // are skipped here: their lifecycle belongs to the top-level
            // consumer that pulls them in via dep resolution. Initializing them
            // here would cause that consumer's doInit() to double-init them and
            // throw LifecycleException.
            if (built instanceof ILifecycle lifecycleObject && !this.spiAutoLoadedBuilders.contains(builder)) {
                try {
                    lifecycleObject.onInit();
                } catch (LifecycleException e) {
                    log.debug("Lifecycle already initialized: {}", built.getClass().getSimpleName());
                }
                try {
                    lifecycleObject.onStart();
                } catch (LifecycleException e) {
                    log.debug("Lifecycle already started: {}", built.getClass().getSimpleName());
                }
            }

            printBuilderComplete(builder.getClass().getSimpleName());
        }

        Duration startupTime = Duration.between(startTime, Instant.now());

        // Print summary
        printSummary(builtObjects, startupTime);

        log.trace("Exiting doBuild()");

        return new BuiltRegistry(builtObjectsRegistry);
    }

    /**
     * Prints a phase header with colored output.
     */
    private void printPhase(int phaseNumber, String phaseName, String details) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Phase {}: {} ({})", phaseNumber, phaseName, details);
            return;
        }

        String CYAN = "\u001B[36m";
        String BOLD = "\u001B[1m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        CONSOLE_OUT.println();
        CONSOLE_OUT.println(CYAN + BOLD + "  ▶ Phase " + phaseNumber + ": " + phaseName + RESET +
                DIM + " (" + details + ")" + RESET);
    }

    /**
     * Prints builder start indicator.
     */
    private void printBuilderStart(String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Building: {}", builderName);
            return;
        }

        String DIM = "\u001B[2m";
        String RESET = "\u001B[0m";
        String YELLOW = "\u001B[33m";

        CONSOLE_OUT.println(DIM + "     ○ " + RESET + YELLOW + builderName + RESET + DIM + " ..." + RESET);
    }

    /**
     * Prints builder completion indicator.
     */
    private void printBuilderComplete(String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("Built: {}", builderName);
            return;
        }

        // Move cursor up and overwrite
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        CONSOLE_OUT.print("\u001B[1A"); // Move up one line
        CONSOLE_OUT.print("\u001B[2K"); // Clear line
        CONSOLE_OUT.println(GREEN + "     ✓ " + RESET + builderName + DIM + " ready" + RESET);
    }

    /**
     * Prints lifecycle action.
     */
    private void printLifecycleAction(String action, String componentName) {
        if (bannerMode == BannerMode.OFF) {
            log.debug("{}: {}", action, componentName);
            return;
        }

        String BLUE = "\u001B[34m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        CONSOLE_OUT.println(BLUE + "     → " + RESET + action + " " + DIM + componentName + RESET);
    }

    /**
     * Prints the bootstrap summary.
     */
    private void printSummary(List<Object> builtObjects, Duration startupTime) {
        BootstrapSummary summary = new BootstrapSummary(bannerMode != BannerMode.OFF)
                .applicationName(applicationName)
                .applicationVersion(applicationVersion)
                .startupTime(startupTime)
                .buildersCount(getBuilders().size())
                .builtObjectsCount(builtObjects.size());

        // Collect summary contributions from built objects
        for (Object built : builtObjects) {
            if (built instanceof IBootstrapSummaryContributor contributor) {
                String category = contributor.getSummaryCategory();
                contributor.getSummaryItems().forEach((name, value) -> summary.addItem(category, name, value));
            }
        }

        if (bannerMode == BannerMode.CONSOLE) {
            summary.print(CONSOLE_OUT);
        } else if (bannerMode == BannerMode.LOG) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            summary.print(new PrintStream(baos, true, StandardCharsets.UTF_8));
            String summaryText = baos.toString(StandardCharsets.UTF_8);
            for (String line : summaryText.split("\n")) {
                if (!line.isBlank()) {
                    log.info(line);
                }
            }
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
        List<IBuilder<?>> sortedBuilders = sortBuildersByDependencies();
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

            // Initialize and start lifecycle objects immediately.
            // Some builders already init/start the object during build() — tolerate that.
            // SPI auto-loaded auxiliaries are skipped here (see same rationale in doBuildInternal).
            if (rebuilt instanceof ILifecycle lifecycleObject && !this.spiAutoLoadedBuilders.contains(builder)) {
                try {
                    lifecycleObject.onInit();
                } catch (LifecycleException e) {
                    log.debug("Lifecycle already initialized during rebuild: {}", rebuilt.getClass().getSimpleName());
                }
                try {
                    lifecycleObject.onStart();
                } catch (LifecycleException e) {
                    log.debug("Lifecycle already started during rebuild: {}", rebuilt.getClass().getSimpleName());
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
    private List<IBuilder<?>> sortBuildersByDependencies() throws DslException {
        log.trace("Entering sortBuildersByDependencies()");

        Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph = new HashMap<>();
        Map<IBuilder<?>, Integer> inDegree = new HashMap<>();

        initializeDependencyGraph(dependencyGraph, inDegree);
        buildDependencyGraph(dependencyGraph, inDegree);
        List<IBuilder<?>> sortedBuilders = performTopologicalSort(dependencyGraph, inDegree);
        validateNoCyclicDependencies(sortedBuilders);

        log.trace("Exiting sortBuildersByDependencies()");
        return sortedBuilders;
    }

    /**
     * Initializes the dependency graph with all builders.
     */
    private void initializeDependencyGraph(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {
        for (IBuilder<?> builder : getBuilders()) {
            dependencyGraph.put(builder, new HashSet<>());
            inDegree.put(builder, 0);
        }
    }

    /**
     * Builds the dependency graph by analyzing builder dependencies.
     */
    private void buildDependencyGraph(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {
        for (IBuilder<?> builder : getBuilders()) {
            if (builder instanceof IDependentBuilder) {
                processDependentBuilder((IDependentBuilder<?, ?>) builder, dependencyGraph, inDegree);
            }
        }
    }

    /**
     * Processes a single dependent builder to update the dependency graph.
     */
    private void processDependentBuilder(
            IDependentBuilder<?, ?> dependentBuilder,
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {

        Set<IClass<? extends IObservableBuilder<?, ?>>> allDeps = new HashSet<>();
        allDeps.addAll(dependentBuilder.require());
        allDeps.addAll(dependentBuilder.use());

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : allDeps) {
            IBuilder<?> dependency = findBuilderInstanceByClass(depClass);
            if (dependency != null) {
                dependencyGraph.get(dependency).add(dependentBuilder);
                inDegree.put(dependentBuilder, inDegree.get(dependentBuilder) + 1);
            }
        }
    }

    /**
     * Performs topological sort using Kahn's algorithm.
     */
    private List<IBuilder<?>> performTopologicalSort(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {

        Queue<IBuilder<?>> queue = initializeQueueWithNoDependencies(inDegree);
        List<IBuilder<?>> sortedBuilders = new ArrayList<>();

        while (!queue.isEmpty()) {
            IBuilder<?> current = queue.poll();
            sortedBuilders.add(current);
            processBuilderDependents(current, dependencyGraph, inDegree, queue);
        }

        return sortedBuilders;
    }

    /**
     * Initializes the queue with builders that have no dependencies.
     */
    private Queue<IBuilder<?>> initializeQueueWithNoDependencies(Map<IBuilder<?>, Integer> inDegree) {
        Queue<IBuilder<?>> queue = new LinkedList<>();
        for (IBuilder<?> builder : getBuilders()) {
            if (inDegree.get(builder) == 0) {
                queue.add(builder);
                log.debug("Builder {} has no dependencies, will be built first",
                        builder.getClass().getSimpleName());
            }
        }
        return queue;
    }

    /**
     * Processes all builders that depend on the current builder.
     */
    private void processBuilderDependents(
            IBuilder<?> current,
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree,
            Queue<IBuilder<?>> queue) {

        for (IBuilder<?> dependent : dependencyGraph.get(current)) {
            int newInDegree = inDegree.get(dependent) - 1;
            inDegree.put(dependent, newInDegree);
            if (newInDegree == 0) {
                queue.add(dependent);
                log.debug("Builder {} dependencies satisfied, adding to build queue",
                        dependent.getClass().getSimpleName());
            }
        }
    }

    /**
     * Validates that there are no cyclic dependencies.
     */
    private void validateNoCyclicDependencies(List<IBuilder<?>> sortedBuilders) throws DslException {
        List<IBuilder<?>> allBuilders = getBuilders();
        if (sortedBuilders.size() != allBuilders.size()) {
            List<String> notProcessed = allBuilders.stream()
                    .filter(b -> !sortedBuilders.contains(b))
                    .map(b -> b.getClass().getSimpleName())
                    .toList();
            throw new DslException("Circular dependency detected among builders: " + notProcessed);
        }
    }

    /**
     * Finds a builder instance by its class.
     *
     * @param builderClass the class to search for
     * @return the builder instance or null if not found
     */
    private IBuilder<?> findBuilderInstanceByClass(IClass<? extends IObservableBuilder<?, ?>> builderClass) {
        return getBuilders().stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves dependencies between builders by providing observable builders to
     * dependent builders.
     *
     * @throws DslException if dependency resolution fails
     */
    private void resolveDependencies() throws DslException {
        log.trace("Entering resolveDependencies()");

        List<IObservableBuilder<?, ?>> observableBuilders = collectObservableBuilders();
        log.debug("Found {} observable builders", observableBuilders.size());

        for (IBuilder<?> builder : getBuilders()) {
            if (builder instanceof IDependentBuilder) {
                resolveDependenciesForBuilder((IDependentBuilder<?, ?>) builder, observableBuilders);
            }
        }

        log.trace("Exiting resolveDependencies()");
    }

    /**
     * Collects all observable builders that can serve as dependencies for
     * other builders' dep resolution. Includes both managed builders (registered
     * via {@code withBuilder()}, which Phase 3 lifecycle-manages) AND provided
     * deps (registered via {@code provide()}, which are NOT lifecycle-managed
     * here — their consumer owns their lifecycle).
     *
     * @return list of observable builders visible to dependency resolution
     */
    private List<IObservableBuilder<?, ?>> collectObservableBuilders() {
        List<IObservableBuilder<?, ?>> result = new ArrayList<>();
        for (IBuilder<?> builder : getBuilders()) {
            if (builder instanceof IObservableBuilder) {
                result.add((IObservableBuilder<?, ?>) builder);
            }
        }
        // Provided deps are also candidates for satisfying other builders'
        // require() / use() — e.g. SPI-loaded InjectionContextBuilder that
        // we register via provide() (not withBuilder()) to avoid Phase 3
        // pre-initializing it.
        for (IObservableBuilder<?, ?> provided : this.providedBuilders) {
            if (!result.contains(provided)) {
                result.add(provided);
            }
        }
        return result;
    }

    /**
     * Resolves dependencies for a single dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @throws DslException if a required dependency is not found
     */
    private void resolveDependenciesForBuilder(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders) throws DslException {

        Set<IClass<? extends IObservableBuilder<?, ?>>> requiredDeps = dependentBuilder.require();
        Set<IClass<? extends IObservableBuilder<?, ?>>> usedDeps = dependentBuilder.use();

        log.debug("Builder {} requires {} dependencies and uses {} dependencies",
                dependentBuilder.getClass().getSimpleName(), requiredDeps.size(), usedDeps.size());

        provideRequiredDependencies(dependentBuilder, observableBuilders, requiredDeps);
        provideOptionalDependencies(dependentBuilder, observableBuilders, usedDeps);
    }

    /**
     * Provides required dependencies to a dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @param requiredDeps       set of required dependency classes
     * @throws DslException if a required dependency is not found
     */
    private void provideRequiredDependencies(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders,
            Set<IClass<? extends IObservableBuilder<?, ?>>> requiredDeps) throws DslException {

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : requiredDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency == null) {
                throw new DslException("Required dependency not found: " + depClass.getName()
                        + " for builder: " + dependentBuilder.getClass().getSimpleName());
            }
            dependentBuilder.provide(dependency);
            log.debug("Provided required dependency {} to {}",
                    depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
        }
    }

    /**
     * Provides optional dependencies to a dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @param usedDeps           set of optional dependency classes
     * @throws DslException if providing the dependency fails
     */
    private void provideOptionalDependencies(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders,
            Set<IClass<? extends IObservableBuilder<?, ?>>> usedDeps) throws DslException {

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : usedDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency != null) {
                dependentBuilder.provide(dependency);
                log.debug("Provided optional dependency {} to {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            } else {
                log.debug("Optional dependency {} not available for {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            }
        }
    }

    /**
     * Finds a builder by its class in the list of observable builders.
     *
     * @param builders     the list of observable builders
     * @param builderClass the class to search for
     * @return the builder instance or null if not found
     */
    private IObservableBuilder<?, ?> findBuilderByClass(
            List<IObservableBuilder<?, ?>> builders,
            IClass<? extends IObservableBuilder<?, ?>> builderClass) {
        return builders.stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
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
