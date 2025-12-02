package com.garganttua.core.nativve;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.atTrace().log("Entering withPackages(packageNames={})", (Object) packageNames);
        this.packages.addAll(Set.of(packageNames));
        log.atInfo().log("Added packages: {}", Arrays.toString(packageNames));
        log.atTrace().log("Exiting withPackages");
        return this;
    }

    @Override
    public INativeConfigurationBuilder withPackage(String packageName) {
        log.atTrace().log("Entering withPackage(packageName={})", packageName);
        this.packages.add(packageName);
        log.atInfo().log("Added package: {}", packageName);
        log.atTrace().log("Exiting withPackage");
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected INativeConfiguration doBuild() throws DslException {

        Objects.requireNonNull(this.reflectionPath, "Reflection path cannot be null");
        Objects.requireNonNull(this.resourcesPath, "Resouces path cannot be null");




        return null;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        this.detectNativeElements();
        this.detectNativeConfigurationBuilders();
    }

    private void detectNativeConfigurationBuilders() {
        this.packages.forEach(
                p -> ObjectReflectionHelper.getClassesWithAnnotation(p,
                        com.garganttua.core.nativve.annotations.NativeConfigurationBuilder.class).forEach(c -> {
                            if (INativeBuilder.class.isAssignableFrom(c)) {
                                INativeBuilder<?, ?> nativeBuilder = (INativeBuilder<?, ?>) ObjectReflectionHelper
                                        .instanciateNewObject(c);
                                nativeBuilder.withPackages(getPackages());
                                INativeConfiguration nativeConfiguration = nativeBuilder.build();
                                reflectionEntries.addAll(nativeConfiguration.nativeConfiguration());
                            }
                        }));

    }

    private void detectNativeElements() {
        this.packages.forEach(
                p -> ObjectReflectionHelper.getClassesWithAnnotation(p, Native.class).forEach(c -> {
                    if (c.isAnnotation()) {
                        reflectionEntries.add(new ReflectConfigEntryBuilder(c).allPublicClasses(true)
                                .allDeclaredClasses(true).allDeclaredFields(true).queryAllDeclaredMethods(true)
                                .queryAllDeclaredConstructors(true));
                    } else {
                        reflectionEntries.add(new ReflectConfigEntryBuilder(c).autoDetect(true));
                    }
                }));
    }

    @Override
    public INativeConfigurationBuilder resourcesPath(String path) {
        this.resourcesPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public INativeConfigurationBuilder reflectionPath(String path) {
        this.reflectionPath = Objects.requireNonNull(path, "Path cannot be null");
        return this;
    }

    @Override
    public IReflectionConfigurationEntryBuilder reflectionEntry(Class<?> clazz) {
        IReflectionConfigurationEntryBuilder entry = new ReflectConfigEntryBuilder(clazz);
        this.reflectionEntries.add(entry);
        return entry;
    }

    @Override
    public INativeConfigurationBuilder resource(Class<?> resource) {
        this.resources.add(Objects.requireNonNull(resource, "ressource cannot be null").getName().replace('.', '/') + ".class");
        return this;
    }

    @Override
    public INativeConfigurationBuilder resource(String resource) {
        this.resources.add("\\Q" + Objects.requireNonNull(resource, "ressource cannot be null") + "\\E");
        return this;
    }

    @Override
    public INativeConfigurationBuilder configurationBuilder(INativeBuilder<?,?> nativeConfigurationBuilder) {
        this.nativeConfigurationBuilder.add(Objects.requireNonNull(nativeConfigurationBuilder, "Native configuration builder cannot be null"));
        return this;
    }

    @Override
    public INativeConfigurationBuilder mode(NativeConfigurationMode mode) {
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
        return this;
    }

}
