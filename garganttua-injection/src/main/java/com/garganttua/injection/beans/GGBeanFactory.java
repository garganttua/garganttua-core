package com.garganttua.injection.beans;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.IBeanSupplier;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.beans.annotation.GGBeanLoadingStrategy;
import com.garganttua.injection.spec.injection.IInjector;
import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;
import com.garganttua.injection.spec.supplier.binder.IMethodBinder;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGBeanFactory<Bean> implements IBeanSupplier<Bean> {

	private IConstructorBinder<Bean> constructorBinder;

	private Set<IMethodBinder<?>> postConstructMethodBinders;

	private IInjector injector;

	@Getter
	private Class<Bean> type;

	@Getter
	private String name;

	@Getter
	private GGBeanLoadingStrategy strategy;

	private Bean bean;

	private IDiContext context;

	public GGBeanFactory(Class<Bean> type, GGBeanLoadingStrategy strategy, Optional<String> name,
			Optional<IConstructorBinder<Bean>> constructorBinder, Set<IMethodBinder<?>> postConstructMethodBinders,
			IInjector injector, IDiContext context) {
		this.context = context;
		this.type = Objects.requireNonNull(type, "Bean type cannot be null");
		this.strategy = Objects.requireNonNull(strategy, "Bean strategy cannot be null");
		this.name = name.orElse(type.getSimpleName());
		this.constructorBinder = constructorBinder.isPresent() ? constructorBinder.get() : null;
		this.postConstructMethodBinders = postConstructMethodBinders;
		this.injector = Objects.requireNonNull(injector, "Injector cannot be null");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		GGBeanFactory<Bean> that = (GGBeanFactory<Bean>) o;
		return Objects.equals(type, that.type) &&
				Objects.equals(name, that.name) &&
				strategy == that.strategy;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name, strategy);
	}

	private Bean getBean() throws DiException {
		Bean bean = createBeanInstance();
		invokePostConstructMethods(bean);
		performInjection(bean);
		return bean;
	}

	private Bean createBeanInstance() throws DiException {
		try {
			if (constructorBinder != null) {
				Optional<Bean> constructed = executeConstructorBinder();
				return constructed.orElseThrow(
						() -> new DiException("Constructor binder returned empty for bean of type " + type.getName()));
			} else {
				return GGObjectReflectionHelper.instanciateNewObject(type);
			}
		} catch (Exception e) {
			throw new DiException("Failed to instantiate bean of type " + type.getName(), e);
		}
	}

	private Optional<Bean> executeConstructorBinder() throws DiException {
		return (context == null)
				? constructorBinder.execute()
				: constructorBinder.execute(context);
	}

	private void invokePostConstructMethods(Bean bean) throws DiException {
		if (postConstructMethodBinders == null || postConstructMethodBinders.isEmpty()) {
			return;
		}

		for (IMethodBinder<?> methodBinder : postConstructMethodBinders) {
			try {
				if (context == null) {
					methodBinder.execute();
				} else {
					methodBinder.execute(context);
				}
			} catch (DiException e) {
				throw new DiException("Post construct method binder failed for bean of type " + type.getName(), e);
			}
		}
	}

	private void performInjection(Bean bean) throws DiException {
		try {
			injector.doInjection(bean);
		} catch (Exception e) {
			throw new DiException("Dependency injection failed for bean of type " + type.getName(), e);
		}
	}

	@Override
	public Optional<Bean> getObject() throws DiException {
		Bean bean = null;
		if (this.strategy == GGBeanLoadingStrategy.newInstance) {
			bean = getBean();
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
		return this.type;
	}
}
