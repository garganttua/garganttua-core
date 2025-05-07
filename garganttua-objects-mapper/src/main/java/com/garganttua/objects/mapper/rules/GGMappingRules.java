package com.garganttua.objects.mapper.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.objects.mapper.GGMapper;
import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.objects.mapper.GGMappingDirection;
import com.garganttua.objects.mapper.IGGMappingRuleExecutor;
import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;
import com.garganttua.objects.mapper.annotations.GGObjectMappingRule;
import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.fields.GGFields;
import com.garganttua.reflection.query.GGObjectQueryFactory;
import com.garganttua.reflection.query.IGGObjectQuery;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGMappingRules {

	public static List<GGMappingRule> parse(Class<?> destinationClass) throws GGMapperException {
		List<GGMappingRule> mappingRules = new ArrayList<GGMappingRule>();
		return GGMappingRules.recursiveParsing(destinationClass, mappingRules, "");
	}

	private static List<GGMappingRule> recursiveParsing(Class<?> destinationClass, List<GGMappingRule> mappingRules, String fieldAddress) throws GGMapperException {
		try {
			if( log.isDebugEnabled() ) {
				log.debug("Looking for mapping rules in "+destinationClass+ " address "+fieldAddress);
			}
			boolean objectMapping = false;
			if( destinationClass.isAnnotationPresent(GGObjectMappingRule.class) ) {
				GGObjectMappingRule annotation = destinationClass.getDeclaredAnnotation(GGObjectMappingRule.class);
				GGObjectAddress fromSourceMethod;
					fromSourceMethod = new GGObjectAddress(annotation.fromSourceMethod());
				GGObjectAddress toSourceMethod = new GGObjectAddress(annotation.toSourceMethod());
				mappingRules.add(new GGMappingRule(null, null, destinationClass, fromSourceMethod, toSourceMethod));
				objectMapping = true;
			}
			
			for( Field field: destinationClass.getDeclaredFields() ) {
				if( field.isAnnotationPresent(GGFieldMappingRule.class) && !objectMapping){
					GGFieldMappingRule annotation = field.getDeclaredAnnotation(GGFieldMappingRule.class);
					
					GGObjectAddress fromSourceMethod = null;
					if( !annotation.fromSourceMethod().isEmpty() ) {
						fromSourceMethod = new GGObjectAddress(annotation.fromSourceMethod());
					}
					GGObjectAddress toSourceMethod = null;
					if( !annotation.toSourceMethod().isEmpty() ) {
						toSourceMethod = new GGObjectAddress(annotation.toSourceMethod());
					}
					
					GGObjectAddress sourceFieldAddress = new GGObjectAddress(annotation.sourceFieldAddress());
					GGObjectAddress destFieldAddress = new GGObjectAddress(fieldAddress+field.getName());
					
					mappingRules.add(new GGMappingRule(sourceFieldAddress, destFieldAddress, destinationClass, fromSourceMethod, toSourceMethod));
				} else {
					if( GGFields.isNotPrimitive(field.getType()) && 
							!Collection.class.isAssignableFrom(field.getType()) &&
							!Map.class.isAssignableFrom(field.getType()) &&
							field.getType().isArray()){
						GGMappingRules.recursiveParsing(field.getType(), mappingRules, fieldAddress+field.getName()+GGObjectAddress.ELEMENT_SEPARATOR);
					} else if ( Collection.class.isAssignableFrom(field.getType()) ) {
						GGMappingRules.recursiveParsing(GGFields.getGenericType(field, 0), mappingRules, fieldAddress+field.getName()+GGObjectAddress.ELEMENT_SEPARATOR);
			        } else if ( Map.class.isAssignableFrom(field.getType()) ) {
			        	GGMappingRules.recursiveParsing(GGFields.getGenericType(field, 0), mappingRules, fieldAddress+field.getName()+GGObjectAddress.ELEMENT_SEPARATOR+GGObjectAddress.MAP_KEY_INDICATOR+GGObjectAddress.ELEMENT_SEPARATOR);
			        	GGMappingRules.recursiveParsing(GGFields.getGenericType(field, 1), mappingRules, fieldAddress+field.getName()+GGObjectAddress.ELEMENT_SEPARATOR+GGObjectAddress.MAP_VALUE_INDICATOR+GGObjectAddress.ELEMENT_SEPARATOR);	        	
			        }
				}
			}
			if( destinationClass.getSuperclass() != null ) {
				GGMappingRules.recursiveParsing(destinationClass.getSuperclass(), mappingRules, fieldAddress);
			}
			return mappingRules;
		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
	}
	
	public static void validate(Class<?> sourceClass, List<GGMappingRule> rules) throws GGMapperException {
		IGGObjectQuery sourceQuery;
		try {
			sourceQuery = GGObjectQueryFactory.objectQuery(sourceClass);
			for( GGMappingRule rule: rules ) {
				GGMappingRules.validate(sourceQuery, rule);
			}
		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
	}

	private static void validate(IGGObjectQuery sourceQuery, GGMappingRule rule)
			throws GGMapperException {
		if( log.isDebugEnabled() ) {
			log.debug("Validating mapping rule "+rule.toString()+" for source query "+sourceQuery.toString());
		}
		try {
			IGGObjectQuery destQuery = GGObjectQueryFactory.objectQuery(rule.destinationClass());
			
			List<Object> sourceField_ = sourceQuery.find(rule.sourceFieldAddress());
			List<Object> destField_ = destQuery.find(rule.destinationFieldAddress());
			
			Field sourceField = (Field) sourceField_.get(sourceField_.size()-1);
			Field destField = (Field) destField_.get(destField_.size()-1);
			
			if( rule.fromSourceMethodAddress() != null ) {
				List<Object> fromMethod_ = destQuery.find(rule.fromSourceMethodAddress());
				Method fromMethod = (Method) fromMethod_.get(fromMethod_.size()-1);
				
				GGMappingRules.validateMethod(rule, sourceField, destField, fromMethod);
			}
			if( rule.toSourceMethodAddress() != null ) {
				List<Object> toMethod_ = destQuery.find(rule.toSourceMethodAddress());
				Method toMethod = (Method) toMethod_.get(toMethod_.size()-1);
				
				GGMappingRules.validateMethod(rule, destField, sourceField, toMethod);
			}
		} catch (GGReflectionException e) {
			if( log.isDebugEnabled() ) {
				log.warn("Error : ", e);
			}
			throw new GGMapperException(e);
		}
	}

	private static void validateMethod(GGMappingRule rule, Field sourceField, Field destField, Method method) throws GGMapperException {
		if( method.getParameterTypes().length != 1 ) {
			throw new GGMapperException("Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : must have exactly one parameter");
		}
		
		Class<?> paramType = method.getParameterTypes()[0];
		Class<?> returnType = method.getReturnType();
		
		if( !paramType.equals(sourceField.getType()) ) {
			throw new GGMapperException("Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : parameter must be of type "+sourceField.getType());
		}
		
		if( !returnType.equals(destField.getType()) ) {
			throw new GGMapperException("Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : return type must be "+destField.getType());
		}
	}

	public static IGGMappingRuleExecutor getRuleExecutor(GGMapper mapper, GGMappingDirection mappingDirection, GGMappingRule rule, Object source, Class<?> destinationClass) throws GGMapperException {
		List<Object> destinationField = null;
		List<Object> sourceField = null;
		List<Object> mappingMethod = null;
		
		try {
			if( mappingDirection == GGMappingDirection.REGULAR ) {
				if( rule.fromSourceMethodAddress() != null ) {
					mappingMethod = GGObjectQueryFactory.objectQuery(destinationClass).find(rule.fromSourceMethodAddress());
				}
				sourceField = GGObjectQueryFactory.objectQuery(source.getClass()).find(rule.sourceFieldAddress());
				destinationField = GGObjectQueryFactory.objectQuery(destinationClass).find(rule.destinationFieldAddress());
			} else {
				if( rule.toSourceMethodAddress() != null ) {
					mappingMethod = GGObjectQueryFactory.objectQuery(source.getClass()).find(rule.toSourceMethodAddress());
				}
				sourceField = GGObjectQueryFactory.objectQuery(source.getClass()).find(rule.destinationFieldAddress());
				destinationField = GGObjectQueryFactory.objectQuery(destinationClass).find(rule.sourceFieldAddress());
			}
			
			Field sourceFieldLeaf = (Field) sourceField.get(sourceField.size()-1);
			Field destinationFieldLeaf = (Field) destinationField.get(destinationField.size()-1);
			
			if( mappingMethod != null ) {
				Method methodLeaf = (Method) mappingMethod.get(mappingMethod.size()-1);
				return new GGAPIMethodMappingExecutor(methodLeaf, sourceFieldLeaf, destinationFieldLeaf, mappingDirection);
			} else if(Collection.class.isAssignableFrom(sourceFieldLeaf.getType()) && Collection.class.isAssignableFrom(destinationFieldLeaf.getType())) {
				Class<?> sourceGenericeType = GGFields.getGenericType(sourceFieldLeaf, 0);
				Class<?> destGenericeType = GGFields.getGenericType(destinationFieldLeaf, 0);
				
				if( GGObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType()) && sourceGenericeType.equals(destGenericeType) ) {
					return new GGAPISimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
				} else if( !GGObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType()) && sourceGenericeType.equals(destGenericeType) ) {
					return new GGAPISimpleCollectionMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
				} else {
					return new GGAPIMapableCollectionMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
				}			
			} else if( GGObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType()) ) {
				return new GGAPISimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
			} else if( !GGFields.isArrayOrMapOrCollectionField(sourceFieldLeaf) && !GGFields.isArrayOrMapOrCollectionField(destinationFieldLeaf) ) {
				return new GGAPISimpleMapableFieldMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
			}

		} catch (GGReflectionException e) {
			throw new GGMapperException(e);
		}
		return null;
	}
}
