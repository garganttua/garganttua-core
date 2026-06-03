package com.garganttua.core.configuration.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;

/**
 * Maps configuration <em>aliases</em> (as declared by {@code @ConfigurableBuilder} and
 * targeted by a file's shebang) to the builder classes that bear them.
 *
 * <p>Built either by scanning the classpath for {@link ConfigurableBuilder} (runtime),
 * or from an explicit map (tests). A bootstrap then resolves a file's alias to the
 * builder <em>instance</em> it is wiring whose class matches the resolved class.</p>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ConfigurableBuilderRegistry {

    private static final Logger log = Logger.getLogger(ConfigurableBuilderRegistry.class);

    private final Map<String, IClass<?>> aliasToBuilder;

    private ConfigurableBuilderRegistry(Map<String, IClass<?>> aliasToBuilder) {
        this.aliasToBuilder = aliasToBuilder;
    }

    /**
     * Builds the registry by discovering every {@link ConfigurableBuilder}-annotated
     * class via the given scanner.
     *
     * @param scanner the annotation scanner (index- or reflection-backed)
     * @return the populated registry
     */
    @SuppressWarnings("unchecked")
    public static ConfigurableBuilderRegistry scan(IAnnotationScanner scanner) {
        Map<String, IClass<?>> map = new HashMap<>();
        IClass<ConfigurableBuilder> anno =
                (IClass<ConfigurableBuilder>) (IClass<?>) IClass.getClass(ConfigurableBuilder.class);
        for (IClass<?> cls : scanner.getClassesWithAnnotation(anno)) {
            ConfigurableBuilder marker = cls.getAnnotation(anno);
            if (marker != null) {
                register(map, marker.value(), cls);
            }
        }
        log.debug("Discovered {} configurable builder alias(es)", map.size());
        return new ConfigurableBuilderRegistry(map);
    }

    /**
     * Builds a registry from an explicit alias-to-builder-class map (primarily for tests).
     *
     * @param aliasToBuilder the alias-to-builder-class mapping
     * @return the registry
     */
    public static ConfigurableBuilderRegistry of(Map<String, IClass<?>> aliasToBuilder) {
        Map<String, IClass<?>> map = new HashMap<>();
        aliasToBuilder.forEach((alias, cls) -> register(map, alias, cls));
        return new ConfigurableBuilderRegistry(map);
    }

    private static void register(Map<String, IClass<?>> map, String alias, IClass<?> cls) {
        IClass<?> previous = map.putIfAbsent(alias, cls);
        if (previous != null && !previous.equals(cls)) {
            log.warn("Duplicate @ConfigurableBuilder alias '{}' on {} and {}; keeping {}",
                    alias, previous.getName(), cls.getName(), previous.getName());
        }
    }

    /**
     * Resolves the builder class registered under an alias.
     *
     * @param alias the configuration alias (e.g. {@code injection})
     * @return the builder class, or empty if no builder declares that alias
     */
    public Optional<IClass<?>> resolve(String alias) {
        return Optional.ofNullable(aliasToBuilder.get(alias));
    }

    /** @return an immutable view of the alias-to-builder-class mapping */
    public Map<String, IClass<?>> aliases() {
        return Map.copyOf(aliasToBuilder);
    }
}
