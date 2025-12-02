package com.garganttua.core.nativve.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes as native image configuration builders.
 *
 * <p>
 * {@code @NativeConfigurationBuilder} is used to identify classes that provide
 * native image reflection configuration. Classes annotated with this annotation
 * are automatically discovered and processed during the native image build to
 * contribute their reflection configuration entries.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * @NativeConfigurationBuilder
 * public class MyNativeConfig implements INativeConfiguration {
 *
 *     @Override
 *     public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
 *         return Set.of(
 *             ReflectionConfigEntryBuilder.forClass(MyService.class)
 *                 .queryAllDeclaredConstructors(true)
 *                 .queryAllDeclaredMethods(true),
 *             ReflectionConfigEntryBuilder.forClass(MyRepository.class)
 *                 .allDeclaredFields(true)
 *                 .fieldsAnnotatedWith(Inject.class)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p>
 * The framework scans for classes with this annotation during the build process
 * and aggregates their configurations into the final reflect-config.json file.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see com.garganttua.core.nativve.INativeConfiguration
 * @see com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeConfigurationBuilder {

}
