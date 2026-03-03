package com.garganttua.core.mapper.rules;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

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
public class MapableCollectionMappingExecutor implements IMappingRuleExecutor {

	private IReflection reflection;
	private IMapper mapper;
	private IField sourceField;
	private IField destinationField;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

	@SuppressWarnings("rawtypes")
	private final IClass<Map> mapClass;
	@SuppressWarnings("rawtypes")
	private final IClass<List> listClass;
	@SuppressWarnings("rawtypes")
	private final IClass<Set> setClass;
	@SuppressWarnings("rawtypes")
	private final IClass<Queue> queueClass;
	@SuppressWarnings("rawtypes")
	private final IClass<Collection> collectionClass;

	public MapableCollectionMappingExecutor(IReflection reflection, IMapper mapper, IField sourceField, IField destinationField) throws ReflectionException {
		this.reflection = reflection;
		this.mapper = mapper;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.sourceFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));

		this.mapClass = reflection.getClass(Map.class);
		this.listClass = reflection.getClass(List.class);
		this.setClass = reflection.getClass(Set.class);
		this.queueClass = reflection.getClass(Queue.class);
		this.collectionClass = reflection.getClass(Collection.class);
	}

	@SuppressWarnings({ "rawtypes"})
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		log.atDebug().log("MapableCollection: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

		if (destinationObject == null) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}

		try {
			Object sourceFieldObject = this.sourceFieldAccessor.getValue(sourceObject).single();

			Object destinationFieldObject = instanciateCollectionField(this.destinationField);

			if( sourceFieldObject == null ) {
				return destinationObject;
			}

			Collection sourceCollection = (Collection) sourceFieldObject;

			IClass<?> genericType = getFieldGenericType(this.destinationField, 0);
			for (Object item: sourceCollection) {
				destination destinationItem = (destination) this.mapper.map(item, genericType);
				((Collection) destinationFieldObject).add(destinationItem);
			}

			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(destinationFieldObject, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.atError().log("Collection mapping failed for {}: {}", this.sourceField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
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

	private Object instanciateCollectionField(IField field) throws ReflectionException {
		IClass<?> fieldType = field.getType();
		try {
			return this.reflection.newInstance(fieldType);
		} catch (Exception e) {
			if (mapClass.isAssignableFrom(fieldType)) {
				return new HashMap<>();
			}
			if (listClass.isAssignableFrom(fieldType)) {
				return new ArrayList<>();
			}
			if (setClass.isAssignableFrom(fieldType)) {
				return new HashSet<>();
			}
			if (queueClass.isAssignableFrom(fieldType)) {
				return new LinkedList<>();
			}
			if (collectionClass.isAssignableFrom(fieldType)) {
				return new Vector<>();
			}
			throw new ReflectionException("Unable to instantiate collection of type " + fieldType.getSimpleName(), e);
		}
	}

}
