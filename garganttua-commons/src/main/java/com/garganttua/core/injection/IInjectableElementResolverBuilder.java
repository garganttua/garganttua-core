package com.garganttua.core.injection;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;

public interface IInjectableElementResolverBuilder extends ILinkedBuilder<IDiContextBuilder, IInjectableElementResolver>{

    IInjectableElementResolverBuilder withResolver(Class<? extends Annotation> annotation, IElementResolver resolver);

}
