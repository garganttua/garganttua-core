package com.garganttua.injection.beans;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.injection.supplier.builder.supplier.FixedObjectSupplierBuilder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanFactory<Bean> implements IBeanFactory<Bean> {

	private Bean bean;

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

		// inject beans
		// inject values

		this.invokePostConstructMethods(bean);
		return bean;
	}

	private Bean createBeanInstance() throws DiException {
		try {
			if (this.definition.constructorBinder().isPresent()) {
				Optional<Bean> constructed = executeConstructorBinder();
				return constructed.orElseThrow(
						() -> new DiException("Constructor binder returned empty for bean of type "
								+ this.definition.effectiveName()));
			} else {
				return GGObjectReflectionHelper.instanciateNewObject(this.definition.type());
			}
		} catch (Exception e) {
			throw new DiException("Failed to instantiate bean of type " + this.definition.effectiveName(), e);
		}
	}

	private Optional<Bean> executeConstructorBinder() throws DiException {
		return this.definition.constructorBinder().get().execute();
	}

	@SuppressWarnings("unchecked")
	private void invokePostConstructMethods(Bean bean) throws DiException {
		if (this.definition.postConstructMethodBinderBuilders().isEmpty()) {
			return;
		}

		for (IBeanPostConstructMethodBinderBuilder<Bean> methodBinderBuilder : this.definition
				.postConstructMethodBinderBuilders()) {
			try {
				IMethodBinder<Void> methodBinder = methodBinderBuilder
						.build((FixedObjectSupplierBuilder<Bean>) FixedObjectSupplierBuilder.of(bean));
				methodBinder.execute();
			} catch (DiException | DslException e) {
				throw new DiException(
						"Post construct method binder failed for bean of type " + this.definition.effectiveName(), e);
			}
		}
	}

	@Override
	public Optional<Bean> getObject() throws DiException {
		Bean bean = null;
		Optional<BeanStrategy> strat = this.definition.strategy();

		if (strat.isPresent()) {
			if (strat.get() == BeanStrategy.prototype) {
				bean = getBean();
			} else {
				if (this.bean == null) {
					this.bean = getBean();
				}
				bean = this.bean;
			}
		} else {
			if (this.bean == null) {
				this.bean = getBean();
			}
			bean = this.bean;
		}
		return Optional.ofNullable(bean);
	}

	@Override
	public Class<Bean> getObjectClass() {
		return this.definition.type();
	}

	@Override
	public boolean matches(BeanDefinition<?> definition) {
		return this.definition.matches(definition);
	}
}
