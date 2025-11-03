package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.context.dsl.IBeanInjectableFieldBuilder;
import com.garganttua.core.injection.context.dsl.IBeanPostConstructMethodBinderBuilder;
import com.garganttua.core.reflection.binders.Dependent;
import com.garganttua.core.reflection.binders.IConstructorBinder;

public record BeanDefinition<Bean>(Class<Bean> type, Optional<BeanStrategy> strategy, Optional<String> name,
        Set<Class<? extends Annotation>> qualifiers,
        Optional<IConstructorBinder<Bean>> constructorBinder,
        Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders,
        Set<IBeanInjectableFieldBuilder<?, Bean>> injectableFields) implements Dependent {

    public String effectiveName() {
        if (name.isPresent())
            return name.get();
        return type.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BeanDefinition<?> other))
            return false;
        return Objects.equals(type, other.type) &&
                Objects.equals(strategy, other.strategy) &&
                Objects.equals(effectiveName(), other.effectiveName()) &&
                Objects.equals(qualifiers, other.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, strategy, effectiveName(), qualifiers);
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "type=" + type.getName() +
                ", strategy=" + strategy +
                ", name='" + effectiveName() + '\'' +
                ", qualifiers=" + qualifiers +
                '}';
    }

    public boolean matches(BeanDefinition<?> def) {
        Objects.requireNonNull(def, "BeanDefinition to match cannot be null");

        if (def.type() != null && !def.type().isAssignableFrom(this.type)) {
            return false;
        }

        if (def.name().isPresent() && !def.effectiveName().equals(this.effectiveName())) {
            return false;
        }

        if (def.strategy().isPresent() && !def.strategy().equals(this.strategy)) {
            return false;
        }

        if (def.qualifiers() != null && !def.qualifiers().isEmpty() && !this.qualifiers.containsAll(def.qualifiers())) {
            return false;
        }

        return true;
    }

    public static <Bean> BeanDefinition<Bean> example(Class<Bean> type, Optional<BeanStrategy> strategy,
            Optional<String> name,
            Set<Class<? extends Annotation>> qualifiers) {
        return new BeanDefinition<>(type, strategy, name, qualifiers, null, null, null);
    }

    public Set<Class<?>> getDependencies() {
        Set<Class<?>> dependencies = new HashSet<>();
        this.injectableFields.stream().forEach(f -> {
            dependencies.addAll(f.getDependencies());
        });
        this.constructorBinder.ifPresent(c -> {
            dependencies.addAll(c.getDependencies());
        });
        this.postConstructMethodBinderBuilders.stream().forEach(m -> {
            dependencies.addAll(m.getDependencies());
        });
        return dependencies;
    }

}
