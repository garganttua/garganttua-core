package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.util.List;

public interface IAnnotationScanner {

	List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation);

}
