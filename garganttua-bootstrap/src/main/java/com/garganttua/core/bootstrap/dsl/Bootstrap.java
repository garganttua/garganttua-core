package com.garganttua.core.bootstrap.dsl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.garganttua.core.bootstrap.banner.BannerMode;
import com.garganttua.core.bootstrap.banner.BootstrapSummary;
import com.garganttua.core.bootstrap.banner.FileBanner;
import com.garganttua.core.bootstrap.banner.GarganttuaBanner;
import com.garganttua.core.bootstrap.banner.IBanner;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.IRebuildableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class Bootstrap extends AbstractAutomaticBuilder<IBoostrap, IBuiltRegistry> implements IBoostrap {

    private static final String DEFAULT_VERSION = "2.0.0-ALPHA01";

    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final List<IBuilder<?>> builders = Collections.synchronizedList(new ArrayList<>());
    private final Map<Class<?>, Object> builtObjectsRegistry = Collections.synchronizedMap(new HashMap<>());

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
        log.atTrace().log("Creating new Bootstrap instance");
        return new Bootstrap();
    }

    /**
     * Default constructor.
     */
    public Bootstrap() {
        log.atDebug().log("Bootstrap initialized");
    }

    @Override
    public IBoostrap withBanner(IBanner banner) {
        log.atTrace().log("Setting custom banner");
        this.banner = banner;
        return this;
    }

    @Override
    public IBoostrap withBannerMode(BannerMode mode) {
        log.atTrace().log("Setting banner mode: {}", mode);
        this.bannerMode = Objects.requireNonNull(mode, "Banner mode cannot be null");
        return this;
    }

    @Override
    public IBoostrap withApplicationName(String name) {
        log.atTrace().log("Setting application name: {}", name);
        this.applicationName = name != null ? name : "Garganttua";
        return this;
    }

    @Override
    public IBoostrap withApplicationVersion(String version) {
        log.atTrace().log("Setting application version: {}", version);
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
                bannerToPrint.print(System.out);
                break;
            case LOG:
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bannerToPrint.print(new PrintStream(baos, true, StandardCharsets.UTF_8));
                String bannerText = baos.toString(StandardCharsets.UTF_8);
                for (String line : bannerText.split("\n")) {
                    log.atInfo().log(line);
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
            log.atDebug().log("Using banner from classpath: {}", FileBanner.DEFAULT_BANNER_LOCATION);
            return fileBanner;
        }

        // Use default Garganttua banner
        return new GarganttuaBanner(applicationVersion, true);
    }

    @Override
    public IBoostrap withPackage(String packageName) {
        log.atTrace().log("Adding package: {}", packageName);
        Objects.requireNonNull(packageName, "Package name cannot be null");
        this.packages.add(packageName);

        // Propagate package to all IPackageableBuilder instances
        synchronized (this.builders) {
            for (IBuilder<?> builder : this.builders) {
                if (builder instanceof IPackageableBuilder) {
                    IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
                    packageableBuilder.withPackage(packageName);
                    log.atDebug().log("Package '{}' propagated to builder: {}",
                            packageName, builder.getClass().getSimpleName());
                }
            }
        }

        log.atDebug().log("Package added: {}", packageName);
        return this;
    }

    @Override
    public IBoostrap withPackages(String[] packageNames) {
        log.atTrace().log("Adding {} packages", packageNames != null ? packageNames.length : 0);
        Objects.requireNonNull(packageNames, "Package names array cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        log.atDebug().log("All packages added");
        return this;
    }

    @Override
    public IBoostrap withBuilder(IBuilder<?> builder) {
        log.atTrace().log("Adding builder: {}", builder != null ? builder.getClass().getSimpleName() : "null");
        Objects.requireNonNull(builder, "Builder cannot be null");
        this.builders.add(builder);

        if (builder instanceof IPackageableBuilder && !this.packages.isEmpty()) {
            IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
            synchronized (this.packages) {
                for (String packageName : this.packages) {
                    packageableBuilder.withPackage(packageName);
                }
            }
            log.atDebug().log("Propagated {} packages to builder: {}",
                    this.packages.size(), builder.getClass().getSimpleName());
        }

        log.atDebug().log("Builder added: {}", builder.getClass().getSimpleName());
        return this;
    }

    @Override
    public String[] getPackages() {
        log.atTrace().log("Getting packages, count: {}", packages.size());
        return packages.toArray(new String[0]);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection()");

        this.packages.stream()
                .flatMap(packageName -> ObjectReflectionHelper.getClassesWithAnnotation(packageName, com.garganttua.core.bootstrap.annotations.Bootstrap.class)
                        .stream())
                .forEach(builderClass -> {
                    try {
                        if (IBuilder.class.isAssignableFrom(builderClass)) {
                            IBuilder<?> builderInstance = (IBuilder<?>) ObjectReflectionHelper
                                    .instanciateNewObject(builderClass);
                            if (builderInstance instanceof IAutomaticBuilder) {
                                IAutomaticBuilder<?, ?> automaticBuilder = (IAutomaticBuilder<?, ?>) builderInstance;
                                automaticBuilder.autoDetect(true);
                                log.atDebug().log("Auto-detected builder {} with auto-detection enabled",
                                        builderClass.getSimpleName());
                            } else {
                                log.atDebug().log("Auto-detected builder {} (not automatic)",
                                        builderClass.getSimpleName());
                            }
                            this.withBuilder(builderInstance);
                        } else {
                            log.atWarn().log("Class {} has @Bootstrap annotation but does not implement IBuilder",
                                    builderClass.getName());
                        }
                    } catch (Exception e) {
                        throw new DslException("Failed to auto-detect builder: " + builderClass.getName(), e);
                    }
                });

        log.atDebug().log("Auto-detection completed for {} packages", packages.size());
        log.atTrace().log("Exiting doAutoDetection()");
    }

    @Override
    protected IBuiltRegistry doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");
        Instant startTime = Instant.now();

        // Print banner at the start of build
        printBanner();

        if (this.builders.isEmpty()) {
            log.atWarn().log("No builders registered, returning null");
            return null;
        }

        printPhase(1, "Resolving dependencies", this.builders.size() + " builders");

        // Phase 1: Resolve dependencies between builders
        resolveDependencies();

        // Phase 2: Sort builders by dependency order (topological sort)
        List<IBuilder<?>> sortedBuilders = sortBuildersByDependencies();
        log.atDebug().log("Builders sorted by dependency order: {}",
                sortedBuilders.stream()
                        .map(b -> b.getClass().getSimpleName())
                        .toList());

        printPhase(2, "Building components", sortedBuilders.size() + " builders");

        // Phase 3: Build all builders in dependency order and register built objects
        List<Object> builtObjects = new ArrayList<>();
        for (IBuilder<?> builder : sortedBuilders) {
            printBuilderStart(builder.getClass().getSimpleName());
            Object built = builder.build();
            builtObjects.add(built);

            // Register the built object by its class
            if (built != null) {
                builtObjectsRegistry.put(built.getClass(), built);
            }

            printBuilderComplete(builder.getClass().getSimpleName());
        }

        printPhase(3, "Starting lifecycle", builtObjects.size() + " components");

        // Phase 4: Initialize and start lifecycle-managed objects
        for (Object built : builtObjects) {
            if (built instanceof ILifecycle lifecycleObject) {
                try {
                    printLifecycleAction("Initializing", built.getClass().getSimpleName());
                    lifecycleObject.onInit();

                    printLifecycleAction("Starting", built.getClass().getSimpleName());
                    lifecycleObject.onStart();
                } catch (LifecycleException e) {
                    log.atError().log("Failed to initialize/start lifecycle object: {}",
                            built.getClass().getSimpleName(), e);
                    throw new DslException("Failed to initialize/start lifecycle object: "
                            + built.getClass().getSimpleName(), e);
                }
            }
        }

        Duration startupTime = Duration.between(startTime, Instant.now());

        // Print summary
        printSummary(builtObjects, startupTime);

        log.atTrace().log("Exiting doBuild()");

        return new BuiltRegistry(builtObjectsRegistry);
    }

    /**
     * Prints a phase header with colored output.
     */
    private void printPhase(int phaseNumber, String phaseName, String details) {
        if (bannerMode == BannerMode.OFF) {
            log.atDebug().log("Phase {}: {} ({})", phaseNumber, phaseName, details);
            return;
        }

        String CYAN = "\u001B[36m";
        String BOLD = "\u001B[1m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        System.out.println();
        System.out.println(CYAN + BOLD + "  ▶ Phase " + phaseNumber + ": " + phaseName + RESET +
                DIM + " (" + details + ")" + RESET);
    }

    /**
     * Prints builder start indicator.
     */
    private void printBuilderStart(String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.atDebug().log("Building: {}", builderName);
            return;
        }

        String DIM = "\u001B[2m";
        String RESET = "\u001B[0m";
        String YELLOW = "\u001B[33m";

        System.out.println(DIM + "     ○ " + RESET + YELLOW + builderName + RESET + DIM + " ..." + RESET);
    }

    /**
     * Prints builder completion indicator.
     */
    private void printBuilderComplete(String builderName) {
        if (bannerMode == BannerMode.OFF) {
            log.atDebug().log("Built: {}", builderName);
            return;
        }

        // Move cursor up and overwrite
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        System.out.print("\u001B[1A"); // Move up one line
        System.out.print("\u001B[2K"); // Clear line
        System.out.println(GREEN + "     ✓ " + RESET + builderName + DIM + " ready" + RESET);
    }

    /**
     * Prints lifecycle action.
     */
    private void printLifecycleAction(String action, String componentName) {
        if (bannerMode == BannerMode.OFF) {
            log.atDebug().log("{}: {}", action, componentName);
            return;
        }

        String BLUE = "\u001B[34m";
        String RESET = "\u001B[0m";
        String DIM = "\u001B[2m";

        System.out.println(BLUE + "     → " + RESET + action + " " + DIM + componentName + RESET);
    }

    /**
     * Prints the bootstrap summary.
     */
    private void printSummary(List<Object> builtObjects, Duration startupTime) {
        BootstrapSummary summary = new BootstrapSummary(bannerMode != BannerMode.OFF)
                .applicationName(applicationName)
                .applicationVersion(applicationVersion)
                .startupTime(startupTime)
                .buildersCount(this.builders.size())
                .builtObjectsCount(builtObjects.size());

        // Collect summary contributions from built objects
        for (Object built : builtObjects) {
            if (built instanceof IBootstrapSummaryContributor contributor) {
                String category = contributor.getSummaryCategory();
                contributor.getSummaryItems().forEach((name, value) ->
                        summary.addItem(category, name, value));
            }
        }

        if (bannerMode == BannerMode.CONSOLE) {
            summary.print(System.out);
        } else if (bannerMode == BannerMode.LOG) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            summary.print(new PrintStream(baos, true, StandardCharsets.UTF_8));
            String summaryText = baos.toString(StandardCharsets.UTF_8);
            for (String line : summaryText.split("\n")) {
                if (!line.isBlank()) {
                    log.atInfo().log(line);
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
     *   <li>Validates that initial build() has been called</li>
     *   <li>Stops all lifecycle-managed objects in reverse order</li>
     *   <li>Re-runs auto-detection to discover new @Bootstrap builders</li>
     *   <li>Rebuilds each builder in dependency order</li>
     *   <li>Re-initializes and starts all lifecycle-managed objects</li>
     * </ol>
     *
     * @return the updated built registry
     * @throws DslException if rebuild fails or if called before initial build()
     */
    @Override
    public IBuiltRegistry rebuild() throws DslException {
        log.atTrace().log("Entering rebuild()");

        if (this.built == null) {
            log.atError().log("Cannot rebuild before initial build()");
            throw new DslException("Cannot rebuild before initial build() has been called");
        }

        // Phase 1: Stop lifecycle objects (reverse order)
        log.atDebug().log("Phase 1: Stopping lifecycle objects in reverse order");
        List<Object> builtObjectsList = new ArrayList<>(builtObjectsRegistry.values());
        java.util.Collections.reverse(builtObjectsList);
        for (Object obj : builtObjectsList) {
            if (obj instanceof ILifecycle lifecycleObject) {
                try {
                    log.atDebug().log("Stopping lifecycle object: {}", obj.getClass().getSimpleName());
                    lifecycleObject.onStop();
                    log.atDebug().log("Stopped: {}", obj.getClass().getSimpleName());
                } catch (LifecycleException e) {
                    log.atWarn().log("Failed to stop lifecycle object: {} - continuing with rebuild",
                            obj.getClass().getSimpleName(), e);
                }
            }
        }

        // Phase 2: Re-run auto-detection for new @Bootstrap builders
        if (this.autoDetect.booleanValue()) {
            log.atDebug().log("Phase 2: Re-running auto-detection for new builders");
            this.doAutoDetection();
            log.atDebug().log("Auto-detection completed during rebuild");
        }

        // Phase 3: Rebuild each builder in dependency order
        log.atDebug().log("Phase 3: Rebuilding {} builders in dependency order", this.builders.size());
        List<IBuilder<?>> sortedBuilders = sortBuildersByDependencies();
        List<Object> newBuiltObjects = new ArrayList<>();

        for (IBuilder<?> builder : sortedBuilders) {
            log.atDebug().log("Rebuilding: {}", builder.getClass().getSimpleName());
            Object rebuilt;

            if (builder instanceof IRebuildableBuilder<?, ?> rebuildable) {
                rebuilt = rebuildable.rebuild();
                log.atDebug().log("Used rebuild() for builder: {}", builder.getClass().getSimpleName());
            } else {
                // For non-rebuildable builders, just call build() which returns cached instance
                rebuilt = builder.build();
                log.atDebug().log("Used build() (cached) for builder: {}", builder.getClass().getSimpleName());
            }

            if (rebuilt != null) {
                builtObjectsRegistry.put(rebuilt.getClass(), rebuilt);
                newBuiltObjects.add(rebuilt);
                log.atDebug().log("Registered rebuilt object of type: {}", rebuilt.getClass().getName());
            }

            log.atDebug().log("Successfully rebuilt: {}", builder.getClass().getSimpleName());
        }

        // Phase 4: Re-init and start lifecycle objects
        log.atDebug().log("Phase 4: Re-initializing and starting lifecycle objects");
        for (Object obj : newBuiltObjects) {
            if (obj instanceof ILifecycle lifecycleObject) {
                try {
                    log.atDebug().log("Initializing lifecycle object: {}", obj.getClass().getSimpleName());
                    lifecycleObject.onInit();
                    log.atDebug().log("Initialized: {}", obj.getClass().getSimpleName());

                    log.atDebug().log("Starting lifecycle object: {}", obj.getClass().getSimpleName());
                    lifecycleObject.onStart();
                    log.atDebug().log("Started: {}", obj.getClass().getSimpleName());
                } catch (LifecycleException e) {
                    log.atError().log("Failed to initialize/start lifecycle object during rebuild: {}",
                            obj.getClass().getSimpleName(), e);
                    throw new DslException("Failed to initialize/start lifecycle object during rebuild: "
                            + obj.getClass().getSimpleName(), e);
                }
            }
        }

        this.built = new BuiltRegistry(builtObjectsRegistry);
        log.atDebug().log("Rebuild completed successfully with {} objects", builtObjectsRegistry.size());
        log.atTrace().log("Exiting rebuild()");

        return this.built;
    }

    /**
     * Sorts builders by their dependencies using topological sort (Kahn's algorithm).
     * Builders with no dependencies are built first, then builders that depend on them, etc.
     *
     * @return list of builders sorted by dependency order
     * @throws DslException if there is a circular dependency
     */
    private List<IBuilder<?>> sortBuildersByDependencies() throws DslException {
        log.atTrace().log("Entering sortBuildersByDependencies()");

        Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph = new HashMap<>();
        Map<IBuilder<?>, Integer> inDegree = new HashMap<>();

        initializeDependencyGraph(dependencyGraph, inDegree);
        buildDependencyGraph(dependencyGraph, inDegree);
        List<IBuilder<?>> sortedBuilders = performTopologicalSort(dependencyGraph, inDegree);
        validateNoCyclicDependencies(sortedBuilders);

        log.atTrace().log("Exiting sortBuildersByDependencies()");
        return sortedBuilders;
    }

    /**
     * Initializes the dependency graph with all builders.
     */
    private void initializeDependencyGraph(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {
        for (IBuilder<?> builder : this.builders) {
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
        for (IBuilder<?> builder : this.builders) {
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

        Set<Class<? extends IObservableBuilder<?, ?>>> allDeps = new HashSet<>();
        allDeps.addAll(dependentBuilder.require());
        allDeps.addAll(dependentBuilder.use());

        for (Class<? extends IObservableBuilder<?, ?>> depClass : allDeps) {
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
        for (IBuilder<?> builder : this.builders) {
            if (inDegree.get(builder) == 0) {
                queue.add(builder);
                log.atDebug().log("Builder {} has no dependencies, will be built first",
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
                log.atDebug().log("Builder {} dependencies satisfied, adding to build queue",
                        dependent.getClass().getSimpleName());
            }
        }
    }

    /**
     * Validates that there are no cyclic dependencies.
     */
    private void validateNoCyclicDependencies(List<IBuilder<?>> sortedBuilders) throws DslException {
        if (sortedBuilders.size() != this.builders.size()) {
            List<String> notProcessed = this.builders.stream()
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
    private IBuilder<?> findBuilderInstanceByClass(Class<? extends IObservableBuilder<?, ?>> builderClass) {
        return this.builders.stream()
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
        log.atTrace().log("Entering resolveDependencies()");

        List<IObservableBuilder<?, ?>> observableBuilders = collectObservableBuilders();
        log.atDebug().log("Found {} observable builders", observableBuilders.size());

        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IDependentBuilder) {
                resolveDependenciesForBuilder((IDependentBuilder<?, ?>) builder, observableBuilders);
            }
        }

        log.atTrace().log("Exiting resolveDependencies()");
    }

    /**
     * Collects all observable builders from the builders list.
     *
     * @return list of observable builders
     */
    private List<IObservableBuilder<?, ?>> collectObservableBuilders() {
        List<IObservableBuilder<?, ?>> result = new ArrayList<>();
        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IObservableBuilder) {
                result.add((IObservableBuilder<?, ?>) builder);
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

        Set<Class<? extends IObservableBuilder<?, ?>>> requiredDeps = dependentBuilder.require();
        Set<Class<? extends IObservableBuilder<?, ?>>> usedDeps = dependentBuilder.use();

        log.atDebug().log("Builder {} requires {} dependencies and uses {} dependencies",
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
            Set<Class<? extends IObservableBuilder<?, ?>>> requiredDeps) throws DslException {

        for (Class<? extends IObservableBuilder<?, ?>> depClass : requiredDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency == null) {
                throw new DslException("Required dependency not found: " + depClass.getName()
                        + " for builder: " + dependentBuilder.getClass().getSimpleName());
            }
            dependentBuilder.provide(dependency);
            log.atDebug().log("Provided required dependency {} to {}",
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
            Set<Class<? extends IObservableBuilder<?, ?>>> usedDeps) throws DslException {

        for (Class<? extends IObservableBuilder<?, ?>> depClass : usedDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency != null) {
                dependentBuilder.provide(dependency);
                log.atDebug().log("Provided optional dependency {} to {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            } else {
                log.atDebug().log("Optional dependency {} not available for {}",
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
            Class<? extends IObservableBuilder<?, ?>> builderClass) {
        return builders.stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the list of registered builders.
     *
     * @return unmodifiable list of builders
     */
    protected List<IBuilder<?>> getBuilders() {
        return List.copyOf(builders);
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

    @Override
    protected IAnnotationScanner getAnnotationScanner() {
        return ObjectReflectionHelper.getAnnotationScanner();
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
     * @param <T> the type of the object to retrieve
     * @param clazz the class of the object to retrieve
     * @return an Optional containing the built object, or empty if not found
     * @throws IllegalStateException if called before build() has been executed
     */
    public <T> Optional<T> getBuiltObject(Class<T> clazz) {
        if (this.built == null) {
            throw new IllegalStateException("Cannot query registry before build() has been called");
        }

        Object obj = builtObjectsRegistry.get(clazz);
        if (obj != null && clazz.isInstance(obj)) {
            return Optional.of(clazz.cast(obj));
        }
        return Optional.empty();
    }

    /**
     * Gets all built objects from the registry.
     *
     * <p>
     * Returns an unmodifiable map of all objects that were built during the bootstrap
     * process, keyed by their runtime class.
     * </p>
     *
     * @return unmodifiable map of built objects
     * @throws IllegalStateException if called before build() has been executed
     */
    public Map<Class<?>, Object> getAllBuiltObjects() {
        if (this.built == null) {
            throw new IllegalStateException("Cannot query registry before build() has been called");
        }
        return Map.copyOf(builtObjectsRegistry);
    }

    /**
     * Implementation of IBuiltRegistry that wraps the built objects registry.
     */
    private static class BuiltRegistry implements IBuiltRegistry {

        private final Map<Class<?>, Object> registry;

        public BuiltRegistry(Map<Class<?>, Object> registry) {
            this.registry = Map.copyOf(registry);
        }

        @Override
        public <T> Optional<T> request(Class<T> clazz) {
            Object obj = registry.get(clazz);
            if (obj != null && clazz.isInstance(obj)) {
                return Optional.of(clazz.cast(obj));
            }
            return Optional.empty();
        }

        @Override
        public Integer size() {
            return registry.size();
        }

        @Override
        public List<Object> toList() {
            return new ArrayList<>(registry.values());
        }
    }
}
