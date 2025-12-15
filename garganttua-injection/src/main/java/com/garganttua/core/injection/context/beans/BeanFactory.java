package com.garganttua.core.injection.context.beans;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.nativve.image.config.reflection.ReflectConfigEntryBuilder;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanFactory<Bean> implements IBeanFactory<Bean> {

	private volatile Bean bean;
	private BeanDefinition<Bean> definition;
	private final Object beanMutex = new Object();
	private boolean singletonBeanInitialized = false;

	public BeanFactory(BeanDefinition<Bean> definition) {
		this(definition, Optional.empty());
	}

	public BeanFactory(BeanDefinition<Bean> definition, Optional<Bean> bean) {
		this(definition, bean.orElse(null));
	}

	public BeanFactory(BeanDefinition<Bean> definition, Bean bean) {
		log.atTrace().log("Entering BeanFactory constructor with definition: {}", definition);
		this.definition = Objects.requireNonNull(definition, "Bean definition cannot be null");
		this.bean = bean;
		if (bean != null) {
			// If bean is provided, force singleton strategy
			log.atInfo().log("BeanFactory initialized with predefined bean, forcing singleton strategy");
			this.definition = new BeanDefinition<>(
					new BeanReference<>(definition.reference().type(), Optional.of(BeanStrategy.singleton),
							definition.reference().name(), definition.reference().qualifiers()),
					definition.constructorBinder(), definition.postConstructMethodBinderBuilders(),
					definition.injectableFields());
		}
		log.atInfo().log("BeanFactory initialized for definition: {}", definition);
		log.atTrace().log("Exiting BeanFactory constructor");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		log.atTrace().log("Entering equals method comparing with object: {}", o);
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BeanFactory<Bean> that = (BeanFactory<Bean>) o;
		boolean result = Objects.equals(this.definition, that.definition);
		log.atTrace().log("Exiting equals method with result: {}", result);
		return result;
	}

	@Override
	public int hashCode() {
		log.atTrace().log("Calculating hashCode for definition: {}", definition);
		return this.definition.hashCode();
	}

	private Bean getBean() throws DiException {
		log.atTrace().log("Creating new bean instance for definition: {}", definition);
		Bean bean = createBeanInstance();
		log.atInfo().log("Bean instance created: {}", bean);
		return bean;
	}

	private void doInjection(Bean onBean) {
		log.atTrace().log("Performing field injection for bean: {}", onBean);
		this.definition.injectableFields()
				.forEach(builder -> builder.valueSupplier(new FixedSupplierBuilder<>(onBean)).build().setValue());
		log.atDebug().log("Field injection completed for bean: {}", onBean);
	}

	private Bean createBeanInstance() throws DiException {
		log.atTrace().log("Instantiating bean of type: {}", definition.reference().type());
		try {
			if (this.definition.constructorBinder().isPresent()) {
				Optional<Bean> constructed = executeConstructorBinder();
				return constructed.orElseThrow(() -> new DiException(
						"Constructor binder returned empty for bean of type "
								+ this.definition.reference().effectiveName()));
			} else {
				return ObjectReflectionHelper.instanciateNewObject(this.definition.reference().type());
			}
		} catch (Exception e) {
			log.atError().log("Failed to instantiate bean of type {}: {}", this.definition.reference().effectiveName(),
					e.getMessage());
			throw new DiException("Failed to instantiate bean of type " + this.definition.reference().effectiveName(),
					e);
		}
	}

	private Optional<Bean> executeConstructorBinder() throws DiException {
		log.atTrace().log("Executing constructor binder for definition: {}", definition);
		try {
			Optional<Bean> result = (Optional<Bean>) this.definition.constructorBinder().get().execute();
			log.atDebug().log("Constructor binder result: {}", result.orElse(null));
			return result;
		} catch (ReflectionException e) {
			log.atError().log("Constructor binder failed for definition {}: {}", definition, e.getMessage());
			throw new DiException(e);
		}
	}

	private void invokePostConstructMethods(Bean bean) throws DiException {
		log.atTrace().log("Invoking post construct methods for bean: {}", bean);
		if (this.definition.postConstructMethodBinderBuilders().isEmpty()) {
			log.atDebug().log("No post construct methods to invoke for bean: {}", bean);
			return;
		}

		for (IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder : this.definition
				.postConstructMethodBinderBuilders()) {
			try {
				IMethodBinder<Void> methodBinder = methodBinderBuilder.build(FixedSupplierBuilder.of(bean));
				methodBinder.execute();
				log.atDebug().log("Post construct method executed for bean: {}", bean);
			} catch (DslException | ReflectionException e) {
				log.atError().log("Post construct method binder failed for bean {}: {}",
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
		log.atTrace().log("Supplying bean for definition: {}", definition);
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
			log.atInfo().log("Bean supplied: {}", bean);
			return Optional.ofNullable(bean);
		} catch (DiException e) {
			log.atError().log("Failed to supply bean for definition {}: {}", definition, e.getMessage());
			throw new SupplyException(e);
		}
	}

	private Bean createAndInitializePrototype() {
		Bean bean;
		synchronized (this.beanMutex) {
			log.atDebug().log("Using prototype strategy for bean");
			bean = getBean();
			this.doInjection(bean);
			this.invokePostConstructMethods(bean);
		}
		return bean;
	}

	private Bean createAnInitializeSingleton(Bean bean) {
		synchronized (this.beanMutex) {
			if (this.bean == null) {
				log.atDebug().log("Creating singleton bean");
				this.bean = getBean();
			}
			if (!this.singletonBeanInitialized) {
				this.doInjection(this.bean);
				this.invokePostConstructMethods(this.bean);
				this.singletonBeanInitialized = true;
				log.atDebug().log("Singleton bean initialized flag set to true");
			}
			return this.bean;
		}
	}

	@Override
	public Type getSuppliedType() {
		log.atTrace().log("Returning supplied type: {}", definition.reference().type());
		return this.definition.reference().type();
	}

	@Override
	public boolean matches(BeanReference<?> query) {
		log.atTrace().log("Checking matches for definition: {} against example: {}", definition, query);
		boolean match = this.definition.reference().matches(query);
		log.atDebug().log("Match result: {}", match);
		return match;
	}

	@Override
	public BeanDefinition<Bean> definition() {
		log.atTrace().log("Returning bean definition: {}", definition);
		return this.definition;
	}

	@Override
	public Set<Class<?>> dependencies() {
		log.atTrace().log("Returning dependencies for definition: {}", definition);
		return this.definition.dependencies();
	}

	@Override
	public IReflectionConfigurationEntryBuilder nativeEntry() {
		log.atTrace().log("Building native configuration entry for definition: {}", definition);
		ReflectConfigEntryBuilder eb = new ReflectConfigEntryBuilder(definition.reference().type());

		// Constructor
		definition.constructorBinder().ifPresentOrElse(c -> {
			log.atDebug().log("Adding constructor binder to native entry for type: {}", definition.reference().type());
			eb.constructor(c.constructor());
		}, () -> {
			try {
				log.atDebug().log("Adding default constructor to native entry for type: {}",
						definition.reference().type());
				eb.constructor(definition.reference().type().getDeclaredConstructor());
			} catch (NoSuchMethodException | SecurityException e) {
				log.atWarn().log("Error adding default constructor for type {}: {}", definition.reference().type(),
						e.getMessage());
			}
		});

		// Fields
		log.atDebug().log("Adding {} injectable fields to native entry", definition.injectableFields().size());
		definition.injectableFields().forEach(f -> eb.field(f.field()));

		// Methods
		log.atDebug().log("Adding {} post construct methods to native entry",
				definition.postConstructMethodBinderBuilders().size());
		definition.postConstructMethodBinderBuilders().forEach(m -> eb.method(m.method()));

		log.atInfo().log("Native configuration entry built for type: {}", definition.reference().type());
		return eb;
	}
}