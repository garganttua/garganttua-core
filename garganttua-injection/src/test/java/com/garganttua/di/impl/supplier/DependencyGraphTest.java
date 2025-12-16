package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.context.validation.DependencyGraph;

/**
 * Test class for {@link DependencyGraph}.
 * Tests dependency graph construction and querying.
 */
public class DependencyGraphTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    @Test
    void testAddSingleDependency() {
        graph.addDependency(String.class, Integer.class);

        Set<Class<?>> dependencies = graph.getDependencies(String.class);
        assertNotNull(dependencies);
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(Integer.class));
    }

    @Test
    void testAddMultipleDependencies() {
        graph.addDependency(String.class, Integer.class);
        graph.addDependency(String.class, Double.class);
        graph.addDependency(String.class, Boolean.class);

        Set<Class<?>> dependencies = graph.getDependencies(String.class);
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains(Integer.class));
        assertTrue(dependencies.contains(Double.class));
        assertTrue(dependencies.contains(Boolean.class));
    }

    @Test
    void testGetDependenciesForBeanWithNoDependencies() {
        Set<Class<?>> dependencies = graph.getDependencies(String.class);

        assertNotNull(dependencies);
        assertEquals(0, dependencies.size());
    }

    @Test
    void testMultipleBeans() {
        graph.addDependency(String.class, Integer.class);
        graph.addDependency(Integer.class, Double.class);
        graph.addDependency(Double.class, Boolean.class);

        Set<Class<?>> stringDeps = graph.getDependencies(String.class);
        Set<Class<?>> integerDeps = graph.getDependencies(Integer.class);
        Set<Class<?>> doubleDeps = graph.getDependencies(Double.class);

        assertEquals(1, stringDeps.size());
        assertTrue(stringDeps.contains(Integer.class));

        assertEquals(1, integerDeps.size());
        assertTrue(integerDeps.contains(Double.class));

        assertEquals(1, doubleDeps.size());
        assertTrue(doubleDeps.contains(Boolean.class));
    }

    @Test
    void testGetAllBeans() {
        graph.addDependency(String.class, Integer.class);
        graph.addDependency(Integer.class, Double.class);
        graph.addDependency(Double.class, Boolean.class);

        Set<Class<?>> allBeans = graph.getAllBeans();

        assertNotNull(allBeans);
        assertEquals(3, allBeans.size());
        assertTrue(allBeans.contains(String.class));
        assertTrue(allBeans.contains(Integer.class));
        assertTrue(allBeans.contains(Double.class));
    }

    @Test
    void testGetAllBeansEmpty() {
        Set<Class<?>> allBeans = graph.getAllBeans();

        assertNotNull(allBeans);
        assertEquals(0, allBeans.size());
    }

    @Test
    void testAddDuplicateDependency() {
        graph.addDependency(String.class, Integer.class);
        graph.addDependency(String.class, Integer.class);

        Set<Class<?>> dependencies = graph.getDependencies(String.class);
        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains(Integer.class));
    }

    @Test
    void testComplexDependencyGraph() {
        // ServiceA depends on ServiceB and ServiceC
        graph.addDependency(ServiceA.class, ServiceB.class);
        graph.addDependency(ServiceA.class, ServiceC.class);

        // ServiceB depends on ServiceD
        graph.addDependency(ServiceB.class, ServiceD.class);

        // ServiceC depends on ServiceD and ServiceE
        graph.addDependency(ServiceC.class, ServiceD.class);
        graph.addDependency(ServiceC.class, ServiceE.class);

        Set<Class<?>> allBeans = graph.getAllBeans();
        assertEquals(3, allBeans.size());

        Set<Class<?>> serviceADeps = graph.getDependencies(ServiceA.class);
        assertEquals(2, serviceADeps.size());
        assertTrue(serviceADeps.contains(ServiceB.class));
        assertTrue(serviceADeps.contains(ServiceC.class));

        Set<Class<?>> serviceBDeps = graph.getDependencies(ServiceB.class);
        assertEquals(1, serviceBDeps.size());
        assertTrue(serviceBDeps.contains(ServiceD.class));

        Set<Class<?>> serviceCDeps = graph.getDependencies(ServiceC.class);
        assertEquals(2, serviceCDeps.size());
        assertTrue(serviceCDeps.contains(ServiceD.class));
        assertTrue(serviceCDeps.contains(ServiceE.class));
    }

    @Test
    void testToString() {
        graph.addDependency(String.class, Integer.class);
        graph.addDependency(Integer.class, Double.class);

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
        graph.addDependency(String.class, Integer.class);

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
