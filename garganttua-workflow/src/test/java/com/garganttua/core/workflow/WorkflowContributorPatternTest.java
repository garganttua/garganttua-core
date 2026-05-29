package com.garganttua.core.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garganttua.core.bootstrap.dsl.Bootstrap;
import com.garganttua.core.bootstrap.dsl.IBuiltRegistry;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyKind;
import com.garganttua.core.dsl.dependency.DependencyRequirement;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.DependencyStage;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.workflow.dsl.IWorkflowsBuilder;

/**
 * End-to-end demonstration of the "contributor" dependency pattern: a third-
 * party module M is Bootstrap-discoverable and uses
 * {@link DependencySpec#configureAndStage} to push a workflow into
 * {@code WorkflowsBuilder} at the CONFIGURATION stage, then receives the
 * {@link WorkflowsRegistry} pre-build to grab its built workflow.
 *
 * <p>Validates the architectural claim made in the design discussion: a module
 * can contribute to an upstream Bootstrap-discoverable builder it doesn't
 * know about, without that upstream builder needing to know about the
 * contributor. The dep graph stays acyclic (M depends on WorkflowsBuilder),
 * but the responsibility is inverted (M acts on WorkflowsBuilder rather than
 * being pulled by it).
 */
@DisplayName("Workflow contributor pattern — configureAndStage(IWorkflowsBuilder)")
class WorkflowContributorPatternTest {

    @BeforeEach
    void clearGlobalReflection() {
        IClass.setReflection(null);
    }

    @AfterEach
    void resetGlobalReflection() {
        IClass.setReflection(null);
    }

    /**
     * The contributor module. Declares the configureAndStage pair on
     * {@link IWorkflowsBuilder}, pushes a workflow during CONFIGURATION, and
     * captures the built {@link WorkflowsRegistry} during PRE_BUILD.
     */
    public static class ContributorBuilder
            extends AbstractAutomaticDependentBuilder<ContributorBuilder, String> {

        private static final Set<DependencySpec> DEPS = DependencySpec.configureAndStage(
                IClass.getClass(IWorkflowsBuilder.class),
                DependencyStage.BUILD,
                DependencyKind.BUILT,
                DependencyRequirement.REQUIRED);

        final AtomicReference<WorkflowsRegistry> capturedRegistry = new AtomicReference<>();
        final AtomicReference<Boolean> contributedInConfigure = new AtomicReference<>(false);

        public ContributorBuilder() {
            super(DEPS);
        }

        @Override
        protected void doConfigureWithDependencyBuilder(IObservableBuilder<?, ?> dep)
                throws DslException {
            if (dep instanceof IWorkflowsBuilder wb) {
                wb.workflow("contrib-workflow")
                        .stage("greet")
                            .script("greeting <- \"hi from contributor module M\"")
                                .output("greeting", "greeting")
                                .up()
                            .up();
                contributedInConfigure.set(true);
            }
        }

        @Override
        protected void doPreBuildWithDependency(Object dep) {
            if (dep instanceof WorkflowsRegistry reg) {
                capturedRegistry.set(reg);
            }
        }

        @Override
        protected void doPostBuildWithDependency(Object dep) {
            // no-op
        }

        @Override
        protected void doAutoDetectionWithDependency(Object dep) {
            // no-op
        }

        @Override
        protected void doAutoDetection() {
            // no-op
        }

        @Override
        protected String doBuild() {
            return "contributor-built";
        }
    }

    @Test
    @DisplayName("M contributes a workflow via CONFIGURATION → it's in the built registry and executable")
    void contributorPushesWorkflowAtConfigurationStage_andReceivesBuiltRegistry() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.autoDetect(true)
                .withPackage("com.garganttua")
                .load();

        ContributorBuilder contributor = new ContributorBuilder();
        bootstrap.withBuilder(contributor);

        IBuiltRegistry registry = bootstrap.build();
        assertNotNull(registry, "Bootstrap.build() must return a registry");

        // 1. CONFIGURATION fired on the contributor with the upstream WorkflowsBuilder
        assertTrue(contributor.contributedInConfigure.get(),
                "doConfigureWithDependencyBuilder(IWorkflowsBuilder) must fire BEFORE WorkflowsBuilder builds");

        // 2. PRE_BUILD fired on the contributor with the built WorkflowsRegistry
        WorkflowsRegistry capturedRegistry = contributor.capturedRegistry.get();
        assertNotNull(capturedRegistry,
                "doPreBuildWithDependency(WorkflowsRegistry) must fire after WorkflowsBuilder.build()");

        // 3. The contributed workflow is present and executable
        IWorkflow contributed = capturedRegistry.get("contrib-workflow");
        assertNotNull(contributed, "The workflow added by M during CONFIGURATION must be in the registry");

        WorkflowResult result = contributed.execute();
        assertTrue(result.isSuccess(),
                "Contributed workflow must execute cleanly; exception=" + result.exception().orElse(null));
        assertEquals(0, result.code());
        assertEquals("hi from contributor module M",
                result.getVariable("greeting", IClass.getClass(String.class)).orElse(null));
    }
}
