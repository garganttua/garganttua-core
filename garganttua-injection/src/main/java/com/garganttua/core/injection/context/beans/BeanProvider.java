package com.garganttua.core.injection.context.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.context.dsl.BeanFactoryBuilder;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.injection.context.validation.DependencyCycleDetector;
import com.garganttua.core.injection.context.validation.DependencyGraph;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.utils.CopyException;

/**
 * {@link IBeanProvider} backed by a list of {@link IBeanFactory} instances. Resolves
 * beans by type or {@link BeanReference} query, performs dependency-cycle detection at
 * init time, and optionally supports runtime bean registration when {@code mutable}.
 */
public class BeanProvider extends AbstractLifecycle implements IBeanProvider {
    private static final Logger log = Logger.getLogger(BeanProvider.class);

	private List<IBeanFactory<?>> beanFactories;
	private final Object copyMutex = new Object();
	private boolean mutable = true;
	private IInjectableElementResolverBuilder resolverBuilder = null;

	/**
	 * Builds an immutable provider over the supplied factories.
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories) {
		this(beanFactories, Optional.empty(), false);
	}

	/**
	 * Builds a provider over the supplied factories.
	 *
	 * @param mutable whether runtime bean registration is allowed
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories, boolean mutable) {
		this(beanFactories, Optional.empty(), mutable);
	}

	/**
	 * Builds an immutable provider that uses the given resolver builder for beans
	 * registered at runtime.
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories, IInjectableElementResolverBuilder resolverBuilder) {
		this(beanFactories, resolverBuilder, false);
	}

	/**
	 * Builds an immutable provider, accepting an optional resolver builder.
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories, Optional<IInjectableElementResolverBuilder> resolverBuilder) {
		this(beanFactories, resolverBuilder.orElse(null), false);
	}

	/**
	 * Builds a provider, accepting an optional resolver builder.
	 *
	 * @param mutable whether runtime bean registration is allowed
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories, Optional<IInjectableElementResolverBuilder> resolverBuilder,
			boolean mutable) {
		this(beanFactories, resolverBuilder.orElse(null), mutable);
	}

	/**
	 * Builds a provider over the supplied factories.
	 *
	 * @param resolverBuilder resolver builder used to bind beans added at runtime, may be {@code null}
	 * @param mutable whether runtime bean registration is allowed
	 */
	public BeanProvider(List<IBeanFactory<?>> beanFactories, IInjectableElementResolverBuilder resolverBuilder, boolean mutable) {
		log.trace("Entering BeanProvider constructor with beanFactories: {}", beanFactories);
		this.mutable = mutable;
		this.resolverBuilder = resolverBuilder;
		this.beanFactories = Collections
				.synchronizedList(Objects.requireNonNull(beanFactories, "Bean factories cannot be null"));
		log.debug("BeanProvider initialized with {} bean factories", beanFactories.size());
		log.trace("Exiting BeanProvider constructor");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(IClass<T> type) throws DiException {
		log.trace("Entering getBean with type: {}", type);
		wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> type.isAssignableFrom(factory.getSuppliedClass()))
				.findFirst();

		if (factoryOpt.isPresent()) {
			try {
				Optional<T> result = (Optional<T>) factoryOpt.get().supply();
				log.debug("Bean found for type {}: {}", type, result.orElse(null));
				return result;
			} catch (SupplyException e) {
				log.error("Failed to supply bean for type {}: {}", type, e.getMessage());
				throw new DiException(e);
			}
		}

		log.warn("No bean found for type {}", type);
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> get(String name, IClass<T> type) throws DiException {
		log.trace("getBean by name '{}' and type {} is not implemented", name, type);
		wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
		throw new UnsupportedOperationException("Unimplemented method 'getBean'");
	}

	@Override
	public boolean isMutable() {
		log.trace("Checking if BeanProvider is mutable: {}", this.mutable);
		return this.mutable;
	}

	@Override
	public <T> List<T> get(IClass<T> interfasse, boolean includePrototypes) {
		log.trace("Getting beans implementing interface: {}", interfasse);
		List<T> result = this.beanFactories.stream()
				.filter(factory -> interfasse.isAssignableFrom(factory.getSuppliedClass()))
				.map(factory -> {
					try {
						return factory.supply().orElse(null);
					} catch (Exception e) {
						log.error("Failed to supply bean from factory {}: {}", factory, e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)
				.map(interfasse::cast)
				.collect(Collectors.toList());
		log.debug("Beans implementing interface {} found: {}", interfasse, result.size());
		return result;
	}

	@Override
	public IReflection reflection() {
		return IClass.getReflection();
	}

	@Override
	protected ILifecycle doInit() throws LifecycleException {
		log.trace("Initializing BeanProvider");
		try {
			this.doDependencyCycleDetection();
		} catch (DiException e) {
			log.error("Dependency cycle detected during init: {}", e.getMessage());
			throw new LifecycleException(e);
		}
		log.trace("BeanProvider initialized");
		return this;
	}

	private void doDependencyCycleDetection() throws DiException {
		log.trace("Performing dependency cycle detection");
		DependencyGraph graph = new DependencyGraph();
		this.beanFactories.forEach(builder -> builder.dependencies()
				.forEach(dep -> graph.addDependency(builder.getSuppliedClass(), dep)));
		new DependencyCycleDetector().detectCycles(graph);
		log.debug("Dependency cycle detection completed");
	}

	@Override
	protected ILifecycle doStart() throws LifecycleException {
		log.trace("Starting BeanProvider");
		return this;
	}

	@Override
	protected ILifecycle doFlush() throws LifecycleException {
		log.debug("Flushing BeanProvider: clearing bean factories");
		this.beanFactories.clear();
		return this;
	}

	@Override
	protected ILifecycle doStop() throws LifecycleException {
		log.trace("Stopping BeanProvider");
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> query(BeanReference<T> query) throws DiException {
		log.trace("Querying single bean with query: {}", query);
		wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> factory.matches(query))
				.findFirst();

		if (factoryOpt.isPresent()) {
			try {
				Optional<T> result = (Optional<T>) factoryOpt.get().supply();
				log.debug("Bean found for query {}: {}", query, result.orElse(null));
				return result;
			} catch (SupplyException e) {
				log.error("Failed to supply bean for query {}: {}", query, e.getMessage());
				throw new DiException(e);
			}
		}

		log.warn("No bean found for definition {}", query);
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> queries(BeanReference<T> query) throws DiException {
		log.trace("Querying multiple beans with query: {}", query);
		wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));

		List<T> result = (List<T>) this.beanFactories.stream()
				.filter(factory -> factory.matches(query))
				.map(ISupplier::supply)
				.map(Optional::get)
				.toList();

		log.debug("Beans found for query {}: {}", query, result.size());
		return result;
	}

	@Override
	public IBeanProvider copy() throws CopyException {
		log.trace("Copying BeanProvider");
		synchronized (this.copyMutex) {
			List<IBeanFactory<?>> copiedFactories = new ArrayList<>(this.beanFactories);
			BeanProvider copy = new BeanProvider(copiedFactories);
			log.debug("BeanProvider copy created with {} factories", copiedFactories.size());
			return copy;
		}
	}

	@Override
	public int size() {
		log.trace("Returning BeanProvider size: {}", this.beanFactories.size());
		return this.beanFactories.size();
	}

	@Override
	public Set<IReflectionConfigurationEntryBuilder> reflectionUsage() {
		log.trace("Building reflection usage from {} bean factories", this.beanFactories.size());
		Set<IReflectionConfigurationEntryBuilder> result = this.beanFactories.stream().map(f -> f.nativeEntry())
				.collect(Collectors.toSet());
		log.debug("Reflection usage built with {} entries", result.size());
		return result;
	}

	@Override
	public <T> void add(BeanReference<T> reference, T bean) throws DiException {
		this.add(reference, bean, false);
	}

	@Override
	public <T> void add(BeanReference<T> reference, Optional<T> bean, boolean autoDetect) throws DiException {
		this.add(reference, bean.orElseGet(null), autoDetect);
	}

	@Override
	public <T> void add(BeanReference<T> reference, Optional<T> bean) throws DiException {
		this.add(reference, bean.orElseGet(null), false);
	}

	@Override
	public <T> void add(BeanReference<T> reference) throws DiException {
		this.add(reference, Optional.empty(), false);
	}

	@Override
	public <T> void add(BeanReference<T> reference, boolean autoDetect) throws DiException {
		this.add(reference, Optional.empty(), autoDetect);
	}

	@Override
	public <T> void add(BeanReference<T> reference, T bean, boolean autoDetect) throws DiException {
		if (!isMutable()) {
			throw new DiException("BeanProvider is not mutable");
		}
		Objects.requireNonNull(reference, "Bean reference cannot be null");
		if (bean != null && reference.strategy().isPresent() && reference.strategy().get() != BeanStrategy.singleton) {
			throw new DiException("Only singleton strategy is supported for manual bean addition, with bean object");
		}
		if (bean == null && ((reference.strategy().isPresent() && reference.strategy().get() != BeanStrategy.prototype)
				|| !reference.strategy().isPresent())) {
			throw new DiException("Only prototype strategy is supported for manual bean addition, without object");
		}

		IBeanFactoryBuilder<T> factory = new BeanFactoryBuilder<>(reference.type())
				.provide(this.resolverBuilder)
				.qualifiers(reference.qualifiers())
				.autoDetect(autoDetect);
		reference.strategy().ifPresent(factory::strategy);
		reference.name().ifPresent(factory::name);
		if (bean != null) {
			factory.bean(bean);
		}
		this.beanFactories.add(factory.build());
	}
}