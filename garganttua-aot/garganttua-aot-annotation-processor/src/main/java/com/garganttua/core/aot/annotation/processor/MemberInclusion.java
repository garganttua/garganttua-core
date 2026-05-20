package com.garganttua.core.aot.annotation.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Computes which members of a @Reflected type end up in the generated AOT
 * descriptor, given the class-level flags and the set of members carrying
 * an explicit @Reflected annotation.
 *
 * <p>The same logic is consumed by {@link DirectBinderGenerator} (for
 * validation, e.g. rejecting private members) and by
 * {@link AOTClassSourceGenerator} (for emitting member references). Keeping
 * a single source of truth avoids drift between the two.</p>
 */
final class MemberInclusion {

    private MemberInclusion() {}

    /** Snapshot of the flags read from the @Reflected annotation on the type. */
    record Flags(boolean queryAllDeclaredConstructors,
                 boolean queryAllPublicConstructors,
                 boolean queryAllDeclaredMethods,
                 boolean queryAllPublicMethods,
                 boolean allDeclaredFields) {}

    /** All fields that must appear in the generated descriptor, in source order. */
    static List<VariableElement> includedFields(TypeElement type, Flags flags, Set<Element> explicit) {
        List<VariableElement> out = new ArrayList<>();
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) enclosed;
            if (flags.allDeclaredFields() || explicit.contains(field)) {
                out.add(field);
            }
        }
        return out;
    }

    /** All methods that must appear in the generated descriptor, in source order. */
    static List<ExecutableElement> includedMethods(TypeElement type, Flags flags, Set<Element> explicit) {
        List<ExecutableElement> out = new ArrayList<>();
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) enclosed;
            boolean coveredByFlag = flags.queryAllDeclaredMethods()
                    || (flags.queryAllPublicMethods() && method.getModifiers().contains(Modifier.PUBLIC));
            if (coveredByFlag || explicit.contains(method)) {
                out.add(method);
            }
        }
        return out;
    }

    /** All constructors that must appear in the generated descriptor, in source order. */
    static List<ExecutableElement> includedConstructors(TypeElement type, Flags flags, Set<Element> explicit) {
        List<ExecutableElement> out = new ArrayList<>();
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            ExecutableElement ctor = (ExecutableElement) enclosed;
            boolean coveredByFlag = flags.queryAllDeclaredConstructors()
                    || (flags.queryAllPublicConstructors() && ctor.getModifiers().contains(Modifier.PUBLIC));
            if (coveredByFlag || explicit.contains(ctor)) {
                out.add(ctor);
            }
        }
        return out;
    }
}
