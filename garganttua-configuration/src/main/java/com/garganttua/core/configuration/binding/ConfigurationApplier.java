package com.garganttua.core.configuration.binding;

import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.observability.Logger;

/**
 * Applies a configuration source to a DSL builder instance: maps the file's nested
 * keys onto the builder's methods (scalars and nested child-builders) via the
 * {@link BuilderPopulator}. The reserved {@code $module} shebang key is ignored
 * during population.
 *
 * <p>This is the integration seam used both by the manual API (resolve the builder,
 * then {@code apply}) and by the bootstrap auto-wiring (which resolves the target
 * builder from the file's shebang at the {@code CONFIGURATION} stage).</p>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ConfigurationApplier {

    private static final Logger log = Logger.getLogger(ConfigurationApplier.class);

    private final BuilderPopulator populator;

    /**
     * @param populator the populator to use — typically obtained from a built
     *                  {@code ConfigurationBuilder} so the registered (classpath-conditional)
     *                  formats, mapping strategy and strict mode are honoured
     */
    public ConfigurationApplier(BuilderPopulator populator) {
        this.populator = populator;
    }

    /**
     * Populates {@code builder} from {@code source}.
     *
     * @param <B>     the builder type
     * @param builder the builder instance to configure
     * @param source  the configuration source
     * @return the same builder, configured
     * @throws ConfigurationException if the source cannot be read or applied
     */
    public <B extends IBuilder<?>> B apply(B builder, IConfigurationSource source) throws ConfigurationException {
        Optional<String> alias = ConfigurationShebang.extract(source);
        log.debug("Applying configuration [{}]{} to {}", source.getDescription(),
                alias.map(a -> " (#!" + a + ")").orElse(""), builder.getClass().getSimpleName());
        return populator.populate(builder, source);
    }

    /**
     * @param source the configuration source
     * @return the target-builder alias declared by the source, if any
     * @throws ConfigurationException if the source cannot be read
     */
    public Optional<String> targetAlias(IConfigurationSource source) throws ConfigurationException {
        return ConfigurationShebang.extract(source);
    }
}
