package com.garganttua.reflection;

import com.garganttua.reflection.query.GGObjectQueryFactory;

public class ObjectAccessor {

	@FunctionalInterface
	public interface ThrowingFunction<T, R> {
		R apply(T t) throws GGReflectionException;
	}

	public static <Infos, ReturnedType> ReturnedType getValue(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, GGObjectAddress> getFieldAddressMethod) throws GGReflectionException {
		if (object == null || getInfosClassMethod == null || getFieldAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}

		Infos infos = getInfosClassMethod.apply(object.getClass());
		return ObjectAccessor.getValue(object, infos, getFieldAddressMethod);

	}

	public static <Infos, SetType> void setValue(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, GGObjectAddress> getFieldAddressMethod, SetType value)
			throws GGReflectionException {
		if (object == null || getInfosClassMethod == null || getFieldAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}

		Infos infos = getInfosClassMethod.apply(object.getClass());
		ObjectAccessor.setValue(object, infos, getFieldAddressMethod, value);

	}

	public static <Infos> Object invoke(Object object,
			ThrowingFunction<Class<?>, Infos> getInfosClassMethod,
			ThrowingFunction<Infos, GGObjectAddress> getMethoddAddressMethod, Object... parameters)
			throws GGReflectionException {
		if (object == null || getInfosClassMethod == null || getMethoddAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}
		Infos infos;

		infos = getInfosClassMethod.apply(object.getClass());
		return ObjectAccessor.invoke(object, infos, getMethoddAddressMethod, parameters);

	}

	public static <Infos, ReturnedType> ReturnedType getValue(Object object,
			Infos infosKeeper,
			ThrowingFunction<Infos, GGObjectAddress> getFieldAddressMethod) throws GGReflectionException {
		if (object == null || infosKeeper == null || getFieldAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}

		GGObjectAddress fieldAddress = getFieldAddressMethod.apply(infosKeeper);

		return (ReturnedType) GGObjectQueryFactory.objectQuery(object).getValue(fieldAddress);

	}

	public static <Infos, SetType> void setValue(Object object,
			Infos infosKeeper,
			ThrowingFunction<Infos, GGObjectAddress> getFieldAddressMethod, SetType value)
			throws GGReflectionException {
		if (object == null || infosKeeper == null || getFieldAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}

		GGObjectAddress fieldAddress = getFieldAddressMethod.apply(infosKeeper);

		GGObjectQueryFactory.objectQuery(object).setValue(fieldAddress, value);

	}

	public static <Infos> Object invoke(Object object,
			Infos infosKeeper,
			ThrowingFunction<Infos, GGObjectAddress> getMethoddAddressMethod, Object... parameters)
			throws GGReflectionException {
		if (object == null || infosKeeper == null || getMethoddAddressMethod == null) {
			throw new GGReflectionException("null parameter");
		}

		GGObjectAddress methodAddress = getMethoddAddressMethod.apply(infosKeeper);

		return GGObjectQueryFactory.objectQuery(object).invoke(methodAddress, parameters);

	}
}
