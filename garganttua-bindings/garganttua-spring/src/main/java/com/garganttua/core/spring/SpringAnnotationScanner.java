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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpringAnnotationScanner implements IAnnotationScanner {

    @SuppressWarnings("null")
    @Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getClassesWithAnnotation() with package={} and annotation={}", package_,
                annotation.getSimpleName());

        List<Class<?>> annotatedClasses = new ArrayList<>();
        log.atDebug().log("Initialized empty list for annotated classes");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        log.atDebug().log("Created ClassPathScanningCandidateComponentProvider");

        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));
        log.atDebug().log("Added include filter for annotation: {}", annotation.getSimpleName());

        scanner.findCandidateComponents(package_).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                annotatedClasses.add(clazz);
                log.atDebug().log("Found class with annotation {}: {}", annotation.getSimpleName(), clazz.getName());
            } catch (ClassNotFoundException e) {
                log.atWarn().log("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.atTrace().log("Exiting getClassesWithAnnotation(), found {} classes", annotatedClasses.size());
        return annotatedClasses;
    }

    @SuppressWarnings("null")
    @Override
    public List<Method> getMethodsWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getMethodsWithAnnotation() with package={} and annotation={}",
                package_, annotation.getSimpleName());

        List<Method> annotatedMethods = new ArrayList<>();
        log.atDebug().log("Initialized empty list for annotated methods");

        // Scanner Spring (sans filtres automatiques)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        log.atDebug().log("Created ClassPathScanningCandidateComponentProvider");

        // Filtre : toutes les classes du package
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        log.atDebug().log("Added include filter to capture all classes");

        // Parcours des classes trouvées
        scanner.findCandidateComponents(package_).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                log.atTrace().log("Inspecting class {}", clazz.getName());

                // Toutes les méthodes déclarées (pas seulement public)
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(annotation)) {
                        annotatedMethods.add(method);
                        log.atDebug().log("Found method with annotation {}: {}.{}",
                                annotation.getSimpleName(), clazz.getName(), method.getName());
                    }
                }

            } catch (ClassNotFoundException e) {
                log.atWarn().log("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.atTrace().log("Exiting getMethodsWithAnnotation(), found {} methods", annotatedMethods.size());
        return annotatedMethods;
    }
}
