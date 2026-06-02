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

import com.garganttua.core.observability.Logger;
import com.garganttua.core.mapper.IMapper;
import com.garganttua.core.mapper.IMappingRecursion;
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

/**
 * Maps a collection field by recursively mapping each source element into the
 * destination collection's generic element type. Instantiates a concrete
 * destination collection (preserving the declared type where possible) and adds
 * the mapped elements one by one.
 */
public class MapableCollectionMappingExecutor implements IMappingRuleExecutor {
    private static final Logger log = Logger.getLogger(MapableCollectionMappingExecutor.class);

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

	/**
	 * Creates an executor that recursively maps each element of the collection in
	 * {@code sourceField} into {@code destinationField}.
	 *
	 * @param reflection the reflection facade used for type resolution and instantiation
	 * @param mapper the mapper used to recursively map collection elements
	 * @param sourceField the collection field to read from the source object
	 * @param destinationField the collection field to write on the destination object
	 * @throws ReflectionException if the field accessors or collection types cannot be resolved
	 */
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

	/**
	 * Maps the collection using the bare mapper (no shared recursion scope).
	 *
	 * @return the populated destination object
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject) throws MapperException {
		return doMappingInternal(destinationClass, destinationObject, sourceObject, bareRecursion());
	}

	private IMappingRecursion bareRecursion() {
		return new IMappingRecursion() {
			@Override
			public <D> D map(Object src, IClass<D> destCls) throws MapperException {
				return MapableCollectionMappingExecutor.this.mapper.map(src, destCls);
			}
		};
	}

	/**
	 * Maps the collection using the supplied recursion scope, which carries cycle
	 * detection across the mapping boundary.
	 *
	 * @param recursion the shared recursion scope to delegate element mapping to
	 * @return the populated destination object
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject, IMappingRecursion recursion) throws MapperException {
		return doMappingInternal(destinationClass, destinationObject, sourceObject, recursion);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <destination> destination doMappingInternal(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject, IMappingRecursion recursion) throws MapperException {
		log.debug("MapableCollection: {} -> {}", this.sourceField.getName(), this.destinationField.getName());

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
				destination destinationItem = (destination) recursion.map(item, genericType);
				((Collection) destinationFieldObject).add(destinationItem);
			}

			this.destinationFieldAccessor.setValue(destinationObject,
					SingleFieldValue.of(destinationFieldObject, (IClass<Object>) this.destinationField.getType()));
		} catch (ReflectionException e) {
			log.error("Collection mapping failed for {}: {}", this.sourceField.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

	private IClass<?> getFieldGenericType(IField field, int index) {
		Type genericType = field.getGenericType();
		if (genericType instanceof ParameterizedType parameterizedType) {
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > index) {
				return resolveType(typeArguments[index]);
			}
		}
		return null;
	}

	/**
	 * Resolves a generic {@link Type} to an {@link IClass} for the most useful
	 * raw form: Class is returned as-is, ParameterizedType yields its raw type
	 * (so {@code List<List<X>>} → {@code List}), wildcards collapse to their
	 * upper bound (so {@code ? extends Foo} → {@code Foo}), and unbounded
	 * variables fall back to {@code Object}. Returning null caused downstream
	 * NPEs in {@code Mapper.mapInternal}.
	 */
	private IClass<?> resolveType(Type type) {
		if (type instanceof Class<?> clazz) {
			return this.reflection.getClass(clazz);
		}
		if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) {
			return this.reflection.getClass(raw);
		}
		if (type instanceof java.lang.reflect.WildcardType wt) {
			Type[] upper = wt.getUpperBounds();
			if (upper.length > 0) {
				return resolveType(upper[0]);
			}
		}
		if (type instanceof java.lang.reflect.TypeVariable<?> tv) {
			Type[] bounds = tv.getBounds();
			if (bounds.length > 0) {
				return resolveType(bounds[0]);
			}
		}
		// Last resort — Object covers passthrough mapping where the actual
		// item type is supplied by the source at runtime.
		return this.reflection.getClass(Object.class);
	}

	private Object instanciateCollectionField(IField field) throws ReflectionException {
		IClass<?> fieldType = field.getType();
		// A concrete type has a usable no-arg ctor — instantiate it directly so a
		// declared LinkedList stays a LinkedList, etc. Only interfaces / abstract
		// collection types (List, Set, Map, …) lack one; calling newInstance on
		// those makes ConstructorDelegate log a spurious ERROR before any
		// fallback, so map them straight to a default concrete impl instead.
		if (!fieldType.isInterface() && !java.lang.reflect.Modifier.isAbstract(fieldType.getModifiers())) {
			return this.reflection.newInstance(fieldType);
		}
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
		throw new ReflectionException("Unable to instantiate collection of type " + fieldType.getSimpleName());
	}

}
