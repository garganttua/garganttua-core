package com.garganttua.core.reflection.fields;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FieldResolver {

        public static ObjectAddress fieldByFieldName(String fieldName, IObjectQuery objectQuery,
                        Class<?> entityClass)
                        throws ReflectionException {
                log.atTrace().log("[fieldByFieldName] Start: fieldName={}, entityClass={}", fieldName, entityClass);
                return FieldResolver.fieldByFieldName(fieldName, objectQuery, entityClass, null);
        }

        public static ObjectAddress fieldByField(Field field, Class<?> entityClass) throws ReflectionException {
                log.atTrace().log("[fieldByField] Start: field={}, entityClass={}", field, entityClass);
                return FieldResolver.fieldByField(field, entityClass, null);
        }

        public static ObjectAddress fieldByAddress(ObjectAddress fieldAddress, IObjectQuery objectQuery,
                        Class<?> entityClass) throws ReflectionException {
                log.atTrace().log("[fieldByAddress] Start: fieldAddress={}, entityClass={}", fieldAddress, entityClass);
                return FieldResolver.fieldByAddress(fieldAddress, objectQuery, entityClass, null);
        }

        public static ObjectAddress fieldByFieldName(String fieldName, IObjectQuery objectQuery,
                        Class<?> entityClass,
                        Class<?> fieldType) throws ReflectionException {
                log.atDebug().log("[fieldByFieldName] Resolving: fieldName={}, fieldType={}, entityClass={}",
                                fieldName, fieldType, entityClass);

                Objects.requireNonNull(fieldName, "Field name cannot be null");
                Objects.requireNonNull(objectQuery, "Object query cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                try {
                        ObjectAddress address = objectQuery.address(fieldName);
                        log.atTrace().log("[fieldByFieldName] Resolved ObjectAddress={} for fieldName={}", address,
                                        fieldName);

                        if (address == null) {
                                log.atWarn().log("[fieldByFieldName] Field {} not found in entity {}", fieldName,
                                                entityClass.getName());
                                throw new ReflectionException(
                                                "Field " + fieldName + " not found in entity " + entityClass.getName());
                        }

                        return FieldResolver.fieldByAddress(address, objectQuery, entityClass, fieldType);
                } catch (ReflectionException e) {
                        log.atError().log("[fieldByFieldName] Reflection error resolving field {} in entity {}",
                                        fieldName, entityClass.getName(), e);
                        throw new ReflectionException(
                                        "Field " + fieldName + " not found in entity " + entityClass.getName(), e);
                }
        }

        public static ObjectAddress fieldByField(Field field, Class<?> entityClass, Class<?> fieldType)
                        throws ReflectionException {
                log.atDebug().log("[fieldByField] Resolving: field={}, fieldType={}, entityClass={}", field, fieldType,
                                entityClass);

                Objects.requireNonNull(field, "Field cannot be null");
                Objects.requireNonNull(entityClass, "Entity class cannot be null");

                String fieldName = field.getName();

                try {
                        IObjectQuery query = ObjectQueryFactory.objectQuery(entityClass);
                        ObjectAddress address = FieldResolver.fieldByFieldName(fieldName, query, entityClass);
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
                                throw new ReflectionException(
                                                "Field " + fieldName + " in entity " + entityClass.getName()
                                                                + " does not match the provided Field object");
                        }

                        log.atDebug().log("[fieldByField] Successfully resolved field {} in entity {}", field.getName(),
                                        entityClass.getName());
                        return address;

                } catch (SecurityException | ReflectionException e) {
                        log.atError().log("[fieldByField] Error resolving field {} in entity {}", field.getName(),
                                        entityClass.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }

        public static ObjectAddress fieldByAddress(ObjectAddress fieldAddress, IObjectQuery objectQuery,
                        Class<?> entityClass, Class<?> fieldType) throws ReflectionException {
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
                                throw new ReflectionException(
                                                "Field " + fieldAddress + " not found in entity "
                                                                + entityClass.getName());
                        }

                        Field field = (Field) leaf;

                        if (fieldType != null && !fieldType.isAssignableFrom(field.getType())) {
                                log.atWarn().log("[fieldByAddress] Field {} in entity {} has type {} but expected {}",
                                                field.getName(), entityClass.getName(), field.getType(), fieldType);
                                throw new ReflectionException(
                                                "Field " + field.getName() + " in entity " + entityClass.getName()
                                                                + " is not of type " + fieldType.getName());
                        }

                        log.atDebug().log("[fieldByAddress] Successfully resolved field {} in entity {}",
                                        field.getName(),
                                        entityClass.getName());
                        return fieldAddress;
                } catch (ReflectionException e) {
                        log.atError().log("[fieldByAddress] Reflection error resolving field {} in entity {}",
                                        fieldAddress,
                                        entityClass.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }
}
