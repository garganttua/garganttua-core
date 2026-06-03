package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.dummies.DummyBean;
import com.garganttua.core.injection.functions.InjectionFunctions;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

/**
 * Behaviour tests for {@link InjectionFunctions} expression functions, exercised
 * against a fully-built master {@link InjectionContext}. Covers happy paths,
 * null/blank argument validation, and not-found semantics.
 */
public class InjectionFunctionsBehaviourTest {

    private String propertyValue = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws DslException, LifecycleException {
        IReflectionBuilder rb = ReflectionBuilder.builder()
                .withProvider(new RuntimeReflectionProvider())
                .withScanner(new ReflectionsAnnotationScanner());
        rb.build();
        InjectionContext.builder().provide(rb).withPackage("com.garganttua")
                .propertyProvider(Predefined.PropertyProviders.garganttua.toString())
                .withProperty(IClass.getClass(String.class), "com.garganttua.dummyPropertyInConstructor", propertyValue)
                .up()
                .autoDetect(true)
                .build().onInit().onStart();
    }

    // ---------- getBean ----------

    @Test
    void getBeanByTypeReturnsConfiguredBean() {
        Object bean = InjectionFunctions.getBean(DummyBean.class);
        assertNotNull(bean);
        assertInstanceOf(DummyBean.class, bean);
        assertEquals(propertyValue, ((DummyBean) bean).getValue());
    }

    @Test
    void getBeanNullTypeThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class, () -> InjectionFunctions.getBean(null));
        assertTrue(ex.getMessage().contains("type cannot be null"));
    }

    @Test
    void getBeanUnknownTypeReturnsNull() {
        // a type with no registered factory -> null
        assertNull(InjectionFunctions.getBean(java.util.regex.Pattern.class));
    }

    // ---------- getBeanByRef ----------

    @Test
    void getBeanByRefNullThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class, () -> InjectionFunctions.getBeanByRef(null));
        assertTrue(ex.getMessage().contains("null or blank"));
    }

    @Test
    void getBeanByRefBlankThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.getBeanByRef("   "));
    }

    @Test
    void getBeanByRefResolvesNamedBean() {
        // BeanReference.parse leaves type null, name -> matches by effectiveName "dummyBeanForTest"
        Object bean = InjectionFunctions.getBeanByRef("#dummyBeanForTest");
        assertInstanceOf(DummyBean.class, bean);
    }

    // ---------- getBeans ----------

    @Test
    void getBeansNullTypeThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.getBeans(null));
    }

    @Test
    void getBeansByTypeContainsDummyBean() {
        List<?> beans = InjectionFunctions.getBeans(DummyBean.class);
        assertEquals(1, beans.size());
        assertInstanceOf(DummyBean.class, beans.get(0));
    }

    @Test
    void getBeansUnknownTypeIsEmptyNotNull() {
        List<?> beans = InjectionFunctions.getBeans(java.util.regex.Pattern.class);
        assertNotNull(beans);
        assertTrue(beans.isEmpty());
    }

    // ---------- hasBean ----------

    @Test
    void hasBeanNullReferenceIsFalse() {
        assertFalse(InjectionFunctions.hasBean(null));
    }

    @Test
    void hasBeanByClassTrueForRegistered() {
        assertTrue(InjectionFunctions.hasBean(DummyBean.class));
    }

    @Test
    void hasBeanByClassFalseForUnregistered() {
        assertFalse(InjectionFunctions.hasBean(java.util.regex.Pattern.class));
    }

    @Test
    void hasBeanByRefTrueForNamedBean() {
        assertTrue(InjectionFunctions.hasBean("#dummyBeanForTest"));
    }

    // ---------- provider / counts ----------

    @Test
    void beanProviderCountIsPositive() {
        assertTrue(InjectionFunctions.beanProviderCount() >= 1);
    }

    @Test
    void beanCountIsPositive() {
        assertTrue(InjectionFunctions.beanCount() >= 1,
                "auto-detected DummyBean (and friends) should be counted");
    }

    @Test
    void beanCountInProviderForGarganttuaScope() {
        int count = InjectionFunctions.beanCountInProvider(Predefined.BeanProviders.garganttua.toString());
        assertTrue(count >= 1);
    }

    @Test
    void beanCountInProviderNullThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.beanCountInProvider(null));
    }

    @Test
    void beanCountInProviderBlankThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.beanCountInProvider("  "));
    }

    @Test
    void beanCountInProviderUnknownProviderThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> InjectionFunctions.beanCountInProvider("no-such-provider"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // ---------- properties ----------

    @Test
    void getPropertyReturnsConfiguredValue() {
        Object value = InjectionFunctions.getProperty("com.garganttua.dummyPropertyInConstructor", String.class);
        assertEquals(propertyValue, value);
    }

    @Test
    void getPropertyNullTypeDefaultsToString() {
        Object value = InjectionFunctions.getProperty("com.garganttua.dummyPropertyInConstructor", null);
        assertEquals(propertyValue, value);
    }

    @Test
    void getPropertyNullKeyThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.getProperty(null, String.class));
    }

    @Test
    void getPropertyBlankKeyThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.getProperty("  ", String.class));
    }

    @Test
    void getPropertyUnknownKeyReturnsNull() {
        assertNull(InjectionFunctions.getProperty("absolutely.not.a.real.key", String.class));
    }

    @Test
    void getPropertyStringReturnsValue() {
        assertEquals(propertyValue,
                InjectionFunctions.getPropertyString("com.garganttua.dummyPropertyInConstructor"));
    }

    @Test
    void getPropertyStringMissingReturnsNull() {
        assertNull(InjectionFunctions.getPropertyString("missing.key.here"));
    }

    @Test
    void hasPropertyTrueForExisting() {
        assertTrue(InjectionFunctions.hasProperty("com.garganttua.dummyPropertyInConstructor"));
    }

    @Test
    void hasPropertyFalseForMissing() {
        assertFalse(InjectionFunctions.hasProperty("missing.key.here"));
    }

    @Test
    void hasPropertyNullKeyIsFalse() {
        assertFalse(InjectionFunctions.hasProperty(null));
    }

    @Test
    void hasPropertyBlankKeyIsFalse() {
        assertFalse(InjectionFunctions.hasProperty("   "));
    }

    @Test
    void setPropertyUpdatesMutableProvider() {
        String newVal = UUID.randomUUID().toString();
        InjectionFunctions.setProperty(Predefined.PropertyProviders.garganttua.toString(),
                "com.garganttua.runtimeSetKey", newVal);
        assertEquals(newVal,
                InjectionFunctions.getProperty("com.garganttua.runtimeSetKey", String.class));
    }

    @Test
    void setPropertyNullProviderThrows() {
        assertThrows(ExpressionException.class, () -> InjectionFunctions.setProperty(null, "k", "v"));
    }

    @Test
    void setPropertyNullKeyThrows() {
        assertThrows(ExpressionException.class,
                () -> InjectionFunctions.setProperty(Predefined.PropertyProviders.garganttua.toString(), null, "v"));
    }

    @Test
    void setPropertyNullValueThrows() {
        assertThrows(ExpressionException.class,
                () -> InjectionFunctions.setProperty(Predefined.PropertyProviders.garganttua.toString(), "k", null));
    }

    @Test
    void setPropertyUnknownProviderThrows() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> InjectionFunctions.setProperty("no-such-provider", "k", "v"));
        assertTrue(ex.getMessage().contains("setProperty: failed"));
    }

    @Test
    void propertyProviderCountIsPositive() {
        assertTrue(InjectionFunctions.propertyProviderCount() >= 1);
    }

    // ---------- info ----------

    @Test
    void injectionInfoContainsSummarySections() {
        String info = InjectionFunctions.injectionInfo();
        assertTrue(info.contains("Injection Context Information"));
        assertTrue(info.contains("Bean Providers:"));
        assertTrue(info.contains("Property Providers:"));
        assertTrue(info.contains("Total Beans:"));
    }
}
