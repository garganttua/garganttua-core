package com.garganttua.core.configuration.populator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

import com.garganttua.core.configuration.ConfigurationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TypeConverter {

    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> targetType) throws ConfigurationException {
        if (value == null) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return (T) value;
            }

            // Primitives and wrappers
            if (targetType == int.class || targetType == Integer.class) {
                return (T) Integer.valueOf(value);
            }
            if (targetType == long.class || targetType == Long.class) {
                return (T) Long.valueOf(value);
            }
            if (targetType == double.class || targetType == Double.class) {
                return (T) Double.valueOf(value);
            }
            if (targetType == float.class || targetType == Float.class) {
                return (T) Float.valueOf(value);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return (T) Boolean.valueOf(value);
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return (T) Byte.valueOf(value);
            }
            if (targetType == short.class || targetType == Short.class) {
                return (T) Short.valueOf(value);
            }
            if (targetType == char.class || targetType == Character.class) {
                if (value.length() != 1) {
                    throw new ConfigurationException("Cannot convert '" + value + "' to char");
                }
                return (T) Character.valueOf(value.charAt(0));
            }

            // Big numbers
            if (targetType == BigDecimal.class) {
                return (T) new BigDecimal(value);
            }
            if (targetType == BigInteger.class) {
                return (T) new BigInteger(value);
            }

            // Temporal
            if (targetType == Duration.class) {
                return (T) Duration.parse(value);
            }
            if (targetType == Period.class) {
                return (T) Period.parse(value);
            }
            if (targetType == Instant.class) {
                return (T) Instant.parse(value);
            }
            if (targetType == LocalDate.class) {
                return (T) LocalDate.parse(value);
            }
            if (targetType == LocalTime.class) {
                return (T) LocalTime.parse(value);
            }
            if (targetType == LocalDateTime.class) {
                return (T) LocalDateTime.parse(value);
            }

            // IO/Net
            if (targetType == Path.class) {
                return (T) Path.of(value);
            }
            if (targetType == URI.class) {
                return (T) URI.create(value);
            }
            if (targetType == URL.class) {
                return (T) URI.create(value).toURL();
            }

            // UUID
            if (targetType == UUID.class) {
                return (T) UUID.fromString(value);
            }

            // Class
            if (targetType == Class.class) {
                return (T) Class.forName(value);
            }

            // Enum
            if (targetType.isEnum()) {
                return convertEnum(value, targetType);
            }

            throw new ConfigurationException("Unsupported type conversion: String -> " + targetType.getName());
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to convert '" + value + "' to " + targetType.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T convertEnum(String value, Class<T> enumType) {
        return (T) Enum.valueOf((Class<Enum>) enumType, value.toUpperCase());
    }

    public Optional<Class<?>> toPrimitiveWrapper(Class<?> type) {
        if (type == int.class) return Optional.of(Integer.class);
        if (type == long.class) return Optional.of(Long.class);
        if (type == double.class) return Optional.of(Double.class);
        if (type == float.class) return Optional.of(Float.class);
        if (type == boolean.class) return Optional.of(Boolean.class);
        if (type == byte.class) return Optional.of(Byte.class);
        if (type == short.class) return Optional.of(Short.class);
        if (type == char.class) return Optional.of(Character.class);
        return Optional.empty();
    }

    public boolean isConvertible(Class<?> type) {
        return type == String.class
                || type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class
                || type == Duration.class
                || type == Period.class
                || type == Instant.class
                || type == LocalDate.class
                || type == LocalTime.class
                || type == LocalDateTime.class
                || type == Path.class
                || type == URI.class
                || type == URL.class
                || type == UUID.class
                || type == Class.class
                || type.isEnum();
    }
}
