package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as an auto-detectable property provider for the injection context.
 *
 * <p>
 * The annotated class must implement {@code IPropertyProviderBuilder}. During auto-detection,
 * the {@code InjectionContextBuilder} discovers classes annotated with {@code @PropertyProvider},
 * instantiates them, and registers them as property providers under the specified scope name.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @PropertyProviderAnnotation("config")
 * public class AppConfigProvider extends PropertiesFileProviderBuilder {
 *     public AppConfigProvider(IInjectionContextBuilder link) {
 *         super(link);
 *         classpathResource("application.properties");
 *         file("/etc/myapp/override.properties");
 *     }
 * }
 * }</pre>
 *
 * <p>When auto-detection is enabled on the injection context builder, this provider
 * is automatically registered as if the user had called:</p>
 * <pre>{@code
 * injectionContextBuilder.propertyProvider("config", new AppConfigProvider(injectionContextBuilder));
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 * @see com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder
 * @see com.garganttua.core.injection.context.dsl.IInjectionContextBuilder
 */
@Indexed
@Reflected
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyProviderAnnotation {

    /**
     * The scope name under which this property provider is registered.
     *
     * @return the provider scope name
     */
    String value();

}
