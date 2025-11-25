package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.core.reflection.IAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionsAnnotationScanner implements IAnnotationScanner {

	@Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getClassesWithAnnotation(package={}, annotation={})", package_, annotation);

        log.atDebug().log("Initializing Reflections scanner for package '{}'", package_);
        Reflections reflections = new Reflections(package_, Scanners.TypesAnnotated);

        log.atDebug().log("Fetching annotated classes for annotation '{}' in package {}", annotation.getName(), package_);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation, true);

        log.atInfo().log("Found {} classes annotated with '{}' in package {}", annotatedClasses.size(), annotation.getName(), package_);

        List<Class<?>> result = annotatedClasses.stream().collect(Collectors.toList());

        log.atTrace().log("Exiting getClassesWithAnnotation(package={}, annotation={})", package_, annotation);
        return result;
    }

}
