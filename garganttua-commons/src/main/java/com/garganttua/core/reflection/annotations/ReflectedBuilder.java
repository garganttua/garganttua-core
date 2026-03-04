package com.garganttua.core.reflection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes that report reflection usage.
 *
 * <p>
 * {@code @ReflectedBuilder} identifies classes that programmatically declare
 * which elements require reflective access at runtime. Classes annotated with
 * this annotation are automatically discovered and their reflection usage
 * contributions are aggregated.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * @ReflectedBuilder
 * public class MyReflectionConfig implements IReflectionUsageReporter {
 *
 *     @Override
 *     public Set<IReflectionConfigurationEntryBuilder> reflectionUsage() {
 *         return Set.of(
 *             ReflectionConfigEntryBuilder.forClass(MyService.class)
 *                 .queryAllDeclaredConstructors(true)
 *                 .queryAllDeclaredMethods(true),
 *             ReflectionConfigEntryBuilder.forClass(MyRepository.class)
 *                 .allDeclaredFields(true)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.reflection.IReflectionUsageReporter
 * @see com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder
 */
@Indexed
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReflectedBuilder {

}
