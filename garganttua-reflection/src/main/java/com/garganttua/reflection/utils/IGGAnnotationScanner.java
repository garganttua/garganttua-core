package com.garganttua.reflection.utils;

import java.lang.annotation.Annotation;
import java.util.List;

public interface IGGAnnotationScanner {

	List<Class<?>> getClassesWithAnnotation(String package_, Class<? extends Annotation> annotation);

}
