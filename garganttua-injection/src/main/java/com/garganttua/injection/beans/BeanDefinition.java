package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.spec.supplier.binder.IConstructorBinder;

public record BeanDefinition<Bean>(Class<Bean> type, Optional<BeanStrategy> strategy, Optional<String> name,
        Set<Class<? extends Annotation>> qualifiers,
        Optional<IConstructorBinder<Bean>> constructorBinder,
        Set<IBeanPostConstructMethodBinderBuilder<Bean>> postConstructMethodBinderBuilders) {

    public String effectiveName() {
        if( name.isPresent() )
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
        return new BeanDefinition<>(type, strategy, name, qualifiers, null, null);
    }

}
