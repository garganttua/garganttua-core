package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.beans.BeanSupplier;
import com.garganttua.core.injection.context.beans.ContextualBeanSupplier;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for an {@link IBeanSupplier}, capturing the bean reference criteria and whether
 * the supplier resolves against the static {@code InjectionContext} or a contextual one.
 *
 * @param <Bean> the type of bean supplied
 */
@Reflected
public class BeanSupplierBuilder<Bean> implements IBeanSupplierBuilder<Bean> {
    private static final Logger log = Logger.getLogger(BeanSupplierBuilder.class);

    private String name = null;
    private String provider = null;
    private IClass<Bean> type;
    private BeanStrategy strategy;
    private IClass<? extends Annotation> qualifier;
    private boolean useStaticContext = true;

    /**
     * Creates a bean supplier builder for the given bean type.
     *
     * @param type the bean type; must not be {@code null}
     */
    public BeanSupplierBuilder(IClass<Bean> type) {
        log.trace("Entering BeanSupplierBuilder constructor with type: {}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("Type set to: {}", this.type.getSimpleName());
        log.trace("Exiting BeanSupplierBuilder constructor");
    }

    /**
     * Creates a bean supplier builder from a bean reference, optionally overriding the provider scope.
     *
     * @param provider the optional provider scope; when present its value overrides the reference's provider
     * @param query    the bean reference to initialise from; must not be {@code null}
     */
    public BeanSupplierBuilder(Optional<String> provider, BeanReference<Bean> query) {
        log.trace("Entering BeanSupplierBuilder constructor with Optional provider: {} and query: {}",
                provider, query);
        Objects.requireNonNull(query, "Query cannot be null");
        log.debug("Query provided: {}", query);

        if (provider != null && provider.isPresent()) {
            initFromQuery(query);
            this.provider = provider.get();
            log.debug("Provider set from Optional: {}", this.provider);
        } else {
            initFromQuery(query);
        }
        log.trace("Exiting BeanSupplierBuilder constructor");
    }

    /**
     * Creates a bean supplier builder from a bean reference.
     *
     * @param query the bean reference to initialise from; must not be {@code null}
     */
    public BeanSupplierBuilder(BeanReference<Bean> query) {
        log.trace("Entering BeanSupplierBuilder constructor with query: {}", query);
        Objects.requireNonNull(query, "query cannot be null");
        initFromQuery(query);
        log.trace("Exiting BeanSupplierBuilder constructor");
    }

    /**
     * Creates a bean supplier builder from a bean reference with an explicit provider scope.
     *
     * @param provider the provider scope; must not be {@code null}
     * @param query    the bean reference to initialise from; must not be {@code null}
     */
    public BeanSupplierBuilder(String provider, BeanReference<Bean> query) {
        log.trace("Entering BeanSupplierBuilder constructor with provider: {} and query: {}", provider,
                query);
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "query cannot be null");

        initFromQuery(query);
        this.provider = provider;
        log.debug("Provider set to: {}", this.provider);
        log.trace("Exiting BeanSupplierBuilder constructor");
    }

    private void initFromQuery(BeanReference<Bean> query) {
        log.trace("Initializing BeanSupplierBuilder from query: {}", query);
        this.type = query.type();
        this.name = query.name().orElse(null);
        this.strategy = query.strategy().orElse(BeanStrategy.singleton);
        if (!query.qualifiers().isEmpty()) {
            this.qualifier = query.qualifiers().iterator().next();
            log.debug("Qualifier set from query: {}", this.qualifier.getSimpleName());
        }
        log.debug("Initialization from query complete. Type: {}, Name: {}, Strategy: {}, Qualifier: {}",
                this.type.getSimpleName(), this.name, this.strategy, this.qualifier);
        log.trace("Exiting initFromQuery method");
    }

    /** {@return the reflective {@link Type} of the bean to be supplied} */
    @Override
    public Type getSuppliedType() {
        log.trace("Entering getSuppliedType() method");
        log.trace("Exiting getSuppliedType() method with type: {}", this.type);
        return type.getType();
    }

    /** {@return the {@link IClass} of the bean to be supplied} */
    @Override
    public IClass<Bean> getSuppliedClass() {
        return this.type;
    }

    /**
     * Builds the bean supplier, choosing a static or contextual implementation based on
     * {@link #useStaticContext(boolean)}.
     *
     * @return the built bean supplier
     * @throws DslException if the bean type was not set
     */
    @Override
    public IBeanSupplier<Bean> build() throws DslException {
        log.trace("Entering build() method");
        if (type == null) {
            log.error("Bean type must be provided before build()");
            throw new DslException("Bean type must be provided");
        }

        Set<IClass<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
            log.debug("Added qualifier to build: {}", this.qualifier.getSimpleName());
        }

        IBeanSupplier<Bean> supplier = null;

        if( this.useStaticContext )
            supplier = new BeanSupplier<>(Optional.ofNullable(this.provider),
                new BeanReference<>(this.type, Optional.ofNullable(this.strategy),
                        Optional.ofNullable(this.name), qualifiers));
        else
            supplier = new ContextualBeanSupplier<>(Optional.ofNullable(this.provider),
                new BeanReference<>(this.type, Optional.ofNullable(this.strategy),
                        Optional.ofNullable(this.name), qualifiers));

        log.debug("BeanSupplier built successfully for type: {}, provider: {}, name: {}",
                this.type.getSimpleName(), this.provider, this.name);
        log.trace("Exiting build() method");
        return supplier;
    }

    /**
     * Sets the name constraint for the supplied bean.
     *
     * @param name the bean name; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IBeanSupplierBuilder<Bean> name(String name) {
        log.trace("Entering name() method with name: {}", name);
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        log.debug("Name set to: {}", this.name);
        log.trace("Exiting name() method");
        return this;
    }

    /**
     * Sets the provider scope from which the bean is resolved.
     *
     * @param provider the provider scope; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IBeanSupplierBuilder<Bean> provider(String provider) {
        log.trace("Entering provider() method with provider: {}", provider);
        this.provider = Objects.requireNonNull(provider, "Bean provider cannot be null");
        log.debug("Provider set to: {}", this.provider);
        log.trace("Exiting provider() method");
        return this;
    }

    /**
     * Sets the instantiation strategy for the supplied bean.
     *
     * @param strategy the bean strategy; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy) {
        log.trace("Entering strategy() method with strategy: {}", strategy);
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        log.debug("Strategy set to: {}", this.strategy);
        log.trace("Exiting strategy() method");
        return this;
    }

    /**
     * Sets the qualifier annotation constraint for the supplied bean.
     *
     * @param qualifier the qualifier annotation class; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IBeanSupplierBuilder<Bean> qualifier(IClass<? extends Annotation> qualifier) {
        log.trace("Entering qualifier() method with qualifier: {}", qualifier);
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        log.debug("Qualifier set to: {}", this.qualifier.getSimpleName());
        log.trace("Exiting qualifier() method");
        return this;
    }

    /** {@return an empty set, as a bean supplier declares no build-time dependencies} */
    @Override
    public Set<IClass<?>> dependencies() {
        log.trace("Entering getDependencies() method");
        log.trace("Exiting getDependencies() method with empty set");
        return Set.of();
    }

    /** {@return {@code true} if the built supplier resolves against a contextual (non-static) context} */
    @Override
    public boolean isContextual() {
        log.trace("Entering isContextual() method");
        log.trace("Exiting isContextual() method with result: {}", !this.useStaticContext);
        return !this.useStaticContext;
    }

    /**
     * Controls whether the built supplier resolves against the static {@code InjectionContext}
     * ({@code true}) or a contextual one ({@code false}).
     *
     * @param useStaticContext {@code true} to use the static context
     * @return this builder for chaining
     */
    @Override
    public IBeanSupplierBuilder<Bean> useStaticContext(boolean useStaticContext) {
        this.useStaticContext = useStaticContext;
        return this;
    }
}
