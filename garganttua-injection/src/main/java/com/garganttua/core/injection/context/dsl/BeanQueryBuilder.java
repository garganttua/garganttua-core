package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.beans.BeanQuery;
import com.garganttua.core.reflection.IClass;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class BeanQueryBuilder<Bean> implements IBeanQueryBuilder<Bean> {

    private IClass<Bean> type;
    private String provider;
    private BeanStrategy strategy;
    private IClass<? extends Annotation> qualifier;
    private String name;

    @Override
    public IBeanQuery<Bean> build() throws DslException {
        log.atTrace().log("Entering build() method");

        Set<IClass<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
            log.atDebug().log("Qualifier added: {}", this.qualifier.getSimpleName());
        }

        log.atDebug().log("Building BeanQuery for type: {}, provider: {}, strategy: {}, qualifier: {}, name: {}",
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
            log.atDebug().log("BeanQuery successfully built for type: {}", getTypeSimpleName());
        } catch (Exception e) {
            log.atError().log("Failed to build BeanQuery for type: {}. Error: {}", getTypeSimpleName(),
                    e.getMessage());
            throw new DslException("Error building BeanQuery", e);
        }

        log.atTrace().log("Exiting build() method");
        return query;
    }

    private String getTypeSimpleName() {
        return type==null?"":type.getSimpleName();
    }

    @Override
    public IBeanQueryBuilder<Bean> type(IClass<Bean> type) {
        log.atTrace().log("Entering type() method with parameter: {}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.atDebug().log("Type set to: {}", getTypeSimpleName());
        log.atTrace().log("Exiting type() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> name(String name) {
        log.atTrace().log("Entering name() method with parameter: {}", name);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        log.atDebug().log("Name set to: {}", this.name);
        log.atTrace().log("Exiting name() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> qualifier(IClass<? extends Annotation> qualifier) throws DiException {
        log.atTrace().log("Entering qualifier() method with parameter: {}", qualifier);
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        log.atDebug().log("Qualifier set to: {}", this.qualifier.getSimpleName());
        log.atTrace().log("Exiting qualifier() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy) {
        log.atTrace().log("Entering strategy() method with parameter: {}", strategy);
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        log.atDebug().log("Strategy set to: {}", this.strategy);
        log.atTrace().log("Exiting strategy() method");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> provider(String provider) {
        log.atTrace().log("Entering provider() method with parameter: {}", provider);
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.atDebug().log("Provider set to: {}", this.provider);
        log.atTrace().log("Exiting provider() method");
        return this;
    }
}
