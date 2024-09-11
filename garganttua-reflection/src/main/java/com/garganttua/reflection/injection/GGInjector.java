package com.garganttua.reflection.injection;

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.beans.IGGBeanLoader;
import com.garganttua.reflection.utils.GGFieldAccessManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGInjector implements IGGInjector {
	
	private IGGBeanLoader beanLoader;

	private GGInjector(IGGBeanLoader beanLoader) {
		this.beanLoader = beanLoader;
	}

	public static IGGInjector injector(IGGBeanLoader beanLoader) {
		return new GGInjector(beanLoader);
	}

	@Override
	public void injectBeans(Object entity) throws GGReflectionException {
	    Class<?> clazz = entity.getClass();
	    for (Field field : clazz.getDeclaredFields()) {
	        if (field.isAnnotationPresent(Inject.class)) {
	            Object bean;
	            if (field.isAnnotationPresent(Qualifier.class)) {
	                String qualifierName = field.getAnnotation(Qualifier.class).toString();
	                bean = this.beanLoader.getBeanNamed(qualifierName);
	            } else {
	                bean = this.beanLoader.getBeanOfType(field.getType());
	            }
	            if (bean == null) {
	                throw new GGReflectionException("Bean not found for field: " + field.getName());
	            }
	
	            try (GGFieldAccessManager accessManager = new GGFieldAccessManager(field)) {
					field.set(entity, bean);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					if( log.isDebugEnabled() ) {
						log.warn("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
					}
					throw new GGReflectionException("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
				}
	        }
	    }
	}

	@Override
	public void injectProperties(Object entity) throws GGReflectionException {
		// TODO Auto-generated method stub
		
	}

}
