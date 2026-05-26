package com.garganttua.core.reflection.fields;

import java.util.List;
import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IObjectQuery;
import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.query.ObjectQueryFactory;

public class FieldResolver {
    private static final IDiagnostic log = Diagnostics.of(FieldResolver.class);

        // ========================================================================
        // Provider-based API (preferred)
        // ========================================================================

        public static ResolvedField fieldByFieldName(IClass<?> ownerType, IReflectionProvider provider,
                        String fieldName) throws ReflectionException {
                return fieldByFieldName(ownerType, provider, fieldName, null);
        }

        public static ResolvedField fieldByFieldName(IClass<?> ownerType, IReflectionProvider provider,
                        String fieldName, IClass<?> fieldType) throws ReflectionException {
                log.debug("[fieldByFieldName] Resolving: fieldName={}, fieldType={}, ownerType={}",
                                fieldName, fieldType, ownerType);

                Objects.requireNonNull(fieldName, "Field name cannot be null");
                Objects.requireNonNull(ownerType, "Owner type cannot be null");
                Objects.requireNonNull(provider, "Reflection provider cannot be null");

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);

                ObjectAddress address = query.address(fieldName);
                if (address == null) {
                        log.warn("[fieldByFieldName] Field {} not found in ownerType {}", fieldName,
                                        ownerType.getName());
                        throw new ReflectionException(
                                        "Field " + fieldName + " not found in entity " + ownerType.getName());
                }

                return resolveAndValidate(query, address, ownerType, fieldType);
        }

        public static ResolvedField fieldByField(IClass<?> ownerType, IReflectionProvider provider,
                        IField field) throws ReflectionException {
                return fieldByField(ownerType, provider, field, null);
        }

        public static ResolvedField fieldByField(IClass<?> ownerType, IReflectionProvider provider,
                        IField field, IClass<?> fieldType) throws ReflectionException {
                log.debug("[fieldByField] Resolving: field={}, fieldType={}, ownerType={}", field, fieldType,
                                ownerType);

                Objects.requireNonNull(field, "Field cannot be null");
                Objects.requireNonNull(ownerType, "Owner type cannot be null");
                Objects.requireNonNull(provider, "Reflection provider cannot be null");

                try {
                        ResolvedField resolved = fieldByFieldName(ownerType, provider, field.getName(), fieldType);

                        if (!resolved.matches(field)) {
                                log.error(
                                                "[fieldByField] Field {} in ownerType {} does not match the provided Field object",
                                                field.getName(), ownerType.getName());
                                throw new ReflectionException(
                                                "Field " + field.getName() + " in entity " + ownerType.getName()
                                                                + " does not match the provided Field object");
                        }

                        log.debug("[fieldByField] Successfully resolved field {} in ownerType {}",
                                        field.getName(), ownerType.getName());
                        return resolved;

                } catch (SecurityException | ReflectionException e) {
                        log.error("[fieldByField] Error resolving field {} in ownerType {}", field.getName(),
                                        ownerType.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }

        public static ResolvedField fieldByAddress(IClass<?> ownerType, IReflectionProvider provider,
                        ObjectAddress fieldAddress) throws ReflectionException {
                return fieldByAddress(ownerType, provider, fieldAddress, null);
        }

        public static ResolvedField fieldByAddress(IClass<?> ownerType, IReflectionProvider provider,
                        ObjectAddress fieldAddress, IClass<?> fieldType) throws ReflectionException {
                log.debug("[fieldByAddress] Resolving: fieldAddress={}, fieldType={}, ownerType={}",
                                fieldAddress, fieldType, ownerType);

                Objects.requireNonNull(fieldAddress, "Field address cannot be null");
                Objects.requireNonNull(ownerType, "Owner type cannot be null");
                Objects.requireNonNull(provider, "Reflection provider cannot be null");

                IObjectQuery<?> query = ObjectQueryFactory.objectQuery(ownerType, provider);
                return resolveAndValidate(query, fieldAddress, ownerType, fieldType);
        }

        // ========================================================================
        // Internal
        // ========================================================================

        private static ResolvedField resolveAndValidate(IObjectQuery<?> query, ObjectAddress address,
                        IClass<?> ownerType, IClass<?> fieldType) throws ReflectionException {
                try {
                        List<Object> struct = query.find(address);
                        log.trace("[resolveAndValidate] Object query returned structure: {}", struct);

                        Object leaf = struct.getLast();
                        if (!(leaf instanceof IField field)) {
                                throw new ReflectionException(
                                                "Field " + address + " not found in entity " + ownerType.getName());
                        }

                        if (fieldType != null && !fieldType.isAssignableFrom(field.getType())) {
                                throw new ReflectionException(
                                                "Field " + field.getName() + " in entity " + ownerType.getName()
                                                                + " is not of type " + fieldType.getName());
                        }

                        log.debug("[resolveAndValidate] Successfully resolved field {} in entity {}",
                                        field.getName(), ownerType.getName());
                        return new ResolvedField(address, struct);
                } catch (ReflectionException e) {
                        log.error("[resolveAndValidate] Error resolving field {} in entity {}",
                                        address, ownerType.getName(), e);
                        throw new ReflectionException(e.getMessage(), e);
                }
        }

        private FieldResolver() {
                /* This utility class should not be instantiated */
        }
}
