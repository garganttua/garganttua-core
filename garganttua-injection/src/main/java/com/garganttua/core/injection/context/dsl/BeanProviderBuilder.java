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

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.dependency.AbstractAutomaticLinkedDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.beans.BeanProvider;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder that assembles an {@link IBeanProvider} from manually registered bean factories
 * and classpath-scanned beans annotated with {@code @Singleton}, {@code @Prototype}, or
 * any registered qualifier annotation.
 *
 * <p>Requires an {@link IInjectableElementResolverBuilder} for auto-detection and an
 * {@link IReflectionBuilder} at build time.
 */
@Reflected
public class BeanProviderBuilder
		extends AbstractAutomaticLinkedDependentBuilder<IBeanProviderBuilder, IInjectionContextBuilder, IBeanProvider>
		implements IBeanProviderBuilder {
    private static final Logger log = Logger.getLogger(BeanProviderBuilder.class);

	private static final String SOURCE_MANUAL = "manual";
	private static final String SOURCE_AUTO_DETECTED = "auto-detected";

	private Map<String, IBeanFactoryBuilder<?>> manualBeanFactoryBuilders = new HashMap<>();
	private Map<String, IBeanFactoryBuilder<?>> autoDetectedBeanFactoryBuilders = new HashMap<>();
	private Set<String> packages = new HashSet<>();

	private Set<IClass<? extends Annotation>> qualifierAnnotations;
	private IReflection reflection;
	private IInjectableElementResolverBuilder resolverBuilder;
	private IObservableBuilder<?, ?> reflectionBuilderRef;

	/**
	 * Creates a bean provider builder linked to its parent injection context builder.
	 *
	 * @param link the parent injection context builder
	 */
	public BeanProviderBuilder(IInjectionContextBuilder link) {
		super(link, Set.of(
				new DependencySpecBuilder(IClass.getClass(IInjectableElementResolverBuilder.class)).requireForAutoDetect().build(),
				DependencySpec.require(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BUILD)));
		log.trace("Entering BeanProviderBuilder constructor with link: {}", link);
		log.trace("BeanProviderBuilder initialized with link: {}", link);
	}

	/**
	 * Registers (or returns the existing) manual bean factory builder for the given bean type.
	 *
	 * @param beanType the bean type to register
	 * @param <BeanType> the bean type parameter
	 * @return the bean factory builder for the type
	 * @throws DslException if the factory builder cannot be created
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <BeanType> IBeanFactoryBuilder<BeanType> withBean(IClass<BeanType> beanType) throws DslException {
		log.trace("Entering withBean() method with beanType: {}", beanType.getSimpleName());
		log.debug("Registering bean type: {}", beanType.getSimpleName());
		String key = beanType.getName();
		IBeanFactoryBuilder<BeanType> builder = (IBeanFactoryBuilder<BeanType>) this.manualBeanFactoryBuilders
				.computeIfAbsent(key,
						k -> {
							log.trace("Creating new BeanFactoryBuilder for type: {}", beanType.getSimpleName());
							return new BeanFactoryBuilder<>(beanType);
						});
		log.trace("Exiting withBean() method for beanType: {}", beanType.getSimpleName());
		return builder;
	}

	@Override
	protected IBeanProvider doBuild() throws DslException {
		log.trace("Entering doBuild() method");

		Map<String, IBeanFactoryBuilder<?>> allBuilders = this.computeBeanFactoryBuilders();
		log.debug("Building IBeanProvider with {} factories", allBuilders.size());

		// Propagate IReflectionBuilder to all bean factory builders before building
		if (this.reflectionBuilderRef != null) {
			allBuilders.values().forEach(b -> b.provide(this.reflectionBuilderRef));
		}

		IBeanProvider provider;
		try {
			provider = new BeanProvider(allBuilders.values().stream()
					.map(IBeanFactoryBuilder::build)
					.collect(Collectors.toList()), Optional.ofNullable(this.resolverBuilder), true);
			log.debug("IBeanProvider successfully built with {} beans", provider.size());
		} catch (Exception e) {
			log.error("Failed to build IBeanProvider. Error: {}", e.getMessage(), e);
			throw new DslException("Error building IBeanProvider", e);
		}

		log.trace("Exiting doBuild() method");
		return provider;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doAutoDetection() throws DslException {
		log.trace("Entering doAutoDetection() method");
		if (this.reflection == null) {
			log.warn("IReflection not set, skipping auto-detection");
			return;
		}

		IClass<? extends Annotation> singletonAnnotation = (IClass<? extends Annotation>) reflection.getClass(Singleton.class);
		IClass<? extends Annotation> prototypeAnnotation = (IClass<? extends Annotation>) reflection.getClass(Prototype.class);

		this.packages.parallelStream().forEach(pkg -> {
			log.debug("Auto-detecting beans in package: {}", pkg);
			detectAndRegister(pkg, singletonAnnotation, BeanStrategy.singleton);
			detectAndRegister(pkg, prototypeAnnotation, BeanStrategy.prototype);
			qualifierAnnotations.forEach(qualifier -> detectAndRegister(pkg, qualifier, BeanStrategy.singleton));
		});
		log.trace("Exiting doAutoDetection() method");
	}

	/** Register every class in {@code pkg} annotated with {@code annotation} as an auto-detected bean factory with the given strategy. */
	private void detectAndRegister(String pkg, IClass<? extends Annotation> annotation, BeanStrategy strategy) {
		reflection.getClassesWithAnnotation(pkg, annotation).forEach(beanClass -> {
			try {
				synchronized (this.autoDetectedBeanFactoryBuilders) {
					this.autoDetectedBeanFactoryBuilders.put(beanClass.getName(),
							this.createBeanFactory(qualifierAnnotations, beanClass).strategy(strategy));
				}
			} catch (DslException e) {
				log.error("Failed to create bean factory for {}", beanClass.getSimpleName(), e);
			}
		});
	}

	private IBeanFactoryBuilder<?> createBeanFactory(Set<IClass<? extends Annotation>> qualifierAnnotations,
			IClass<?> beanClass) throws DslException {
		log.trace("Entering createBeanFactory() for class: {}", beanClass.getSimpleName());

		Set<IClass<? extends Annotation>> classQualifiers = qualifierAnnotations.stream()
				.filter(qualifier -> beanClass.isAnnotationPresent(qualifier))
				.collect(Collectors.toSet());

		IBeanFactoryBuilder<?> builder = new BeanFactoryBuilder<>(beanClass);
		if (this.reflectionBuilderRef != null) {
			builder.provide(this.reflectionBuilderRef);
		}
		builder.provide(this.resolverBuilder)
				.autoDetect(true)
				.qualifiers(classQualifiers);

		IClass<Named> namedClass = IClass.getClass(Named.class);
		Named namedAnnotation = beanClass.getAnnotation(namedClass);
		if (namedAnnotation != null && !namedAnnotation.value().isBlank()) {
			builder.name(namedAnnotation.value());
			log.debug("Bean class {} has @Named annotation with value: {}", beanClass.getSimpleName(),
					namedAnnotation.value());
		}

		log.trace("Created BeanFactoryBuilder for class: {} with qualifiers: {}", beanClass.getSimpleName(),
				classQualifiers);
		log.trace("Exiting createBeanFactory() for class: {}", beanClass.getSimpleName());
		return builder;
	}

	/**
	 * Adds a package to scan for annotated beans during auto-detection.
	 *
	 * @param packageName the package to scan
	 * @return this builder for chaining
	 */
	@Override
	public IBeanProviderBuilder withPackage(String packageName) {
		log.trace("Entering withPackage() method with package: {}", packageName);
		log.debug("Adding package for auto-detection: {}", packageName);
		this.packages.add(packageName);
		log.trace("Exiting withPackage() method for package: {}", packageName);
		return this;
	}

	/**
	 * Adds several packages to scan for annotated beans during auto-detection.
	 *
	 * @param packageNames the packages to scan
	 * @return this builder for chaining
	 */
	@Override
	public IBeanProviderBuilder withPackages(String[] packageNames) {
		log.trace("Entering withPackages() method with packages: {}", Arrays.toString(packageNames));
		log.debug("Adding multiple packages for auto-detection: {}", Arrays.toString(packageNames));
		this.packages.addAll(Arrays.asList(packageNames));
		log.trace("Exiting withPackages() method with packages: {}", Arrays.toString(packageNames));
		return this;
	}

	private ISupplier<Map<String, IBeanFactoryBuilder<?>>> beanFactorySupplier(
			Map<String, IBeanFactoryBuilder<?>> builders) {
		return new ISupplier<Map<String, IBeanFactoryBuilder<?>>>() {
			@Override
			public Optional<Map<String, IBeanFactoryBuilder<?>>> supply() throws SupplyException {
				return Optional.of(builders);
			}

			@Override
			public Type getSuppliedType() {
				throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
			}

			@Override
			public IClass<Map<String, IBeanFactoryBuilder<?>>> getSuppliedClass() {
				throw new UnsupportedOperationException("Unimplemented method 'getSuppliedClass'");
			}
		};
	}

	private Map<String, IBeanFactoryBuilder<?>> computeBeanFactoryBuilders() {
		MultiSourceCollector<String, IBeanFactoryBuilder<?>> collector = new MultiSourceCollector<>();

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

	/**
	 * Sets the resolved {@link IReflection} used to scan packages during auto-detection.
	 *
	 * @param reflection the reflection facade
	 */
	public void setReflection(IReflection reflection) {
		this.reflection = reflection;
	}

	/**
	 * Sets the qualifier annotations whose presence on a class marks it as an auto-detectable bean.
	 *
	 * @param qualifierAnnotations the qualifier annotation classes
	 */
	public void setQualifierAnnotations(Set<IClass<? extends Annotation>> qualifierAnnotations) {
		this.qualifierAnnotations = qualifierAnnotations;
	}

	/**
	 * Captures the {@link IInjectableElementResolverBuilder} and {@link IReflectionBuilder} dependencies
	 * when supplied, then delegates to the superclass.
	 *
	 * @param dependency the dependency builder being provided
	 * @return this builder for chaining
	 */
	@Override
    public IBeanProviderBuilder provide(IObservableBuilder<?, ?> dependency) {
        if (dependency instanceof IInjectableElementResolverBuilder rb) {
            this.resolverBuilder = rb;
        }
        if (dependency instanceof IReflectionBuilder) {
            this.reflectionBuilderRef = dependency;
        }
        return super.provide(dependency);
    }
}
