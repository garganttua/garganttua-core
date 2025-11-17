package com.garganttua.core.injection.context.resolver;

import static com.garganttua.core.injection.IInjectableElementResolver.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;

public class InjectableElementResolver implements IInjectableElementResolver {

    private Map<Class<? extends Annotation>, IElementResolver> resolvers;

    public InjectableElementResolver(Map<Class<? extends Annotation>, IElementResolver> resolvers){
        this.resolvers = Objects.requireNonNull(resolvers, "Resolvers map cannot be null");
    }

    @Override
    public Resolved resolve(Class<?> elementType,
            AnnotatedElement element) throws DiException {

        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            IElementResolver resolver = this.resolvers.get(type);
            if (resolver != null) {
                return resolver.resolve(elementType, element);
            }
        }

        return new Resolved(false, elementType, null, isNullable(element));
    }

    @Override
    public Set<Resolved> resolve(Executable executable) throws DiException {
        Set<Resolved> paramResolved = new LinkedHashSet<>();
        for( Parameter parameter: executable.getParameters() ){
            paramResolved.add(resolve(parameter.getType(), parameter));
        }
        return paramResolved;
    }
}
