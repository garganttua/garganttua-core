package com.garganttua.core.reflection.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an annotation type for compile-time indexing.
 *
 * <p>
 * When placed on an annotation definition, the annotation processor will
 * generate an index file at compile-time listing all classes and methods
 * annotated with that annotation. This enables near-instantaneous discovery
 * at runtime instead of expensive classpath scanning.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Indexed
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.METHOD)
 * public @interface Expression {
 *     String name();
 * }
 * }</pre>
 *
 * <p>
 * The processor generates index files in {@code META-INF/garganttua/index/}
 * with the fully qualified annotation name as the filename.
 * </p>
 *
 * <h2>Generated Index Format</h2>
 * <p>The index file contains one entry per line:</p>
 * <ul>
 *   <li>For classes: {@code C:fully.qualified.ClassName}</li>
 *   <li>For methods: {@code M:fully.qualified.ClassName#methodName(ParamType1,ParamType2)}</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IAnnotationIndex
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Indexed {
}
