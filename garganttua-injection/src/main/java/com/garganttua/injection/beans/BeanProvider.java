package com.garganttua.injection.beans;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.injection.AbstractLifecycle;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.ILifecycle;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanProvider extends AbstractLifecycle implements IBeanProvider {

	private List<IBeanFactory<?>> beanFactories;

	public BeanProvider(List<IBeanFactory<?>> beanFactories) {
		this.beanFactories = Objects.requireNonNull(beanFactories, "Bean factories cannot be null");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getBean(Class<T> type) throws DiException {
		ensureInitializedAndStarted();
		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> type.isAssignableFrom(factory.getObjectClass())).findFirst();
		if (factoryOpt.isPresent()) {
			return (Optional<T>) factoryOpt.get().getObject();
		}
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
		ensureInitializedAndStarted();
		throw new UnsupportedOperationException("Unimplemented method 'getBean'");
	}

	/* @Override
	public void registerBean(String name, Object bean) throws DiException {
		ensureInitializedAndStarted();
		throw new UnsupportedOperationException("Unimplemented method 'registerBean'");
	} */

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
		List<T> l = (List<T>) this.beanFactories.stream()
				.filter(factory -> GGObjectReflectionHelper.isImplementingInterface(interfasse,
						factory.getObjectClass()))
				.map(factory -> {
					try {
						return factory.getObject().orElse(null);
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
	protected ILifecycle doInit() throws DiException {
		this.doDependencyCycleDetection();
		return this;
	}

	private void doDependencyCycleDetection() throws DiException {
		DependencyGraph graph = new DependencyGraph();
		this.beanFactories.stream().forEach(builder -> {
			builder.getDependencies().stream().forEach(dep -> {
				graph.addDependency(builder.getObjectClass(), dep);
			});
		});
		new DependencyCycleDetector().detectCycles(graph);
	}

	@Override
	protected ILifecycle doStart() throws DiException {
		return this;
	}

	@Override
	protected ILifecycle doFlush() throws DiException {
		return this;
	}

	@Override
	protected ILifecycle doStop() throws DiException {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
		ensureInitializedAndStarted();

		Optional<IBeanFactory<?>> factoryOpt = this.beanFactories.stream()
				.filter(factory -> factory.matches(definition)).findFirst();
		if (factoryOpt.isPresent()) {
			return (Optional<T>) factoryOpt.get().getObject();
		}
		return Optional.empty();
	}

}
