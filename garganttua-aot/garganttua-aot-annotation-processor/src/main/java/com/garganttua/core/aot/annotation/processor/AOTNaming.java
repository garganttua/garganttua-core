package com.garganttua.core.aot.annotation.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Naming conventions for the AOT-generated classes.
 *
 * <ul>
 *   <li>Class descriptor:      {@code AOTClass_<SimpleName>}</li>
 *   <li>Field descriptor:      {@code AOTField_<SimpleName>_<fieldName>}</li>
 *   <li>Method descriptor:     {@code AOTMethod_<SimpleName>_<methodName>_<overloadIndex>}</li>
 *   <li>Constructor descriptor:{@code AOTConstructor_<SimpleName>_<index>}</li>
 * </ul>
 *
 * <p>Method overloads are differentiated by their position in the source-order
 * list of methods sharing the same name. Constructors are differentiated by
 * their position in the source-order list of constructors.</p>
 */
final class AOTNaming {

    private AOTNaming() {}

    static String classDescriptorName(TypeElement type) {
        return "AOTClass_" + type.getSimpleName();
    }

    static String fieldDescriptorName(TypeElement enclosing, VariableElement field) {
        return "AOTField_" + enclosing.getSimpleName() + "_" + field.getSimpleName();
    }

    /**
     * Returns method descriptor names indexed by their {@link ExecutableElement}.
     * Overloads of the same method name receive incrementing indices in source order.
     */
    static Map<ExecutableElement, String> methodDescriptorNames(TypeElement enclosing,
                                                                List<ExecutableElement> methods) {
        Map<String, Integer> counts = new HashMap<>();
        Map<ExecutableElement, String> result = new HashMap<>();
        for (ExecutableElement method : methods) {
            String name = method.getSimpleName().toString();
            int idx = counts.merge(name, 1, Integer::sum) - 1;
            result.put(method, "AOTMethod_" + enclosing.getSimpleName() + "_" + name + "_" + idx);
        }
        return result;
    }

    /** Returns constructor descriptor names indexed by their declaration order. */
    static Map<ExecutableElement, String> constructorDescriptorNames(TypeElement enclosing,
                                                                     List<ExecutableElement> constructors) {
        Map<ExecutableElement, String> result = new HashMap<>();
        int i = 0;
        for (ExecutableElement ctor : constructors) {
            result.put(ctor, "AOTConstructor_" + enclosing.getSimpleName() + "_" + i);
            i++;
        }
        return result;
    }
}
