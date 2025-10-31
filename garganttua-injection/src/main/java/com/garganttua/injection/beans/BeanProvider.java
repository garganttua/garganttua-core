package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.AbstractLifecycle;
import com.garganttua.injection.DiException;
import com.garganttua.injection.IInjectableBuilderRegistry;
import com.garganttua.injection.InjectBuilderFactory;
import com.garganttua.injection.InjectableBuilderRegistry;
import com.garganttua.injection.PropertyBuilderFactory;
import com.garganttua.injection.PrototypeBuilderFactory;
import com.garganttua.injection.SingletonBuilderFactory;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.ILifecycle;
import com.garganttua.injection.spec.beans.annotation.Property;
import com.garganttua.injection.spec.beans.annotation.Prototype;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanProvider extends AbstractLifecycle implements IBeanProvider {

	private static final String BEAN_PROVIDER = "garganttua";

	private Collection<String> packages;

	private Map<Class<?>, IBeanFactoryBuilder<?>> beansFactoryBuilders = new HashMap<>();

	private List<IBeanFactory<?>> beanFactories;

	public BeanProvider(Collection<String> packages) {
		this.packages = Objects.requireNonNull(packages, "Packages collection cannot be null");
	}

	@Override
	public String getName() {
		return BEAN_PROVIDER;
	}

	private IBeanFactoryBuilder<?> createBeanFactory(List<Class<? extends Annotation>> qualifierAnnotations,
			Class<?> beanClass, IInjectableBuilderRegistry registry)
			throws DslException {
		Set<Class<? extends Annotation>> classQualifiers = qualifierAnnotations.stream()
				.filter(qualifier -> beanClass
						.isAnnotationPresent((Class<? extends Annotation>) qualifier))
				.map(q -> (Class<? extends Annotation>) q)
				.collect(Collectors.toSet());

		IBeanFactoryBuilder<?> builder = new BeanFactoryBuilder<>(beanClass, registry)
				.autoDetect(true)
				.qualifiers(classQualifiers);

		Named namedAnnotation = beanClass.getAnnotation(Named.class);
		if (namedAnnotation != null && !namedAnnotation.value().isEmpty() && !namedAnnotation.value().isBlank()) {
			builder.name(namedAnnotation.value());
		}

		return builder;
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

	@Override
	public void registerBean(String name, Object bean) throws DiException {
		ensureInitializedAndStarted();
		throw new UnsupportedOperationException("Unimplemented method 'registerBean'");
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
		List<T> l = (List<T>) this.beansFactoryBuilders.entrySet().stream()
				.filter(entry -> GGObjectReflectionHelper.isImplementingInterface(interfasse,
						entry.getValue().getObjectClass()))
				.map(entry -> {
					try {
						return entry.getValue().build().getObject().orElse(null);
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

	@SuppressWarnings("unchecked")
	@Override
	protected ILifecycle doInit() throws DiException {
		packages.forEach(package_ -> {

			IInjectableBuilderRegistry registry = new InjectableBuilderRegistry();
			registry.registerFactory(Prototype.class, new PrototypeBuilderFactory(new HashSet<>()));
			registry.registerFactory(Singleton.class, new SingletonBuilderFactory(new HashSet<>()));
			registry.registerFactory(Inject.class, new InjectBuilderFactory(new HashSet<>()));
			registry.registerFactory(Property.class, new PropertyBuilderFactory());

			// 1. Récupère toutes les classes annotées avec @Qualifier
			List<Class<? extends Annotation>> qualifierAnnotations = GGObjectReflectionHelper
					.getClassesWithAnnotation(package_,
							Qualifier.class)
					.stream()
					.filter(qualifierAnnotation -> qualifierAnnotation.getAnnotation(Qualifier.class) != null)
					.map(qualifierAnnotation -> (Class<? extends Annotation>) qualifierAnnotation)
					.collect(Collectors.toList());

			// 2. Récupère toutes les classes annotées avec @Singleton
			GGObjectReflectionHelper.getClassesWithAnnotation(package_,
					Singleton.class).forEach(singletonClass -> {
						try {
							this.beansFactoryBuilders
									.put(singletonClass, this.createBeanFactory(qualifierAnnotations, singletonClass, registry)
											.strategy(BeanStrategy.singleton));
						} catch (DslException e) {
							e.printStackTrace();
						}
					});

			// 3. Récupère toutes les classes annotées avec @Prototype
			GGObjectReflectionHelper.getClassesWithAnnotation(package_,
					Prototype.class).forEach(prototypeClass -> {
						try {
							this.beansFactoryBuilders
									.put(prototypeClass, this.createBeanFactory(qualifierAnnotations, prototypeClass, registry)
											.strategy(BeanStrategy.prototype));
						} catch (DslException e) {
							e.printStackTrace();
						}
					});

			// 4. Récupère toutes les classes annotées avec les annotation qualifier
			qualifierAnnotations.forEach(qualifierAnnotation -> {
				GGObjectReflectionHelper.getClassesWithAnnotation(package_,
						qualifierAnnotation).forEach(prototypeClass -> {
							try {
								this.beansFactoryBuilders
										.put(prototypeClass,
												this.createBeanFactory(qualifierAnnotations, prototypeClass, registry)
														.strategy(BeanStrategy.singleton));
							} catch (DslException e) {
								e.printStackTrace();
							}
						});
			});

		});
		return this;
	}

	@Override
	protected ILifecycle doStart() throws DiException {
		this.beanFactories = Collections.synchronizedList(this.beansFactoryBuilders.values().stream().map(builder -> {
			try {
				return builder.build();
			} catch (DslException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList()));

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
