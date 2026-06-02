package com.garganttua.core.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IField;
import com.garganttua.core.reflection.IRecordComponent;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ObjectAddress;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.fields.FieldAccessor;
import com.garganttua.core.reflection.fields.ResolvedField;

/**
 * Maps a source object into a {@code record} destination by reading source
 * fields component-by-component and invoking the record's canonical constructor.
 * Extracted from {@code Mapper} to keep that type focused on the regular and
 * reverse mapping flows.
 */
final class RecordMapping {

    private RecordMapping() {
    }

    @SuppressWarnings("unchecked")
    static <destination> destination map(IReflection reflection, IClass<destination> destinationIClass, Object source)
            throws MapperException {
        try {
            IRecordComponent[] components = destinationIClass.getRecordComponents();
            Map<String, Object> values = new LinkedHashMap<>();
            for (IRecordComponent component : components) {
                values.put(component.getName(), defaultValueForClass(component.getType()));
            }
            readSourceValues(reflection, components, source, values);
            return instantiateRecord(destinationIClass, components, values);
        } catch (MapperException e) {
            throw e;
        } catch (Exception e) {
            throw new MapperException("Record mapping failed: " + e.getMessage(), e);
        }
    }

    /** Read each record component's value directly from the source object's matching field. */
    private static void readSourceValues(IReflection reflection, IRecordComponent[] components, Object source,
            Map<String, Object> values) {
        IClass<?> sourceClass = reflection.getClass(source.getClass());
        for (IRecordComponent component : components) {
            String name = component.getName();
            try {
                var sourceFields = reflection.query(sourceClass).find(new ObjectAddress(name));
                if (sourceFields.isEmpty()) {
                    continue;
                }
                IField sourceField = (IField) sourceFields.get(sourceFields.size() - 1);
                FieldAccessor<Object> accessor = new FieldAccessor<>(
                        new ResolvedField(new ObjectAddress(name, false), List.of(sourceField)));
                Object val = accessor.getValue(source).single();
                if (val != null) {
                    values.put(name, val);
                }
            } catch (ReflectionException e) {
                // Field not found in source, keep default
            }
        }
    }

    /** Build the record instance via its canonical constructor using the collected component values. */
    @SuppressWarnings("unchecked")
    private static <destination> destination instantiateRecord(IClass<destination> destinationIClass,
            IRecordComponent[] components, Map<String, Object> values) throws Exception {
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = (Class<?>) components[i].getType().getType();
            args[i] = values.get(components[i].getName());
        }
        java.lang.reflect.Constructor<?> ctor =
                ((Class<?>) destinationIClass.getType()).getDeclaredConstructor(paramTypes);
        ctor.setAccessible(true);
        return (destination) ctor.newInstance(args);
    }

    private static Object defaultValueForClass(IClass<?> type) {
        if (type.isPrimitive()) {
            return switch (type.getName()) {
                case "int" -> 0;
                case "long" -> 0L;
                case "double" -> 0.0;
                case "float" -> 0.0f;
                case "boolean" -> false;
                case "byte" -> (byte) 0;
                case "short" -> (short) 0;
                case "char" -> '\0';
                default -> null;
            };
        }
        return null;
    }
}
