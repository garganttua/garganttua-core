package com.garganttua.reflection.injection;

import com.garganttua.reflection.GGReflectionException;

public interface IGGInjector {

	void injectBeans(Object entity) throws GGReflectionException;

	void injectProperties(Object entity) throws GGReflectionException;

//    Class<?> clazz = entity.getClass();
//    for (Field field : clazz.getDeclaredFields()) {
//        if (field.isAnnotationPresent(Inject.class)) {
//            Object bean;
//            if (field.isAnnotationPresent(Qualifier.class)) {
//                String qualifierName = field.getAnnotation(Qualifier.class).toString();
//                bean = this.beanLoader.getBeanNamed(qualifierName);
//            } else {
//                bean = this.beanLoader.getBeanOfType(field.getType());
//            }
//            if (bean == null) {
//                throw new GGAPIEngineException(GGAPIExceptionCode.INJECTION_ERROR, "Bean not found for field: " + field.getName());
//            }
//
//            try (GGAPIFieldAccessManager accessManager = new GGAPIFieldAccessManager(field)) {
//				field.set(entity, bean);
//			} catch (IllegalArgumentException | IllegalAccessException e) {
//				if( log.isDebugEnabled() ) {
//					log.warn("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
//				}
//				throw new GGAPIEngineException(GGAPIExceptionCode.INJECTION_ERROR, "Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
//			}
//        } else if (field.isAnnotationPresent(GGAPIProperty.class)) {
//            String value = field.getAnnotation(GGAPIProperty.class).value();
//            if (value.startsWith("${") && value.endsWith("}")) {
//                String propertyName = value.substring(2, value.length() - 1);
//                String propertyValue = this.propertyLoader.getProperty(propertyName);
//                if( propertyValue == null ) {
//                	throw new GGAPIEngineException(GGAPIExceptionCode.INJECTION_ERROR, "Value not found: " + propertyName);
//                }
//                try (GGAPIFieldAccessManager accessManager = new GGAPIFieldAccessManager(field)) {
//					field.set(entity, propertyValue);
//				} catch (IllegalArgumentException | IllegalAccessException e) {
//					if( log.isDebugEnabled() ) {
//						log.warn("Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
//					}
//					throw new GGAPIEngineException(GGAPIExceptionCode.INJECTION_ERROR, "Field  "+field.getName()+" of entity of type "+entity.getClass().getName()+" cannot be set", e);
//				}
//            } else {
//            	 throw new GGAPIEngineException(GGAPIExceptionCode.INJECTION_ERROR, "Malformed value annotation: " + field.getName());
//            }
//        }
//    }

}
