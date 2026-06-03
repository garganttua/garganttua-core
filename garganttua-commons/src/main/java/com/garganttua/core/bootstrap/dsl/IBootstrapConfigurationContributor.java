package com.garganttua.core.bootstrap.dsl;

import java.util.List;

import com.garganttua.core.dsl.IBuilder;

/**
 * SPI hook letting an optional module contribute configuration to the builders being
 * bootstrapped, during the {@code CONFIGURATION} stage (before any builder is built).
 *
 * <p>
 * Implementations are discovered by {@code Bootstrap} via {@link java.util.ServiceLoader}
 * (declared in {@code META-INF/services/com.garganttua.core.bootstrap.dsl.IBootstrapConfigurationContributor}).
 * Because discovery is classpath-driven, a contributor is active only when its module is
 * present — i.e. it is an <em>optional</em> participant, exactly the contract wanted for
 * {@code garganttua-configuration}: when present it discovers configuration files and
 * applies them to the matching {@code @ConfigurableBuilder} instances; when absent the
 * bootstrap behaves as before.
 * </p>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IBootstrapConfigurationContributor {

    /**
     * Contributes configuration to the given builders. Invoked once per Bootstrap build,
     * during the CONFIGURATION stage, with every builder known to the bootstrap. Must be
     * side-effect idempotent and must not throw for builders it does not handle.
     *
     * @param builders all builders registered in the bootstrap (mutable, not yet built)
     */
    void contribute(List<IBuilder<?>> builders);
}
