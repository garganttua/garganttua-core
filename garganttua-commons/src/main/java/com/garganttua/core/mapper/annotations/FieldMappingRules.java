package com.garganttua.core.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Container annotation for {@link FieldMappingRule}, enabling per-source field
 * mapping rules on a single destination field.
 * <p>
 * Users normally do not declare this annotation directly — the compiler synthesizes
 * it when two or more {@code @FieldMappingRule} annotations are declared on the
 * same field. It is exposed publicly so AOT scanners and indexers can register it.
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldMappingRules {

	/**
	 * @return the field mapping rules declared on the annotated field, one per source
	 */
	FieldMappingRule[] value();
}
