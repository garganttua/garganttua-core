package com.garganttua.core.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Container annotation for {@link ObjectMappingRule}, enabling per-source
 * object-level mapping rules on a single destination class.
 * <p>
 * Users normally do not declare this annotation directly — the compiler synthesizes
 * it when two or more {@code @ObjectMappingRule} annotations are declared on the
 * same class. It is exposed publicly so AOT scanners and indexers can register it.
 *
 * @since 2.0.0-ALPHA02
 */
@Indexed
@Reflected
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ObjectMappingRules {

	/**
	 * @return the object-level mapping rules declared on the annotated class, one per source
	 */
	ObjectMappingRule[] value();
}
