package com.garganttua.core.mapper.rules;

import java.util.List;

import com.garganttua.core.observability.Logger;
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

/**
 * Maps a field by invoking a user-supplied conversion method. The source field
 * value is passed to the configured method and its return value is written to
 * the destination field. The method is invoked on the destination object for
 * {@link MappingDirection#REGULAR} mappings and on the source object otherwise.
 */
public class MethodMappingExecutor implements IMappingRuleExecutor {
    private static final Logger log = Logger.getLogger(MethodMappingExecutor.class);

	private IMethod method;
	private IField sourceField;
	private IField destinationField;
	private MappingDirection mappingDirection;
	private FieldAccessor<Object> sourceFieldAccessor;
	private FieldAccessor<Object> destinationFieldAccessor;
	private MethodInvoker<Object, Object> methodInvoker;

	/**
	 * Creates an executor that maps {@code sourceField} into {@code destinationField}
	 * via the given conversion method.
	 *
	 * @param method the conversion method that transforms the source field value
	 * @param sourceField the field to read from the source object
	 * @param destinationField the field to write on the destination object
	 * @param mappingDirection the direction that determines which object the method is invoked on
	 * @throws ReflectionException if the field accessors or method invoker cannot be resolved
	 */
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

	/**
	 * Reads the source field value, invokes the conversion method on it, and
	 * writes the result to the destination field. A null source value is skipped.
	 *
	 * @return the populated destination object
	 * @throws MapperException if the conversion method throws or fails to invoke
	 */
	@Override
	public <destination> destination doMapping(IClass<destination> destinationClass, destination destinationObject, Object sourceObject) throws MapperException {
		log.debug("Method: {} via {} ({})", this.sourceField.getName(), this.method.getName(), this.mappingDirection);

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
			log.error("Method mapping failed for {}: {}", this.method.getName(), e.getMessage());
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
