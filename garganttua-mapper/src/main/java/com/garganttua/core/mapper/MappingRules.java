package com.garganttua.core.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.mapper.rules.MapableCollectionMappingExecutor;
import com.garganttua.core.mapper.rules.MethodMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleCollectionMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleFieldMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleMapableFieldMappingExecutor;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.Fields;
import com.garganttua.core.reflection.query.ObjectQueryFactory;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MappingRules {

	public static List<MappingRule> parse(Class<?> destinationClass) throws MapperException {
		log.atTrace().log("Entering parse(destinationClass={})", destinationClass);
		log.atDebug().log("Parsing mapping rules for class: {}", destinationClass.getSimpleName());

		List<MappingRule> mappingRules = new ArrayList<MappingRule>();
		List<MappingRule> result = MappingRules.recursiveParsing(destinationClass, mappingRules, "");

		log.atDebug().log("Parsed {} mapping rules for class {}", result.size(), destinationClass.getSimpleName());
		log.atTrace().log("Exiting parse() with {} rules", result.size());
		return result;
	}

	private static List<MappingRule> recursiveParsing(Class<?> destinationClass, List<MappingRule> mappingRules,
			String fieldAddress) throws MapperException {
		log.atTrace().log("Entering recursiveParsing(destinationClass={}, fieldAddress={})", destinationClass, fieldAddress);
		log.atDebug().log("Looking for mapping rules in class {} at address {}", destinationClass.getSimpleName(), fieldAddress);

		try {
			boolean objectMapping = false;
			if (destinationClass.isAnnotationPresent(ObjectMappingRule.class)) {
				log.atDebug().log("Found ObjectMappingRule annotation on class {}", destinationClass.getSimpleName());
				ObjectMappingRule annotation = destinationClass.getDeclaredAnnotation(ObjectMappingRule.class);
				ObjectAddress fromSourceMethod;
				fromSourceMethod = new ObjectAddress(annotation.fromSourceMethod());
				ObjectAddress toSourceMethod = new ObjectAddress(annotation.toSourceMethod());
				mappingRules.add(new MappingRule(null, null, destinationClass, fromSourceMethod, toSourceMethod));
				log.atDebug().log("Added object mapping rule for class {}", destinationClass.getSimpleName());
				objectMapping = true;
			}

			for (Field field : destinationClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(FieldMappingRule.class) && !objectMapping) {
					log.atDebug().log("Found FieldMappingRule annotation on field {}", field.getName());
					FieldMappingRule annotation = field.getDeclaredAnnotation(FieldMappingRule.class);

					ObjectAddress fromSourceMethod = null;
					if (!annotation.fromSourceMethod().isEmpty()) {
						fromSourceMethod = new ObjectAddress(annotation.fromSourceMethod());
					}
					ObjectAddress toSourceMethod = null;
					if (!annotation.toSourceMethod().isEmpty()) {
						toSourceMethod = new ObjectAddress(annotation.toSourceMethod());
					}

					ObjectAddress sourceFieldAddress = new ObjectAddress(annotation.sourceFieldAddress());
					ObjectAddress destFieldAddress = new ObjectAddress(fieldAddress + field.getName());

					mappingRules.add(new MappingRule(sourceFieldAddress, destFieldAddress, destinationClass,
							fromSourceMethod, toSourceMethod));
					log.atDebug().log("Added field mapping rule: {} -> {}", sourceFieldAddress, destFieldAddress);
				} else {
					if (Fields.isNotPrimitive(field.getType()) &&
							!Collection.class.isAssignableFrom(field.getType()) &&
							!Map.class.isAssignableFrom(field.getType()) &&
							field.getType().isArray()) {
						log.atDebug().log("Recursively parsing array field type: {}", field.getType().getSimpleName());
						MappingRules.recursiveParsing(field.getType(), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
					} else if (Collection.class.isAssignableFrom(field.getType())) {
						log.atDebug().log("Recursively parsing collection field: {}", field.getName());
						MappingRules.recursiveParsing(Fields.getGenericType(field, 0), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
					} else if (Map.class.isAssignableFrom(field.getType())) {
						log.atDebug().log("Recursively parsing map field: {}", field.getName());
						MappingRules.recursiveParsing(Fields.getGenericType(field, 0), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR
										+ ObjectAddress.MAP_KEY_INDICATOR + ObjectAddress.ELEMENT_SEPARATOR);
						MappingRules.recursiveParsing(Fields.getGenericType(field, 1), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR
										+ ObjectAddress.MAP_VALUE_INDICATOR + ObjectAddress.ELEMENT_SEPARATOR);
					}
				}
			}
			if (destinationClass.getSuperclass() != null) {
				log.atDebug().log("Recursively parsing superclass: {}", destinationClass.getSuperclass().getSimpleName());
				MappingRules.recursiveParsing(destinationClass.getSuperclass(), mappingRules, fieldAddress);
			}

			log.atTrace().log("Exiting recursiveParsing() with {} rules", mappingRules.size());
			return mappingRules;
		} catch (ReflectionException e) {
			log.atError().log("Reflection error during recursive parsing: {}", e.getMessage());
			throw new MapperException(e);
		}
	}

	public static void validate(Class<?> sourceClass, List<MappingRule> rules) throws MapperException {
		log.atTrace().log("Entering validate(sourceClass={}, rules count={})", sourceClass, rules.size());
		log.atDebug().log("Validating {} mapping rules for class {}", rules.size(), sourceClass.getSimpleName());

		IObjectQuery sourceQuery;
		try {
			sourceQuery = ObjectQueryFactory.objectQuery(sourceClass);
			for (MappingRule rule : rules) {
				log.atDebug().log("Validating mapping rule: {}", rule);
				MappingRules.validate(sourceQuery, rule);
			}
			log.atDebug().log("All mapping rules validated successfully");
			log.atTrace().log("Exiting validate()");
		} catch (ReflectionException e) {
			log.atError().log("Validation failed for class {}: {}", sourceClass.getSimpleName(), e.getMessage());
			throw new MapperException(e);
		}
	}

	private static void validate(IObjectQuery sourceQuery, MappingRule rule)
			throws MapperException {
		log.atTrace().log("Entering validate(sourceQuery={}, rule={})", sourceQuery, rule);
		log.atDebug().log("Validating mapping rule {} for source query {}", rule.toString(), sourceQuery.toString());

		try {
			IObjectQuery destQuery = ObjectQueryFactory.objectQuery(rule.destinationClass());

			List<Object> sourceField_ = sourceQuery.find(rule.sourceFieldAddress());
			List<Object> destField_ = destQuery.find(rule.destinationFieldAddress());

			Field sourceField = (Field) sourceField_.get(sourceField_.size() - 1);
			Field destField = (Field) destField_.get(destField_.size() - 1);

			log.atDebug().log("Validating field mapping: {} -> {}", sourceField.getName(), destField.getName());

			if (rule.fromSourceMethodAddress() != null) {
				log.atDebug().log("Validating fromSourceMethod: {}", rule.fromSourceMethodAddress());
				List<Object> fromMethod_ = destQuery.find(rule.fromSourceMethodAddress());
				Method fromMethod = (Method) fromMethod_.get(fromMethod_.size() - 1);

				MappingRules.validateMethod(rule, sourceField, destField, fromMethod);
			}
			if (rule.toSourceMethodAddress() != null) {
				log.atDebug().log("Validating toSourceMethod: {}", rule.toSourceMethodAddress());
				List<Object> toMethod_ = destQuery.find(rule.toSourceMethodAddress());
				Method toMethod = (Method) toMethod_.get(toMethod_.size() - 1);

				MappingRules.validateMethod(rule, destField, sourceField, toMethod);
			}

			log.atTrace().log("Exiting validate()");
		} catch (ReflectionException e) {
			log.atError().log("Validation error for mapping rule: {}", e.getMessage());
			throw new MapperException(e);
		}
	}

	private static void validateMethod(MappingRule rule, Field sourceField, Field destField, Method method)
			throws MapperException {
		log.atTrace().log("Entering validateMethod(method={}, sourceField={}, destField={})", method.getName(), sourceField.getName(), destField.getName());
		log.atDebug().log("Validating method {} for rule {}", method.getName(), rule);

		if (method.getParameterTypes().length != 1) {
			log.atError().log("Method {} has invalid parameter count: {}", method.getName(), method.getParameterTypes().length);
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : must have exactly one parameter");
		}

		Class<?> paramType = method.getParameterTypes()[0];
		Class<?> returnType = method.getReturnType();

		log.atDebug().log("Method signature validation - paramType: {}, returnType: {}", paramType.getSimpleName(), returnType.getSimpleName());

		if (!paramType.equals(sourceField.getType())) {
			log.atError().log("Method {} parameter type mismatch: expected {}, got {}", method.getName(), sourceField.getType().getSimpleName(), paramType.getSimpleName());
			throw new MapperException(
					"Invalid method " + method.getName() + " of class " + rule.destinationClass().getSimpleName()
							+ " : parameter must be of type " + sourceField.getType());
		}

		if (!returnType.equals(destField.getType())) {
			log.atError().log("Method {} return type mismatch: expected {}, got {}", method.getName(), destField.getType().getSimpleName(), returnType.getSimpleName());
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : return type must be " + destField.getType());
		}

		log.atDebug().log("Method {} validated successfully", method.getName());
		log.atTrace().log("Exiting validateMethod()");
	}

	public static IMappingRuleExecutor getRuleExecutor(IMapper mapper, MappingDirection mappingDirection,
			MappingRule rule, Object source, Class<?> destinationClass) throws MapperException {
		log.atTrace().log("Entering getRuleExecutor(mappingDirection={}, rule={}, source={}, destinationClass={})", mappingDirection, rule, source, destinationClass);
		log.atDebug().log("Getting rule executor for mapping direction {} from {} to {}", mappingDirection, source.getClass().getSimpleName(), destinationClass.getSimpleName());

		List<Object> destinationField = null;
		List<Object> sourceField = null;
		List<Object> mappingMethod = null;

		try {
			if (mappingDirection == MappingDirection.REGULAR) {
				log.atDebug().log("Processing REGULAR mapping direction");
				if (rule.fromSourceMethodAddress() != null) {
					log.atDebug().log("Finding fromSourceMethod: {}", rule.fromSourceMethodAddress());
					mappingMethod = ObjectQueryFactory.objectQuery(destinationClass)
							.find(rule.fromSourceMethodAddress());
				}
				sourceField = ObjectQueryFactory.objectQuery(source.getClass()).find(rule.sourceFieldAddress());
				destinationField = ObjectQueryFactory.objectQuery(destinationClass)
						.find(rule.destinationFieldAddress());
			} else {
				log.atDebug().log("Processing REVERSE mapping direction");
				if (rule.toSourceMethodAddress() != null) {
					log.atDebug().log("Finding toSourceMethod: {}", rule.toSourceMethodAddress());
					mappingMethod = ObjectQueryFactory.objectQuery(source.getClass())
							.find(rule.toSourceMethodAddress());
				}
				sourceField = ObjectQueryFactory.objectQuery(source.getClass()).find(rule.destinationFieldAddress());
				destinationField = ObjectQueryFactory.objectQuery(destinationClass).find(rule.sourceFieldAddress());
			}

			Field sourceFieldLeaf = (Field) sourceField.get(sourceField.size() - 1);
			Field destinationFieldLeaf = (Field) destinationField.get(destinationField.size() - 1);

			log.atDebug().log("Field transformation: {} ({}) -> {} ({})", sourceFieldLeaf.getName(), sourceFieldLeaf.getType().getSimpleName(), destinationFieldLeaf.getName(), destinationFieldLeaf.getType().getSimpleName());

			if (mappingMethod != null) {
				Method methodLeaf = (Method) mappingMethod.get(mappingMethod.size() - 1);
				log.atDebug().log("Using MethodMappingExecutor for method: {}", methodLeaf.getName());
				IMappingRuleExecutor result = new MethodMappingExecutor(methodLeaf, sourceFieldLeaf, destinationFieldLeaf, mappingDirection);
				log.atTrace().log("Exiting getRuleExecutor() with MethodMappingExecutor");
				return result;
			} else if (Collection.class.isAssignableFrom(sourceFieldLeaf.getType())
					&& Collection.class.isAssignableFrom(destinationFieldLeaf.getType())) {
				Class<?> sourceGenericeType = Fields.getGenericType(sourceFieldLeaf, 0);
				Class<?> destGenericeType = Fields.getGenericType(destinationFieldLeaf, 0);

				log.atDebug().log("Processing collection mapping: source generic={}, dest generic={}", sourceGenericeType.getSimpleName(), destGenericeType.getSimpleName());

				if (ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())
						&& sourceGenericeType.equals(destGenericeType)) {
					log.atDebug().log("Using SimpleFieldMappingExecutor for identical collection types");
					IMappingRuleExecutor result = new SimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
					log.atTrace().log("Exiting getRuleExecutor() with SimpleFieldMappingExecutor");
					return result;
				} else if (!ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())
						&& sourceGenericeType.equals(destGenericeType)) {
					log.atDebug().log("Using SimpleCollectionMappingExecutor for different collection types");
					IMappingRuleExecutor result = new SimpleCollectionMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
					log.atTrace().log("Exiting getRuleExecutor() with SimpleCollectionMappingExecutor");
					return result;
				} else {
					log.atDebug().log("Using MapableCollectionMappingExecutor for mappable collection elements");
					IMappingRuleExecutor result = new MapableCollectionMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
					log.atTrace().log("Exiting getRuleExecutor() with MapableCollectionMappingExecutor");
					return result;
				}
			} else if (ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())) {
				log.atDebug().log("Using SimpleFieldMappingExecutor for identical field types");
				IMappingRuleExecutor result = new SimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
				log.atTrace().log("Exiting getRuleExecutor() with SimpleFieldMappingExecutor");
				return result;
			} else if (!Fields.isArrayOrMapOrCollectionField(sourceFieldLeaf)
					&& !Fields.isArrayOrMapOrCollectionField(destinationFieldLeaf)) {
				log.atDebug().log("Using SimpleMapableFieldMappingExecutor for mappable fields");
				IMappingRuleExecutor result = new SimpleMapableFieldMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
				log.atTrace().log("Exiting getRuleExecutor() with SimpleMapableFieldMappingExecutor");
				return result;
			}

			log.atWarn().log("No suitable executor found for rule: {}", rule);
			log.atTrace().log("Exiting getRuleExecutor() with null");

		} catch (ReflectionException e) {
			log.atError().log("Failed to get rule executor: {}", e.getMessage());
			throw new MapperException(e);
		}
		return null;
	}
}
