package com.garganttua.core.injection.context.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.context.validation.DependencyCycleDetector;
import com.garganttua.core.injection.context.validation.DependencyGraph;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.utils.CopyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanProvider extends AbstractLifecycle implements IBeanProvider {

	private List<IBeanFactory<?>> beanFactories;

	private final Object copyMutex = new Object();

	public BeanProvider(List<IBeanFactory<?>> beanFactories) {
		log.atTrace().log("Entering BeanProvider constructor with beanFactories: {}", beanFactories);
		this.beanFactories = Collections
				.synchronizedList(Objects.requireNonNull(beanFactories, "Bean factories cannot be null"));
		log.atDebug().log("BeanProvider initialized with {} bean factories", beanFactories.size());
		log.atTrace().log("Exiting BeanProvider constructor");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getBean(Class<T> type) throws DiException {
		log.atTrace().log("Entering getBean with type: {}", type);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> type.isAssignableFrom(factory.getSuppliedType()))
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
	public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
		log.atTrace().log("getBean by name '{}' and type {} is not implemented", name, type);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
		throw new UnsupportedOperationException("Unimplemented method 'getBean'");
	}

	@Override
	public boolean isMutable() {
		log.atTrace().log("Checking if BeanProvider is mutable: returning false");
		return false;
	}

	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
		log.atTrace().log("Getting beans implementing interface: {}", interfasse);
		List<T> result = this.beanFactories.stream()
				.filter(factory -> ObjectReflectionHelper.isImplementingInterface(interfasse,
						factory.getSuppliedType()))
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
		this.beanFactories.forEach(builder -> builder.getDependencies()
				.forEach(dep -> graph.addDependency(builder.getSuppliedType(), dep)));
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
	public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
		log.atTrace().log("Querying single bean with definition: {}", definition);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> factory.matches(definition))
				.findFirst();

		if (factoryOpt.isPresent()) {
			try {
				Optional<T> result = (Optional<T>) factoryOpt.get().supply();
				log.atInfo().log("Bean found for definition {}: {}", definition, result.orElse(null));
				return result;
			} catch (SupplyException e) {
				log.atError().log("Failed to supply bean for definition {}: {}", definition, e.getMessage());
				throw new DiException(e);
			}
		}

		log.atWarn().log("No bean found for definition {}", definition);
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> queryBeans(BeanDefinition<T> definition) throws DiException {
		log.atTrace().log("Querying multiple beans with definition: {}", definition);
		wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);

		List<T> result = (List<T>) this.beanFactories.stream()
				.filter(factory -> factory.matches(definition))
				.map(IObjectSupplier::supply)
				.map(Optional::get)
				.toList();

		log.atInfo().log("Beans found for definition {}: {}", definition, result.size());
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
		return this.beanFactories.size();
	}

	@Override
	public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
		return this.beanFactories.stream().map(f -> f.nativeEntry()).collect(Collectors.toSet());
	}
}