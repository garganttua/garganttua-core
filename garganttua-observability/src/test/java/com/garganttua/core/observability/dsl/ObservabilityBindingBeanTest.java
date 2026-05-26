package com.garganttua.core.observability.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Verifies that {@link ObservabilityBuilder} publishes the freshly-built
 * {@link ObservabilityBinding} into the {@link IInjectionContext} provided
 * as a build-phase dependency, so any user bean can {@code @Inject} it.
 */
@DisplayName("ObservabilityBinding registered as DI bean")
class ObservabilityBindingBeanTest {

    @BeforeAll
    static void wireReflection() throws DslException {
        IClass.setReflection(ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider(), 0)
                .withScanner(new ReflectionsAnnotationScanner(), 0)
                .build());
    }

    @Test
    @DisplayName("After build with IInjectionContextBuilder, binding is queryable as bean")
    void bindingPublishedToContext() throws DslException, DiException {
        IInjectionContextBuilder injCtxBuilder = InjectionContext.builder()
                .autoDetect(false)
                .provide(ReflectionBuilder.builder()
                        .withProvider(new RuntimeReflectionProvider(), 0)
                        .withScanner(new ReflectionsAnnotationScanner(), 0));

        ObservabilityBinding binding = ObservabilityBuilder.create()
                .provide(injCtxBuilder)
                .build();

        IInjectionContext context = injCtxBuilder.build();
        context.onInit().onStart();

        BeanReference<ObservabilityBinding> ref = new BeanReference<>(
                IClass.getClass(ObservabilityBinding.class),
                Optional.of(BeanStrategy.singleton),
                Optional.of("ObservabilityBinding"),
                Set.of());

        Optional<ObservabilityBinding> queried = context.queryBean(
                Predefined.BeanProviders.garganttua.toString(), ref);

        assertTrue(queried.isPresent(),
                "ObservabilityBinding must be registered as a bean in the InjectionContext");
        assertNotNull(queried.get());
        assertSame(binding, queried.get(),
                "Queried bean must be the same instance returned by build()");
        assertEquals(0, queried.get().count(),
                "No source attached yet → no registrations");
    }

    @Test
    @DisplayName("Without an IInjectionContextBuilder, build() still succeeds (registration skipped silently)")
    void worksWithoutContext() throws DslException {
        ObservabilityBinding binding = ObservabilityBuilder.create().build();
        assertNotNull(binding);
        assertEquals(0, binding.count());
    }
}
