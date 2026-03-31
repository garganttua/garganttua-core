package com.garganttua.core.nativve;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.IReflectionUsageReporter;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.reflection.annotations.ReflectedBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bootstrap
public class NativeConfigurationBuilder
        extends AbstractAutomaticDependentBuilder<INativeConfigurationBuilder, INativeConfiguration>
        implements INativeConfigurationBuilder {

    private Set<IReflectionConfigurationEntryBuilder> reflectionEntries = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
    private NativeConfigurationMode mode = NativeConfigurationMode.override;
    private final Set<String> resources = new HashSet<>();
    private String resourcesPath = null;
    private String reflectionPath = null;
    private Set<INativeBuilder<?, ?>> nativeConfigurationBuilder = new HashSet<>();

    public NativeConfigurationBuilder() {
        super(Set.of(DependencySpec.use(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BOTH)));
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.atTrace().log("Starting auto-detection for native configuration with dependency "
                + dependency.getClass().getSimpleName());
        if (dependency instanceof IReflection reflection) {
            this.detectNativeElements(reflection);
            this.detectNativeConfigurationBuilders(reflection);
        }
        log.atTrace().log("Completed auto-detection for native configuration with Dependency "
                + dependency.getClass().getSimpleName());
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
        log.atTrace().log("Adding {} packages to native configuration", packageNames.length);
        this.packages.addAll(Set.of(packageNames));
        log.atDebug().log("Added packages: {}", String.join(", ", packageNames));
        return this;
    }

    @Override
    public INativeConfigurationBuilder withPackage(String packageName) {
        log.atTrace().log("Adding package to native configuration: {}", packageName);
        this.packages.add(packageName);
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected INativeConfiguration doBuild() throws DslException {
        log.atTrace().log("Building native configuration");
        Objects.requireNonNull(this.reflectionPath, "Reflection path cannot be null");
        Objects.requireNonNull(this.resourcesPath, "Resouces path cannot be null");

        log.atDebug().log("Building native configuration with {} reflection entries and {} resources",
                reflectionEntries.size(), resources.size());
        INativeConfiguration config = new NativeConfiguration(
                this.mode,
                this.reflectionEntries.stream().map(e -> e.build()).collect(Collectors.toSet()),
                this.resources,
                this.resourcesPath,
                this.reflectionPath);
        log.atDebug().log("Native configuration built successfully");
        return config;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Starting auto-detection for native configuration");
        log.atTrace().log("Completed auto-detection for native configuration");
    }

    private void detectNativeConfigurationBuilders(IReflection reflection) {
        log.atTrace().log("Detecting reflection usage reporters in packages");
        IClass<ReflectedBuilder> reflectedBuilderAnnotation = reflection
                .getClass(ReflectedBuilder.class);
        IClass<INativeBuilder> nativeBuilderInterface = reflection.getClass(INativeBuilder.class);
        this.packages.forEach(
                p -> {
                    log.atDebug().log("Scanning package for @ReflectedBuilder: {}", p);
                    reflection.getClassesWithAnnotation(p, reflectedBuilderAnnotation).forEach(c -> {
                        if (nativeBuilderInterface.isAssignableFrom(c)) {
                            log.atDebug().log("Found reflection usage reporter: {}", c.getName());
                            try {
                                INativeBuilder<?, ?> nativeBuilder = (INativeBuilder<?, ?>) reflection.newInstance(c);
                                nativeBuilder.withPackages(getPackages());
                                IReflectionUsageReporter reporter = nativeBuilder.build();
                                reflectionEntries.addAll(reporter.reflectionUsage());
                                log.atDebug().log("Loaded reflection usage from reporter: {}", c.getName());
                            } catch (ReflectionException e) {
                                log.atError().log("Failed to instantiate reflection usage reporter: {}",
                                        c.getName());
                            }
                        }
                    });
                });
    }

    private void detectNativeElements(IReflection reflection) {
        log.atTrace().log("Detecting @Reflected annotated elements in packages");
        IClass<Reflected> reflectedAnnotation = reflection.getClass(Reflected.class);
        this.packages.forEach(
                p -> {
                    log.atDebug().log("Scanning package for @Reflected annotations: {}", p);
                    reflection.getClassesWithAnnotation(p, reflectedAnnotation).forEach(c -> {
                        if (c.isAnnotation()) {
                            log.atDebug().log("Found @Reflected annotation type: {}", c.getName());
                            reflectionEntries.add(new ReflectConfigEntryBuilder(c).allPublicClasses(true)
                                    .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                                    .queryAllDeclaredConstructors(true));
                        } else {
                            log.atDebug().log("Found @Reflected class: {}", c.getName());
                            reflectionEntries.add(new ReflectConfigEntryBuilder(c).autoDetect(true));
                        }
                    });
                });
    }

    @Override
    public INativeConfigurationBuilder resourcesPath(String path) {
        log.atTrace().log("Setting resources path: {}", path);
        this.resourcesPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public INativeConfigurationBuilder reflectionPath(String path) {
        log.atTrace().log("Setting reflection path: {}", path);
        this.reflectionPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public IReflectionConfigurationEntryBuilder reflectionEntry(IClass<?> clazz) {
        log.atTrace().log("Adding reflection entry for class: {}", clazz.getName());
        IReflectionConfigurationEntryBuilder entry = new ReflectConfigEntryBuilder(clazz);
        this.reflectionEntries.add(entry);
        log.atDebug().log("Added reflection entry: {}", clazz.getName());
        return entry;
    }

    @Override
    public INativeConfigurationBuilder resource(IClass<?> resource) {
        log.atTrace().log("Adding resource for class: {}", resource.getName());
        this.resource(
                Objects.requireNonNull(resource, "ressource cannot be null").getName().replace('.', '/') + ".class");
        return this;
    }

    @Override
    public INativeConfigurationBuilder resource(String resource) {
        log.atTrace().log("Adding resource: {}", resource);
        this.resources.add("\\Q" + Objects.requireNonNull(resource, "ressource cannot be null") + "\\E");
        log.atDebug().log("Added resource to native configuration: {}", resource);
        return this;
    }

    @Override
    public INativeConfigurationBuilder configurationBuilder(INativeBuilder<?, ?> nativeConfigurationBuilder) {
        log.atTrace().log("Adding native configuration builder: {}", nativeConfigurationBuilder.getClass().getName());
        this.nativeConfigurationBuilder
                .add(Objects.requireNonNull(nativeConfigurationBuilder, "Native configuration builder cannot be null"));
        return this;
    }

    @Override
    public INativeConfigurationBuilder mode(NativeConfigurationMode mode) {
        log.atTrace().log("Setting native configuration mode: {}", mode);
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        return this;
    }

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
