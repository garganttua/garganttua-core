package com.garganttua.core.mapper.rules;

import java.util.List;

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
public class SimpleMapableFieldMappingExecutor implements IMappingRuleExecutor {

	private IReflection reflection;
	private IField sourceFieldLeaf;
	private IField destinationFieldLeaf;
	private IMapper mapper;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;

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

	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atDebug().log("Mapable: {} ({}) -> {} ({})", this.sourceFieldLeaf.getName(), this.sourceFieldLeaf.getType().getSimpleName(), this.destinationFieldLeaf.getName(), this.destinationFieldLeaf.getType().getSimpleName());

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
				destination destinationObjectMapped = (destination) this.mapper.map(sourceObjectToMap, this.destinationFieldLeaf.getType());
				this.destinationFieldAccessor.setValue(destinationObject,
						SingleFieldValue.of((Object) destinationObjectMapped, (IClass<Object>) this.destinationFieldLeaf.getType()));
			}

		} catch (ReflectionException e) {
			log.atError().log("Mapping failed for {} -> {}: {}", this.sourceFieldLeaf.getName(), this.destinationFieldLeaf.getName(), e.getMessage());
			throw new MapperException(e);
		}

		return destinationObject;
	}

}
