package com.garganttua.core.injection.context.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.context.dsl.BeanFactoryBuilder;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.injection.context.validation.DependencyCycleDetector;
import com.garganttua.core.injection.context.validation.DependencyGraph;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.utils.CopyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanProvider extends AbstractLifecycle implements IBeanProvider {

	private List<IBeanFactory<?>> beanFactories;
	private final Object copyMutex = new Object();
	private boolean mutable = true;

	public BeanProvider(List<IBeanFactory<?>> beanFactories) {
		this(beanFactories, false);
	}

	public BeanProvider(List<IBeanFactory<?>> beanFactories, boolean mutable) {
		log.atTrace().log("Entering BeanProvider constructor with beanFactories: {}", beanFactories);
		this.mutable = mutable;
		this.beanFactories = Collections
				.synchronizedList(Objects.requireNonNull(beanFactories, "Bean factories cannot be null"));
		log.atDebug().log("BeanProvider initialized with {} bean factories", beanFactories.size());
		log.atTrace().log("Exiting BeanProvider constructor");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(Class<T> type) throws DiException {
		log.atTrace().log("Entering getBean with type: {}", type);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> type.isAssignableFrom(factory.getSuppliedClass()))
				.findFirst();

		if (factoryOpt.isPresent()) {
			try {
				Optional<T> result = (Optional<T>) factoryOpt.get().supply();
				log.atInfo().log("Bean found for type {}: {}", type, result.orElse(null));
				return result;
			} catch (SupplyException e) {
				log.atError().log("Failed to supply bean for type {}: {}", type, e.getMessage());
				throw new DiException(e);
			}
		}

		log.atWarn().log("No bean found for type {}", type);
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> get(String name, Class<T> type) throws DiException {
		log.atTrace().log("getBean by name '{}' and type {} is not implemented", name, type);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
		throw new UnsupportedOperationException("Unimplemented method 'getBean'");
	}

	@Override
	public boolean isMutable() {
		log.atTrace().log("Checking if BeanProvider is mutable: returning false");
		return this.mutable;
	}

	@Override
	public <T> List<T> get(Class<T> interfasse, boolean includePrototypes) {
		log.atTrace().log("Getting beans implementing interface: {}", interfasse);
		List<T> result = this.beanFactories.stream()
				.filter(factory -> ObjectReflectionHelper.isImplementingInterface(interfasse,
						factory.getSuppliedClass()))
				.map(factory -> {
					try {
						return factory.supply().orElse(null);
					} catch (Exception e) {
						log.atError().log("Failed to supply bean from factory {}: {}", factory, e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)
				.map(interfasse::cast)
				.collect(Collectors.toList());
		log.atInfo().log("Beans implementing interface {} found: {}", interfasse, result.size());
		return result;
	}

	@Override
	protected ILifecycle doInit() throws LifecycleException {
		log.atTrace().log("Initializing BeanProvider");
		try {
			this.doDependencyCycleDetection();
		} catch (DiException e) {
			log.atError().log("Dependency cycle detected during init: {}", e.getMessage());
			throw new LifecycleException(e);
		}
		log.atTrace().log("BeanProvider initialized");
		return this;
	}

	private void doDependencyCycleDetection() throws DiException {
		log.atTrace().log("Performing dependency cycle detection");
		DependencyGraph graph = new DependencyGraph();
		this.beanFactories.forEach(builder -> builder.dependencies()
				.forEach(dep -> graph.addDependency(builder.getSuppliedClass(), dep)));
		new DependencyCycleDetector().detectCycles(graph);
		log.atInfo().log("Dependency cycle detection completed");
	}

	@Override
	protected ILifecycle doStart() throws LifecycleException {
		log.atTrace().log("Starting BeanProvider");
		return this;
	}

	@Override
	protected ILifecycle doFlush() throws LifecycleException {
		log.atInfo().log("Flushing BeanProvider: clearing bean factories");
		this.beanFactories.clear();
		return this;
	}

	@Override
	protected ILifecycle doStop() throws LifecycleException {
		log.atTrace().log("Stopping BeanProvider");
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> query(BeanReference<T> query) throws DiException {
		log.atTrace().log("Querying single bean with query: {}", query);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> factory.matches(query))
				.findFirst();

		if (factoryOpt.isPresent()) {
			try {
				Optional<T> result = (Optional<T>) factoryOpt.get().supply();
				log.atInfo().log("Bean found for query {}: {}", query, result.orElse(null));
				return result;
			} catch (SupplyException e) {
				log.atError().log("Failed to supply bean for query {}: {}", query, e.getMessage());
				throw new DiException(e);
			}
		}

		log.atWarn().log("No bean found for definition {}", query);
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> queries(BeanReference<T> query) throws DiException {
		log.atTrace().log("Querying multiple beans with query: {}", query);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		List<T> result = (List<T>) this.beanFactories.stream()
				.filter(factory -> factory.matches(query))
				.map(ISupplier::supply)
				.map(Optional::get)
				.toList();

		log.atInfo().log("Beans found for query {}: {}", query, result.size());
		return result;
	}

	@Override
	public IBeanProvider copy() throws CopyException {
		log.atTrace().log("Copying BeanProvider");
		synchronized (this.copyMutex) {
			List<IBeanFactory<?>> copiedFactories = new ArrayList<>(this.beanFactories);
			BeanProvider copy = new BeanProvider(copiedFactories);
			log.atInfo().log("BeanProvider copy created with {} factories", copiedFactories.size());
			return copy;
		}
	}

	@Override
	public int size() {
		log.atTrace().log("Returning BeanProvider size: {}", this.beanFactories.size());
		return this.beanFactories.size();
	}

	@Override
	public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
		log.atTrace().log("Building native configuration from {} bean factories", this.beanFactories.size());
		Set<IReflectionConfigurationEntryBuilder> result = this.beanFactories.stream().map(f -> f.nativeEntry())
				.collect(Collectors.toSet());
		log.atDebug().log("Native configuration built with {} entries", result.size());
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

		IBeanFactoryBuilder<?> factory = new BeanFactoryBuilder<>(reference.type())
				.qualifiers(reference.qualifiers())
				.autoDetect(autoDetect);
		reference.strategy().ifPresent(factory::strategy);
		reference.name().ifPresent(factory::name);
		this.beanFactories.add(factory.build());
	}
}