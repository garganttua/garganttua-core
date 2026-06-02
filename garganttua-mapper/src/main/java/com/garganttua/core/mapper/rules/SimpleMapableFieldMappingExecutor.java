package com.garganttua.core.mapper.rules;

import java.util.List;

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
 * Maps a single field whose value is itself a mappable (nested) object by
 * delegating the field value to the {@link IMapper}. Used when the source and
 * destination field types differ and require recursive mapping.
 */
public class SimpleMapableFieldMappingExecutor implements IMappingRuleExecutor {
    private static final Logger log = Logger.getLogger(SimpleMapableFieldMappingExecutor.class);

	private IReflection reflection;
	private IField sourceFieldLeaf;
	private IField destinationFieldLeaf;
	private IMapper mapper;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

	/**
	 * Creates an executor that recursively maps the nested object held by
	 * {@code sourceFieldLeaf} into {@code destinationFieldLeaf}.
	 *
	 * @param reflection the reflection facade used to instantiate the destination
	 * @param mapper the mapper used to recursively map the nested field value
	 * @param sourceFieldLeaf the field to read from the source object
	 * @param destinationFieldLeaf the field to write on the destination object
	 * @throws ReflectionException if the field accessors cannot be resolved
	 */
	public SimpleMapableFieldMappingExecutor(IReflection reflection, IMapper mapper, IField sourceFieldLeaf, IField destinationFieldLeaf) throws ReflectionException {
		this.reflection = reflection;
		this.sourceFieldLeaf = sourceFieldLeaf;
		this.destinationFieldLeaf = destinationFieldLeaf;
		this.mapper = mapper;
		this.sourceFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceFieldLeaf.getName(), false), List.of(sourceFieldLeaf)));
		this.destinationFieldAccessor = (FieldAccessor<Object>) new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationFieldLeaf.getName(), false), List.of(destinationFieldLeaf)));
	}

	/**
	 * Maps the nested source field using the bare mapper (no shared recursion
	 * scope, so cross-boundary cycle detection does not apply).
	 *
	 * @return the populated destination object
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		// Fallback path (no recursion scope provided): use the bare mapper. Cycle
		// detection won't span this boundary — prefer the IMappingRecursion overload.
		return doMappingInternal(destinationClass, destinationObject, sourceObject, bareRecursion());
	}

	private IMappingRecursion bareRecursion() {
		return new IMappingRecursion() {
			@Override
			public <D> D map(Object src, IClass<D> destCls) throws MapperException {
				return SimpleMapableFieldMappingExecutor.this.mapper.map(src, destCls);
			}
		};
	}

	/**
	 * Maps the nested source field using the supplied recursion scope, which
	 * carries cycle detection across the mapping boundary.
	 *
	 * @param recursion the shared recursion scope to delegate nested mapping to
	 * @return the populated destination object
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject, IMappingRecursion recursion) throws MapperException {
		return doMappingInternal(destinationClass, destinationObject, sourceObject, recursion);
	}

	@SuppressWarnings("unchecked")
	private <destination> destination doMappingInternal(IClass<destination> destinationClass, destination destinationObject,
			Object sourceObject, IMappingRecursion recursion) throws MapperException {
		log.debug("Mapable: {} ({}) -> {} ({})", this.sourceFieldLeaf.getName(), this.sourceFieldLeaf.getType().getSimpleName(), this.destinationFieldLeaf.getName(), this.destinationFieldLeaf.getType().getSimpleName());

		if( destinationObject == null ) {
			try {
				destinationObject = this.reflection.newInstance(destinationClass);
			} catch (ReflectionException e) {
				throw new MapperException(e);
			}
		}
		try {
			Object sourceObjectToMap = this.sourceFieldAccessor.getValue(sourceObject).single();

			if( sourceObjectToMap != null ) {
				destination destinationObjectMapped = (destination) recursion.map(sourceObjectToMap, this.destinationFieldLeaf.getType());
				this.destinationFieldAccessor.setValue(destinationObject,
						SingleFieldValue.of((Object) destinationObjectMapped, (IClass<Object>) this.destinationFieldLeaf.getType()));
			}

		} catch (ReflectionException e) {
			log.error("Mapping failed for {} -> {}: {}", this.sourceFieldLeaf.getName(), this.destinationFieldLeaf.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

}
