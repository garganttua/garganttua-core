package com.garganttua.core.configuration;

import com.garganttua.core.dsl.IBuilder;

public interface IConfigurationPopulator {

    <B extends IBuilder<?>> B populate(B builder, IConfigurationNode node) throws ConfigurationException;

    <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source) throws ConfigurationException;

    <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source, IConfigurationFormat format) throws ConfigurationException;
}
