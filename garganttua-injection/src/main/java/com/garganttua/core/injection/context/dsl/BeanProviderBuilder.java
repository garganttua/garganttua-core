package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.beans.BeanProvider;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanProviderBuilder
		extends AbstractAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider>
		implements IBeanProviderBuilder {

	private Map<Class<?>, IBeanFactoryBuilder<?>> beanFactoryBuilders = new HashMap<>();
	private Set<String> packages = new HashSet<>();

	@Setter
	private IInjectableElementResolver resolver;

	@Setter
	private Set<Class<? extends Annotation>> qualifierAnnotations;

	public BeanProviderBuilder(IDiContextBuilder link) {
		super(link);
		log.atTrace().log("Entering BeanProviderBuilder constructor with link: {}", link);
		log.atTrace().log("BeanProviderBuilder initialized with link: {}", link);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException {
		log.atTrace().log("Entering withBean() method with beanType: {}", beanType.getSimpleName());
		log.atDebug().log("Registering bean type: {}", beanType.getSimpleName());
		IBeanFactoryBuilder<BeanType> builder = (IBeanFactoryBuilder<BeanType>) this.beanFactoryBuilders
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
		log.atInfo().log("Building IBeanProvider with {} factories", beanFactoryBuilders.size());

		IBeanProvider provider;
		try {
			provider = new BeanProvider(this.beanFactoryBuilders.values().stream()
					.map(IBeanFactoryBuilder::build)
					.collect(Collectors.toList()), Optional.ofNullable(this.resolver), true);
			log.atInfo().log("IBeanProvider successfully built with {} beans", provider.size());
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
							this.beanFactoryBuilders.put(singletonClass,
									this.createBeanFactory(qualifierAnnotations, singletonClass, resolver)
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
							this.beanFactoryBuilders.put(prototypeClass,
									this.createBeanFactory(qualifierAnnotations, prototypeClass, resolver)
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
								this.beanFactoryBuilders.put(qualifiedClass,
										this.createBeanFactory(qualifierAnnotations, qualifiedClass, resolver)
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
			Class<?> beanClass,
			IInjectableElementResolver resolver) throws DslException {
		log.atTrace().log("Entering createBeanFactory() for class: {}", beanClass.getSimpleName());

		Set<Class<? extends Annotation>> classQualifiers = qualifierAnnotations.stream()
				.filter(qualifier -> beanClass.isAnnotationPresent(qualifier))
				.collect(Collectors.toSet());

		IBeanFactoryBuilder<?> builder = new BeanFactoryBuilder<>(beanClass, resolver)
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
}