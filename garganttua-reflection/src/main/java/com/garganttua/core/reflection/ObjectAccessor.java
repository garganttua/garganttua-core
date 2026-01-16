package com.garganttua.core.reflection;

import java.util.Arrays;

import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectAccessor {

	@FunctionalInterface
	public interface ThrowingFunction<T, R> {
		R apply(T t) throws ReflectionException;
	}

	public static <Infos, ReturnedType> ReturnedType getValue(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, ObjectAddress> getFieldAddressMethod) throws ReflectionException {
		log.atTrace().log("getValue called with object={}, getInfosClassMethod={}, getFieldAddressMethod={}", object,
				getInfosClassMethod, getFieldAddressMethod);
		if (object == null || getInfosClassMethod == null || getFieldAddressMethod == null) {
			log.atError().log("Null parameter in getValue");
			throw new ReflectionException("null parameter");
		}

		Infos infos = getInfosClassMethod.apply(object.getClass());
		log.atDebug().log("Infos obtained: {}", infos);

		ReturnedType value = ObjectAccessor.getValue(object, infos, getFieldAddressMethod);
		log.atDebug().log("Value obtained: {}", value);
		log.atTrace().log("Exiting getValue");
		return value;
	}

	public static <Infos, SetType> void setValue(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, ObjectAddress> getFieldAddressMethod, SetType value)
			throws ReflectionException {
		log.atTrace().log("setValue called with object={}, getInfosClassMethod={}, getFieldAddressMethod={}, value={}",
				object, getInfosClassMethod, getFieldAddressMethod, value);
		if (object == null || getInfosClassMethod == null || getFieldAddressMethod == null) {
			log.atError().log("Null parameter in setValue");
			throw new ReflectionException("null parameter");
		}

		Infos infos = getInfosClassMethod.apply(object.getClass());
		log.atDebug().log("Infos obtained: {}", infos);

		ObjectAccessor.setValue(object, infos, getFieldAddressMethod, value);
		log.atTrace().log("Exiting setValue");
	}

	public static <Infos, R> R invoke(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, ObjectAddress> getMethodAddressMethod, Class<R> returnType, Object... parameters)
			throws ReflectionException {
		log.atTrace().log(
				"invoke called with object={}, getInfosClassMethod={}, getMethodAddressMethod={}, parameters={}",
				object, getInfosClassMethod, getMethodAddressMethod, Arrays.toString(parameters));
		if (object == null || getInfosClassMethod == null || getMethodAddressMethod == null) {
			log.atError().log("Null parameter in invoke");
			throw new ReflectionException("null parameter");
		}

		Infos infos = getInfosClassMethod.apply(object.getClass());
		log.atDebug().log("Infos obtained: {}", infos);

		R result = ObjectAccessor.invoke(object, infos, getMethodAddressMethod, returnType, parameters);
		log.atDebug().log("Invocation result: {}", result);
		log.atTrace().log("Exiting invoke");
		return result;
	}

	public static <Infos, ReturnedType> ReturnedType getValue(Object object,
			Infos infosKeeper,
			ThrowingFunction<Infos, ObjectAddress> getFieldAddressMethod) throws ReflectionException {
		log.atTrace().log("getValue with infosKeeper called, object={}, infosKeeper={}, getFieldAddressMethod={}",
				object, infosKeeper, getFieldAddressMethod);
		if (object == null || infosKeeper == null || getFieldAddressMethod == null) {
			log.atError().log("Null parameter in getValue with infosKeeper");
			throw new ReflectionException("null parameter");
		}

		ObjectAddress fieldAddress = getFieldAddressMethod.apply(infosKeeper);
		log.atDebug().log("Field address resolved: {}", fieldAddress);

		ReturnedType value = (ReturnedType) ObjectQueryFactory.objectQuery(object).getValue(fieldAddress);
		log.atDebug().log("Value obtained: {}", value);
		log.atTrace().log("Exiting getValue with infosKeeper");
		return value;
	}

	public static <Infos, SetType> void setValue(Object object,
			Infos infosKeeper,
			ThrowingFunction<Infos, ObjectAddress> getFieldAddressMethod, SetType value)
			throws ReflectionException {
		log.atTrace().log(
				"setValue with infosKeeper called, object={}, infosKeeper={}, getFieldAddressMethod={}, value={}",
				object, infosKeeper, getFieldAddressMethod, value);
		if (object == null || infosKeeper == null || getFieldAddressMethod == null) {
			log.atError().log("Null parameter in setValue with infosKeeper");
			throw new ReflectionException("null parameter");
		}

		ObjectAddress fieldAddress = getFieldAddressMethod.apply(infosKeeper);
		log.atDebug().log("Field address resolved: {}", fieldAddress);

		ObjectQueryFactory.objectQuery(object).setValue(fieldAddress, value);
		log.atTrace().log("Exiting setValue with infosKeeper");
	}

	public static <Infos, T, R> R invoke(T object,
			Infos infosKeeper,
			ThrowingFunction<Infos, ObjectAddress> getMethodAddressMethod, Class<R> returnType, Object... parameters)
			throws ReflectionException {
		log.atTrace().log(
				"invoke with infosKeeper called, object={}, infosKeeper={}, getMethodAddressMethod={}, parameters={}",
				object, infosKeeper, getMethodAddressMethod, Arrays.toString(parameters));
		if (object == null || infosKeeper == null || getMethodAddressMethod == null) {
			log.atError().log("Null parameter in invoke with infosKeeper");
			throw new ReflectionException("null parameter");
		}

		ObjectAddress methodAddress = getMethodAddressMethod.apply(infosKeeper);
		log.atDebug().log("Method address resolved: {}", methodAddress);

		IMethodReturn<R> methodReturn = ObjectQueryFactory.objectQuery((Class<T>) object.getClass(), object).invoke(methodAddress, returnType, parameters);

		// Extract the result from IMethodReturn
		R result = null;
		if (methodReturn != null) {
			if (methodReturn.isSingle()) {
				result = methodReturn.single();
			} else if (!methodReturn.isEmpty()) {
				// For multiple results, get the first one
				result = methodReturn.first();
			}
		}

		log.atDebug().log("Invocation result: {}", result);
		log.atTrace().log("Exiting invoke with infosKeeper");
		return result;
	}
}
