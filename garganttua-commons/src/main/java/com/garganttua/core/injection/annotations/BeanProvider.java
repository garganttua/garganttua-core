package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.garganttua.core.reflection.annotations.Indexed;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Marks a class as an auto-detectable bean provider for the injection context.
 *
 * <p>
 * The annotated class must implement {@code IBeanProviderBuilder}. During auto-detection,
 * the {@code InjectionContextBuilder} discovers classes annotated with {@code @BeanProvider},
 * instantiates them, and registers them as bean providers under the specified scope name.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @BeanProviderAnnotation("services")
 * public class ServiceBeanProvider extends BeanProviderBuilder {
 *     public ServiceBeanProvider(IInjectionContextBuilder link) {
 *         super(link);
 *     }
 *
 *     @Override
 *     protected void doAutoDetection() {
 *         withPackage("com.myapp.services");
 *     }
 * }
 * }</pre>
 *
 * <p>When auto-detection is enabled on the injection context builder, this provider
 * is automatically registered as if the user had called:</p>
 * <pre>{@code
 * injectionContextBuilder.beanProvider("services", new ServiceBeanProvider(injectionContextBuilder));
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 * @see com.garganttua.core.injection.context.dsl.IBeanProviderBuilder
 * @see com.garganttua.core.injection.context.dsl.IInjectionContextBuilder
 */
@Indexed
@Reflected
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanProvider {

    /**
     * The scope name under which this bean provider is registered.
     *
     * @return the provider scope name
     */
    String scope();

}
