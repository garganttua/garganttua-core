package com.garganttua.core.reflection.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IFieldValue;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.constructors.ConstructorAccessManager;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldAccessor<T> implements ISupplier<IFieldValue<T>> {

	private final IClass<?> ownerType;
	private final List<Object> fieldPath;
	private final ObjectAddress address;
	private final IClass<T> fieldType;
	private final boolean force;

	public FieldAccessor(ResolvedField resolvedField) throws ReflectionException {
		this(resolvedField, false);
	}

	public FieldAccessor(ResolvedField resolvedField, boolean force) throws ReflectionException {
		Objects.requireNonNull(resolvedField, "Resolved field cannot be null");
		log.atTrace().log("Creating FieldAccessor for resolved field={}, force={}", resolvedField, force);

		this.ownerType = Objects.requireNonNull(
				((IField) resolvedField.fieldPath().getFirst()).getDeclaringClass(),
				"Owner type cannot be null");
		this.fieldPath = Objects.requireNonNull(resolvedField.fieldPath(), "Field path cannot be null");
		this.address = Objects.requireNonNull(resolvedField.address(), "Address cannot be null");
		this.fieldType = (IClass<T>) ((IField) fieldPath.getLast()).getType();
		this.force = force;
		log.atDebug().log("FieldAccessor initialized for ownerType={}, address={}, force={}", ownerType.getName(), address, force);
	}

	// ──────────────────────────────────────────────────────────────
	// GET
	// ──────────────────────────────────────────────────────────────

	public IFieldValue<T> getValue(Object object) throws ReflectionException {
		log.atTrace().log("getValue entry: object={}, ownerType={}, address={}", object, this.ownerType, this.address);
		validateObject(object);

		if (fieldPath.size() == 1) {
			IField field = (IField) fieldPath.get(0);
			String fieldName = this.address.getElement(0);
			log.atDebug().log("Single field access: fieldName={}", fieldName);
			checkFieldNameMatch(field, fieldName);
			T value = (T) getFieldValue(object, field);
			log.atDebug().log("Successfully retrieved value for address={}", this.address);
			return SingleFieldValue.of(value, fieldType);
		} else {
			log.atDebug().log("Recursive field access with {} fields", fieldPath.size());
			IFieldValue<T> result = this.getValueRecursively(object, 0, 0);
			log.atDebug().log("Successfully retrieved value for address={}", this.address);
			return result;
		}
	}

	private IFieldValue<T> getValueRecursively(Object object, int addressIndex, int fieldIndex) throws ReflectionException {
		boolean isLastIteration = (fieldIndex + 2 == this.fieldPath.size());
		log.atTrace().log("getValueRecursively: addressIndex={}, fieldIndex={}, lastIteration={}", addressIndex, fieldIndex, isLastIteration);
		String fieldName = this.address.getElement(addressIndex);
		String nextFieldName = this.address.getElement(addressIndex + 1);
		IField field = (IField) this.fieldPath.get(fieldIndex);
		IField nextField = (IField) this.fieldPath.get(fieldIndex + 1);

		Object temp = getFieldValue(object, field);
		if (temp == null) {
			log.atDebug().log("Field {} value is null, returning null", fieldName);
			return SingleFieldValue.of(null, fieldType);
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.atDebug().log("Field {} is array/map/collection type", fieldName);
			List<SingleFieldValue<T>> collected = new ArrayList<>();
			doGetIfIsCollection(collected, field, nextField, temp, isLastIteration, addressIndex, fieldIndex);
			doGetIfIsMap(collected, nextFieldName, field, nextField, temp, isLastIteration, fieldName, addressIndex, fieldIndex);
			doGetIfIsArray(collected, field, nextField, temp, isLastIteration, addressIndex, fieldIndex);
			return MultipleFieldValue.ofValues(fieldType, collected);
		} else {
			if (isLastIteration) {
				log.atDebug().log("Last iteration, getting final field value");
				return SingleFieldValue.of((T) getFieldValue(temp, nextField), fieldType);
			} else {
				return this.getValueRecursively(temp, addressIndex + 1, fieldIndex + 1);
			}
		}
	}

	private void doGetIfIsArray(List<SingleFieldValue<T>> collected, IField field, IField nextField, Object list,
			boolean isLastIteration, int addressIndex, int fieldIndex) throws ReflectionException {
		if (field.getType().isArray()) {
			log.atDebug().log("Processing array field for get: {}", field.getName());
			Object[] array = (Object[]) list;
			for (Object obj : array) {
				processGetArrayElement(collected, nextField, isLastIteration, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	private void doGetIfIsMap(List<SingleFieldValue<T>> collected, String nextFieldName, IField field, IField nextField, Object list,
			boolean isLastIteration, String fieldName, int addressIndex, int fieldIndex) throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Map.class.isAssignableFrom(rawType)) {
			log.atDebug().log("Processing map field for get: {}, nextFieldName={}", fieldName, nextFieldName);
			Map<Object, Object> map = (Map<Object, Object>) list;
			if (nextFieldName.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				for (Object obj : map.keySet()) {
					processGetArrayElement(collected, nextField, isLastIteration, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else if (nextFieldName.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				for (Object obj : map.values()) {
					processGetArrayElement(collected, nextField, isLastIteration, addressIndex, fieldIndex, obj, 1, 2);
				}
			} else {
				throw new ReflectionException("Invalid address, " + nextFieldName + " should be either #key or #value");
			}
		}
	}

	private void doGetIfIsCollection(List<SingleFieldValue<T>> collected, IField field, IField nextField, Object list,
			boolean isLastIteration, int addressIndex, int fieldIndex) throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Collection.class.isAssignableFrom(rawType)) {
			log.atDebug().log("Processing collection field for get: {}", field.getName());
			Collection<Object> collection = (Collection<Object>) list;
			for (Object obj : collection) {
				processGetArrayElement(collected, nextField, isLastIteration, addressIndex, fieldIndex, obj, 1, 1);
			}
		}
	}

	private void processGetArrayElement(List<SingleFieldValue<T>> collected, IField nextField, boolean isLastIteration,
			int addressIndex, int fieldIndex, Object obj, int fieldIncrement, int addressIncrement)
			throws ReflectionException {
		if (isLastIteration) {
			collected.add(SingleFieldValue.of((T) getFieldValue(obj, nextField), fieldType));
		} else {
			IFieldValue<T> sub = this.getValueRecursively(obj, addressIndex + addressIncrement, fieldIndex + fieldIncrement);
			collectResults(sub, collected);
		}
	}

	private void collectResults(IFieldValue<T> result, List<SingleFieldValue<T>> collected) {
		if (result.isSingle()) {
			if (result.hasException()) {
				collected.add(SingleFieldValue.ofException(result.getException(), fieldType));
			} else {
				collected.add(SingleFieldValue.of(result.single(), fieldType));
			}
		} else if (result instanceof MultipleFieldValue<T> multiple) {
			collected.addAll(multiple.getValues());
		} else {
			for (T value : result.multiple()) {
				collected.add(SingleFieldValue.of(value, fieldType));
			}
		}
	}

	// ──────────────────────────────────────────────────────────────
	// SET (on existing object)
	// ──────────────────────────────────────────────────────────────

	public IFieldValue<T> setValue(Object object, IFieldValue<T> value) throws ReflectionException {
		log.atTrace().log("setValue entry: object={}, value={}, ownerType={}, address={}", object, value, this.ownerType, this.address);
		validateObject(object);

		if (this.fieldPath.size() == 1) {
			IField field = (IField) this.fieldPath.get(0);
			String fieldName = this.address.getElement(0);
			log.atDebug().log("Direct field assignment: fieldName={}", fieldName);
			checkFieldNameMatch(field, fieldName);
			setFieldValue(object, field, value.first());
		} else {
			log.atDebug().log("Recursive field assignment with {} fields", this.fieldPath.size());
			this.setValueRecursively(object, value.first(), 0, 0);
		}
		log.atDebug().log("Successfully set value for address={}", this.address);
		return value;
	}

	// ──────────────────────────────────────────────────────────────
	// SET (creates new instance)
	// ──────────────────────────────────────────────────────────────

	public IFieldValue<T> setValue(IFieldValue<T> fieldValue) throws ReflectionException {
		log.atTrace().log("setValue(fieldValue) entry: ownerType={}, address={}", this.ownerType, this.address);
		Object newInstance = instantiateNewObject(this.ownerType);
		log.atDebug().log("Created new instance of {}", this.ownerType.getName());
		return this.setValue(newInstance, fieldValue);
	}

	private void setValueRecursively(Object object, Object value, int fieldIndex, int fieldNameIndex)
			throws ReflectionException {
		log.atTrace().log("setValueRecursively: fieldIndex={}, fieldNameIndex={}", fieldIndex, fieldNameIndex);
		boolean isLastIteration = (fieldIndex + 2 == this.fieldPath.size());
		IField field = (IField) this.fieldPath.get(fieldIndex);
		IField nextField = (IField) this.fieldPath.get(fieldIndex + 1);
		String fieldName = this.address.getElement(fieldNameIndex);

		checkFieldNameMatch(field, fieldName);

		Object temp = getFieldValue(object, field);
		if (temp == null) {
			log.atDebug().log("Field {} is null, instantiating new object", fieldName);
			temp = Fields.instanciate(field);
			setFieldValue(object, field, temp);
		}

		if (Fields.isArrayOrMapOrCollectionField(field)) {
			log.atDebug().log("Field {} is array/map/collection type", fieldName);
			doSetIfIsCollection(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			doSetIfIsArray(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
			doSetIfIsMap(object, value, fieldIndex, fieldNameIndex, isLastIteration, field, nextField, temp);
		} else {
			if (isLastIteration) {
				log.atDebug().log("Last iteration, setting final field value");
				setFieldValue(temp, nextField, value);
			} else {
				this.setValueRecursively(temp, value, fieldIndex + 1, fieldNameIndex + 1);
			}
		}
	}

	private void doSetIfIsMap(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, IField field, IField nextField, Object temp)
			throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Map.class.isAssignableFrom(rawType)) {
			log.atDebug().log("Processing map field for set: {}", field.getName());
			Map<Object, Object> collectionTarget = (Map<Object, Object>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					IClass<?> keyType = Fields.getGenericType(field, 0);
					IClass<?> valueType = Fields.getGenericType(field, 1);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.put(
								instantiateNewObject(keyType),
								instantiateNewObject(valueType));
					}
				}
			}

			String nextFieldName = this.address.getElement(fieldNameIndex + 1);
			Iterator<?> it = null;
			if (nextFieldName.equals(ObjectAddress.MAP_KEY_INDICATOR)) {
				it = collectionTarget.keySet().iterator();
			}
			if (nextFieldName.equals(ObjectAddress.MAP_VALUE_INDICATOR)) {
				it = collectionTarget.values().iterator();
			}
			if (it == null) {
				throw new ReflectionException("Invalid address, " + nextFieldName + " should be either #key or #value");
			}
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					setFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 2);
				}
			}
		}
	}

	private void doSetIfIsArray(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, IField field, IField nextField, Object temp)
			throws ReflectionException {
		if (field.getType().isArray()) {
			log.atDebug().log("Processing array field for set: {}", field.getName());
			if (field.getType().getComponentType().isArray()) {
				this.handleMultiDimensionalArray(object, value, fieldIndex, fieldNameIndex, isLastIteration, field,
						nextField, value);
			} else {
				Object[] collectionTarget = (Object[]) temp;
				List<?> collectionSource = ((List<?>) value);

				IClass<?> listObjectType = field.getType().getComponentType();

				if (collectionSource.size() != collectionTarget.length) {
					int nbToCreate = collectionSource.size() - collectionTarget.length;
					if (nbToCreate > 0) {
						collectionTarget = Arrays.copyOf(collectionTarget, collectionTarget.length + nbToCreate);
					}
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget[collectionTarget.length + i - 2] = instantiateNewObject(listObjectType);
					}
					setFieldValue(object, field, collectionTarget);
				}

				for (int i = 0; i < collectionSource.size(); i++) {
					Object tempObject = collectionTarget[i];
					if (isLastIteration) {
						setFieldValue(tempObject, nextField, collectionSource.get(i));
					} else {
						this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
					}
				}
			}
		}
	}

	private void doSetIfIsCollection(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, IField field, IField nextField, Object temp)
			throws ReflectionException {
		Class<?> rawType = (Class<?>) field.getType().getType();
		if (Collection.class.isAssignableFrom(rawType)) {
			log.atDebug().log("Processing collection field for set: {}", field.getName());
			Collection<Object> collectionTarget = (Collection<Object>) temp;
			List<?> collectionSource = ((List<?>) value);

			if (collectionSource.size() != collectionTarget.size()) {
				int nbToCreate = collectionSource.size() - collectionTarget.size();
				if (nbToCreate > 0) {
					IClass<?> listObjectType = Fields.getGenericType(field, 0);
					for (int i = 0; i < nbToCreate; i++) {
						collectionTarget.add(instantiateNewObject(listObjectType));
					}
				}
			}
			Iterator<?> it = collectionTarget.iterator();
			for (int i = 0; i < collectionSource.size(); i++) {
				Object tempObject = it.next();
				if (isLastIteration) {
					setFieldValue(tempObject, nextField, collectionSource.get(i));
				} else {
					this.setValueRecursively(tempObject, collectionSource.get(i), fieldIndex + 1, fieldNameIndex + 1);
				}
			}
		}
	}

	private void handleMultiDimensionalArray(Object object, Object value, int fieldIndex, int fieldNameIndex,
			boolean isLastIteration, IField field, IField nextField, Object fieldValue)
			throws ReflectionException {
		Object[] array = (Object[]) fieldValue;
		Object[] sourceArray = (Object[]) value;

		if (array == null || array.length != sourceArray.length) {
			Class<?> componentType = (Class<?>) field.getType().getComponentType().getType();
			array = (Object[]) Array.newInstance(componentType, sourceArray.length);
			setFieldValue(object, field, array);
		}

		for (int i = 0; i < sourceArray.length; i++) {
			Object subArray = array[i];
			if (subArray == null) {
				Class<?> componentType = (Class<?>) field.getType().getComponentType().getType();
				subArray = Array.newInstance(componentType, sourceArray.length);
				array[i] = subArray;
			}
			if (isLastIteration) {
				Array.set(subArray, i, sourceArray[i]);
			} else {
				handleMultiDimensionalArray(subArray, sourceArray[i], fieldIndex + 1, fieldNameIndex + 1,
						isLastIteration, field, nextField, fieldValue);
			}
		}
	}

	// ──────────────────────────────────────────────────────────────
	// Helpers
	// ──────────────────────────────────────────────────────────────

	private void validateObject(Object object) throws ReflectionException {
		if (object == null) {
			log.atError().log("object parameter is null");
			throw new ReflectionException("object is null");
		}
		if (!this.ownerType.isInstance(object)) {
			log.atError().log("object type {} is not assignable from {}", object.getClass(), this.ownerType);
			throw new ReflectionException("object is not of type " + this.ownerType);
		}
	}

	private static void checkFieldNameMatch(IField field, String fieldName) throws ReflectionException {
		if (!field.getName().equals(fieldName)) {
			log.atError().log("field names mismatch: address={}, field={}", fieldName, field.getName());
			throw new ReflectionException(
					"field names of address " + fieldName + " and fields list " + field.getName() + " do not match");
		}
	}

	private Object getFieldValue(Object object, IField field) throws ReflectionException {
		try (var mgr = new FieldAccessManager(field, this.force)) {
			return field.get(object);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ReflectionException(
					"Cannot get field " + field.getName() + " of object " + object.getClass().getName(), e);
		}
	}

	private void setFieldValue(Object object, IField field, Object value) throws ReflectionException {
		try (var mgr = new FieldAccessManager(field, this.force)) {
			field.set(object, value);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ReflectionException(
					"Cannot set field " + field.getName() + " of object " + object.getClass().getName()
							+ " with value " + value, e);
		}
	}

	private <X> X instantiateNewObject(IClass<X> clazz) throws ReflectionException {
		try {
			var ctor = clazz.getDeclaredConstructor();
			try (var mgr = new ConstructorAccessManager(ctor, this.force)) {
				return ctor.newInstance();
			}
		} catch (Exception e) {
			throw new ReflectionException(
					"Class " + clazz.getSimpleName() + " does not have constructor with no params", e);
		}
	}

	// --- ISupplier<IFieldValue<T>> ---

	@Override
	public Optional<IFieldValue<T>> supply() throws SupplyException {
		try {
			Object instance = instantiateNewObject(this.ownerType);
			IFieldValue<T> result = getValue(instance);
			if (result.hasException()) {
				throw new SupplyException("Field access failed", result.getException());
			}
			return Optional.of(result);
		} catch (ReflectionException e) {
			throw new SupplyException(e);
		}
	}

	@Override
	public Type getSuppliedType() {
		return getSuppliedClass().getType();
	}

	@Override
	public IClass<IFieldValue<T>> getSuppliedClass() {
		return (IClass<IFieldValue<T>>) (IClass<?>) IClass.getClass(IFieldValue.class);
	}
}
