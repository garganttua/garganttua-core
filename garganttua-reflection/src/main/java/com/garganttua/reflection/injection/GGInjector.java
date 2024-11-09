package com.garganttua.reflection.injection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

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
	    this.injectBeans(entity, clazz);
	}

	private void injectBeans(Object entity, Class<?> clazz) throws GGReflectionException {
		for (Field field : clazz.getDeclaredFields()) {
	        if (field.isAnnotationPresent(Inject.class)) {
	            Object bean = this.getBean(entity, field);
	    		this.doInjection(entity, field, bean);
	        }
	    }
		
		if( clazz.getSuperclass() != null ) {
			this.injectBeans(entity, clazz.getSuperclass());
		}
	}

	private void doInjection(Object entity, Field field, Object bean) throws GGReflectionException {
		try (GGFieldAccessManager accessManager = new GGFieldAccessManager(field)) {
			field.set(entity, bean);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			if( log.isDebugEnabled() ) {
				log.warn("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
			}
			throw new GGReflectionException("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
		}
	}

	@SuppressWarnings("unchecked")
	private Object getBean(Object entity, Field field) throws GGReflectionException {
		Object bean = null;
			
		if( Optional.class.isAssignableFrom(field.getType()) ) {
			Class<?> optionalClass = this.getOptionalFieldType(field.getGenericType());
			try {
				Object optionalBean = this.researchBean(field, optionalClass);
				return Optional.ofNullable(optionalBean);
			} catch (Exception e) {
				return Optional.empty();
			}

		} else {
			bean = this.researchBean(field, field.getType());			
		}
		
		if (bean == null) {
		    throw new GGReflectionException("Bean not found for field: " + field.getName());
		}

		return bean;
	}
	
	private Class<?> getOptionalFieldType(Type type) throws GGReflectionException {
        Type genericType = type;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            
            Type[] typeArguments = paramType.getActualTypeArguments();
            return (Class<?>) typeArguments[0];
        } 
        
        throw new GGReflectionException("Invalid clazz "+type.getTypeName()+" should be Optional<?>");
    }

	private Object researchBean(Field field, Class<?> fieldType) throws GGReflectionException {
		Object bean;
		if (field.isAnnotationPresent(Qualifier.class)) {
		    String qualifierName = field.getAnnotation(Qualifier.class).toString();
		    bean = this.beanLoader.getBeanNamed(qualifierName);
		} else {
		    bean = this.beanLoader.getBeanOfType(fieldType);
		}
		if (bean == null) {
		    throw new GGReflectionException("Bean not found for field: " + field.getName());
		}
		return bean;
	}

	@Override
	public void injectProperties(Object entity) throws GGReflectionException {
		// TODO Auto-generated method stub
		
	}

}
