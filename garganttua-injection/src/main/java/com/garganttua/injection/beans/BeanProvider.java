package com.garganttua.injection.beans;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supplying.SupplyException;

public class BeanProvider extends AbstractLifecycle implements IBeanProvider {

	private List<IBeanFactory<?>> beanFactories;

	public BeanProvider(List<IBeanFactory<?>> beanFactories) {
		this.beanFactories = Objects.requireNonNull(beanFactories, "Bean factories cannot be null");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getBean(Class<T> type) throws DiException {
		wrapLifecycle(this::ensureInitializedAndStarted);
		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> type.isAssignableFrom(factory.getSuppliedType())).findFirst();
		if (factoryOpt.isPresent()) {
			try {
				return (Optional<T>) factoryOpt.get().supply();
			} catch (SupplyException e) {
				throw new DiException(e);
			}
		}
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
		wrapLifecycle(this::ensureInitializedAndStarted);
		throw new UnsupportedOperationException("Unimplemented method 'getBean'");
	}

	/*
	 * @Override
	 * public void registerBean(String name, Object bean) throws DiException {
	 * ensureInitializedAndStarted();
	 * throw new
	 * UnsupportedOperationException("Unimplemented method 'registerBean'");
	 * }
	 */

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
		List<T> l = (List<T>) this.beanFactories.stream()
				.filter(factory -> ObjectReflectionHelper.isImplementingInterface(interfasse,
						factory.getSuppliedType()))
				.map(factory -> {
					try {
						return factory.supply().orElse(null);
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				})
				.filter(Objects::nonNull)
				.map(interfasse::cast)
				.collect(Collectors.toList());
		return l;
	}

	@Override
	protected ILifecycle doInit() throws LifecycleException {
		try {
			this.doDependencyCycleDetection();
		} catch (DiException e) {
			throw new LifecycleException(e);
		}
		return this;
	}

	private void doDependencyCycleDetection() throws DiException {
		DependencyGraph graph = new DependencyGraph();
		this.beanFactories.stream().forEach(builder -> {
			builder.getDependencies().stream().forEach(dep -> {
				graph.addDependency(builder.getSuppliedType(), dep);
			});
		});
		new DependencyCycleDetector().detectCycles(graph);
	}

	@Override
	protected ILifecycle doStart() throws LifecycleException {
		return this;
	}

	@Override
	protected ILifecycle doFlush() throws LifecycleException {
		return this;
	}

	@Override
	protected ILifecycle doStop() throws LifecycleException {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
		wrapLifecycle(this::ensureInitializedAndStarted);

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> factory.matches(definition)).findFirst();
		if (factoryOpt.isPresent()) {
			try {
				return (Optional<T>) factoryOpt.get().supply();
			} catch (SupplyException e) {
				throw new DiException(e);
			}
		}
		return Optional.empty();
	}

	private void wrapLifecycle(RunnableWithException runnable) throws DiException {
		try {
			runnable.run();
		} catch (LifecycleException e) {
			throw new DiException(e);
		}
	}

	@FunctionalInterface
	interface RunnableWithException {
		void run() throws LifecycleException;
	}

}
