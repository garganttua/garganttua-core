package com.garganttua.core.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.runtime.RuntimeClass;
import com.garganttua.core.reflection.runtime.RuntimeMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpringAnnotationScanner implements IAnnotationScanner {

    @Override
    public List<IClass<?>> getClassesWithAnnotation(IClass<? extends Annotation> annotation) {
        return getClassesWithAnnotation("", annotation);
    }

    @SuppressWarnings("null")
    @Override
    public List<IClass<?>> getClassesWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        log.atTrace().log("Entering getClassesWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

        Class<? extends Annotation> rawAnnotation = unwrapAnnotation(annotation);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(rawAnnotation));

        List<IClass<?>> result = new ArrayList<>();

        scanner.findCandidateComponents(packageName).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                result.add(RuntimeClass.ofUnchecked(clazz));
                log.atDebug().log("Found class with annotation {}: {}", annotation.getName(), clazz.getName());
            } catch (ClassNotFoundException e) {
                log.atWarn().log("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.atTrace().log("Exiting getClassesWithAnnotation(), found {} classes", result.size());
        return result;
    }

    @Override
    public List<IMethod> getMethodsWithAnnotation(IClass<? extends Annotation> annotation) {
        return getMethodsWithAnnotation("", annotation);
    }

    @SuppressWarnings("null")
    @Override
    public List<IMethod> getMethodsWithAnnotation(String packageName, IClass<? extends Annotation> annotation) {
        log.atTrace().log("Entering getMethodsWithAnnotation(package={}, annotation={})", packageName, annotation.getName());

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
                        log.atDebug().log("Found method with annotation {}: {}.{}",
                                annotation.getName(), clazz.getName(), method.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                log.atWarn().log("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.atTrace().log("Exiting getMethodsWithAnnotation(), found {} methods", result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> unwrapAnnotation(IClass<? extends Annotation> annotation) {
        if (annotation instanceof RuntimeClass<?> rc) {
            return (Class<? extends Annotation>) rc.unwrap();
        }
        try {
            return (Class<? extends Annotation>) Class.forName(annotation.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot resolve annotation class: " + annotation.getName(), e);
        }
    }
}
