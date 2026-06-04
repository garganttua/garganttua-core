package com.garganttua.core.configuration.binding;

import java.util.LinkedHashMap;
import java.util.Map;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Deeply-nested fixtures exercising the populator at depth 5 while mixing ALL four supported
 * method shapes at different levels:
 * <ul>
 *   <li><b>scalar setter</b> — single-arg, returns the builder (e.g. {@code title}, {@code mode},
 *       {@code threshold}, {@code level}),</li>
 *   <li><b>child-builder opener</b> — no-arg, returns a different builder (e.g. {@code settings},
 *       {@code advanced}, {@code tuning}),</li>
 *   <li><b>keyed-child opener</b> — single key arg (String or IClass), returns a child builder
 *       (e.g. {@code group(String)}, {@code entry(IClass)}),</li>
 *   <li><b>keyed-scalar setter</b> — {@code (String key, String value)}, returns the builder
 *       (e.g. {@code option}, {@code flag}, {@code param}).</li>
 * </ul>
 * It also covers BOTH ascent styles: {@code up()} (ILinkedBuilder) for the deep chain, and
 * {@code and()} (plain IBuilder) for the {@code entry} grandchild. Every setter records into the
 * root's flat sink map so a test can assert that every leaf at every depth landed.
 *
 * <p>Depth chain: Root → group(String) → settings() → advanced() → tuning() (5 levels).</p>
 */
public final class DeepNestingFixtures {

    private DeepNestingFixtures() {
    }

    /** Scalar enum target. */
    public enum Mode {
        fast, safe
    }

    /** Level 0. */
    @ConfigurableBuilder("deep")
    public static final class RootBuilder implements IBuilder<Map<String, String>> {
        final Map<String, String> sink = new LinkedHashMap<>();

        public RootBuilder title(String title) {
            sink.put("title", title);
            return this;
        }

        /** 0-arg flag setter — invoked only when the config value is {@code true}. */
        public RootBuilder verbose() {
            sink.put("verbose", "true");
            return this;
        }

        public GroupBuilder group(String name) {
            return new GroupBuilder(this, name);
        }

        @Override
        public Map<String, String> build() {
            return sink;
        }
    }

    /** Level 1 — opened by a keyed-child opener; ascends via {@code up()}. */
    public static final class GroupBuilder implements ILinkedBuilder<RootBuilder, String> {
        private RootBuilder root;
        final String name;

        GroupBuilder(RootBuilder root, String name) {
            this.root = root;
            this.name = name;
        }

        RootBuilder root() {
            return root;
        }

        public GroupBuilder label(String label) {
            root.sink.put("group." + name + ".label", label);
            return this;
        }

        public SettingsBuilder settings() {
            return new SettingsBuilder(this);
        }

        public EntryBuilder entry(IClass<?> type) {
            return new EntryBuilder(this, type.getName());
        }

        @Override
        public RootBuilder up() {
            return root;
        }

        @Override
        public void setUp(RootBuilder up) {
            this.root = up;
        }

        @Override
        public String build() {
            return name;
        }
    }

    /** Level 2 — opened by a no-arg child-builder opener; mixes a keyed-scalar and a scalar enum. */
    public static final class SettingsBuilder implements ILinkedBuilder<GroupBuilder, String> {
        private GroupBuilder group;

        SettingsBuilder(GroupBuilder group) {
            this.group = group;
        }

        Map<String, String> sink() {
            return group.root().sink;
        }

        String prefix() {
            return "group." + group.name + ".settings";
        }

        public SettingsBuilder mode(Mode mode) {
            sink().put(prefix() + ".mode", mode.name());
            return this;
        }

        public SettingsBuilder option(String key, String value) {
            sink().put(prefix() + ".option." + key, value);
            return this;
        }

        public AdvancedBuilder advanced() {
            return new AdvancedBuilder(this);
        }

        @Override
        public GroupBuilder up() {
            return group;
        }

        @Override
        public void setUp(GroupBuilder up) {
            this.group = up;
        }

        @Override
        public String build() {
            return "settings";
        }
    }

    /** Level 3 — scalar int + keyed-scalar + child opener. */
    public static final class AdvancedBuilder implements ILinkedBuilder<SettingsBuilder, String> {
        private SettingsBuilder settings;

        AdvancedBuilder(SettingsBuilder settings) {
            this.settings = settings;
        }

        SettingsBuilder settings() {
            return settings;
        }

        String prefix() {
            return settings.prefix() + ".advanced";
        }

        public AdvancedBuilder threshold(int threshold) {
            settings.sink().put(prefix() + ".threshold", String.valueOf(threshold));
            return this;
        }

        public AdvancedBuilder flag(String key, String value) {
            settings.sink().put(prefix() + ".flag." + key, value);
            return this;
        }

        public TuningBuilder tuning() {
            return new TuningBuilder(this);
        }

        @Override
        public SettingsBuilder up() {
            return settings;
        }

        @Override
        public void setUp(SettingsBuilder up) {
            this.settings = up;
        }

        @Override
        public String build() {
            return "advanced";
        }
    }

    /** Level 4 (deepest) — scalar + keyed-scalar. */
    public static final class TuningBuilder implements ILinkedBuilder<AdvancedBuilder, String> {
        private AdvancedBuilder advanced;

        TuningBuilder(AdvancedBuilder advanced) {
            this.advanced = advanced;
        }

        public TuningBuilder level(String level) {
            advanced.settings().sink().put(advanced.prefix() + ".tuning.level", level);
            return this;
        }

        public TuningBuilder param(String key, String value) {
            advanced.settings().sink().put(advanced.prefix() + ".tuning.param." + key, value);
            return this;
        }

        @Override
        public AdvancedBuilder up() {
            return advanced;
        }

        @Override
        public void setUp(AdvancedBuilder up) {
            this.advanced = up;
        }

        @Override
        public String build() {
            return "tuning";
        }
    }

    /** Keyed-child grandchild reached via {@code entry(IClass)} — ascends via {@code and()}. */
    public static final class EntryBuilder implements IBuilder<String> {
        private final GroupBuilder group;
        private final String type;

        EntryBuilder(GroupBuilder group, String type) {
            this.group = group;
            this.type = type;
        }

        public EntryBuilder weight(int weight) {
            group.root().sink.put("group." + group.name + ".entry." + type + ".weight", String.valueOf(weight));
            return this;
        }

        public GroupBuilder and() {
            return group;
        }

        @Override
        public String build() {
            return type;
        }
    }
}
