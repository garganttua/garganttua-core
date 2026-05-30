package com.garganttua.core.workflow.aot;

import com.garganttua.core.observability.annotations.Observer;

/**
 * Top-level test observer used by {@link PureAotIntegrationTest} to verify
 * that the index → scanner chain works for top-level @Observer-annotated
 * classes (the dominant real-world pattern). Nested-class coverage lives
 * inside {@code PureAotIntegrationTest$TestObserverBean}.
 */
@Observer
public class TopLevelObserverBean {
    public TopLevelObserverBean() {}
}
