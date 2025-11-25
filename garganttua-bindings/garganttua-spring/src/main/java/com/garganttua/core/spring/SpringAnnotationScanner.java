package com.garganttua.core.spring;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.garganttua.core.reflection.IAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpringAnnotationScanner implements IAnnotationScanner {

    @SuppressWarnings("null")
    @Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getClassesWithAnnotation() with package={} and annotation={}", package_, annotation.getSimpleName());

        List<Class<?>> annotatedClasses = new ArrayList<>();
        log.atDebug().log("Initialized empty list for annotated classes");

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        log.atDebug().log("Created ClassPathScanningCandidateComponentProvider");

        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));
        log.atInfo().log("Added include filter for annotation: {}", annotation.getSimpleName());

        scanner.findCandidateComponents(package_).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                annotatedClasses.add(clazz);
                log.atInfo().log("Found class with annotation {}: {}", annotation.getSimpleName(), clazz.getName());
            } catch (ClassNotFoundException e) {
                log.atWarn().log("Class not found for bean definition: {}", beanDefinition.getBeanClassName(), e);
            }
        });

        log.atTrace().log("Exiting getClassesWithAnnotation(), found {} classes", annotatedClasses.size());
        return annotatedClasses;
    }
}
