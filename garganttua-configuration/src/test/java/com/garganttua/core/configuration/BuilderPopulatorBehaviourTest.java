package com.garganttua.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.configuration.annotations.ConfigIgnore;
import com.garganttua.core.configuration.annotations.ConfigProperty;
import com.garganttua.core.configuration.format.JsonConfigurationFormat;
import com.garganttua.core.configuration.populator.BuilderPopulator;
import com.garganttua.core.configuration.populator.MethodMappingStrategy;
import com.garganttua.core.configuration.source.StringConfigurationSource;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.reflection.JdkReflectionProvider;
import com.garganttua.core.reflection.dsl.ReflectionBuilder;

/**
 * Behaviour tests for {@link BuilderPopulator}: nested builders, arrays into List/array params,
 * type conversion of scalars, no-arg flag methods, annotation mapping, format resolution failures.
 */
class BuilderPopulatorBehaviourTest {

    @BeforeAll
    static void setUpReflection() throws Exception {
        ReflectionBuilder.builder().withProvider(new JdkReflectionProvider()).build();
    }

    private BuilderPopulator populator;

    @BeforeEach
    void setUp() {
        this.populator = new BuilderPopulator(
                List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, false);
    }

    // ---------- scalar type coercion ----------

    public static class TypedBuilder implements IBuilder<String> {
        long count;
        double ratio;
        boolean enabled;

        public TypedBuilder count(long count) { this.count = count; return this; }
        public TypedBuilder ratio(double ratio) { this.ratio = ratio; return this; }
        public TypedBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }

        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void scalarsAreConvertedToDeclaredParamTypes() throws Exception {
        var json = "{\"count\": 1000000, \"ratio\": 2.5, \"enabled\": true}";
        var b = new TypedBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals(1000000L, b.count);
        assertEquals(2.5, b.ratio, 0.0001);
        assertTrue(b.enabled);
    }

    // ---------- array into List<> and into array param ----------

    public static class ListBuilder implements IBuilder<String> {
        List<?> received;
        public ListBuilder values(List<String> values) { this.received = values; return this; }
        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void arrayPassedAsSingleListArgumentWhenMethodTakesList() throws Exception {
        var json = "{\"values\": [\"a\", \"b\", \"c\"]}";
        var b = new ListBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals(List.of("a", "b", "c"), b.received);
    }

    public static class IntArrayBuilder implements IBuilder<String> {
        int[] nums;
        public IntArrayBuilder nums(int[] nums) { this.nums = nums; return this; }
        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void arrayConvertedElementWiseIntoTypedArray() throws Exception {
        var json = "{\"nums\": [\"1\", \"2\", \"3\"]}";
        var b = new IntArrayBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertArrayEquals(new int[]{1, 2, 3}, b.nums);
    }

    // ---------- no-arg flag method ----------
    // A no-arg flag setter (returns void or the builder) IS resolvable and is invoked only when
    // the config value is true — the flag-style branch in BuilderPopulator.handleValueNode.
    // (MethodMapping.isMappable admits no-arg flag setters / child openers; the prior >= 1 rule
    // wrongly excluded them — fixed 2026-06-04.)

    public static class FlagBuilder implements IBuilder<String> {
        boolean called;
        public FlagBuilder activate() { this.called = true; return this; }
        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void noArgFlagMethodIsInvokedWhenValueTrue() throws Exception {
        var b = new FlagBuilder();
        this.populator.populate(b, new StringConfigurationSource("{\"activate\": true}", "json"));
        assertTrue(b.called, "no-arg flag method must be invoked when its config value is true");
    }

    @Test
    void noArgFlagMethodIsNotInvokedWhenValueFalse() throws Exception {
        var b = new FlagBuilder();
        this.populator.populate(b, new StringConfigurationSource("{\"activate\": false}", "json"));
        assertFalse(b.called, "no-arg flag method must NOT be invoked when its config value is false");
    }

    // ---------- nested object: parameterless child-builder accessor ----------
    // A child-builder accessor with zero params (the common DSL idiom, e.g. server()) IS now
    // descended into: the nested object configures the child, then ascends via up().
    // (Previously skipped by the >= 1 param rule — fixed 2026-06-04.)

    public static class RootBuilder implements IBuilder<String> {
        String name;
        final ChildBuilder child = new ChildBuilder(this);
        public RootBuilder name(String n) { this.name = n; return this; }
        public ChildBuilder server() { return this.child; }
        @Override public String build() throws DslException { return "ok"; }
    }

    public static class ChildBuilder implements ILinkedBuilder<RootBuilder, String> {
        final RootBuilder parent;
        String host;
        int port;
        boolean upCalled;
        RootBuilder linkedParent;
        ChildBuilder(RootBuilder parent) { this.parent = parent; }
        public ChildBuilder host(String h) { this.host = h; return this; }
        public ChildBuilder port(int p) { this.port = p; return this; }
        @Override public void setUp(RootBuilder up) { this.linkedParent = up; }
        @Override public RootBuilder up() { this.upCalled = true; return this.parent; }
        @Override public String build() throws DslException { return "child"; }
    }

    @Test
    void parameterlessChildBuilderAccessorIsDescendedInto() throws Exception {
        var json = "{\"name\": \"app\", \"server\": {\"host\": \"localhost\", \"port\": 8080}}";
        var b = new RootBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals("app", b.name);
        // child IS populated: server() (0 params, returns a child builder) is now descended into
        assertEquals("localhost", b.child.host);
        assertEquals(8080, b.child.port);
        assertTrue(b.child.upCalled, "ascent via up() must happen after the child is configured");
    }

    // ---------- @ConfigProperty / @ConfigIgnore ----------

    public static class AnnotatedBuilder implements IBuilder<String> {
        String resolved;
        String ignoredValue;

        @ConfigProperty("db-url")
        public AnnotatedBuilder databaseUrl(String url) { this.resolved = url; return this; }

        @ConfigIgnore
        public AnnotatedBuilder secret(String s) { this.ignoredValue = s; return this; }

        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void configPropertyAnnotationMapsExplicitKey() throws Exception {
        var json = "{\"db-url\": \"jdbc:x\"}";
        var b = new AnnotatedBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals("jdbc:x", b.resolved);
    }

    @Test
    void configIgnoreMethodIsNotInvokedAndIsUnknownInStrict() {
        var strict = new BuilderPopulator(
                List.of(new JsonConfigurationFormat()), MethodMappingStrategy.SMART, true);
        var json = "{\"secret\": \"top\"}";
        var b = new AnnotatedBuilder();
        // @ConfigIgnore removes it from resolution -> unknown key -> strict error
        assertThrows(ConfigurationException.class,
                () -> strict.populate(b, new StringConfigurationSource(json, "json")));
        assertNull(b.ignoredValue);
    }

    // ---------- format resolution ----------

    @Test
    void unresolvableFormatHintThrows() {
        var b = new TypedBuilder();
        var src = new StringConfigurationSource("{}", "xml-not-loaded-zzz");
        var ex = assertThrows(ConfigurationException.class, () -> this.populator.populate(b, src));
        assertTrue(ex.getMessage().contains("No format found"));
    }

    @Test
    void missingFormatHintThrows() {
        var b = new TypedBuilder();
        var src = new StringConfigurationSource("{}", null);
        assertThrows(ConfigurationException.class, () -> this.populator.populate(b, src));
    }

    // ---------- non-object root ----------

    @Test
    void nonObjectRootNodeThrows() throws Exception {
        var format = new JsonConfigurationFormat();
        var node = format.parse(new StringConfigurationSource("[1,2,3]", "json").getInputStream());
        var b = new TypedBuilder();
        var ex = assertThrows(ConfigurationException.class, () -> this.populator.populate(b, node));
        assertTrue(ex.getMessage().contains("Expected OBJECT"));
    }

    // ---------- camelCase / kebab mapping ----------

    public static class CamelBuilder implements IBuilder<String> {
        String maxPoolSize;
        public CamelBuilder maxPoolSize(String v) { this.maxPoolSize = v; return this; }
        @Override public String build() throws DslException { return "ok"; }
    }

    @Test
    void snakeCaseKeyMapsToCamelCaseMethodInSmartMode() throws Exception {
        var json = "{\"max_pool_size\": \"10\"}";
        var b = new CamelBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals("10", b.maxPoolSize);
    }

    @Test
    void kebabCaseKeyMapsToCamelCaseMethodInSmartMode() throws Exception {
        var json = "{\"max-pool-size\": \"20\"}";
        var b = new CamelBuilder();
        this.populator.populate(b, new StringConfigurationSource(json, "json"));
        assertEquals("20", b.maxPoolSize);
    }
}
