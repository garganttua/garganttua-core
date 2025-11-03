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
		List<MappingRule> mappingRules = new ArrayList<MappingRule>();
		return MappingRules.recursiveParsing(destinationClass, mappingRules, "");
	}

	private static List<MappingRule> recursiveParsing(Class<?> destinationClass, List<MappingRule> mappingRules,
			String fieldAddress) throws MapperException {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Looking for mapping rules in " + destinationClass + " address " + fieldAddress);
			}
			boolean objectMapping = false;
			if (destinationClass.isAnnotationPresent(ObjectMappingRule.class)) {
				ObjectMappingRule annotation = destinationClass.getDeclaredAnnotation(ObjectMappingRule.class);
				ObjectAddress fromSourceMethod;
				fromSourceMethod = new ObjectAddress(annotation.fromSourceMethod());
				ObjectAddress toSourceMethod = new ObjectAddress(annotation.toSourceMethod());
				mappingRules.add(new MappingRule(null, null, destinationClass, fromSourceMethod, toSourceMethod));
				objectMapping = true;
			}

			for (Field field : destinationClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(FieldMappingRule.class) && !objectMapping) {
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
				} else {
					if (Fields.isNotPrimitive(field.getType()) &&
							!Collection.class.isAssignableFrom(field.getType()) &&
							!Map.class.isAssignableFrom(field.getType()) &&
							field.getType().isArray()) {
						MappingRules.recursiveParsing(field.getType(), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
					} else if (Collection.class.isAssignableFrom(field.getType())) {
						MappingRules.recursiveParsing(Fields.getGenericType(field, 0), mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
					} else if (Map.class.isAssignableFrom(field.getType())) {
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
				MappingRules.recursiveParsing(destinationClass.getSuperclass(), mappingRules, fieldAddress);
			}
			return mappingRules;
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
	}

	public static void validate(Class<?> sourceClass, List<MappingRule> rules) throws MapperException {
		IObjectQuery sourceQuery;
		try {
			sourceQuery = ObjectQueryFactory.objectQuery(sourceClass);
			for (MappingRule rule : rules) {
				MappingRules.validate(sourceQuery, rule);
			}
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
	}

	private static void validate(IObjectQuery sourceQuery, MappingRule rule)
			throws MapperException {
		if (log.isDebugEnabled()) {
			log.debug("Validating mapping rule " + rule.toString() + " for source query " + sourceQuery.toString());
		}
		try {
			IObjectQuery destQuery = ObjectQueryFactory.objectQuery(rule.destinationClass());

			List<Object> sourceField_ = sourceQuery.find(rule.sourceFieldAddress());
			List<Object> destField_ = destQuery.find(rule.destinationFieldAddress());

			Field sourceField = (Field) sourceField_.get(sourceField_.size() - 1);
			Field destField = (Field) destField_.get(destField_.size() - 1);

			if (rule.fromSourceMethodAddress() != null) {
				List<Object> fromMethod_ = destQuery.find(rule.fromSourceMethodAddress());
				Method fromMethod = (Method) fromMethod_.get(fromMethod_.size() - 1);

				MappingRules.validateMethod(rule, sourceField, destField, fromMethod);
			}
			if (rule.toSourceMethodAddress() != null) {
				List<Object> toMethod_ = destQuery.find(rule.toSourceMethodAddress());
				Method toMethod = (Method) toMethod_.get(toMethod_.size() - 1);

				MappingRules.validateMethod(rule, destField, sourceField, toMethod);
			}
		} catch (ReflectionException e) {
			if (log.isDebugEnabled()) {
				log.warn("Error : ", e);
			}
			throw new MapperException(e);
		}
	}

	private static void validateMethod(MappingRule rule, Field sourceField, Field destField, Method method)
			throws MapperException {
		if (method.getParameterTypes().length != 1) {
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : must have exactly one parameter");
		}

		Class<?> paramType = method.getParameterTypes()[0];
		Class<?> returnType = method.getReturnType();

		if (!paramType.equals(sourceField.getType())) {
			throw new MapperException(
					"Invalid method " + method.getName() + " of class " + rule.destinationClass().getSimpleName()
							+ " : parameter must be of type " + sourceField.getType());
		}

		if (!returnType.equals(destField.getType())) {
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : return type must be " + destField.getType());
		}
	}

	public static IMappingRuleExecutor getRuleExecutor(IMapper mapper, MappingDirection mappingDirection,
			MappingRule rule, Object source, Class<?> destinationClass) throws MapperException {
		List<Object> destinationField = null;
		List<Object> sourceField = null;
		List<Object> mappingMethod = null;

		try {
			if (mappingDirection == MappingDirection.REGULAR) {
				if (rule.fromSourceMethodAddress() != null) {
					mappingMethod = ObjectQueryFactory.objectQuery(destinationClass)
							.find(rule.fromSourceMethodAddress());
				}
				sourceField = ObjectQueryFactory.objectQuery(source.getClass()).find(rule.sourceFieldAddress());
				destinationField = ObjectQueryFactory.objectQuery(destinationClass)
						.find(rule.destinationFieldAddress());
			} else {
				if (rule.toSourceMethodAddress() != null) {
					mappingMethod = ObjectQueryFactory.objectQuery(source.getClass())
							.find(rule.toSourceMethodAddress());
				}
				sourceField = ObjectQueryFactory.objectQuery(source.getClass()).find(rule.destinationFieldAddress());
				destinationField = ObjectQueryFactory.objectQuery(destinationClass).find(rule.sourceFieldAddress());
			}

			Field sourceFieldLeaf = (Field) sourceField.get(sourceField.size() - 1);
			Field destinationFieldLeaf = (Field) destinationField.get(destinationField.size() - 1);

			if (mappingMethod != null) {
				Method methodLeaf = (Method) mappingMethod.get(mappingMethod.size() - 1);
				return new MethodMappingExecutor(methodLeaf, sourceFieldLeaf, destinationFieldLeaf,
						mappingDirection);
			} else if (Collection.class.isAssignableFrom(sourceFieldLeaf.getType())
					&& Collection.class.isAssignableFrom(destinationFieldLeaf.getType())) {
				Class<?> sourceGenericeType = Fields.getGenericType(sourceFieldLeaf, 0);
				Class<?> destGenericeType = Fields.getGenericType(destinationFieldLeaf, 0);

				if (ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())
						&& sourceGenericeType.equals(destGenericeType)) {
					return new SimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
				} else if (!ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())
						&& sourceGenericeType.equals(destGenericeType)) {
					return new SimpleCollectionMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
				} else {
					return new MapableCollectionMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
				}
			} else if (ObjectReflectionHelper.equals(sourceFieldLeaf.getType(), destinationFieldLeaf.getType())) {
				return new SimpleFieldMappingExecutor(sourceFieldLeaf, destinationFieldLeaf);
			} else if (!Fields.isArrayOrMapOrCollectionField(sourceFieldLeaf)
					&& !Fields.isArrayOrMapOrCollectionField(destinationFieldLeaf)) {
				return new SimpleMapableFieldMappingExecutor(mapper, sourceFieldLeaf, destinationFieldLeaf);
			}

		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
		return null;
	}
}
