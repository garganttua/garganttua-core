package com.garganttua.core.configuration.dsl;

import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationPopulator;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;

public interface IConfigurationBuilder extends IAutomaticBuilder<IConfigurationBuilder, IConfigurationPopulator>, IDependentBuilder<IConfigurationBuilder, IConfigurationPopulator> {

    IConfigurationSourceBuilder source();

    IConfigurationBuilder withFormat(IConfigurationFormat format);

    IConfigurationBuilder withMappingStrategy(String strategy);

    IConfigurationBuilder strict(boolean strict) throws DslException;
}
