package com.garganttua.core.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.nativve.annotations.Native;
import com.garganttua.core.reflection.annotations.Indexed;

/**
 * Marks a field to be excluded from convention-based mapping.
 * <p>
 * When the mapper uses automatic convention mapping (matching fields by name),
 * fields annotated with {@code @MappingIgnore} will be skipped. This is useful
 * for fields that should not participate in mapping even if a same-named field
 * exists in the source class.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * public class UserDTO {
 *     private String name;       // will be mapped by convention
 *     private String email;      // will be mapped by convention
 *
 *     {@literal @}MappingIgnore
 *     private String internalId; // will NOT be mapped
 * }
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Indexed
@Native
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MappingIgnore {
}
