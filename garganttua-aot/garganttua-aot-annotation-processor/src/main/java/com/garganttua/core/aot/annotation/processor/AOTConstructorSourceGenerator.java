package com.garganttua.core.aot.annotation.processor;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates a typed subclass of {@code AOTConstructor} for one declared
 * constructor, with {@code newInstance} implemented as a direct {@code new}
 * expression — no {@link java.lang.reflect.Constructor} involved at runtime.
 */
final class AOTConstructorSourceGenerator {

    private final ExecutableElement constructor;
    private final Types types;
    private final String packageName;
    private final String enclosingSimpleName;
    private final String enclosingQualifiedName;
    private final String generatedSimpleName;

    AOTConstructorSourceGenerator(Types types, TypeElement enclosing, ExecutableElement constructor, String generatedSimpleName) {
        this.types = types;
        this.constructor = constructor;
        this.generatedSimpleName = generatedSimpleName;
        this.enclosingQualifiedName = enclosing.getQualifiedName().toString();
        this.enclosingSimpleName = enclosing.getSimpleName().toString();
        int lastDot = enclosingQualifiedName.lastIndexOf('.');
        this.packageName = lastDot > 0 ? enclosingQualifiedName.substring(0, lastDot) : "";
    }

    String getGeneratedQualifiedName() {
        return packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    String generate() {
        List<? extends VariableElement> params = constructor.getParameters();
        StringBuilder src = new StringBuilder();
        if (!packageName.isEmpty()) {
            src.append("package ").append(packageName).append(";\n\n");
        }
        src.append("import com.garganttua.core.aot.reflection.AOTConstructor;\n");
        src.append("import java.lang.annotation.Annotation;\n\n");

        src.append("/** AOT constructor descriptor for {@code ").append(enclosingSimpleName)
           .append("(...)} — generated, do not edit. */\n");
        src.append("@SuppressWarnings(\"all\")\n");
        src.append("public final class ").append(generatedSimpleName)
           .append(" extends AOTConstructor<").append(enclosingSimpleName).append("> {\n\n");
        src.append("    public static final ").append(generatedSimpleName)
           .append(" INSTANCE = new ").append(generatedSimpleName).append("();\n\n");

        src.append("    private ").append(generatedSimpleName).append("() {\n");
        src.append("        super(\"").append(enclosingQualifiedName).append("\", ")
           .append(AOTMethodSourceGenerator.buildStringArray(typeNames(params))).append(", ")
           .append(AOTMethodSourceGenerator.buildStringArray(paramNames(params))).append(", ")
           .append(TypeNames.toReflectModifiers(constructor.getModifiers())).append(", ")
           .append("new Annotation[0], ")
           .append(constructor.isVarArgs()).append(", ")
           .append(AOTMethodSourceGenerator.buildStringArray(exceptionTypeNames())).append(");\n");
        src.append("    }\n\n");

        // newInstance(Object...)
        src.append("    @Override\n");
        src.append("    public ").append(enclosingSimpleName).append(" newInstance(Object... args) {\n");
        src.append("        return new ").append(enclosingSimpleName)
           .append("(").append(buildArgCasts(params)).append(");\n");
        src.append("    }\n");

        src.append("}\n");
        return src.toString();
    }

    private String[] typeNames(List<? extends VariableElement> params) {
        String[] out = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            out[i] = TypeNames.getTypeName(types, params.get(i).asType());
        }
        return out;
    }

    private String[] paramNames(List<? extends VariableElement> params) {
        String[] out = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            out[i] = params.get(i).getSimpleName().toString();
        }
        return out;
    }

    private String[] exceptionTypeNames() {
        List<? extends TypeMirror> thrown = constructor.getThrownTypes();
        String[] out = new String[thrown.size()];
        for (int i = 0; i < thrown.size(); i++) {
            out[i] = TypeNames.getTypeName(types, thrown.get(i));
        }
        return out;
    }

    private String buildArgCasts(List<? extends VariableElement> params) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(AOTMethodSourceGenerator.castArg(types, params.get(i).asType(), i));
        }
        return sb.toString();
    }
}
