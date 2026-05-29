package com.garganttua.core.reflection.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflectionProvider;

/**
 * Pure-AOT mode fix: when only one provider is registered and its
 * {@code supports()} returns false for a given type (strict hybrid-contract
 * AOT behaviour), {@link ProviderSelector} must still route the call to that
 * provider so its internal fallback path can engage. Without this, the AOT
 * provider's fallback was unreachable in pure-AOT mode — every unregistered
 * type threw {@code UnsupportedOperationException} before reaching the
 * provider.
 */
class ProviderSelectorFallbackTest {

    @Test
    void single_provider_with_false_supports_still_gets_called() {
        var stub = new StubProvider();
        var selector = new ProviderSelector(List.of(stub));

        IClass<String> result = selector.getClass(String.class);

        assertNotNull(result);
        assertTrue(stub.getClassCalled,
                "Provider.getClass() must be reached even when supports()=false");
    }

    @Test
    void supporter_wins_over_non_supporter() {
        var notSupporter = new StubProvider();
        var supporter = new StubProvider();
        supporter.supportsAlways = true;
        var selector = new ProviderSelector(List.of(notSupporter, supporter));

        IClass<String> result = selector.getClass(String.class);

        assertNotNull(result);
        assertTrue(supporter.getClassCalled, "Supporter must be the one called");
        assertTrue(!notSupporter.getClassCalled,
                "Non-supporter must NOT be called when a supporter is available — "
                + "this preserves hybrid-mode semantics where runtime wins for "
                + "types AOT only knows shallowly.");
    }

    @Test
    void empty_providers_throws_with_helpful_message() {
        var selector = new ProviderSelector(List.of());
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> selector.getClass(String.class));
        assertTrue(ex.getMessage().contains("No IReflectionProvider registered"),
                "Empty provider list must produce an actionable error: " + ex.getMessage());
    }

    /** Stub provider tracking whether getClass was called. */
    static class StubProvider implements IReflectionProvider {
        boolean supportsAlways = false;
        boolean getClassCalled = false;

        @Override
        @SuppressWarnings("unchecked")
        public <T> IClass<T> getClass(Class<T> clazz) {
            getClassCalled = true;
            // Dynamic proxy: only getName() is exercised in routing tests.
            return (IClass<T>) Proxy.newProxyInstance(
                    StubProvider.class.getClassLoader(),
                    new Class<?>[] { IClass.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getName" -> clazz.getName();
                        case "toString" -> "stub(" + clazz.getName() + ")";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        @Override
        public <T> IClass<T> forName(String className) { return null; }

        @Override
        public <T> IClass<T> forName(String className, boolean initialize, ClassLoader loader) {
            return null;
        }

        @Override
        public boolean supports(Class<?> type) { return supportsAlways; }
    }
}
