package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class DependencySpecBuilderBehaviourTest {

    interface ITestBuilder extends IObservableBuilder<ITestBuilder, Object> {
    }

    @SuppressWarnings("unchecked")
    private static IClass<? extends IObservableBuilder<?, ?>> dep() {
        return (IClass<? extends IObservableBuilder<?, ?>>) (IClass<?>) JdkClass.of(ITestBuilder.class);
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new DependencySpecBuilder(null));
    }

    @Test
    void requireForAutoDetectProducesRequiredAutoDetect() {
        DependencySpec s = new DependencySpecBuilder(dep()).requireForAutoDetect().build();
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertEquals(DependencyKind.BUILT, s.kind());
        assertTrue(s.isRequired());
    }

    @Test
    void useForAutoDetectProducesOptionalAutoDetect() {
        DependencySpec s = new DependencySpecBuilder(dep()).useForAutoDetect().build();
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertTrue(s.isOptional());
    }

    @Test
    void requireForBuildProducesRequiredBuild() {
        DependencySpec s = new DependencySpecBuilder(dep()).requireForBuild().build();
        assertEquals(DependencyStage.BUILD, s.stage());
        assertTrue(s.isRequired());
    }

    @Test
    void useForBuildProducesOptionalBuild() {
        DependencySpec s = new DependencySpecBuilder(dep()).useForBuild().build();
        assertEquals(DependencyStage.BUILD, s.stage());
        assertTrue(s.isOptional());
    }

    @Test
    void getDelegatesToBuild() {
        DependencySpec s = new DependencySpecBuilder(dep()).useForBuild().get();
        assertEquals(DependencyStage.BUILD, s.stage());
    }

    @Test
    void noPhaseConfiguredRejected() {
        assertThrows(IllegalStateException.class, () -> new DependencySpecBuilder(dep()).build());
    }

    @Test
    void bothPhasesConfiguredRejected() {
        assertThrows(IllegalStateException.class,
                () -> new DependencySpecBuilder(dep()).useForAutoDetect().useForBuild().build());
        assertThrows(IllegalStateException.class,
                () -> new DependencySpecBuilder(dep()).requireForAutoDetect().requireForBuild().build());
    }

    @Test
    void lastRequirementWinsWithinSamePhase() {
        // useForAutoDetect then requireForAutoDetect: still auto-detect, required wins
        DependencySpec s = new DependencySpecBuilder(dep())
                .useForAutoDetect().requireForAutoDetect().build();
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertTrue(s.isRequired());
    }
}
