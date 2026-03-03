package com.garganttua.core.mapper.rules;

import java.util.List;

import com.garganttua.core.mapper.IMappingRuleExecutor;
import com.garganttua.core.mapper.MapperException;
import com.garganttua.core.mapper.MappingDirection;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.ResolvedField;
import com.garganttua.core.reflection.fields.SingleFieldValue;
import com.garganttua.core.reflection.methods.MethodInvoker;
import com.garganttua.core.reflection.methods.ResolvedMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MethodMappingExecutor implements IMappingRuleExecutor {

	private IMethod method;
	private IField sourceField;
	private IField destinationField;
	private MappingDirection mappingDirection;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;
	private MethodInvoker<Object, Object> methodInvoker;

	public MethodMappingExecutor(IMethod method, IField sourceField, IField destinationField, MappingDirection mappingDirection) throws ReflectionException {
		this.method = method;
		this.sourceField = sourceField;
		this.destinationField = destinationField;
		this.mappingDirection = mappingDirection;
		this.sourceFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(sourceField.getName(), false), List.of(sourceField)));
		this.destinationFieldAccessor = new FieldAccessor<>(
				new ResolvedField(new ObjectAddress(destinationField.getName(), false), List.of(destinationField)));
		this.methodInvoker = new MethodInvoker<>(
				new ResolvedMethod(new ObjectAddress(method.getName(), false), List.of(method)));
	}

	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.atDebug().log("Method: {} via {} ({})", this.sourceField.getName(), this.method.getName(), this.mappingDirection);

		try {
			Object sourceObjectToMap = this.sourceFieldAccessor.getValue(sourceObject).single();
			if( sourceObjectToMap == null ) {
				return destinationObject;
			}

			if( this.mappingDirection == MappingDirection.REGULAR) {
				IMethodReturn<Object> methodResult = this.methodInvoker.invoke(destinationObject, sourceObjectToMap);
				checkMethodResult(methodResult);
				Object destinationMappedObject = methodResult.single();
				this.destinationFieldAccessor.setValue(destinationObject,
						SingleFieldValue.of(destinationMappedObject, (IClass<Object>) this.destinationField.getType()));
			} else {
				IMethodReturn<Object> methodResult = this.methodInvoker.invoke(sourceObject, sourceObjectToMap);
				checkMethodResult(methodResult);
				Object destinationMappedObject = methodResult.single();
				this.destinationFieldAccessor.setValue(destinationObject,
						SingleFieldValue.of(destinationMappedObject, (IClass<Object>) this.destinationField.getType()));
			}

		} catch (ReflectionException e) {
			log.atError().log("Method mapping failed for {}: {}", this.method.getName(), e.getMessage());
			throw new MapperException(e);
		}
		return destinationObject;
	}

	private static void checkMethodResult(IMethodReturn<?> result) throws MapperException {
		if (result.hasException()) {
			Throwable cause = result.getException();
			throw new MapperException(cause instanceof Exception ex ? ex : new RuntimeException(cause));
		}
	}

}
