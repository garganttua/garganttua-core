package com.garganttua.core.dsl.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.JdkClass;

class DependencySpecBehaviourTest {

    interface ITestBuilder extends IObservableBuilder<ITestBuilder, Object> {
    }

    @SuppressWarnings("unchecked")
    private static IClass<? extends IObservableBuilder<?, ?>> dep() {
        return (IClass<? extends IObservableBuilder<?, ?>>) (IClass<?>) JdkClass.of(ITestBuilder.class);
    }

    // --- canonical constructor validation ---

    @Test
    void rejectsNullComponents() {
        assertThrows(NullPointerException.class,
                () -> new DependencySpec(null, DependencyStage.BUILD, DependencyKind.BUILT,
                        DependencyRequirement.OPTIONAL));
        assertThrows(NullPointerException.class,
                () -> new DependencySpec(dep(), null, DependencyKind.BUILT, DependencyRequirement.OPTIONAL));
        assertThrows(NullPointerException.class,
                () -> new DependencySpec(dep(), DependencyStage.BUILD, null, DependencyRequirement.OPTIONAL));
        assertThrows(NullPointerException.class,
                () -> new DependencySpec(dep(), DependencyStage.BUILD, DependencyKind.BUILT, null));
    }

    @Test
    void rejectsConfigurationWithBuiltKind() {
        assertThrows(IllegalArgumentException.class,
                () -> new DependencySpec(dep(), DependencyStage.CONFIGURATION, DependencyKind.BUILT,
                        DependencyRequirement.OPTIONAL));
    }

    @Test
    void rejectsObsoletePhaseSpecificRequirements() {
        assertThrows(IllegalArgumentException.class,
                () -> new DependencySpec(dep(), DependencyStage.BUILD, DependencyKind.BUILT,
                        DependencyRequirement.REQUIRED_FOR_AUTO_DETECT));
        assertThrows(IllegalArgumentException.class,
                () -> new DependencySpec(dep(), DependencyStage.BUILD, DependencyKind.BUILT,
                        DependencyRequirement.REQUIRED_FOR_BUILD));
    }

    // --- single-stage factories ---

    @Test
    void configureFactory() {
        DependencySpec s = DependencySpec.configure(dep());
        assertEquals(DependencyStage.CONFIGURATION, s.stage());
        assertEquals(DependencyKind.BUILDER, s.kind());
        assertEquals(DependencyRequirement.OPTIONAL, s.requirement());
        assertTrue(s.isConfiguration());
        assertTrue(s.isBuilderKind());
        assertTrue(s.isOptional());
    }

    @Test
    void requireConfigureFactory() {
        DependencySpec s = DependencySpec.requireConfigure(dep());
        assertEquals(DependencyStage.CONFIGURATION, s.stage());
        assertEquals(DependencyKind.BUILDER, s.kind());
        assertTrue(s.isRequired());
    }

    @Test
    void autoDetectFactory() {
        DependencySpec s = DependencySpec.autoDetect(dep());
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertEquals(DependencyKind.BUILT, s.kind());
        assertTrue(s.isAutoDetect());
        assertTrue(s.isBuiltKind());
        assertTrue(s.isOptional());
    }

    @Test
    void requireAutoDetectFactory() {
        DependencySpec s = DependencySpec.requireAutoDetect(dep());
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertTrue(s.isRequired());
    }

    @Test
    void autoDetectBuilderFactory() {
        DependencySpec s = DependencySpec.autoDetectBuilder(dep());
        assertEquals(DependencyStage.AUTO_DETECT, s.stage());
        assertEquals(DependencyKind.BUILDER, s.kind());
        assertTrue(s.isBuilderKind());
    }

    @Test
    void useFactoryIsBuildBuiltOptional() {
        DependencySpec s = DependencySpec.use(dep());
        assertEquals(DependencyStage.BUILD, s.stage());
        assertEquals(DependencyKind.BUILT, s.kind());
        assertTrue(s.isBuild());
        assertTrue(s.isOptional());
    }

    @Test
    void requireFactoryIsBuildBuiltRequired() {
        DependencySpec s = DependencySpec.require(dep());
        assertEquals(DependencyStage.BUILD, s.stage());
        assertEquals(DependencyKind.BUILT, s.kind());
        assertTrue(s.isRequired());
    }

    @Test
    void useBuilderAndRequireBuilderFactories() {
        DependencySpec u = DependencySpec.useBuilder(dep());
        assertEquals(DependencyKind.BUILDER, u.kind());
        assertTrue(u.isOptional());

        DependencySpec r = DependencySpec.requireBuilder(dep());
        assertEquals(DependencyKind.BUILDER, r.kind());
        assertTrue(r.isRequired());
        assertEquals(DependencyStage.BUILD, r.stage());
    }

    // --- configureAndStage ---

    @Test
    void configureAndStageProducesTwoEntries() {
        Set<DependencySpec> specs = DependencySpec.configureAndStage(dep(),
                DependencyStage.BUILD, DependencyKind.BUILT, DependencyRequirement.REQUIRED);
        assertEquals(2, specs.size());
        assertTrue(specs.stream().anyMatch(s -> s.stage() == DependencyStage.CONFIGURATION
                && s.kind() == DependencyKind.BUILDER));
        assertTrue(specs.stream().anyMatch(s -> s.stage() == DependencyStage.BUILD
                && s.kind() == DependencyKind.BUILT));
        assertTrue(specs.stream().allMatch(DependencySpec::isRequired));
    }

    // --- of() builder entry point ---

    @Test
    void ofReturnsBuilder() {
        DependencySpec s = DependencySpec.of(dep()).useForBuild().build();
        assertEquals(DependencyStage.BUILD, s.stage());
        assertTrue(s.isOptional());
    }

    // --- predicates exhaustive ---

    @Test
    void predicatesAreMutuallyConsistent() {
        DependencySpec s = DependencySpec.require(dep());
        assertFalse(s.isConfiguration());
        assertFalse(s.isAutoDetect());
        assertTrue(s.isBuild());
        assertFalse(s.isBuilderKind());
        assertTrue(s.isBuiltKind());
        assertTrue(s.isRequired());
        assertFalse(s.isOptional());
    }

    // --- fromAnnotations null branch ---

    @Test
    void fromAnnotationsNullClassReturnsEmpty() {
        assertTrue(DependencySpec.fromAnnotations(null).isEmpty());
    }

    @Test
    void fromAnnotationsUnannotatedClassReturnsEmpty() {
        assertTrue(DependencySpec.fromAnnotations(String.class).isEmpty());
    }

    // --- legacy deprecated surface ---

    @Test
    @SuppressWarnings("deprecation")
    void legacyPhaseMapping() {
        assertEquals(DependencyPhase.AUTO_DETECT, DependencySpec.configure(dep()).phase());
        assertEquals(DependencyPhase.AUTO_DETECT, DependencySpec.autoDetect(dep()).phase());
        assertEquals(DependencyPhase.BUILD, DependencySpec.use(dep()).phase());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyNeededPredicates() {
        assertTrue(DependencySpec.configure(dep()).isNeededForAutoDetect());
        assertTrue(DependencySpec.autoDetect(dep()).isNeededForAutoDetect());
        assertFalse(DependencySpec.use(dep()).isNeededForAutoDetect());
        assertTrue(DependencySpec.use(dep()).isNeededForBuild());
        assertFalse(DependencySpec.autoDetect(dep()).isNeededForBuild());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyRequiredOptionalPredicates() {
        DependencySpec reqBuild = DependencySpec.require(dep());
        assertTrue(reqBuild.isRequiredForBuild());
        assertFalse(reqBuild.isRequiredForAutoDetect());
        assertFalse(reqBuild.isOptionalForBuild());

        DependencySpec optAuto = DependencySpec.autoDetect(dep());
        assertTrue(optAuto.isOptionalForAutoDetect());
        assertFalse(optAuto.isRequiredForAutoDetect());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyUseAndRequireWithPhase() {
        DependencySpec u = DependencySpec.use(dep(), DependencyPhase.AUTO_DETECT);
        assertEquals(DependencyStage.AUTO_DETECT, u.stage());
        assertTrue(u.isOptional());

        DependencySpec r = DependencySpec.require(dep(), DependencyPhase.BUILD);
        assertEquals(DependencyStage.BUILD, r.stage());
        assertTrue(r.isRequired());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyBothPhaseRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> DependencySpec.use(dep(), DependencyPhase.BOTH));
        assertThrows(IllegalArgumentException.class,
                () -> DependencySpec.require(dep(), DependencyPhase.BOTH));
    }
}
