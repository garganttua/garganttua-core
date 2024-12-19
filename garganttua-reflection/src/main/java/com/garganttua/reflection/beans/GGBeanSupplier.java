package com.garganttua.reflection.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.properties.IGGPropertyLoader;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.Setter;

public class GGBeanSupplier implements IGGBeanSupplier {

	private static final String BEAN_SUPPLIER = "gg";

	private List<GGBeanFactory> beans = Collections.synchronizedList(new ArrayList<GGBeanFactory>());

	@Setter
	private IGGBeanLoader beanLoader;

	@FunctionalInterface
	protected interface IGGBeanLoaderAccessor {
		IGGBeanLoader getBeanLoader();
	}

	public GGBeanSupplier(Collection<String> packages, IGGPropertyLoader propLoader) {
		packages.parallelStream().forEach(package_ -> {
			List<Class<?>> annotatedClasses = GGObjectReflectionHelper.getClassesWithAnnotation(package_, GGBean.class);
			annotatedClasses.forEach(annotatedClass -> {
				GGBeanFactory beanFactory = new GGBeanFactory(annotatedClass.getAnnotation(GGBean.class),
						annotatedClass, () -> {
							return this.beanLoader;
						}, propLoader);
				this.beans.add(beanFactory);
			});
		});
	}

	@Override
	public String getName() {
		return BEAN_SUPPLIER;
	}

	@Override
	public Object getBeanNamed(String name) throws GGReflectionException {
		Optional<GGBeanFactory> beanFactory = beans.parallelStream().filter(bf -> bf.getName().equals(name))
				.findFirst();
		if (beanFactory.isEmpty()) {
			throw new GGReflectionException("Bean " + name + " not found");
		}
		return beanFactory.get().getBean();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBeanOfType(Class<T> type) throws GGReflectionException {
		Optional<GGBeanFactory> beanFactory = beans.parallelStream().filter(bf -> bf.getType().equals(type))
				.findFirst();
		if (beanFactory.isEmpty()) {
			throw new GGReflectionException("Bean " + type + " not found");
		}
		return (T) beanFactory.get().getBean();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getBeansImplementingInterface(Class<T> type) throws GGReflectionException {
		List<T> l = (List<T>) beans.parallelStream()
				.filter(bf -> GGObjectReflectionHelper.isImplementingInterface(type, bf.getType()))
				.collect(Collectors.toList());
		return l;
	}
}
