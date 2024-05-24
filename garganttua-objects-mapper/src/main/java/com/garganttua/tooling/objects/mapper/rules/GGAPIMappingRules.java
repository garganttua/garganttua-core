package com.garganttua.tooling.objects.mapper.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.api.core.mapper.annotations.GGAPIFieldMappingRule;
import com.garganttua.api.core.mapper.annotations.GGAPIObjectMappingRule;
import com.garganttua.api.spec.GGAPICoreExceptionCode;
import com.garganttua.reflection.GGAPIObjectAddress;
import com.garganttua.reflection.GGAPIObjectAddressException;
import com.garganttua.reflection.fields.GGFields;
import com.garganttua.reflection.query.GGAPIObjectQueryException;
import com.garganttua.reflection.query.GGObjectQueryFactory;
import com.garganttua.reflection.query.IGGObjectQuery;
import com.garganttua.tooling.objects.mapper.GGAPIMapperException;
import com.garganttua.tooling.objects.mapper.GGAPIMappingDirection;
import com.garganttua.tooling.objects.mapper.IGGAPIMappingRuleExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGAPIMappingRules {

	public static List<GGAPIMappingRule> parse(Class<?> destinationClass) throws GGAPIMappingRuleException {
		List<GGAPIMappingRule> mappingRules = new ArrayList<GGAPIMappingRule>();
		return GGAPIMappingRules.recursiveParsing(destinationClass, mappingRules, "");
	}

	private static List<GGAPIMappingRule> recursiveParsing(Class<?> destinationClass, List<GGAPIMappingRule> mappingRules, String fieldAddress) throws GGAPIMappingRuleException {
		try {
			if( log.isDebugEnabled() ) {
				log.debug("Looking for mapping rules in "+destinationClass+ " address "+fieldAddress);
			}
			boolean objectMapping = false;
			if( destinationClass.isAnnotationPresent(GGAPIObjectMappingRule.class) ) {
				GGAPIObjectMappingRule annotation = destinationClass.getDeclaredAnnotation(GGAPIObjectMappingRule.class);
				GGAPIObjectAddress fromSourceMethod;
					fromSourceMethod = new GGAPIObjectAddress(annotation.fromSourceMethod());
				GGAPIObjectAddress toSourceMethod = new GGAPIObjectAddress(annotation.toSourceMethod());
				mappingRules.add(new GGAPIMappingRule(null, null, destinationClass, fromSourceMethod, toSourceMethod));
				objectMapping = true;
			}
			
			for( Field field: destinationClass.getDeclaredFields() ) {
				if( field.isAnnotationPresent(GGAPIFieldMappingRule.class) && !objectMapping){
					GGAPIFieldMappingRule annotation = field.getDeclaredAnnotation(GGAPIFieldMappingRule.class);
					
					GGAPIObjectAddress fromSourceMethod = null;
					if( !annotation.fromSourceMethod().isEmpty() ) {
						fromSourceMethod = new GGAPIObjectAddress(annotation.fromSourceMethod());
					}
					GGAPIObjectAddress toSourceMethod = null;
					if( !annotation.toSourceMethod().isEmpty() ) {
						toSourceMethod = new GGAPIObjectAddress(annotation.toSourceMethod());
					}
					
					GGAPIObjectAddress sourceFieldAddress = new GGAPIObjectAddress(annotation.sourceFieldAddress());
					GGAPIObjectAddress destFieldAddress = new GGAPIObjectAddress(fieldAddress+field.getName());
					
					mappingRules.add(new GGAPIMappingRule(sourceFieldAddress, destFieldAddress, destinationClass, fromSourceMethod, toSourceMethod));
				} else {
					if( GGFields.isNotPrimitive(field.getType()) && 
							!Collection.class.isAssignableFrom(field.getType()) &&
							!Map.class.isAssignableFrom(field.getType()) &&
							field.getType().isArray()){
						GGAPIMappingRules.recursiveParsing(field.getType(), mappingRules, fieldAddress+field.getName()+GGAPIObjectAddress.ELEMENT_SEPARATOR);
					} else if ( Collection.class.isAssignableFrom(field.getType()) ) {
						GGAPIMappingRules.recursiveParsing(GGFields.getGenericType(field, 0), mappingRules, fieldAddress+field.getName()+GGAPIObjectAddress.ELEMENT_SEPARATOR);
			        } else if ( Map.class.isAssignableFrom(field.getType()) ) {
			        	GGAPIMappingRules.recursiveParsing(GGFields.getGenericType(field, 0), mappingRules, fieldAddress+field.getName()+GGAPIObjectAddress.ELEMENT_SEPARATOR+GGAPIObjectAddress.MAP_KEY_INDICATOR+GGAPIObjectAddress.ELEMENT_SEPARATOR);
			        	GGAPIMappingRules.recursiveParsing(GGFields.getGenericType(field, 1), mappingRules, fieldAddress+field.getName()+GGAPIObjectAddress.ELEMENT_SEPARATOR+GGAPIObjectAddress.MAP_VALUE_INDICATOR+GGAPIObjectAddress.ELEMENT_SEPARATOR);	        	
			        }
				}
			}
			if( destinationClass.getSuperclass() != null ) {
				GGAPIMappingRules.recursiveParsing(destinationClass.getSuperclass(), mappingRules, fieldAddress);
			}
			return mappingRules;
		} catch (GGAPIObjectAddressException e) {
			throw new GGAPIMappingRuleException(e);
		}
	}
	
	public static void validate(Class<?> sourceClass, List<GGAPIMappingRule> rules) throws GGAPIMappingRuleException {
		try {
			IGGObjectQuery sourceQuery = GGObjectQueryFactory.objectQuery(sourceClass);
				
			for( GGAPIMappingRule rule: rules ) {
				GGAPIMappingRules.validate(sourceQuery, rule);
			}
		} catch (GGAPIObjectQueryException e) {
			throw new GGAPIMappingRuleException(e);
		}
	}

	private static void validate(IGGObjectQuery sourceQuery, GGAPIMappingRule rule)
			throws GGAPIObjectQueryException, GGAPIMappingRuleException {
		IGGObjectQuery destQuery = GGObjectQueryFactory.objectQuery(rule.destinationClass());
		
		List<Object> sourceField_ = sourceQuery.find(rule.sourceFieldAddress());
		List<Object> destField_ = destQuery.find(rule.sourceFieldAddress());
		
		Field sourceField = (Field) sourceField_.get(sourceField_.size()-1);
		Field destField = (Field) destField_.get(destField_.size()-1);
		
		if( rule.fromSourceMethodAddress() != null ) {
			List<Object> fromMethod_ = destQuery.find(rule.fromSourceMethodAddress());
			Method fromMethod = (Method) fromMethod_.get(fromMethod_.size()-1);
			
			GGAPIMappingRules.validateMethod(rule, sourceField, destField, fromMethod);
		}
		if( rule.toSourceMethodAddress() != null ) {
			List<Object> toMethod_ = destQuery.find(rule.toSourceMethodAddress());
			Method toMethod = (Method) toMethod_.get(toMethod_.size()-1);
			
			GGAPIMappingRules.validateMethod(rule, destField, sourceField, toMethod);
		}
	}

	private static void validateMethod(GGAPIMappingRule rule, Field sourceField, Field destField, Method method) throws GGAPIMappingRuleException {
		if( method.getParameterTypes().length != 1 ) {
			throw new GGAPIMappingRuleException(GGAPICoreExceptionCode.UNKNOWN_ERROR, "Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : must have exactly one parameter");
		}
		
		Class<?> paramType = method.getParameterTypes()[0];
		Class<?> returnType = method.getReturnType();
		
		if( !paramType.equals(sourceField.getType()) ) {
			throw new GGAPIMappingRuleException(GGAPICoreExceptionCode.UNKNOWN_ERROR, "Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : parameter must be of type "+sourceField.getType());
		}
		
		if( !returnType.equals(destField.getType()) ) {
			throw new GGAPIMappingRuleException(GGAPICoreExceptionCode.UNKNOWN_ERROR, "Invalid method "+method.getName()+" of class "+rule.destinationClass().getSimpleName()+" : return type must be "+destField.getType());
		}
	}

	public static IGGAPIMappingRuleExecutor getRuleExecutor(GGAPIMappingDirection mappingDirection, GGAPIMappingRule rule, Object source, Class<?> destinationClass) throws GGAPIMapperException {
		List<Object> destinationField = null;
		List<Object> sourceField = null;
		
		try {
			if( mappingDirection == GGAPIMappingDirection.REGULAR ) {
				sourceField = GGObjectQueryFactory.objectQuery(source.getClass()).find(rule.sourceFieldAddress());
				destinationField = GGObjectQueryFactory.objectQuery(destinationClass).find(rule.destinationFieldAddress());
			} else {
				sourceField = GGObjectQueryFactory.objectQuery(source.getClass()).find(rule.destinationFieldAddress());
				destinationField = GGObjectQueryFactory.objectQuery(destinationClass).find(rule.sourceFieldAddress());
			}
			
			Field sourceFieldLeaf = (Field) sourceField.get(sourceField.size()-1);
			Field destinationFieldLeaf = (Field) destinationField.get(destinationField.size()-1);
			
			if( sourceFieldLeaf.getType().equals(destinationFieldLeaf.getType()) ) {
				return new GGAPISimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
			}

		} catch (GGAPIObjectQueryException e) {
			throw new GGAPIMapperException(e);
		}
		return null;
	}
}
