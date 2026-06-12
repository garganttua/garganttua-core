package com.garganttua.core.aot.annotation.processor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates Java source code for an AOTClass subclass that extends
 * {@code com.garganttua.core.aot.reflection.AOTClass<T>} and registers
 * itself with the {@code AOTRegistry}.
 *
 * <p>The generated class references the per-member descriptor singletons
 * (AOTField_X_y.INSTANCE, AOTMethod_X_m_0.INSTANCE, ...) so that no member
 * metadata is constructed at runtime. It overrides {@code getType()}
 * to return the raw {@code Class<T>} directly.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public final class AOTClassSourceGenerator {

    private final Types types;
    private final TypeElement typeElement;
    private final String packageName;
    private final String simpleName;
    private final String qualifiedName;
    private final String generatedSimpleName;

    private final List<VariableElement> fields;
    private final List<ExecutableElement> methods;
    private final Map<ExecutableElement, String> methodNames;
    private final List<ExecutableElement> constructors;
    private final Map<ExecutableElement, String> constructorNames;

    /**
     * Creates a generator for the class descriptor of {@code typeElement}.
     *
     * @param types            the type utilities used to erase generic types
     * @param typeElement      the {@code @Reflected} type to describe
     * @param fields           the fields to expose, in source order
     * @param methods          the methods to expose, in source order
     * @param methodNames      mapping of each method to its generated descriptor class name
     * @param constructors     the constructors to expose, in source order
     * @param constructorNames mapping of each constructor to its generated descriptor class name
     */
    public AOTClassSourceGenerator(Types types,
                                   TypeElement typeElement,
                                   List<VariableElement> fields,
                                   List<ExecutableElement> methods,
                                   Map<ExecutableElement, String> methodNames,
                                   List<ExecutableElement> constructors,
                                   Map<ExecutableElement, String> constructorNames) {
        this.types = types;
        this.typeElement = typeElement;
        this.qualifiedName = typeElement.getQualifiedName().toString();
        this.simpleName = typeElement.getSimpleName().toString();
        this.generatedSimpleName = AOTNaming.classDescriptorName(typeElement);

        int lastDot = qualifiedName.lastIndexOf('.');
        this.packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";

        this.fields = fields;
        this.methods = methods;
        this.methodNames = methodNames;
        this.constructors = constructors;
        this.constructorNames = constructorNames;
    }

    /**
     * Returns the fully-qualified name of the generated class descriptor.
     *
     * @return the package-qualified name of the {@code AOTClass_*} to be emitted
     */
    public String getGeneratedQualifiedName() {
        return packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    /**
     * Renders the complete Java source of the {@code AOTClass_*} descriptor.
     *
     * @return the generated source code
     */
    public String generate() {
        Set<String> imports = new TreeSet<>();
        imports.add("com.garganttua.core.aot.reflection.AOTClass");
        imports.add("com.garganttua.core.aot.reflection.AOTField");
        imports.add("com.garganttua.core.aot.reflection.AOTMethod");
        imports.add("com.garganttua.core.aot.reflection.AOTConstructor");
        imports.add("com.garganttua.core.aot.commons.AOTRegistry");
        imports.add("java.lang.annotation.Annotation");

        String fieldsArray = buildFieldsArray();
        String methodsArray = buildMethodsArray();
        String constructorsArray = buildConstructorsArray();
        String superClassName = getSuperClassName();
        String interfaceNamesArray = buildInterfaceNamesArray();
        int modifierFlags = TypeNames.toReflectModifiers(typeElement.getModifiers());

        boolean isInterfaceFlag = typeElement.getKind() == ElementKind.INTERFACE;
        boolean isAnnotationFlag = typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
        boolean isEnumFlag = typeElement.getKind() == ElementKind.ENUM;
        boolean isRecordFlag = typeElement.getKind() == ElementKind.RECORD;

        StringBuilder src = new StringBuilder();
        if (!packageName.isEmpty()) {
            src.append("package ").append(packageName).append(";\n\n");
        }
        for (String imp : imports) {
            src.append("import ").append(imp).append(";\n");
        }
        src.append("\n");
        src.append("/**\n");
        src.append(" * AOT-generated class descriptor for {@link ").append(simpleName).append("}.\n");
        src.append(" *\n");
        src.append(" * <p>Generated at compile time by the Garganttua AOT annotation processor.\n");
        src.append(" * Do not edit manually.</p>\n");
        src.append(" */\n");
        src.append("@SuppressWarnings(\"all\")\n");
        src.append("public final class ").append(generatedSimpleName)
           .append(" extends AOTClass<").append(simpleName).append("> {\n\n");
        src.append("    public static final ").append(generatedSimpleName)
           .append(" INSTANCE = new ").append(generatedSimpleName).append("();\n\n");
        src.append("    static {\n");
        src.append("        AOTRegistry.getInstance().register(\"").append(qualifiedName)
           .append("\", INSTANCE);\n");
        src.append("    }\n\n");
        // Constructor MUST be public — ServiceLoader.load(IAOTSelfRegistering.class)
        // instantiates AOT descriptors via reflection and rejects non-public ctors.
        src.append("    public ").append(generatedSimpleName).append("() {\n");
        src.append("        super(\n");
        src.append("            \"").append(qualifiedName).append("\",\n");
        src.append("            \"").append(simpleName).append("\",\n");
        src.append("            \"").append(qualifiedName).append("\",\n");
        src.append("            \"").append(packageName).append("\",\n");
        src.append("            ").append(modifierFlags).append(",\n");
        src.append("            ").append(superClassName).append(",\n");
        src.append("            ").append(interfaceNamesArray).append(",\n");
        src.append("            ").append(fieldsArray).append(",\n");
        src.append("            ").append(methodsArray).append(",\n");
        src.append("            ").append(constructorsArray).append(",\n");
        // Class-level annotations sourced from the live Class<?>. Java's
        // {@code MyClass.class.getAnnotations()} is AOT-friendly (the class
        // literal compiles to a constant pool entry, no reflection at
        // descriptor construction time). Without this the framework sees an
        // empty annotation array on every AOTClass_* and qualifier discovery
        // (@Observer, @BeanProvider, @Resolver, …) silently no-ops —
        // observers never get notified, bean providers never get registered,
        // resolvers never get wired. The fix that ended the
        // "@Observer not detected" loop.
        src.append("            ").append(qualifiedName).append(".class.getAnnotations(),\n");
        src.append("            ").append(isInterfaceFlag).append(",\n");
        src.append("            false,\n");
        src.append("            false,\n");
        src.append("            ").append(isAnnotationFlag).append(",\n");
        src.append("            ").append(isEnumFlag).append(",\n");
        src.append("            ").append(isRecordFlag).append(",\n");
        src.append("            false,\n");
        src.append("            false,\n");
        src.append("            ").append(typeElement.getNestingKind().isNested()).append(",\n");
        src.append("            false,\n");
        src.append("            false,\n");
        src.append("            false\n");
        src.append("        );\n");
        src.append("    }\n\n");
        src.append("    @Override\n");
        src.append("    public Class<").append(simpleName).append("> getType() {\n");
        src.append("        return ").append(simpleName).append(".class;\n");
        src.append("    }\n\n");
        src.append("}\n");
        return src.toString();
    }

    private String buildFieldsArray() {
        if (fields.isEmpty()) return "new AOTField[0]";
        StringBuilder sb = new StringBuilder("new AOTField[]{\n");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("            ")
              .append(AOTNaming.fieldDescriptorName(typeElement, fields.get(i)))
              .append(".INSTANCE");
        }
        sb.append("\n        }");
        return sb.toString();
    }

    private String buildMethodsArray() {
        if (methods.isEmpty()) return "new AOTMethod[0]";
        StringBuilder sb = new StringBuilder("new AOTMethod[]{\n");
        for (int i = 0; i < methods.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("            ").append(methodNames.get(methods.get(i))).append(".INSTANCE");
        }
        sb.append("\n        }");
        return sb.toString();
    }

    private String buildConstructorsArray() {
        if (constructors.isEmpty()) return "new AOTConstructor[0]";
        StringBuilder sb = new StringBuilder("new AOTConstructor<?>[]{\n");
        for (int i = 0; i < constructors.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("            ").append(constructorNames.get(constructors.get(i))).append(".INSTANCE");
        }
        sb.append("\n        }");
        return sb.toString();
    }

    private String getSuperClassName() {
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            return "null";
        }
        return "\"" + TypeNames.getTypeName(types, superclass) + "\"";
    }

    private String buildInterfaceNamesArray() {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        if (interfaces.isEmpty()) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[]{");
        for (int i = 0; i < interfaces.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(TypeNames.getTypeName(types, interfaces.get(i))).append("\"");
        }
        sb.append('}');
        return sb.toString();
    }
}
