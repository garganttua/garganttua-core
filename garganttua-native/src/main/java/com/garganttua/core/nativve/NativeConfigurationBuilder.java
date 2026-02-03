package com.garganttua.core.nativve;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bootstrap
public class NativeConfigurationBuilder
        extends AbstractAutomaticBuilder<INativeConfigurationBuilder, INativeConfiguration>
        implements INativeConfigurationBuilder {

    private Set<IReflectionConfigurationEntryBuilder> reflectionEntries = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
    private NativeConfigurationMode mode = NativeConfigurationMode.override;
    private final Set<String> resources = new HashSet<>();
    private String resourcesPath = null;
    private String reflectionPath = null;
    private Set<INativeBuilder<?,?>> nativeConfigurationBuilder = new HashSet<>();

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
        this.detectNativeElements();
        this.detectNativeConfigurationBuilders();
        log.atTrace().log("Completed auto-detection for native configuration");
    }

    private void detectNativeConfigurationBuilders() {
        log.atTrace().log("Detecting native configuration builders in packages");
        this.packages.forEach(
                p -> {
                    log.atDebug().log("Scanning package for @NativeConfigurationBuilder: {}", p);
                    ObjectReflectionHelper.getClassesWithAnnotation(p,
                        com.garganttua.core.nativve.annotations.NativeConfigurationBuilder.class).forEach(c -> {
                            if (INativeBuilder.class.isAssignableFrom(c)) {
                                log.atDebug().log("Found native configuration builder: {}", c.getName());
                                INativeBuilder<?, ?> nativeBuilder = (INativeBuilder<?, ?>) ObjectReflectionHelper
                                        .instanciateNewObject(c);
                                nativeBuilder.withPackages(getPackages());
                                INativeReflectionConfiguration nativeConfiguration = nativeBuilder.build();
                                reflectionEntries.addAll(nativeConfiguration.nativeConfiguration());
                                log.atDebug().log("Loaded native configuration from builder: {}", c.getName());
                            }
                        });
                });

    }

    private void detectNativeElements() {
        log.atTrace().log("Detecting @Native annotated elements in packages");
        this.packages.forEach(
                p -> {
                    log.atDebug().log("Scanning package for @Native annotations: {}", p);
                    ObjectReflectionHelper.getClassesWithAnnotation(p, Native.class).forEach(c -> {
                        if (c.isAnnotation()) {
                            log.atDebug().log("Found @Native annotation type: {}", c.getName());
                            reflectionEntries.add(new ReflectConfigEntryBuilder(c).allPublicClasses(true)
                                    .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                                    .queryAllDeclaredConstructors(true));
                        } else {
                            log.atDebug().log("Found @Native class: {}", c.getName());
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
    public IReflectionConfigurationEntryBuilder reflectionEntry(Class<?> clazz) {
        log.atTrace().log("Adding reflection entry for class: {}", clazz.getName());
        IReflectionConfigurationEntryBuilder entry = new ReflectConfigEntryBuilder(clazz);
        this.reflectionEntries.add(entry);
        log.atDebug().log("Added reflection entry: {}", clazz.getName());
        return entry;
    }

    @Override
    public INativeConfigurationBuilder resource(Class<?> resource) {
        log.atTrace().log("Adding resource for class: {}", resource.getName());
        this.resource(Objects.requireNonNull(resource, "ressource cannot be null").getName().replace('.', '/') + ".class");
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
    public INativeConfigurationBuilder configurationBuilder(INativeBuilder<?,?> nativeConfigurationBuilder) {
        log.atTrace().log("Adding native configuration builder: {}", nativeConfigurationBuilder.getClass().getName());
        this.nativeConfigurationBuilder.add(Objects.requireNonNull(nativeConfigurationBuilder, "Native configuration builder cannot be null"));
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
