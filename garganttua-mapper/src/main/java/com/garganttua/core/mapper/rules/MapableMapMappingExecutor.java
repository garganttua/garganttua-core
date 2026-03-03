package com.garganttua.core.mapper.rules;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.reflection.fields.SingleFieldValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapableMapMappingExecutor implements IMappingRuleExecutor {

	private final IReflection reflection;
	private final IMapper mapper;
	private final IField sourceField;
	private final IField destinationField;
	private final FieldAccessor<Object> sourceFieldAccessor;
	private final FieldAccessor<Object> destinationFieldAccessor;
	private final IClass<?> destKeyType;
	private final IClass<?> destValueType;
	private final IClass<?> srcKeyType;
	private final IClass<?> srcValueType;

	public MapableMapMappingExecutor(IReflection reflection, IMapper mapper, IField sourceField,
			IField destinationField) throws ReflectionException {
		this.reflection = reflection;
		this.mapper = mapper;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.sourceFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
		this.srcKeyType = getFieldGenericType(sourceField, 0, reflection);
		this.srcValueType = getFieldGenericType(sourceField, 1, reflection);
		this.destKeyType = getFieldGenericType(destinationField, 0, reflection);
		this.destValueType = getFieldGenericType(destinationField, 1, reflection);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.atDebug().log("MapableMap: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

		if (destinationObject == null) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		try {
			Object sourceFieldObj = this.sourceFieldAccessor.getValue(sourceObject).single();

			if (sourceFieldObj == null) {
				return destinationObject;
			}

			Map sourceMap = (Map) sourceFieldObj;
			Map destMap = new HashMap<>();

			boolean keysNeedMapping = srcKeyType != null && destKeyType != null
					&& !srcKeyType.getType().equals(destKeyType.getType());
			boolean valuesNeedMapping = srcValueType != null && destValueType != null
					&& !srcValueType.getType().equals(destValueType.getType());

			for (Object entry : sourceMap.entrySet()) {
				Map.Entry mapEntry = (Map.Entry) entry;
				Object key = mapEntry.getKey();
				Object value = mapEntry.getValue();

				Object destKey = keysNeedMapping && key != null ? this.mapper.map(key, destKeyType) : key;
				Object destValue = valuesNeedMapping && value != null ? this.mapper.map(value, destValueType) : value;
				destMap.put(destKey, destValue);
			}

			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(destMap, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.atError().log("Map mapping failed for {}: {}", this.sourceField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

	private static IClass<?> getFieldGenericType(IField field, int index, IReflection reflection) {
		Type genericType = field.getGenericType();
		if (genericType instanceof ParameterizedType parameterizedType) {
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > index && typeArguments[index] instanceof Class<?> clazz) {
				return reflection.getClass(clazz);
			}
		}
		return null;
	}
}
