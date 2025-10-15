package com.garganttua.injection.supplier.builder.binder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import com.garganttua.injection.DiException;
import com.garganttua.reflection.GGObjectAddress;
import com.garganttua.reflection.GGReflectionException;
import com.garganttua.reflection.query.GGObjectQueryFactory;
import com.garganttua.reflection.query.IGGObjectQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldResolver {

        public static GGObjectAddress fieldByFieldName(String fieldName, IGGObjectQuery objectQuery,
                        Class<?> entityClass)
                        throws DiException {
                log.atTrace().log("[fieldByFieldName] Start: fieldName={}, entityClass={}", fieldName, entityClass);
                return FieldResolver.fieldByFieldName(fieldName, objectQuery, entityClass, null);
        }

        public static GGObjectAddress fieldByField(Field field, Class<?> entityClass) throws DiException {
                log.atTrace().log("[fieldByField] Start: field={}, entityClass={}", field, entityClass);
                return FieldResolver.fieldByField(field, entityClass, null);
        }

        public static GGObjectAddress fieldByAddress(GGObjectAddress fieldAddress, IGGObjectQuery objectQuery,
                        Class<?> entityClass) throws DiException {
                log.atTrace().log("[fieldByAddress] Start: fieldAddress={}, entityClass={}", fieldAddress, entityClass);
                return FieldResolver.fieldByAddress(fieldAddress, objectQuery, entityClass, null);
        }

        public static GGObjectAddress fieldByFieldName(String fieldName, IGGObjectQuery objectQuery,
                        Class<?> entityClass,
                        Class<?> fieldType) throws DiException {
                log.atDebug().log("[fieldByFieldName] Resolving: fieldName={}, fieldType={}, entityClass={}",
                                fieldName, fieldType, entityClass);

                Objects.requireNonNull(fieldName, "Field name cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        GGObjectAddress address = objectQuery.address(fieldName);
                        log.atTrace().log("[fieldByFieldName] Resolved GGObjectAddress={} for fieldName={}", address,
                                        fieldName);

                        if (address == null) {
                                log.atWarn().log("[fieldByFieldName] Field {} not found in entity {}", fieldName,
                                                entityClass.getName());
                                throw new DiException(
                                                "Field " + fieldName + " not found in entity " + entityClass.getName());
                        }

                        return FieldResolver.fieldByAddress(address, objectQuery, entityClass, fieldType);
                } catch (GGReflectionException e) {
                        log.atError().log("[fieldByFieldName] Reflection error resolving field {} in entity {}",
                                        fieldName, entityClass.getName(), e);
                        throw new DiException(
                                        "Field " + fieldName + " not found in entity " + entityClass.getName(), e);
                }
        }

        public static GGObjectAddress fieldByField(Field field, Class<?> entityClass, Class<?> fieldType)
                        throws DiException {
                log.atDebug().log("[fieldByField] Resolving: field={}, fieldType={}, entityClass={}", field, fieldType,
                                entityClass);

                Objects.requireNonNull(field, "Field cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                String fieldName = field.getName();

                try {
                        IGGObjectQuery query = GGObjectQueryFactory.objectQuery(entityClass);
                        GGObjectAddress address = FieldResolver.fieldByFieldName(fieldName, query, entityClass);
                        address = FieldResolver.fieldByAddress(address, query, entityClass, fieldType);

                        List<Object> struct = query.find(address);
                        log.atTrace().log("[fieldByField] Object query returned structure: {}", struct);

                        Object leaf = struct.getLast();
                        log.atTrace().log("[fieldByField] Leaf object resolved: {}", leaf);

                        Field fieldFound = (Field) leaf;

                        if (!fieldFound.equals(field)) {
                                log.atError().log(
                                                "[fieldByField] Field {} in entity {} does not match the provided Field object",
                                                fieldName, entityClass.getName());
                                throw new DiException(
                                                "Field " + fieldName + " in entity " + entityClass.getName()
                                                                + " does not match the provided Field object");
                        }

                        log.atInfo().log("[fieldByField] Successfully resolved field {} in entity {}", field.getName(),
                                        entityClass.getName());
                        return address;

                } catch (SecurityException | GGReflectionException e) {
                        log.atError().log("[fieldByField] Error resolving field {} in entity {}", field.getName(),
                                        entityClass.getName(), e);
                        throw new DiException(e.getMessage(), e);
                }
        }

        public static GGObjectAddress fieldByAddress(GGObjectAddress fieldAddress, IGGObjectQuery objectQuery,
                        Class<?> entityClass, Class<?> fieldType) throws DiException {
                log.atDebug().log("[fieldByAddress] Resolving: fieldAddress={}, fieldType={}, entityClass={}",
                                fieldAddress, fieldType, entityClass);

                Objects.requireNonNull(fieldAddress, "Field address cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        List<Object> struct = objectQuery.find(fieldAddress);
                        log.atTrace().log("[fieldByAddress] Object query returned structure: {}", struct);

                        Object leaf = struct.getLast();
                        log.atTrace().log("[fieldByAddress] Leaf object resolved: {}", leaf);

                        if (!(leaf instanceof Field)) {
                                log.atWarn().log("[fieldByAddress] Leaf object {} is not a Field", leaf);
                                throw new DiException(
                                                "Field " + fieldAddress + " not found in entity "
                                                                + entityClass.getName());
                        }

                        Field field = (Field) leaf;

                        if (fieldType != null && !fieldType.isAssignableFrom(field.getType())) {
                                log.atWarn().log("[fieldByAddress] Field {} in entity {} has type {} but expected {}",
                                                field.getName(), entityClass.getName(), field.getType(), fieldType);
                                throw new DiException(
                                                "Field " + field.getName() + " in entity " + entityClass.getName()
                                                                + " is not of type " + fieldType.getName());
                        }

                        log.atInfo().log("[fieldByAddress] Successfully resolved field {} in entity {}",
                                        field.getName(),
                                        entityClass.getName());
                        return fieldAddress;
                } catch (GGReflectionException e) {
                        log.atError().log("[fieldByAddress] Reflection error resolving field {} in entity {}",
                                        fieldAddress,
                                        entityClass.getName(), e);
                        throw new DiException(e.getMessage(), e);
                }
        }
}
