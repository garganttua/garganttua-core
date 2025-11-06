package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.context.InjectableElementResolver;

public class InjectableElementResolverBuilder
        extends AbstractLinkedBuilder<IDiContextBuilder, IInjectableElementResolver>
        implements IInjectableElementResolverBuilder {

    private final Map<Class<? extends Annotation>, IElementResolver> resolvers = new HashMap<>();

    public InjectableElementResolverBuilder(IDiContextBuilder link) {
        super(link);
    }

    @Override
    public IInjectableElementResolverBuilder withResolver(Class<? extends Annotation> annotation,
            IElementResolver resolver) {
        resolvers.put(Objects.requireNonNull(annotation, "Annotation cannot be null"),
                Objects.requireNonNull(resolver, "Resolver cannot be null"));
        return this;
    }

    @Override
    public IInjectableElementResolver build() throws DslException {
        return new InjectableElementResolver(this.resolvers);
    }

}
