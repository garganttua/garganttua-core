package com.garganttua.core.nativve;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;

public interface INativeConfigurationBuilder extends IPackageableBuilder<INativeConfigurationBuilder, INativeConfiguration>, IAutomaticBuilder<INativeConfigurationBuilder, INativeConfiguration> {

    INativeConfigurationBuilder resourcesPath(String path);

    INativeConfigurationBuilder reflectionPath(String path);

    IReflectionConfigurationEntryBuilder reflectionEntry(Class<?> clazz);

    INativeConfigurationBuilder configurationBuilder(INativeBuilder<?,?> nativeConfigurationBuilder);

    INativeConfigurationBuilder resource(String resourcePath);

    INativeConfigurationBuilder resource(Class<?> resource);

    INativeConfigurationBuilder mode(NativeConfigurationMode mode);

}
