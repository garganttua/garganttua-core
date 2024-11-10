package com.garganttua.reflection.beans;

import java.lang.reflect.Field;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.beans.GGBeanSupplier.IGGBeanLoaderAccessor;
import com.garganttua.reflection.beans.annotation.GGBean;
import com.garganttua.reflection.beans.annotation.GGBeanLoadingStrategy;
import com.garganttua.reflection.properties.GGProperty;
import com.garganttua.reflection.properties.IGGPropertyLoader;
import com.garganttua.reflection.utils.GGFieldAccessManager;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGBeanFactory {

	private IGGPropertyLoader propLoader;

	private IGGBeanLoaderAccessor beanLoaderAccessor;

	public GGBeanFactory(GGBean annotation, Class<?> type, IGGBeanLoaderAccessor beanLoaderAccessor, IGGPropertyLoader propLoader) {
		this.beanLoaderAccessor = beanLoaderAccessor;
		this.propLoader = propLoader;
		this.name = annotation.name().isEmpty() ? type.getSimpleName() : annotation.name() ;
		this.strategy = annotation.strategy();
		this.type = type;
	}

	@Getter
	private Class<?> type;
	
	@Getter
	private String name;
	
	@Getter
	private GGBeanLoadingStrategy strategy;

	private Object bean;
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GGBeanFactory that = (GGBeanFactory) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(name, that.name) &&
                strategy == that.strategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, strategy);
    }

	public Object getBean() throws GGReflectionException {
		if( this.strategy == GGBeanLoadingStrategy.newInstance ) {
			return this.injectDependenciesAndValues(GGObjectReflectionHelper.instanciateNewObject(this.type));
		} else {
			if( this.bean == null ) {
				this.bean = this.injectDependenciesAndValues(GGObjectReflectionHelper.instanciateNewObject(this.type));
			}
			return this.bean;
		}
	}
	
	private Object injectDependenciesAndValues(Object object) throws GGReflectionException {
        Class<?> clazz = object.getClass();
        return this.injectDependenciesAndValues(object, clazz);
    }

	private Object injectDependenciesAndValues(Object object, Class<?> clazz) throws GGReflectionException {
		if( clazz.getSuperclass() != null ) {
			object = this.injectDependenciesAndValues(object, clazz.getSuperclass());
		}
		
		for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Object bean;
                if (field.isAnnotationPresent(Qualifier.class)) {
                    String qualifierName = field.getAnnotation(Qualifier.class).toString();
                    bean = this.beanLoaderAccessor.getBeanLoader().getBeanNamed(qualifierName);
                } else {
                    bean = this.beanLoaderAccessor.getBeanLoader().getBeanOfType(field.getType());
                }
                if (bean == null) {
                    throw new GGReflectionException("Bean not found for field: " + field.getName());
                }

                try (GGFieldAccessManager accessManager = new GGFieldAccessManager(field)) {
					field.set(object, bean);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					if( log.isDebugEnabled() ) {
						log.warn("Field  "+field.getName()+" of object of type "+object.getClass().getName()+" cannot be set", e);
					}
					throw new GGReflectionException("Field  "+field.getName()+" of entity of type "+object.getClass().getName()+" cannot be set", e);
				}
            } else if (field.isAnnotationPresent(GGProperty.class)) {
                String value = field.getAnnotation(GGProperty.class).value();
                if (value.startsWith("${") && value.endsWith("}")) {
                    String propertyName = value.substring(2, value.length() - 1);
                    String propertyValue = this.propLoader.getProperty(propertyName);
                    if( propertyValue == null ) {
                    	throw new GGReflectionException("Value not found: " + propertyName);
                    }
                    try (GGFieldAccessManager accessManager = new GGFieldAccessManager(field)) {
    					field.set(object, propertyValue);
    				} catch (IllegalArgumentException | IllegalAccessException e) {
    					if( log.isDebugEnabled() ) {
    						log.warn("Field  "+field.getName()+" of entity of type "+object.getClass().getName()+" cannot be set", e);
    					}
    					throw new GGReflectionException("Field  "+field.getName()+" of entity of type "+object.getClass().getName()+" cannot be set", e);
    				}
                } else {
                	 throw new GGReflectionException("Malformed value annotation: " + field.getName());
                }
            }   
        }
        return object;
	}
}
