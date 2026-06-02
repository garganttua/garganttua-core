package com.garganttua.core.injection.context.beans;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.observability.ObservabilityEmitter;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

/**
 * {@link IBeanFactory} implementation that instantiates a single bean definition,
 * applying constructor binding, field injection and post-construct callbacks, and
 * honouring the singleton/prototype strategy declared by the {@link BeanDefinition}.
 *
 * @param <Bean> the type produced by this factory
 */
public class BeanFactory<Bean> implements IBeanFactory<Bean> {
    private static final Logger log = Logger.getLogger(BeanFactory.class);

	private volatile Bean bean;
	private BeanDefinition<Bean> definition;
	private final Object beanMutex = new Object();
	private boolean singletonBeanInitialized = false;

	/**
	 * Builds a factory that lazily instantiates the bean from the given definition.
	 */
	public BeanFactory(BeanDefinition<Bean> definition) {
		this(definition, Optional.empty());
	}

	/**
	 * Builds a factory, optionally seeded with a pre-built bean instance.
	 *
	 * @param bean an existing instance to wrap, or empty to instantiate lazily
	 */
	public BeanFactory(BeanDefinition<Bean> definition, Optional<Bean> bean) {
		this(definition, bean.orElse(null));
	}

	/**
	 * Builds a factory; when {@code bean} is non-null it is wrapped as a forced
	 * singleton, otherwise the bean is instantiated lazily on first {@link #supply()}.
	 *
	 * @param bean an existing instance to wrap, or {@code null} to instantiate lazily
	 */
	public BeanFactory(BeanDefinition<Bean> definition, Bean bean) {
		log.trace("Entering BeanFactory constructor with definition: {}", definition);
		this.definition = Objects.requireNonNull(definition, "Bean definition cannot be null");
		this.bean = bean;
		if (bean != null) {
			// If bean is provided, force singleton strategy
			log.debug("BeanFactory initialized with predefined bean, forcing singleton strategy");
			this.definition = new BeanDefinition<>(
					new BeanReference<>(definition.reference().type(), Optional.of(BeanStrategy.singleton),
							definition.reference().name(), definition.reference().qualifiers()),
					definition.constructorBinder(), definition.postConstructMethodBinderBuilders(),
					definition.injectableFields());
		}
		log.debug("BeanFactory initialized for definition: {}", definition);
		log.trace("Exiting BeanFactory constructor");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		log.trace("Entering equals method comparing with object: {}", o);
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BeanFactory<Bean> that = (BeanFactory<Bean>) o;
		boolean result = Objects.equals(this.definition, that.definition);
		log.trace("Exiting equals method with result: {}", result);
		return result;
	}

	@Override
	public int hashCode() {
		log.trace("Calculating hashCode for definition: {}", definition);
		return this.definition.hashCode();
	}

	private Bean getBean() throws DiException {
		log.trace("Creating new bean instance for definition: {}", definition);
		Bean bean = createBeanInstance();
		log.debug("Bean instance created: {}", bean);
		return bean;
	}

	private void doInjection(Bean onBean) {
		log.trace("Performing field injection for bean: {}", onBean);
		this.definition.injectableFields()
				.forEach(builder -> builder.ownerSupplierBuilder(new FixedSupplierBuilder<>(onBean, this.definition.reference().type())).build().setValue());
		log.debug("Field injection completed for bean: {}", onBean);
	}

	private Bean createBeanInstance() throws DiException {
		log.trace("Instantiating bean of type: {}", definition.reference().type());
		String source = "injection:bean:" + this.definition.reference().effectiveName();
		try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.joinCurrent()) {
			scope.fireStart(source);
			try {
				Bean result;
				if (this.definition.constructorBinder().isPresent()) {
					Optional<Bean> constructed = executeConstructorBinder();
					result = constructed.orElseThrow(() -> new DiException(
							"Constructor binder returned empty for bean of type "
									+ this.definition.reference().effectiveName()));
				} else {
					result = IClass.getReflection().newInstance(this.definition.reference().type());
				}
				scope.fireEnd(source);
				return result;
			} catch (Exception e) {
				log.error("Failed to instantiate bean of type {}: {}",
						this.definition.reference().effectiveName(), e.getMessage());
				scope.fireError(source, e);
				throw new DiException(
						"Failed to instantiate bean of type " + this.definition.reference().effectiveName(), e);
			}
		}
	}

	private Optional<Bean> executeConstructorBinder() throws DiException {
		log.trace("Executing constructor binder for definition: {}", definition);
		try {
			var result = this.definition.constructorBinder().get().execute();

			if (result.isEmpty()) {
				log.warn("Constructor binder returned empty result for definition: {}", definition);
				return Optional.empty();
			}

			var methodReturn = result.get();

			if (methodReturn.hasException()) {
				Throwable exception = methodReturn.getException();
				log.error("Constructor binder threw exception for definition {}: {}", definition, exception.getMessage());
				throw new DiException("Constructor threw exception for bean of type " + this.definition.reference().effectiveName(), exception);
			}

			Bean constructedBean = methodReturn.single();
			log.debug("Constructor binder result: {}", constructedBean);
			return Optional.ofNullable(constructedBean);
		} catch (ReflectionException e) {
			log.error("Constructor binder failed for definition {}: {}", definition, e.getMessage());
			throw new DiException(e);
		}
	}

	private void invokePostConstructMethods(Bean bean) throws DiException {
		log.trace("Invoking post construct methods for bean: {}", bean);
		if (this.definition.postConstructMethodBinderBuilders().isEmpty()) {
			log.debug("No post construct methods to invoke for bean: {}", bean);
			return;
		}

		for (IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder : this.definition
				.postConstructMethodBinderBuilders()) {
			try {
				IMethodBinder<Void> methodBinder = methodBinderBuilder.build(FixedSupplierBuilder.of(bean, this.definition.reference().type()));
				methodBinder.execute();
				log.debug("Post construct method executed for bean: {}", bean);
			} catch (DslException | ReflectionException e) {
				log.error("Post construct method binder failed for bean {}: {}",
						this.definition.reference().effectiveName(), e.getMessage());
				throw new DiException(
						"Post construct method binder failed for bean of type "
								+ this.definition.reference().effectiveName(),
						e);
			}
		}
	}

	@Override
	public Optional<Bean> supply() throws SupplyException {
		log.trace("Supplying bean for definition: {}", definition);
		Bean bean = null;
		Optional<BeanStrategy> strat = this.definition.reference().strategy();
		try {
			if (strat.isPresent()) {
				if (strat.get() == BeanStrategy.prototype) {
					bean = createAndInitializePrototype();
				} else {
					bean = createAnInitializeSingleton(bean);
				}
			} else {
				bean = createAnInitializeSingleton(bean);
			}
			log.debug("Bean supplied: {}", bean);
			return Optional.ofNullable(bean);
		} catch (DiException e) {
			log.error("Failed to supply bean for definition {}: {}", definition, e.getMessage());
			throw new SupplyException(e);
		}
	}

	private Bean createAndInitializePrototype() {
		Bean bean;
		synchronized (this.beanMutex) {
			log.debug("Using prototype strategy for bean");
			bean = getBean();
			this.doInjection(bean);
			this.invokePostConstructMethods(bean);
		}
		return bean;
	}

	private Bean createAnInitializeSingleton(Bean bean) {
		synchronized (this.beanMutex) {
			if (this.bean == null) {
				log.debug("Creating singleton bean");
				this.bean = getBean();
			}
			if (!this.singletonBeanInitialized) {
				this.doInjection(this.bean);
				this.invokePostConstructMethods(this.bean);
				this.singletonBeanInitialized = true;
				log.debug("Singleton bean initialized flag set to true");
			}
			return this.bean;
		}
	}

	@Override
	public Type getSuppliedType() {
		log.trace("Returning supplied type: {}", definition.reference().type());
		return this.definition.reference().type().getType();
	}

	@Override
	public IClass<Bean> getSuppliedClass() {
		return this.definition.reference().type();
	}

	@Override
	public boolean matches(BeanReference<?> query) {
		log.trace("Checking matches for definition: {} against example: {}", definition, query);
		boolean match = this.definition.reference().matches(query);
		log.debug("Match result: {}", match);
		return match;
	}

	@Override
	public BeanDefinition<Bean> definition() {
		log.trace("Returning bean definition: {}", definition);
		return this.definition;
	}

	@Override
	public Set<IClass<?>> dependencies() {
		log.trace("Returning dependencies for definition: {}", definition);
		return this.definition.dependencies();
	}

	@Override
	public IReflectionConfigurationEntryBuilder nativeEntry() {
		log.trace("Building native configuration entry for definition: {}", definition);
		ReflectConfigEntryBuilder eb = new ReflectConfigEntryBuilder(definition.reference().type());

		// Constructor
		definition.constructorBinder().ifPresentOrElse(c -> {
			log.debug("Adding constructor binder to native entry for type: {}", definition.reference().type());
			eb.constructor(c.constructor());
		}, () -> {
			try {
				log.debug("Adding default constructor to native entry for type: {}",
						definition.reference().type());
				eb.constructor(definition.reference().type().getDeclaredConstructor());
			} catch (NoSuchMethodException | SecurityException e) {
				log.warn("Error adding default constructor for type {}: {}", definition.reference().type(),
						e.getMessage());
			}
		});

		// Fields
		log.debug("Adding {} injectable fields to native entry", definition.injectableFields().size());
		definition.injectableFields().forEach(f -> eb.field(f.field().getName()));

		// Methods
		log.debug("Adding {} post construct methods to native entry",
				definition.postConstructMethodBinderBuilders().size());
		definition.postConstructMethodBinderBuilders().forEach(m -> {
			IMethod iMethod = m.method();
			eb.method(iMethod.getName(), iMethod.getParameterTypes());
		});

		log.debug("Native configuration entry built for type: {}", definition.reference().type());
		return eb;
	}
}