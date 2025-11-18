package com.garganttua.core.injection.context.beans;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanFactory;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supplying.SupplyException;
import com.garganttua.core.supplying.dsl.FixedObjectSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanFactory<Bean> implements IBeanFactory<Bean> {

	private volatile Bean bean;

	private BeanDefinition<Bean> definition;

	public BeanFactory(BeanDefinition<Bean> definition) {
		this.definition = Objects.requireNonNull(definition, "Bean definition cannot be null");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BeanFactory<Bean> that = (BeanFactory<Bean>) o;
		return Objects.equals(this.definition, that.definition);
	}

	@Override
	public int hashCode() {
		return this.definition.hashCode();
	}

	private Bean getBean() throws DiException {
		Bean bean = createBeanInstance();
		this.doInjection(bean);
		this.invokePostConstructMethods(bean);
		return bean;
	}

	private void doInjection(Bean onBean) {
		this.definition.injectableFields()
				.forEach(builder -> builder.setBean(new FixedObjectSupplierBuilder<>(onBean)).build().setValue());
	}

	private Bean createBeanInstance() throws DiException {
		try {
			if (this.definition.constructorBinder().isPresent()) {
				Optional<Bean> constructed = executeConstructorBinder();
				return constructed.orElseThrow(
						() -> new DiException("Constructor binder returned empty for bean of type "
								+ this.definition.effectiveName()));
			} else {
				return ObjectReflectionHelper.instanciateNewObject(this.definition.type());
			}
		} catch (Exception e) {
			throw new DiException("Failed to instantiate bean of type " + this.definition.effectiveName(), e);
		}
	}

	private Optional<Bean> executeConstructorBinder() throws DiException {
		try {
			return this.definition.constructorBinder().get().execute();
		} catch (ReflectionException e) {
			throw new DiException(e);
		}
	}

	private void invokePostConstructMethods(Bean bean) throws DiException {
		if (this.definition.postConstructMethodBinderBuilders().isEmpty()) {
			return;
		}

		for (IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder : this.definition
				.postConstructMethodBinderBuilders()) {
			try {
				IMethodBinder<Void> methodBinder = methodBinderBuilder
						.build(FixedObjectSupplierBuilder.of(bean));
				methodBinder.execute();
			} catch (DslException | ReflectionException e) {
				throw new DiException(
						"Post construct method binder failed for bean of type " + this.definition.effectiveName(), e);
			}
		}
	}

	@Override
	public Optional<Bean> supply() throws SupplyException {
		Bean bean = null;
		Optional<BeanStrategy> strat = this.definition.strategy();
		try {

			if (strat.isPresent()) {
				if (strat.get() == BeanStrategy.prototype) {
					bean = getBean();
				} else {
					synchronized (this) {
						if (this.bean == null) {
							this.bean = getBean();
						}
					}
					bean = this.bean;
				}
			} else {
				synchronized (this) {
					if (this.bean == null) {
						this.bean = getBean();
					}
				}
				bean = this.bean;
			}
		} catch (DiException e) {
			throw new SupplyException(e);
		}
		return Optional.ofNullable(bean);
	}

	@Override
	public Class<Bean> getSuppliedType() {
		return this.definition.type();
	}

	@Override
	public boolean matches(BeanDefinition<?> example) {
		return this.definition.matches(example);
	}

	@Override
	public BeanDefinition<Bean> getDefinition() {
		return this.definition;
	}

	@Override
	public Set<Class<?>> getDependencies() {
		return this.definition.getDependencies();
	}
}
