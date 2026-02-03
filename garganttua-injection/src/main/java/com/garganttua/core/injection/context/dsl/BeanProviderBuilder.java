package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.beans.BeanProvider;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanProviderBuilder
		extends AbstractAutomaticLinkedDependentBuilder<IBeanProviderBuilder, IInjectionContextBuilder, IBeanProvider>
		implements IBeanProviderBuilder {

	private static final String SOURCE_MANUAL = "manual";
	private static final String SOURCE_AUTO_DETECTED = "auto-detected";

	private Map<Class<?>, IBeanFactoryBuilder<?>> manualBeanFactoryBuilders = new HashMap<>();
	private Map<Class<?>, IBeanFactoryBuilder<?>> autoDetectedBeanFactoryBuilders = new HashMap<>();
	private Set<String> packages = new HashSet<>();

	@Setter
	private Set<Class<? extends Annotation>> qualifierAnnotations;
	private IInjectableElementResolverBuilder resolverBuilder;

	public BeanProviderBuilder(IInjectionContextBuilder link) {
		super(link, Set.of(new DependencySpecBuilder(IInjectableElementResolverBuilder.class).requireForAutoDetect().build()));
		log.atTrace().log("Entering BeanProviderBuilder constructor with link: {}", link);
		log.atTrace().log("BeanProviderBuilder initialized with link: {}", link);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException {
		log.atTrace().log("Entering withBean() method with beanType: {}", beanType.getSimpleName());
		log.atDebug().log("Registering bean type: {}", beanType.getSimpleName());
		IBeanFactoryBuilder<BeanType> builder = (IBeanFactoryBuilder<BeanType>) this.manualBeanFactoryBuilders
				.computeIfAbsent(beanType,
						key -> {
							log.atTrace().log("Creating new BeanFactoryBuilder for type: {}", key.getSimpleName());
							return new BeanFactoryBuilder<>(key);
						});
		log.atTrace().log("Exiting withBean() method for beanType: {}", beanType.getSimpleName());
		return builder;
	}

	@Override
	protected IBeanProvider doBuild() throws DslException {
		log.atTrace().log("Entering doBuild() method");

		Map<Class<?>, IBeanFactoryBuilder<?>> allBuilders = this.computeBeanFactoryBuilders();
		log.atDebug().log("Building IBeanProvider with {} factories", allBuilders.size());

		IBeanProvider provider;
		try {
			provider = new BeanProvider(allBuilders.values().stream()
					.map(IBeanFactoryBuilder::build)
					.collect(Collectors.toList()), Optional.ofNullable(this.resolverBuilder), true);
			log.atDebug().log("IBeanProvider successfully built with {} beans", provider.size());
		} catch (Exception e) {
			log.atError().log("Failed to build IBeanProvider. Error: {}", e.getMessage(), e);
			throw new DslException("Error building IBeanProvider", e);
		}

		log.atTrace().log("Exiting doBuild() method");
		return provider;
	}

	@Override
	protected void doAutoDetection() throws DslException {
		log.atTrace().log("Entering doAutoDetection() method");
		this.packages.forEach(pkg -> {
			log.atDebug().log("Auto-detecting beans in package: {}", pkg);

			// 1. Singleton
			ObjectReflectionHelper.getClassesWithAnnotation(pkg, Singleton.class)
					.forEach(singletonClass -> {
						try {
							log.atTrace().log("Detected @Singleton class: {}", singletonClass.getSimpleName());
							this.autoDetectedBeanFactoryBuilders.put(singletonClass,
									this.createBeanFactory(qualifierAnnotations, singletonClass)
											.strategy(BeanStrategy.singleton));
						} catch (DslException e) {
							log.atError().log("Failed to create singleton bean factory for {}",
									singletonClass.getSimpleName(), e);
						}
					});

			// 2. Prototype
			ObjectReflectionHelper.getClassesWithAnnotation(pkg, Prototype.class)
					.forEach(prototypeClass -> {
						try {
							log.atTrace().log("Detected @Prototype class: {}", prototypeClass.getSimpleName());
							this.autoDetectedBeanFactoryBuilders.put(prototypeClass,
									this.createBeanFactory(qualifierAnnotations, prototypeClass)
											.strategy(BeanStrategy.prototype));
						} catch (DslException e) {
							log.atError().log("Failed to create prototype bean factory for {}",
									prototypeClass.getSimpleName(), e);
						}
					});

			// 3. Qualifiers
			qualifierAnnotations.forEach(qualifierAnnotation -> {
				ObjectReflectionHelper.getClassesWithAnnotation(pkg, qualifierAnnotation)
						.forEach(qualifiedClass -> {
							try {
								log.atTrace().log("Detected @Qualifier class: {} with qualifier {}",
										qualifiedClass.getSimpleName(), qualifierAnnotation.getSimpleName());
								this.autoDetectedBeanFactoryBuilders.put(qualifiedClass,
										this.createBeanFactory(qualifierAnnotations, qualifiedClass)
												.strategy(BeanStrategy.singleton));
							} catch (DslException e) {
								log.atError().log("Failed to create bean factory for qualifier class {}",
										qualifiedClass.getSimpleName(), e);
							}
						});
			});
		});
		log.atTrace().log("Exiting doAutoDetection() method");
	}

	private IBeanFactoryBuilder<?> createBeanFactory(Set<Class<? extends Annotation>> qualifierAnnotations,
			Class<?> beanClass) throws DslException {
		log.atTrace().log("Entering createBeanFactory() for class: {}", beanClass.getSimpleName());

		Set<Class<? extends Annotation>> classQualifiers = qualifierAnnotations.stream()
				.filter(qualifier -> beanClass.isAnnotationPresent(qualifier))
				.collect(Collectors.toSet());

		IBeanFactoryBuilder<?> builder = new BeanFactoryBuilder<>(beanClass)
				.provide(this.resolverBuilder)
				.autoDetect(true)
				.qualifiers(classQualifiers);

		Named namedAnnotation = beanClass.getAnnotation(Named.class);
		if (namedAnnotation != null && !namedAnnotation.value().isBlank()) {
			builder.name(namedAnnotation.value());
			log.atDebug().log("Bean class {} has @Named annotation with value: {}", beanClass.getSimpleName(),
					namedAnnotation.value());
		}

		log.atTrace().log("Created BeanFactoryBuilder for class: {} with qualifiers: {}", beanClass.getSimpleName(),
				classQualifiers);
		log.atTrace().log("Exiting createBeanFactory() for class: {}", beanClass.getSimpleName());
		return builder;
	}

	@Override
	public IBeanProviderBuilder withPackage(String packageName) {
		log.atTrace().log("Entering withPackage() method with package: {}", packageName);
		log.atDebug().log("Adding package for auto-detection: {}", packageName);
		this.packages.add(packageName);
		log.atTrace().log("Exiting withPackage() method for package: {}", packageName);
		return this;
	}

	@Override
	public IBeanProviderBuilder withPackages(String[] packageNames) {
		log.atTrace().log("Entering withPackages() method with packages: {}", Arrays.toString(packageNames));
		log.atDebug().log("Adding multiple packages for auto-detection: {}", Arrays.toString(packageNames));
		this.packages.addAll(Arrays.asList(packageNames));
		log.atTrace().log("Exiting withPackages() method with packages: {}", Arrays.toString(packageNames));
		return this;
	}

	private ISupplier<Map<Class<?>, IBeanFactoryBuilder<?>>> beanFactorySupplier(
			Map<Class<?>, IBeanFactoryBuilder<?>> builders) {
		return new ISupplier<Map<Class<?>, IBeanFactoryBuilder<?>>>() {
			@Override
			public Optional<Map<Class<?>, IBeanFactoryBuilder<?>>> supply() throws SupplyException {
				return Optional.of(builders);
			}

			@Override
			public Type getSuppliedType() {
				throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
			}
		};
	}

	private Map<Class<?>, IBeanFactoryBuilder<?>> computeBeanFactoryBuilders() {
		MultiSourceCollector<Class<?>, IBeanFactoryBuilder<?>> collector = new MultiSourceCollector<>();

		collector.source(beanFactorySupplier(manualBeanFactoryBuilders), 0, SOURCE_MANUAL);
		collector.source(beanFactorySupplier(autoDetectedBeanFactoryBuilders), 1, SOURCE_AUTO_DETECTED);

		return collector.build();
	}

	@Override
	protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
	}

	@Override
	protected void doPreBuildWithDependency(Object dependency) {
	}

	@Override
	protected void doPostBuildWithDependency(Object dependency) {
	}

	@Override
    public IBeanProviderBuilder provide(IObservableBuilder<?, ?> dependency) {
        if (dependency instanceof IInjectableElementResolverBuilder rb) {
            this.resolverBuilder = rb;
        }
        return super.provide(dependency);
    }
}