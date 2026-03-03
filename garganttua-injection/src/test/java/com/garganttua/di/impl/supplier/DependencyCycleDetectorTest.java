package com.garganttua.di.impl.supplier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.context.validation.DependencyCycleDetector;
import com.garganttua.core.injection.context.validation.DependencyGraph;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflection.runtime.RuntimeReflectionProvider;

class DependencyCycleDetectorTest {

    private DependencyCycleDetector detector;
    private DependencyGraph graph;

    // Classes factices pour représenter les beans
    static class A {
    }

    static class B {
    }

    static class C {
    }

    static class D {
    }

    @BeforeEach
    void setUp() {
        ReflectionBuilder.builder().withProvider(new RuntimeReflectionProvider()).build();
        detector = new DependencyCycleDetector();
        graph = new DependencyGraph();
    }

    @Test
    void test_NoCycle_ShouldPass() {
        // A -> B -> C (pas de cycle)
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(C.class));
        graph.addDependency(IClass.getClass(C.class), IClass.getClass(D.class));

        assertDoesNotThrow(() -> detector.detectCycles(graph),
                "Un graphe sans cycle ne doit pas lever d’exception");
    }

    @Test
    void test_SimpleCycle_ShouldThrow() {
        // A -> B -> A
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(A.class));

        DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(graph));
        assertTrue(ex.getMessage().contains("Circular dependency detected"),
                "Le message doit indiquer une dépendance circulaire");
        assertTrue(ex.getMessage().contains("A") && ex.getMessage().contains("B"),
                "Le message doit mentionner les classes impliquées dans le cycle");
    }

    @Test
    void test_ComplexCycle_ShouldThrow() {
        // A -> B -> C -> A
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(C.class));
        graph.addDependency(IClass.getClass(C.class), IClass.getClass(A.class));

        DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(graph));
        assertTrue(ex.getMessage().contains("Circular dependency detected"),
                "Le message doit indiquer un cycle complexe détecté");
        assertTrue(ex.getMessage().contains("A -> B -> C -> A"),
                "Le message doit décrire correctement le cycle");
    }

    @Test
    void test_SelfDependency_ShouldThrow() {
        // A -> A (auto-cycle)
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(A.class));

        DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(graph));
        assertTrue(ex.getMessage().contains("A"),
                "Un bean qui dépend de lui-même doit être détecté comme un cycle");
    }

    @Test
    void test_PartialGraphWithCycle_ShouldThrow() {
        // A -> B -> C (pas de cycle)
        // D -> E -> D (cycle)
        class E {
        }
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(C.class));
        graph.addDependency(IClass.getClass(D.class), IClass.getClass(E.class));
        graph.addDependency(IClass.getClass(E.class), IClass.getClass(D.class));

        DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(graph));
        assertTrue(ex.getMessage().contains("Circular dependency detected"),
                "Un cycle dans une partie du graphe doit être détecté");
    }

    @Test
    void test_EmptyGraph_ShouldPass() {
        assertDoesNotThrow(() -> detector.detectCycles(graph),
                "Un graphe vide ne doit pas lever d’exception");
    }

    @Test
    void test_DisconnectedGraphsWithoutCycle_ShouldPass() {
        // Graphe 1 : A -> B
        // Graphe 2 : C -> D
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(C.class), IClass.getClass(D.class));

        assertDoesNotThrow(() -> detector.detectCycles(graph),
                "Deux graphes sans cycle ne doivent pas lever d’exception");
    }

    @Test
    void test_GraphWithLeafNodes_ShouldPass() {
        // A -> B, C sans dépendance
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(C.class));
        // D n’a pas de dépendance mais est présent dans le graphe
        graph.addDependency(IClass.getClass(D.class), IClass.getClass(Set.of().getClass()));

        assertDoesNotThrow(() -> detector.detectCycles(graph),
                "La présence de feuilles sans dépendances ne doit pas causer d’erreur");
    }

    @Test
    void test_ExactCycleMessage_ShouldMatch() {
        // Arrange
        DependencyGraph graph = new DependencyGraph();
        graph.addDependency(IClass.getClass(A.class), IClass.getClass(B.class));
        graph.addDependency(IClass.getClass(B.class), IClass.getClass(C.class));
        graph.addDependency(IClass.getClass(C.class), IClass.getClass(A.class));

        DependencyCycleDetector detector = new DependencyCycleDetector();

        // Act
        DiException ex = assertThrows(DiException.class, () -> detector.detectCycles(graph));
        String msg = ex.getMessage();

        // Assert
        assertTrue(msg.contains("A -> B -> C -> A"),
                () -> "Expected cycle message to contain ‘A -> B -> C -> A’ but got: " + msg);
    }
}
