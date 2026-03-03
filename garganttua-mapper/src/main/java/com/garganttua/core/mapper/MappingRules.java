package com.garganttua.core.mapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.mapper.annotations.MappingIgnore;
import com.garganttua.core.mapper.annotations.ObjectMappingRule;
import com.garganttua.core.mapper.rules.ImplicitConversionMappingExecutor;
import com.garganttua.core.mapper.rules.ImplicitConversions;
import com.garganttua.core.mapper.rules.MapableCollectionMappingExecutor;
import com.garganttua.core.mapper.rules.MapableMapMappingExecutor;
import com.garganttua.core.mapper.rules.MethodMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleCollectionMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleFieldMappingExecutor;
import com.garganttua.core.mapper.rules.SimpleMapableFieldMappingExecutor;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MappingRules {

	private final IReflection reflection;

	@SuppressWarnings("rawtypes")
	private final IClass<Collection> collectionClass;
	@SuppressWarnings("rawtypes")
	private final IClass<Map> mapClass;

	public MappingRules(IReflection reflection) {
		this.reflection = reflection;
		this.collectionClass = reflection.getClass(Collection.class);
		this.mapClass = reflection.getClass(Map.class);
	}

	public List<MappingRule> parse(IClass<?> destinationClass) throws MapperException {
		log.atDebug().log("Parsing mapping rules for {}", destinationClass.getSimpleName());
		List<MappingRule> mappingRules = new ArrayList<>();
		List<MappingRule> result = this.recursiveParsing(destinationClass, mappingRules, "");
		log.atDebug().log("Parsed {} rules for {}", result.size(), destinationClass.getSimpleName());
		return result;
	}

	private List<MappingRule> recursiveParsing(IClass<?> destinationClass, List<MappingRule> mappingRules,
			String fieldAddress) throws MapperException {
		try {
			boolean objectMapping = false;
			IClass<ObjectMappingRule> objectMappingRuleClass = this.reflection.getClass(ObjectMappingRule.class);
			if (destinationClass.isAnnotationPresent(objectMappingRuleClass)) {
				ObjectMappingRule annotation = destinationClass.getDeclaredAnnotation(objectMappingRuleClass);
				ObjectAddress fromSourceMethod = new ObjectAddress(annotation.fromSourceMethod());
				ObjectAddress toSourceMethod = new ObjectAddress(annotation.toSourceMethod());
				mappingRules.add(new MappingRule(null, null, destinationClass, fromSourceMethod, toSourceMethod));
				objectMapping = true;
			}

			IClass<FieldMappingRule> fieldMappingRuleClass = this.reflection.getClass(FieldMappingRule.class);
			for (IField field : destinationClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(fieldMappingRuleClass) && !objectMapping) {
					FieldMappingRule annotation = field.getDeclaredAnnotation(fieldMappingRuleClass);

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
					IClass<?> fieldType = field.getType();
					if (isNotPrimitive(fieldType) &&
							!collectionClass.isAssignableFrom(fieldType) &&
							!mapClass.isAssignableFrom(fieldType) &&
							fieldType.isArray()) {
						this.recursiveParsing(fieldType, mappingRules,
								fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
					} else if (collectionClass.isAssignableFrom(fieldType)) {
						IClass<?> genericType = getFieldGenericType(field, 0);
						if (genericType != null) {
							this.recursiveParsing(genericType, mappingRules,
									fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR);
						}
					} else if (mapClass.isAssignableFrom(fieldType)) {
						IClass<?> keyType = getFieldGenericType(field, 0);
						IClass<?> valueType = getFieldGenericType(field, 1);
						if (keyType != null) {
							this.recursiveParsing(keyType, mappingRules,
									fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR
											+ ObjectAddress.MAP_KEY_INDICATOR + ObjectAddress.ELEMENT_SEPARATOR);
						}
						if (valueType != null) {
							this.recursiveParsing(valueType, mappingRules,
									fieldAddress + field.getName() + ObjectAddress.ELEMENT_SEPARATOR
											+ ObjectAddress.MAP_VALUE_INDICATOR + ObjectAddress.ELEMENT_SEPARATOR);
						}
					}
				}
			}
			IClass<?> superclass = destinationClass.getSuperclass();
			if (superclass != null) {
				this.recursiveParsing(superclass, mappingRules, fieldAddress);
			}

			return mappingRules;
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
	}

	public void validate(IClass<?> sourceClass, List<MappingRule> rules) throws MapperException {
		log.atDebug().log("Validating {} rules for {}", rules.size(), sourceClass.getSimpleName());
		try {
			IObjectQuery<?> sourceQuery = this.reflection.query(sourceClass);
			for (MappingRule rule : rules) {
				this.validate(sourceQuery, rule);
			}
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
	}

	private void validate(IObjectQuery<?> sourceQuery, MappingRule rule)
			throws MapperException {
		try {
			IObjectQuery<?> destQuery = this.reflection.query(rule.destinationClass());

			List<Object> sourceField_ = sourceQuery.find(rule.sourceFieldAddress());
			List<Object> destField_ = destQuery.find(rule.destinationFieldAddress());

			IField sourceField = (IField) sourceField_.get(sourceField_.size() - 1);
			IField destField = (IField) destField_.get(destField_.size() - 1);

			if (rule.fromSourceMethodAddress() != null) {
				List<Object> fromMethod_ = destQuery.find(rule.fromSourceMethodAddress());
				IMethod fromMethod = (IMethod) fromMethod_.get(fromMethod_.size() - 1);
				MappingRules.validateMethod(rule, sourceField, destField, fromMethod);
			}
			if (rule.toSourceMethodAddress() != null) {
				List<Object> toMethod_ = destQuery.find(rule.toSourceMethodAddress());
				IMethod toMethod = (IMethod) toMethod_.get(toMethod_.size() - 1);
				MappingRules.validateMethod(rule, destField, sourceField, toMethod);
			}
		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
	}

	private static void validateMethod(MappingRule rule, IField sourceField, IField destField, IMethod method)
			throws MapperException {
		if (method.getParameterTypes().length != 1) {
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : must have exactly one parameter");
		}

		IClass<?> paramType = method.getParameterTypes()[0];
		IClass<?> returnType = method.getReturnType();

		if (!paramType.getType().equals(sourceField.getType().getType())) {
			throw new MapperException(
					"Invalid method " + method.getName() + " of class " + rule.destinationClass().getSimpleName()
							+ " : parameter must be of type " + sourceField.getType().getType());
		}

		if (!returnType.getType().equals(destField.getType().getType())) {
			throw new MapperException("Invalid method " + method.getName() + " of class "
					+ rule.destinationClass().getSimpleName() + " : return type must be " + destField.getType().getType());
		}
	}

	public IMappingRuleExecutor getRuleExecutor(IMapper mapper, MappingDirection mappingDirection,
			MappingRule rule, IClass<?> sourceClass, IClass<?> destinationClass) throws MapperException {
		log.atDebug().log("Resolving executor for {} -> {} ({})", sourceClass.getSimpleName(), destinationClass.getSimpleName(), mappingDirection);

		List<Object> destinationField = null;
		List<Object> sourceField = null;
		List<Object> mappingMethod = null;

		try {
			if (mappingDirection == MappingDirection.REGULAR) {
				if (rule.fromSourceMethodAddress() != null) {
					mappingMethod = this.reflection.query(destinationClass)
							.find(rule.fromSourceMethodAddress());
				}
				sourceField = this.reflection.query(sourceClass).find(rule.sourceFieldAddress());
				destinationField = this.reflection.query(destinationClass)
						.find(rule.destinationFieldAddress());
			} else {
				if (rule.toSourceMethodAddress() != null) {
					mappingMethod = this.reflection.query(sourceClass)
							.find(rule.toSourceMethodAddress());
				}
				sourceField = this.reflection.query(sourceClass).find(rule.destinationFieldAddress());
				destinationField = this.reflection.query(destinationClass).find(rule.sourceFieldAddress());
			}

			IField sourceFieldLeaf = (IField) sourceField.get(sourceField.size() - 1);
			IField destinationFieldLeaf = (IField) destinationField.get(destinationField.size() - 1);

			IClass<?> sourceFieldType = sourceFieldLeaf.getType();
			IClass<?> destFieldType = destinationFieldLeaf.getType();

			if (mappingMethod != null) {
				IMethod methodLeaf = (IMethod) mappingMethod.get(mappingMethod.size() - 1);
				return new MethodMappingExecutor(methodLeaf, sourceFieldLeaf, destinationFieldLeaf, mappingDirection);
			} else if (mapClass.isAssignableFrom(sourceFieldType)
					&& mapClass.isAssignableFrom(destFieldType)) {
				// Map<K,V> mapping
				IClass<?> srcKeyType = getFieldGenericType(sourceFieldLeaf, 0);
				IClass<?> srcValType = getFieldGenericType(sourceFieldLeaf, 1);
				IClass<?> dstKeyType = getFieldGenericType(destinationFieldLeaf, 0);
				IClass<?> dstValType = getFieldGenericType(destinationFieldLeaf, 1);
				boolean keysMatch = srcKeyType != null && dstKeyType != null
						&& srcKeyType.getType().equals(dstKeyType.getType());
				boolean valsMatch = srcValType != null && dstValType != null
						&& srcValType.getType().equals(dstValType.getType());
				if (keysMatch && valsMatch) {
					return new SimpleFieldMappingExecutor(this.reflection, sourceFieldLeaf, destinationFieldLeaf);
				} else {
					return new MapableMapMappingExecutor(this.reflection, mapper, sourceFieldLeaf, destinationFieldLeaf);
				}
			} else if (collectionClass.isAssignableFrom(sourceFieldType)
					&& collectionClass.isAssignableFrom(destFieldType)) {
				IClass<?> sourceGenericType = getFieldGenericType(sourceFieldLeaf, 0);
				IClass<?> destGenericType = getFieldGenericType(destinationFieldLeaf, 0);

				if (sourceFieldType.getType().equals(destFieldType.getType())
						&& sourceGenericType != null && sourceGenericType.getType().equals(destGenericType != null ? destGenericType.getType() : null)) {
					return new SimpleFieldMappingExecutor(this.reflection, sourceFieldLeaf, destinationFieldLeaf);
				} else if (!sourceFieldType.getType().equals(destFieldType.getType())
						&& sourceGenericType != null && sourceGenericType.getType().equals(destGenericType != null ? destGenericType.getType() : null)) {
					return new SimpleCollectionMappingExecutor(this.reflection, sourceFieldLeaf, destinationFieldLeaf);
				} else {
					return new MapableCollectionMappingExecutor(this.reflection, mapper, sourceFieldLeaf, destinationFieldLeaf);
				}
			} else if (sourceFieldType.getType().equals(destFieldType.getType())) {
				return new SimpleFieldMappingExecutor(this.reflection, sourceFieldLeaf, destinationFieldLeaf);
			} else {
				// Try implicit conversion before falling back to mapable field
				java.util.Optional<java.util.function.Function<Object, Object>> conv =
						ImplicitConversions.findConversion(sourceFieldType, destFieldType);
				if (conv.isPresent()) {
					return new ImplicitConversionMappingExecutor(this.reflection, sourceFieldLeaf, destinationFieldLeaf, conv.get());
				} else if (!isArrayOrMapOrCollection(sourceFieldLeaf)
						&& !isArrayOrMapOrCollection(destinationFieldLeaf)) {
					return new SimpleMapableFieldMappingExecutor(this.reflection, mapper, sourceFieldLeaf, destinationFieldLeaf);
				}
			}

			log.atWarn().log("No suitable executor found for rule: {}", rule);

		} catch (ReflectionException e) {
			throw new MapperException(e);
		}
		return null;
	}

	public List<MappingRule> generateConventionRules(IClass<?> source, IClass<?> destination) {
		log.atDebug().log("Generating convention rules: {} -> {}", source.getSimpleName(), destination.getSimpleName());
		List<MappingRule> rules = new ArrayList<>();
		IClass<MappingIgnore> mappingIgnoreClass;
		try {
			mappingIgnoreClass = this.reflection.getClass(MappingIgnore.class);
		} catch (Exception e) {
			mappingIgnoreClass = null;
		}
		generateConventionRulesRecursive(source, destination, rules, mappingIgnoreClass);
		log.atDebug().log("Generated {} convention rules", rules.size());
		return rules;
	}

	private void generateConventionRulesRecursive(IClass<?> source, IClass<?> destination,
			List<MappingRule> rules, IClass<MappingIgnore> mappingIgnoreClass) {
		for (IField destField : destination.getDeclaredFields()) {
			int modifiers = destField.getModifiers();
			if (java.lang.reflect.Modifier.isStatic(modifiers)
					|| java.lang.reflect.Modifier.isTransient(modifiers)
					|| destField.isSynthetic()) {
				continue;
			}
			if (mappingIgnoreClass != null && destField.isAnnotationPresent(mappingIgnoreClass)) {
				continue;
			}

			String fieldName = destField.getName();
			if (hasField(source, fieldName)) {
				ObjectAddress srcAddr = new ObjectAddress(fieldName);
				ObjectAddress destAddr = new ObjectAddress(fieldName);
				rules.add(new MappingRule(srcAddr, destAddr, destination, null, null));
			}
		}

		IClass<?> superclass = destination.getSuperclass();
		if (superclass != null && !superclass.getName().equals("java.lang.Object")) {
			generateConventionRulesRecursive(source, superclass, rules, mappingIgnoreClass);
		}
	}

	private boolean hasField(IClass<?> clazz, String fieldName) {
		IClass<?> current = clazz;
		while (current != null && !current.getName().equals("java.lang.Object")) {
			for (IField field : current.getDeclaredFields()) {
				if (field.getName().equals(fieldName)) {
					return true;
				}
			}
			current = current.getSuperclass();
		}
		return false;
	}

	private IClass<?> getFieldGenericType(IField field, int index) {
		Type genericType = field.getGenericType();
		if (genericType instanceof ParameterizedType parameterizedType) {
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > index && typeArguments[index] instanceof Class<?> clazz) {
				return this.reflection.getClass(clazz);
			}
		}
		return null;
	}

	private boolean isArrayOrMapOrCollection(IField field) {
		IClass<?> type = field.getType();
		return collectionClass.isAssignableFrom(type)
				|| mapClass.isAssignableFrom(type)
				|| type.isArray();
	}

	static boolean isNotPrimitive(IClass<?> clazz) {
		if (clazz.isPrimitive()) {
			return false;
		}
		String name = clazz.getName();
		if (name.equals("java.lang.Integer") || name.equals("java.lang.Long")
				|| name.equals("java.lang.Float") || name.equals("java.lang.Double")
				|| name.equals("java.lang.Short") || name.equals("java.lang.Byte")
				|| name.equals("java.lang.Character") || name.equals("java.lang.Boolean")
				|| name.equals("java.lang.String") || name.equals("java.util.Date")) {
			return false;
		}
		return true;
	}
}
