package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.garganttua.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.dsl.DslException;
import com.garganttua.injection.beans.BeanFactoryBuilder;
import com.garganttua.injection.beans.BeanProvider;
import com.garganttua.injection.beans.BeanStrategy;
import com.garganttua.injection.beans.IBeanFactoryBuilder;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IBeanProviderBuilder;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.injection.spec.beans.annotation.Property;
import com.garganttua.injection.spec.beans.annotation.Prototype;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class BeanProviderBuilder
        extends AbstractAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider>
        implements IBeanProviderBuilder {

    private Map<Class<?>, IBeanFactoryBuilder<?>> beanFactoryBuilders = new HashMap<>();
    private Set<String> packages = new HashSet<>();

    protected BeanProviderBuilder(IDiContextBuilder link) {
        super(link);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException {
        return (IBeanFactoryBuilder<BeanType>) this.beanFactoryBuilders.computeIfAbsent(beanType,
                key -> new BeanFactoryBuilder<>(key));
    }

    @Override
    protected IBeanProvider doBuild() throws DslException {
        return new BeanProvider(this.beanFactoryBuilders.values().stream().map(value -> {
            try {
                return value.build();
            } catch (DslException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetection() throws DslException {
        this.packages.forEach(package_ -> {

			IInjectableBuilderRegistry registry = new InjectableBuilderRegistry();
			registry.registerFactory(Prototype.class, new PrototypeBuilderFactory(new HashSet<>()));
			registry.registerFactory(Singleton.class, new SingletonBuilderFactory(new HashSet<>()));
			registry.registerFactory(Inject.class, new InjectBuilderFactory(new HashSet<>()));
			registry.registerFactory(Property.class, new PropertyBuilderFactory());

			// 1. Récupère toutes les classes annotées avec @Qualifier
			List<Class<? extends Annotation>> qualifierAnnotations = GGObjectReflectionHelper
					.getClassesWithAnnotation(package_,
							Qualifier.class)
					.stream()
					.filter(qualifierAnnotation -> qualifierAnnotation.getAnnotation(Qualifier.class) != null)
					.map(qualifierAnnotation -> (Class<? extends Annotation>) qualifierAnnotation)
					.collect(Collectors.toList());

			// 2. Récupère toutes les classes annotées avec @Singleton
			GGObjectReflectionHelper.getClassesWithAnnotation(package_,
					Singleton.class).forEach(singletonClass -> {
						try {
							this.beanFactoryBuilders
									.put(singletonClass, this.createBeanFactory(qualifierAnnotations, singletonClass, registry)
											.strategy(BeanStrategy.singleton));
						} catch (DslException e) {
							e.printStackTrace();
						}
					});

			// 3. Récupère toutes les classes annotées avec @Prototype
			GGObjectReflectionHelper.getClassesWithAnnotation(package_,
					Prototype.class).forEach(prototypeClass -> {
						try {
							this.beanFactoryBuilders
									.put(prototypeClass, this.createBeanFactory(qualifierAnnotations, prototypeClass, registry)
											.strategy(BeanStrategy.prototype));
						} catch (DslException e) {
							e.printStackTrace();
						}
					});

			// 4. Récupère toutes les classes annotées avec les annotation qualifier
			qualifierAnnotations.forEach(qualifierAnnotation -> {
				GGObjectReflectionHelper.getClassesWithAnnotation(package_,
						qualifierAnnotation).forEach(prototypeClass -> {
							try {
								this.beanFactoryBuilders
										.put(prototypeClass,
												this.createBeanFactory(qualifierAnnotations, prototypeClass, registry)
														.strategy(BeanStrategy.singleton));
							} catch (DslException e) {
								e.printStackTrace();
							}
						});
			});

		});
    }

    private IBeanFactoryBuilder<?> createBeanFactory(List<Class<? extends Annotation>> qualifierAnnotations,
			Class<?> beanClass, IInjectableBuilderRegistry registry)
			throws DslException {
		Set<Class<? extends Annotation>> classQualifiers = qualifierAnnotations.stream()
				.filter(qualifier -> beanClass
						.isAnnotationPresent((Class<? extends Annotation>) qualifier))
				.map(q -> (Class<? extends Annotation>) q)
				.collect(Collectors.toSet());

		IBeanFactoryBuilder<?> builder = new BeanFactoryBuilder<>(beanClass, registry)
				.autoDetect(true)
				.qualifiers(classQualifiers);

		Named namedAnnotation = beanClass.getAnnotation(Named.class);
		if (namedAnnotation != null && !namedAnnotation.value().isEmpty() && !namedAnnotation.value().isBlank()) {
			builder.name(namedAnnotation.value());
		}

		return builder;
	}

    @Override
    public IBeanProviderBuilder withPackage(String packageName) {
        this.packages.add(packageName);
        return this;
    }

    @Override
    public IBeanProviderBuilder withPackages(String[] packageNames) {
        this.packages.addAll(packages);
        return this;
    }
}
