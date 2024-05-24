package com.garganttua.tooling.objects.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GGAPIFieldMappingRule {

	String sourceFieldAddress();

	String fromSourceMethod() default "";

	String toSourceMethod() default "";

}
