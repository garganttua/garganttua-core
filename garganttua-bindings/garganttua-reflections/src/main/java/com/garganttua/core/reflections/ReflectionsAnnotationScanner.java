package com.garganttua.core.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.garganttua.core.reflection.IAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionsAnnotationScanner implements IAnnotationScanner {

	private final Map<String, Reflections> typeAnnotationCache = new ConcurrentHashMap<>();
	private final Map<String, Reflections> methodAnnotationCache = new ConcurrentHashMap<>();

	@Override
    public List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getClassesWithAnnotation(package={}, annotation={})", package_, annotation);

        Reflections reflections = typeAnnotationCache.computeIfAbsent(package_, pkg -> {
            log.atDebug().log("Initializing Reflections scanner for package '{}' (TypesAnnotated)", pkg);
            return new Reflections(pkg, Scanners.TypesAnnotated);
        });

        log.atDebug().log("Fetching annotated classes for annotation '{}' in package {}", annotation.getName(), package_);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation, true);

        log.atDebug().log("Found {} classes annotated with '{}' in package {}", annotatedClasses.size(), annotation.getName(), package_);

        log.atTrace().log("Exiting getClassesWithAnnotation(package={}, annotation={})", package_, annotation);
        return new ArrayList<>(annotatedClasses);
    }

    @Override
    public List<Method> getMethodsWithAnnotation(String package_, Class<? extends Annotation> annotation) {
        log.atTrace().log("Entering getMethodsWithAnnotation(package={}, annotation={})", package_, annotation);

        Reflections reflections = methodAnnotationCache.computeIfAbsent(package_, pkg -> {
            log.atDebug().log("Initializing Reflections scanner for package '{}' (MethodsAnnotated)", pkg);
            return new Reflections(pkg, Scanners.MethodsAnnotated);
        });

        log.atDebug().log("Fetching annotated methods for annotation '{}' in package {}", annotation.getName(), package_);
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(annotation);

        log.atDebug().log("Found {} methods annotated with '{}' in package {}", annotatedMethods.size(), annotation.getName(), package_);

        log.atTrace().log("Exiting getMethodsWithAnnotation(package={}, annotation={})", package_, annotation);
        return new ArrayList<>(annotatedMethods);
    }

}
