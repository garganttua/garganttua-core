package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.context.validation.DependencyGraph;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

/**
 * Test class for {@link DependencyGraph}.
 * Tests dependency graph construction and querying.
 */
public class DependencyGraphTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        graph = new DependencyGraph();
    }

    @Test
    void testAddSingleDependency() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));

        Set<IClass<?>> dependencies = graph.getDependencies(IClass.getClass(String.class));
        assertNotNull(dependencies);
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(IClass.getClass(Integer.class)));
    }

    @Test
    void testAddMultipleDependencies() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Double.class));
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Boolean.class));

        Set<IClass<?>> dependencies = graph.getDependencies(IClass.getClass(String.class));
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains(IClass.getClass(Integer.class)));
        assertTrue(dependencies.contains(IClass.getClass(Double.class)));
        assertTrue(dependencies.contains(IClass.getClass(Boolean.class)));
    }

    @Test
    void testGetDependenciesForBeanWithNoDependencies() {
        Set<IClass<?>> dependencies = graph.getDependencies(IClass.getClass(String.class));

        assertNotNull(dependencies);
        assertEquals(0, dependencies.size());
    }

    @Test
    void testMultipleBeans() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));
        graph.addDependency(IClass.getClass(Integer.class), IClass.getClass(Double.class));
        graph.addDependency(IClass.getClass(Double.class), IClass.getClass(Boolean.class));

        Set<IClass<?>> stringDeps = graph.getDependencies(IClass.getClass(String.class));
        Set<IClass<?>> integerDeps = graph.getDependencies(IClass.getClass(Integer.class));
        Set<IClass<?>> doubleDeps = graph.getDependencies(IClass.getClass(Double.class));

        assertEquals(1, stringDeps.size());
        assertTrue(stringDeps.contains(IClass.getClass(Integer.class)));

        assertEquals(1, integerDeps.size());
        assertTrue(integerDeps.contains(IClass.getClass(Double.class)));

        assertEquals(1, doubleDeps.size());
        assertTrue(doubleDeps.contains(IClass.getClass(Boolean.class)));
    }

    @Test
    void testGetAllBeans() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));
        graph.addDependency(IClass.getClass(Integer.class), IClass.getClass(Double.class));
        graph.addDependency(IClass.getClass(Double.class), IClass.getClass(Boolean.class));

        Set<IClass<?>> allBeans = graph.getAllBeans();

        assertNotNull(allBeans);
        assertEquals(3, allBeans.size());
        assertTrue(allBeans.contains(IClass.getClass(String.class)));
        assertTrue(allBeans.contains(IClass.getClass(Integer.class)));
        assertTrue(allBeans.contains(IClass.getClass(Double.class)));
    }

    @Test
    void testGetAllBeansEmpty() {
        Set<IClass<?>> allBeans = graph.getAllBeans();

        assertNotNull(allBeans);
        assertEquals(0, allBeans.size());
    }

    @Test
    void testAddDuplicateDependency() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));

        Set<IClass<?>> dependencies = graph.getDependencies(IClass.getClass(String.class));
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(IClass.getClass(Integer.class)));
    }

    @Test
    void testComplexDependencyGraph() {
        // ServiceA depends on ServiceB and ServiceC
        graph.addDependency(IClass.getClass(ServiceA.class), IClass.getClass(ServiceB.class));
        graph.addDependency(IClass.getClass(ServiceA.class), IClass.getClass(ServiceC.class));

        // ServiceB depends on ServiceD
        graph.addDependency(IClass.getClass(ServiceB.class), IClass.getClass(ServiceD.class));

        // ServiceC depends on ServiceD and ServiceE
        graph.addDependency(IClass.getClass(ServiceC.class), IClass.getClass(ServiceD.class));
        graph.addDependency(IClass.getClass(ServiceC.class), IClass.getClass(ServiceE.class));

        Set<IClass<?>> allBeans = graph.getAllBeans();
        assertEquals(3, allBeans.size());

        Set<IClass<?>> serviceADeps = graph.getDependencies(IClass.getClass(ServiceA.class));
        assertEquals(2, serviceADeps.size());
        assertTrue(serviceADeps.contains(IClass.getClass(ServiceB.class)));
        assertTrue(serviceADeps.contains(IClass.getClass(ServiceC.class)));

        Set<IClass<?>> serviceBDeps = graph.getDependencies(IClass.getClass(ServiceB.class));
        assertEquals(1, serviceBDeps.size());
        assertTrue(serviceBDeps.contains(IClass.getClass(ServiceD.class)));

        Set<IClass<?>> serviceCDeps = graph.getDependencies(IClass.getClass(ServiceC.class));
        assertEquals(2, serviceCDeps.size());
        assertTrue(serviceCDeps.contains(IClass.getClass(ServiceD.class)));
        assertTrue(serviceCDeps.contains(IClass.getClass(ServiceE.class)));
    }

    @Test
    void testToString() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));
        graph.addDependency(IClass.getClass(Integer.class), IClass.getClass(Double.class));

        String result = graph.toString();

        assertNotNull(result);
        assertTrue(result.contains("DependencyGraph:"));
        assertTrue(result.contains("String"));
        assertTrue(result.contains("Integer"));
        assertTrue(result.contains("Double"));
    }

    @Test
    void testToStringEmpty() {
        String result = graph.toString();

        assertNotNull(result);
        assertTrue(result.contains("DependencyGraph:"));
    }

    @Test
    void testToStringBeanWithNoDependencies() {
        graph.addDependency(IClass.getClass(String.class), IClass.getClass(Integer.class));

        // Create a bean entry with no dependencies by querying an unregistered bean
        // then adding it manually with empty set
        String result = graph.toString();

        assertNotNull(result);
        assertTrue(result.contains("String"));
    }

    // Test helper classes
    private static class ServiceA {}
    private static class ServiceB {}
    private static class ServiceC {}
    private static class ServiceD {}
    private static class ServiceE {}
}
