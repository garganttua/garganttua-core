package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;

import javax.inject.Qualifier;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.annotations.BeanProvider;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.injection.annotations.PropertyProvider;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;

/**
 * Classpath auto-detection for {@link InjectionContextBuilder}: scans the
 * builder's configured packages for qualifier annotations, {@code @ChildContext}
 * factories, and {@code @BeanProvider}/{@code @PropertyProvider} builders, then
 * propagates the {@link IReflection} to the sub-builders.
 *
 * <p>Extracted from {@code InjectionContextBuilder} to keep that class focused on
 * the builder DSL. Mutates the builder's collectors via its package-private state.
 *
 * @since 2.0.0-ALPHA02
 */
final class InjectionAutoDetector {

    private static final Logger log = Logger.getLogger(InjectionAutoDetector.class);

    private InjectionAutoDetector() {
    }

    @SuppressWarnings("unchecked")
    static void detect(InjectionContextBuilder b, IReflection reflection) throws DslException {
        log.debug("Received IReflection dependency, starting auto-detection");
        b.reflection = reflection;
        detectQualifiers(b, reflection);
        detectChildContextFactories(b, reflection);
        detectBeanProviders(b, reflection);
        detectPropertyProviders(b, reflection);
        propagateReflectionToSubBuilders(b, reflection);
        log.debug("Auto-detection with IReflection completed");
    }

    @SuppressWarnings("unchecked")
    private static void detectQualifiers(InjectionContextBuilder b, IReflection reflection) {
        IClass<? extends Annotation> qualifierAnnotation =
                (IClass<? extends Annotation>) reflection.getClass(Qualifier.class);
        b.packages.stream()
                .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, qualifierAnnotation).stream()
                        .filter(clazz -> clazz.isAnnotationPresent(qualifierAnnotation)))
                .map(clazz -> (IClass<? extends Annotation>) clazz)
                .forEach(q -> b.autoDetectedQualifiers.put(q.getName(), q));
        log.debug("Auto-detected qualifiers: {}", b.autoDetectedQualifiers);
    }

    @SuppressWarnings("unchecked")
    private static void detectChildContextFactories(InjectionContextBuilder b, IReflection reflection) {
        IClass<? extends Annotation> childContextAnnotation =
                (IClass<? extends Annotation>) reflection.getClass(ChildContext.class);
        IClass<?> childContextFactoryInterface = reflection.getClass(IInjectionChildContextFactory.class);
        b.packages.stream()
                .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, childContextAnnotation).stream())
                .forEach(factoryClass -> {
                    try {
                        if (childContextFactoryInterface.isAssignableFrom(factoryClass)) {
                            IInjectionChildContextFactory<? extends IInjectionContext> factory =
                                    (IInjectionChildContextFactory<? extends IInjectionContext>) reflection.newInstance(factoryClass);
                            b.autoDetectedChildContextFactories.put(factoryClass.getName(), factory);
                            log.debug("Auto-registered child context factory: {}", factoryClass.getName());
                        } else {
                            log.warn("Class {} annotated with @ChildContext but does not implement IInjectionChildContextFactory",
                                    factoryClass.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to instantiate child context factory {}: {}",
                                factoryClass.getName(), e.getMessage(), e);
                        throw new DslException("Failed to auto-detect child context factory: " + factoryClass.getName(), e);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static void detectBeanProviders(InjectionContextBuilder b, IReflection reflection) {
        IClass<? extends Annotation> beanProviderAnnotation =
                (IClass<? extends Annotation>) reflection.getClass(BeanProvider.class);
        IClass<?> beanProviderBuilderInterface = reflection.getClass(IBeanProviderBuilder.class);
        b.packages.stream()
                .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, beanProviderAnnotation).stream())
                .forEach(providerClass -> {
                    try {
                        if (beanProviderBuilderInterface.isAssignableFrom(providerClass)) {
                            String scope = ((Class<?>) providerClass.getType()).getAnnotation(BeanProvider.class).scope();
                            IBeanProviderBuilder provider = (IBeanProviderBuilder) reflection.newInstance(providerClass, b);
                            b.beanProvider(scope, provider);
                            log.info("Auto-registered bean provider '{}' from {}", scope, providerClass.getName());
                        } else {
                            log.warn("Class {} annotated with @BeanProviderAnnotation but does not implement IBeanProviderBuilder",
                                    providerClass.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to instantiate bean provider {}: {}", providerClass.getName(), e.getMessage(), e);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static void detectPropertyProviders(InjectionContextBuilder b, IReflection reflection) {
        IClass<? extends Annotation> propertyProviderAnnotation =
                (IClass<? extends Annotation>) reflection.getClass(PropertyProvider.class);
        IClass<?> propertyProviderBuilderInterface = reflection.getClass(IPropertyProviderBuilder.class);
        b.packages.stream()
                .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, propertyProviderAnnotation).stream())
                .forEach(providerClass -> {
                    try {
                        if (propertyProviderBuilderInterface.isAssignableFrom(providerClass)) {
                            String scope = ((Class<?>) providerClass.getType()).getAnnotation(PropertyProvider.class).scope();
                            IPropertyProviderBuilder provider = (IPropertyProviderBuilder) reflection.newInstance(providerClass, b);
                            b.propertyProvider(scope, provider);
                            log.info("Auto-registered property provider '{}' from {}", scope, providerClass.getName());
                        } else {
                            log.warn("Class {} annotated with @PropertyProviderAnnotation but does not implement IPropertyProviderBuilder",
                                    providerClass.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to instantiate property provider {}: {}", providerClass.getName(), e.getMessage(), e);
                    }
                });
    }

    private static void propagateReflectionToSubBuilders(InjectionContextBuilder b, IReflection reflection) {
        b.getAllBeanProviders().values().forEach(bp -> {
            if (bp instanceof BeanProviderBuilder bpb) {
                bpb.setReflection(reflection);
            }
        });
        if (b.resolvers instanceof InjectableElementResolverBuilder ier) {
            ier.setReflection(reflection);
        }
    }
}
