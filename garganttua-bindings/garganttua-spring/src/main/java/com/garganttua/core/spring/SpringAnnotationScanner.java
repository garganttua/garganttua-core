package com.garganttua.core.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeMethod;

public class SpringAnnotationScanner implements IAnnotationScanner {
    private static final Logger log = Logger.getLogger(SpringAnnotationScanner.class);

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return getClassesWithAnnotation("", annotation);
    }

    @SuppressWarnings("null")
    @Override
    public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        log.trace("Entering getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

        Class<? extends Annotation> rawAnnotation = unwrapAnnotation(annotation);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(rawAnnotation));

        List<IClass<?>> result = new ArrayList<>();

        scanner.findCandidateComponents(packageName).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                result.add(RuntimeClass.ofUnchecked(clazz));
                log.debug("Found class with annotation {}: {}", annotation.getName(), clazz.getName());
            } catch (ClassNotFoundException e) {
                log.warn("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.trace("Exiting getClassesWithAnnotation(), found {} classes", result.size());
        return result;
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return getMethodsWithAnnotation("", annotation);
    }

    @SuppressWarnings("null")
    @Override
    public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        log.trace("Entering getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

        Class<? extends Annotation> rawAnnotation = unwrapAnnotation(annotation);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        List<IMethod> result = new ArrayList<>();

        scanner.findCandidateComponents(packageName).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(rawAnnotation)) {
                        result.add(RuntimeMethod.of(method));
                        log.debug("Found method with annotation {}: {}.{}",
                                annotation.getName(), clazz.getName(), method.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                log.warn("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.trace("Exiting getMethodsWithAnnotation(), found {} methods", result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> unwrapAnnotation(IClass<? extends Annotation> annotation) {
        return (Class<? extends Annotation>) RuntimeClass.unwrapClass(annotation);
    }
}
