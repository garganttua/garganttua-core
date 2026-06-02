package com.garganttua.core.nativve;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.IReflectionUsageReporter;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.ReflectedBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

/**
 * Fluent builder that assembles an {@link INativeConfiguration} from explicitly
 * declared reflection entries and resources plus packages scanned for
 * {@link Reflected @Reflected} elements and {@link ReflectedBuilder @ReflectedBuilder}
 * reporters.
 *
 * <p>Annotated with {@link Bootstrap} so it is auto-detected and registered when
 * {@code Bootstrap.autoDetect(true)} is used; it depends on an
 * {@link IReflectionBuilder} for classpath scanning.
 */
@Bootstrap
public class NativeConfigurationBuilder
        extends AbstractAutomaticDependentBuilder<INativeConfigurationBuilder, INativeConfiguration>
        implements INativeConfigurationBuilder {
    private static final Logger log = Logger.getLogger(NativeConfigurationBuilder.class);

    private Set<IReflectionConfigurationEntryBuilder> reflectionEntries = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
    private NativeConfigurationMode mode = NativeConfigurationMode.override;
    private final Set<String> resources = new HashSet<>();
    private String resourcesPath = null;
    private String reflectionPath = null;
    private String configNamespace = "";
    private Set<INativeBuilder<?, ?>> nativeConfigurationBuilder = new HashSet<>();

    /** Creates a builder that depends on an {@link IReflectionBuilder} for package scanning. */
    public NativeConfigurationBuilder() {
        super(Set.of(
                DependencySpec.autoDetect(IClass.getClass(IReflectionBuilder.class)),
                DependencySpec.use(IClass.getClass(IReflectionBuilder.class))));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.trace("Starting auto-detection for native configuration with dependency {}",
                dependency.getClass().getSimpleName());
        if (dependency instanceof IReflection reflection) {
            this.detectNativeElements(reflection);
            this.detectNativeConfigurationBuilders(reflection);
        }
        log.trace("Completed auto-detection for native configuration with dependency {}",
                dependency.getClass().getSimpleName());
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // No pre-build behavior needed
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build behavior needed
    }

    @Override
    public INativeConfigurationBuilder withPackages(String[] packageNames) {
        log.trace("Adding {} packages to native configuration", packageNames.length);
        this.packages.addAll(Set.of(packageNames));
        log.debug("Added packages: {}", String.join(", ", packageNames));
        return this;
    }

    @Override
    public INativeConfigurationBuilder withPackage(String packageName) {
        log.trace("Adding package to native configuration: {}", packageName);
        this.packages.add(packageName);
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected INativeConfiguration doBuild() throws DslException {
        log.trace("Building native configuration");
        Objects.requireNonNull(this.reflectionPath, "Reflection path cannot be null");
        Objects.requireNonNull(this.resourcesPath, "Resouces path cannot be null");

        log.debug("Building native configuration with {} reflection entries and {} resources",
                reflectionEntries.size(), resources.size());
        INativeConfiguration config = new NativeConfiguration(
                this.mode,
                this.reflectionEntries.stream().map(e -> e.build()).collect(Collectors.toSet()),
                this.resources,
                this.resourcesPath,
                this.reflectionPath,
                this.configNamespace);
        log.debug("Native configuration built successfully");
        return config;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Starting auto-detection for native configuration");
        log.trace("Completed auto-detection for native configuration");
    }

    private void detectNativeConfigurationBuilders(IReflection reflection) {
        log.trace("Detecting reflection usage reporters in packages");
        IClass<ReflectedBuilder> reflectedBuilderAnnotation = reflection
                .getClass(ReflectedBuilder.class);
        IClass<INativeBuilder> nativeBuilderInterface = reflection.getClass(INativeBuilder.class);
        this.packages.forEach(
                p -> {
                    log.debug("Scanning package for @ReflectedBuilder: {}", p);
                    reflection.getClassesWithAnnotation(p, reflectedBuilderAnnotation).forEach(c -> {
                        if (nativeBuilderInterface.isAssignableFrom(c)) {
                            log.debug("Found reflection usage reporter: {}", c.getName());
                            try {
                                INativeBuilder<?, ?> nativeBuilder = (INativeBuilder<?, ?>) reflection.newInstance(c);
                                nativeBuilder.withPackages(getPackages());
                                IReflectionUsageReporter reporter = nativeBuilder.build();
                                reflectionEntries.addAll(reporter.reflectionUsage());
                                log.debug("Loaded reflection usage from reporter: {}", c.getName());
                            } catch (ReflectionException e) {
                                log.error("Failed to instantiate reflection usage reporter: {}",
                                        c.getName());
                            }
                        }
                    });
                });
    }

    private void detectNativeElements(IReflection reflection) {
        log.trace("Detecting @Reflected annotated elements in packages");
        IClass<Reflected> reflectedAnnotation = reflection.getClass(Reflected.class);
        this.packages.forEach(
                p -> {
                    log.debug("Scanning package for @Reflected annotations: {}", p);
                    reflection.getClassesWithAnnotation(p, reflectedAnnotation).forEach(c -> {
                        if (c.isAnnotation()) {
                            log.debug("Found @Reflected annotation type: {}", c.getName());
                            reflectionEntries.add(new ReflectConfigEntryBuilder(c).allPublicClasses(true)
                                    .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                                    .queryAllDeclaredConstructors(true));
                        } else {
                            log.debug("Found @Reflected class: {}", c.getName());
                            reflectionEntries.add(new ReflectConfigEntryBuilder(c).autoDetect(true));
                        }
                    });
                });
    }

    @Override
    public INativeConfigurationBuilder resourcesPath(String path) {
        log.trace("Setting resources path: {}", path);
        this.resourcesPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public INativeConfigurationBuilder reflectionPath(String path) {
        log.trace("Setting reflection path: {}", path);
        this.reflectionPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public INativeConfigurationBuilder configNamespace(String namespace) {
        log.trace("Setting config namespace: {}", namespace);
        this.configNamespace = namespace != null ? namespace.trim() : "";
        return this;
    }

    @Override
    public IReflectionConfigurationEntryBuilder reflectionEntry(IClass<?> clazz) {
        log.trace("Adding reflection entry for class: {}", clazz.getName());
        IReflectionConfigurationEntryBuilder entry = new ReflectConfigEntryBuilder(clazz);
        this.reflectionEntries.add(entry);
        log.debug("Added reflection entry: {}", clazz.getName());
        return entry;
    }

    @Override
    public INativeConfigurationBuilder resource(IClass<?> resource) {
        log.trace("Adding resource for class: {}", resource.getName());
        this.resource(
                Objects.requireNonNull(resource, "ressource cannot be null").getName().replace('.', '/') + ".class");
        return this;
    }

    @Override
    public INativeConfigurationBuilder resource(String resource) {
        log.trace("Adding resource: {}", resource);
        this.resources.add("\\Q" + Objects.requireNonNull(resource, "ressource cannot be null") + "\\E");
        log.debug("Added resource to native configuration: {}", resource);
        return this;
    }

    @Override
    public INativeConfigurationBuilder configurationBuilder(INativeBuilder<?, ?> nativeConfigurationBuilder) {
        log.trace("Adding native configuration builder: {}", nativeConfigurationBuilder.getClass().getName());
        this.nativeConfigurationBuilder
                .add(Objects.requireNonNull(nativeConfigurationBuilder, "Native configuration builder cannot be null"));
        return this;
    }

    @Override
    public INativeConfigurationBuilder mode(NativeConfigurationMode mode) {
        log.trace("Setting native configuration mode: {}", mode);
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        return this;
    }

    /**
     * Creates a fresh native configuration builder.
     *
     * @return a new {@link INativeConfigurationBuilder} instance
     */
    public static INativeConfigurationBuilder builder() {
        return new NativeConfigurationBuilder();
    }

    @Override
    public IReflectionConfigurationEntryBuilder reflectionEntry(IReflectionConfigurationEntry entry) {
        IReflectionConfigurationEntryBuilder builder = new ReflectConfigEntryBuilder(entry);
        this.reflectionEntries.add(builder);
        return builder;
    }

}
