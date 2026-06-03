package com.garganttua.core.configuration.binding;

import java.util.LinkedHashMap;
import java.util.Map;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Public fixtures exercising the populator's keyed child-builder traversal — mirroring the
 * injection DSL shape: parent.{@code provider(String)} → provider.{@code item(IClass)} →
 * item.{@code strategy(enum)}. The provider is an {@link ILinkedBuilder} (ascend via
 * {@code up()}); the item uses {@code and()} (the bean-DSL style). All types are public so the
 * populator can invoke their methods reflectively.
 */
public final class KeyedChildFixtures {

    private KeyedChildFixtures() {
    }

    /** Bean strategy stand-in. */
    public enum Strategy {
        singleton, prototype
    }

    /** A configured item: which class, with which strategy, under which provider. */
    public record Item(String provider, String type, Strategy strategy) {
    }

    /** Root builder: {@code provider(String)} opens a keyed child scope. */
    @ConfigurableBuilder("demo")
    public static final class ParentBuilder implements IBuilder<String> {
        final Map<String, ProviderBuilder> providers = new LinkedHashMap<>();

        public ProviderBuilder provider(String name) {
            return providers.computeIfAbsent(name, n -> new ProviderBuilder(this, n));
        }

        /** All items registered across all providers, in declaration order. */
        public java.util.List<Item> items() {
            return providers.values().stream().flatMap(p -> p.items.stream()).toList();
        }

        @Override
        public String build() {
            return "parent[" + providers.keySet() + "]";
        }
    }

    /** Provider builder: {@code item(IClass)} opens a keyed grandchild; ascends via {@code up()}. */
    public static final class ProviderBuilder implements ILinkedBuilder<ParentBuilder, String> {
        private ParentBuilder parent;
        private final String name;
        final java.util.List<Item> items = new java.util.ArrayList<>();

        ProviderBuilder(ParentBuilder parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public ItemBuilder item(IClass<?> type) {
            return new ItemBuilder(this, type.getName());
        }

        @Override
        public ParentBuilder up() {
            return parent;
        }

        @Override
        public void setUp(ParentBuilder up) {
            this.parent = up;
        }

        @Override
        public String build() {
            return name;
        }
    }

    /**
     * Item builder: {@code strategy(enum)} scalar; ascends via {@code and()} (it is an
     * {@link IBuilder} but NOT an {@link ILinkedBuilder} — mirroring {@code IBeanFactoryBuilder}).
     */
    public static final class ItemBuilder implements IBuilder<String> {
        private final ProviderBuilder provider;
        private final String type;
        private Strategy strategy = Strategy.singleton;

        ItemBuilder(ProviderBuilder provider, String type) {
            this.provider = provider;
            this.type = type;
        }

        public ItemBuilder strategy(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public ProviderBuilder and() {
            this.provider.items.add(new Item(provider.name, type, strategy));
            return this.provider;
        }

        @Override
        public String build() {
            return type;
        }
    }
}
