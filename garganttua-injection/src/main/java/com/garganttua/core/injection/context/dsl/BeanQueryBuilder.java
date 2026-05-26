package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.beans.BeanQuery;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class BeanQueryBuilder<Bean> implements IBeanQueryBuilder<Bean> {
    public BeanQueryBuilder() {
    }

    private static final IDiagnostic log = Diagnostics.of(BeanQueryBuilder.class);

    private IClass<Bean> type;
    private String provider;
    private BeanStrategy strategy;
    private IClass<? extends Annotation> qualifier;
    private String name;

    @Override
    public IBeanQuery<Bean> build() throws DslException {
        log.trace("Entering build() method");

        Set<IClass<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
            log.debug("Qualifier added: {}", this.qualifier.getSimpleName());
        }

        log.debug("Building BeanQuery for type: {}, provider: {}, strategy: {}, qualifier: {}, name: {}",
                getTypeSimpleName(), provider, strategy, qualifier, name);

        IBeanQuery<Bean> query;
        try {
            query = new BeanQuery<>(
                    Optional.ofNullable(this.provider),
                    new BeanReference<>(
                            this.type,
                            Optional.ofNullable(this.strategy),
                            Optional.ofNullable(this.name),
                            qualifiers));
            log.debug("BeanQuery successfully built for type: {}", getTypeSimpleName());
        } catch (Exception e) {
            log.error("Failed to build BeanQuery for type: {}. Error: {}", getTypeSimpleName(),
                    e.getMessage());
            throw new DslException("Error building BeanQuery", e);
        }

        log.trace("Exiting build() method");
        return query;
    }

    private String getTypeSimpleName() {
        return type==null?"":type.getSimpleName();
    }

    @Override
    public IBeanQueryBuilder<Bean> type(IClass<Bean> type) {
        log.trace("Entering type() method with parameter: {}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("Type set to: {}", getTypeSimpleName());
        log.trace("Exiting type() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> name(String name) {
        log.trace("Entering name() method with parameter: {}", name);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        log.debug("Name set to: {}", this.name);
        log.trace("Exiting name() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> qualifier(IClass<? extends Annotation> qualifier) throws DiException {
        log.trace("Entering qualifier() method with parameter: {}", qualifier);
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        log.debug("Qualifier set to: {}", this.qualifier.getSimpleName());
        log.trace("Exiting qualifier() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy) {
        log.trace("Entering strategy() method with parameter: {}", strategy);
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        log.debug("Strategy set to: {}", this.strategy);
        log.trace("Exiting strategy() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> provider(String provider) {
        log.trace("Entering provider() method with parameter: {}", provider);
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.debug("Provider set to: {}", this.provider);
        log.trace("Exiting provider() method");
        return this;
    }
}
