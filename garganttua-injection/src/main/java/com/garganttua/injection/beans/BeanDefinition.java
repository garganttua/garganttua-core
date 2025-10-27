package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;

public record BeanDefinition<Bean>(Class<Bean> type, BeanStrategy strategy, Optional<String> name,
        Set<Class<? extends Annotation>> qualifiers,
        Optional<IConstructorBinder<Bean>> constructorBinder, Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders) {

    public String effectiveName() {
        return name.orElse(type.getSimpleName());
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
        boolean result = true;
        if (type != null && !type.isAssignableFrom(def.type())) return false;
        if (!effectiveName().equals(def.effectiveName())) return false;
        if (!qualifiers.isEmpty() && !def.qualifiers().containsAll(qualifiers)) return false;
        if (strategy != null && strategy != def.strategy()) return false;

        return result;
    }

}
