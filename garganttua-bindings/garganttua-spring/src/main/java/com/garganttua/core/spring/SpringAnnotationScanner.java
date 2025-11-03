package com.garganttua.core.spring;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.garganttua.core.reflection.IAnnotationScanner;

public class SpringAnnotationScanner implements IAnnotationScanner {

    @SuppressWarnings("null")
    @Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        List<Class<?>> annotatedClasses = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));

        scanner.findCandidateComponents(package_).forEach(beanDefinition -> {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                annotatedClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        return annotatedClasses;
    }

}
