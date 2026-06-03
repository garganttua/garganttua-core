package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.reflection.IReflectionProvider;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.script.nodes.CatchClause;

/**
 * Behaviour tests for {@link CatchClause}: catch-all detection and the
 * {@link CatchClause#matches(Throwable)} resolution (FQCN, simple name, and
 * unloadable-type suffix fallback).
 */
class CatchClauseBehaviourTest {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupReflection() throws Exception {
        Class<? extends IReflectionProvider> providerClass =
                (Class<? extends IReflectionProvider>) Class.forName(
                        "com.garganttua.core.reflection.runtime.RuntimeReflectionProvider");
        IReflectionBuilder reflectionBuilder = ReflectionBuilder.builder()
                .withProvider(providerClass.getDeclaredConstructor().newInstance())
                .withScanner(new ReflectionsAnnotationScanner());
        reflectionBuilder.build();
    }

    @Test
    void nullTypesIsCatchAll() {
        CatchClause cc = new CatchClause(null, null);
        assertTrue(cc.isCatchAll());
        assertTrue(cc.matches(new RuntimeException("x")));
        assertTrue(cc.matches(new Error("e")));
    }

    @Test
    void emptyTypesIsCatchAll() {
        CatchClause cc = new CatchClause(List.of(), null);
        assertTrue(cc.isCatchAll());
        assertTrue(cc.matches(new IllegalStateException()));
    }

    @Test
    void matchesByFullyQualifiedName() {
        CatchClause cc = new CatchClause(List.of("java.lang.IllegalArgumentException"), null);
        assertFalse(cc.isCatchAll());
        assertTrue(cc.matches(new IllegalArgumentException("bad")));
        assertFalse(cc.matches(new IllegalStateException("nope")));
    }

    @Test
    void matchesSubclassOfDeclaredType() {
        // IllegalArgumentException is a subclass of RuntimeException
        CatchClause cc = new CatchClause(List.of("java.lang.RuntimeException"), null);
        assertTrue(cc.matches(new IllegalArgumentException()));
        assertTrue(cc.matches(new NumberFormatException()));
    }

    @Test
    void doesNotMatchUnrelatedType() {
        CatchClause cc = new CatchClause(List.of("java.io.IOException"), null);
        assertFalse(cc.matches(new RuntimeException()));
        assertTrue(cc.matches(new IOException()));
    }

    @Test
    void matchesBySimpleNameWhenTypeCannotBeLoaded() {
        // "RuntimeException" alone is not a loadable FQCN -> falls back to simple-name match
        CatchClause cc = new CatchClause(List.of("RuntimeException"), null);
        assertTrue(cc.matches(new RuntimeException("x")));
        assertFalse(cc.matches(new Exception("plain")));
    }

    @Test
    void matchesByFqcnSuffixWhenTypeCannotBeLoaded() {
        // Unloadable name but is a suffix ".IllegalStateException" of the FQCN
        CatchClause cc = new CatchClause(List.of("lang.IllegalStateException"), null);
        assertTrue(cc.matches(new IllegalStateException()));
    }

    @Test
    void multipleTypesMatchesIfAnyMatches() {
        CatchClause cc = new CatchClause(
                List.of("java.io.IOException", "java.lang.IllegalStateException"), null);
        assertTrue(cc.matches(new IllegalStateException()));
        assertTrue(cc.matches(new IOException()));
        assertFalse(cc.matches(new IllegalArgumentException()));
    }

    @Test
    void codeDefaultsToNull() {
        CatchClause cc = new CatchClause(List.of("X"), null);
        assertNull(cc.code());
    }

    @Test
    void codeIsPreserved() {
        CatchClause cc = new CatchClause(List.of("X"), null, 503);
        assertEquals(503, cc.code());
    }
}
