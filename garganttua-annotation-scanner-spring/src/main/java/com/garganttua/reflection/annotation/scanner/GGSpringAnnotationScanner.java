package com.garganttua.reflection.annotation.scanner;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.garganttua.reflection.utils.IGGAnnotationScanner;

public class GGSpringAnnotationScanner implements IGGAnnotationScanner {

	@Override
	public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
		List<Class<?>> annotatedClasses = new ArrayList<>();

        // Configure a Spring scanner
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);

        // Add a filter to include classes with the specified annotation
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotation));

        // Find candidate components in the specified package
        scanner.findCandidateComponents(package_).forEach(beanDefinition -> {
            try {
                // Load the class from the bean definition
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                annotatedClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); // Handle or log appropriately
            }
        });

        return annotatedClasses;
	}

}
