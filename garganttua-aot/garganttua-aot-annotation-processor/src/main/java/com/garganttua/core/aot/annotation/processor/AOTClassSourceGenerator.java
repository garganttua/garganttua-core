package com.garganttua.core.aot.annotation.processor;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Generates Java source code for an AOTClass subclass that extends
 * {@code com.garganttua.core.aot.reflection.AOTClass<T>} and registers
 * itself with the {@code AOTRegistry}.
 *
 * <p>The generated class is a concrete, final subclass with all metadata
 * pre-computed as constructor arguments. It overrides {@code getType()}
 * to return the raw {@code Class<T>} directly.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public final class AOTClassSourceGenerator {

    private final TypeElement typeElement;
    private final ProcessingEnvironment processingEnv;
    private final String packageName;
    private final String simpleName;
    private final String qualifiedName;
    private final String generatedSimpleName;

    // @Reflected flags
    private final boolean queryAllDeclaredConstructors;
    private final boolean queryAllPublicConstructors;
    private final boolean queryAllDeclaredMethods;
    private final boolean queryAllPublicMethods;
    private final boolean allDeclaredFields;
    private final boolean allDeclaredClasses;

    public AOTClassSourceGenerator(TypeElement typeElement, ProcessingEnvironment processingEnv,
                                   boolean queryAllDeclaredConstructors, boolean queryAllPublicConstructors,
                                   boolean queryAllDeclaredMethods, boolean queryAllPublicMethods,
                                   boolean allDeclaredFields, boolean allDeclaredClasses) {
        this.typeElement = typeElement;
        this.processingEnv = processingEnv;
        this.qualifiedName = typeElement.getQualifiedName().toString();
        this.simpleName = typeElement.getSimpleName().toString();
        this.generatedSimpleName = "AOTClass_" + simpleName;

        int lastDot = qualifiedName.lastIndexOf('.');
        this.packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";

        this.queryAllDeclaredConstructors = queryAllDeclaredConstructors;
        this.queryAllPublicConstructors = queryAllPublicConstructors;
        this.queryAllDeclaredMethods = queryAllDeclaredMethods;
        this.queryAllPublicMethods = queryAllPublicMethods;
        this.allDeclaredFields = allDeclaredFields;
        this.allDeclaredClasses = allDeclaredClasses;
    }

    /**
     * Returns the fully qualified name of the generated class.
     */
    public String getGeneratedQualifiedName() {
        return packageName.isEmpty() ? generatedSimpleName : packageName + "." + generatedSimpleName;
    }

    /**
     * Generates the full Java source file content.
     */
    public String generate() {
        Set<String> imports = new TreeSet<>();

        // Always needed
        imports.add("com.garganttua.core.aot.reflection.AOTClass");
        imports.add("com.garganttua.core.aot.reflection.AOTField");
        imports.add("com.garganttua.core.aot.reflection.AOTMethod");
        imports.add("com.garganttua.core.aot.reflection.AOTConstructor");
        imports.add("com.garganttua.core.aot.commons.AOTRegistry");
        imports.add("java.lang.annotation.Annotation");

        // Collect member descriptors
        String fieldsArray = buildFieldsArray(imports);
        String methodsArray = buildMethodsArray(imports);
        String constructorsArray = buildConstructorsArray(imports);

        // Type metadata
        String superClassName = getSuperClassName();
        String interfaceNamesArray = buildInterfaceNamesArray();
        int modifierFlags = toReflectModifiers(typeElement.getModifiers());

        // Boolean flags
        boolean isInterfaceFlag = typeElement.getKind() == ElementKind.INTERFACE;
        boolean isAnnotationFlag = typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
        boolean isEnumFlag = typeElement.getKind() == ElementKind.ENUM;
        boolean isRecordFlag = typeElement.getKind() == ElementKind.RECORD;

        // Build source
        StringBuilder src = new StringBuilder();

        // Package
        if (!packageName.isEmpty()) {
            src.append("package ").append(packageName).append(";\n\n");
        }

        // Imports
        for (String imp : imports) {
            src.append("import ").append(imp).append(";\n");
        }
        src.append("\n");

        // Javadoc
        src.append("/**\n");
        src.append(" * AOT-generated class descriptor for {@link ").append(simpleName).append("}.\n");
        src.append(" *\n");
        src.append(" * <p>Generated at compile time by the Garganttua AOT annotation processor.\n");
        src.append(" * Do not edit manually.</p>\n");
        src.append(" */\n");

        // Class declaration — extends AOTClass<OriginalType>
        src.append("@SuppressWarnings(\"all\")\n");
        src.append("public final class ").append(generatedSimpleName)
           .append(" extends AOTClass<").append(simpleName).append("> {\n\n");

        // INSTANCE singleton
        src.append("    public static final ").append(generatedSimpleName)
           .append(" INSTANCE = new ").append(generatedSimpleName).append("();\n\n");

        // Static registration block
        src.append("    static {\n");
        src.append("        AOTRegistry.getInstance().register(\"").append(qualifiedName)
           .append("\", INSTANCE);\n");
        src.append("    }\n\n");

        // Private constructor calling super with all metadata
        src.append("    private ").append(generatedSimpleName).append("() {\n");
        src.append("        super(\n");
        src.append("            \"").append(qualifiedName).append("\",\n");                    // name
        src.append("            \"").append(simpleName).append("\",\n");                       // simpleName
        src.append("            \"").append(qualifiedName).append("\",\n");                    // canonicalName
        src.append("            \"").append(packageName).append("\",\n");                      // packageName
        src.append("            ").append(modifierFlags).append(",\n");                        // modifiers
        src.append("            ").append(superClassName).append(",\n");                       // superclassName
        src.append("            ").append(interfaceNamesArray).append(",\n");                  // interfaceNames
        src.append("            ").append(fieldsArray).append(",\n");                          // fields
        src.append("            ").append(methodsArray).append(",\n");                         // methods
        src.append("            ").append(constructorsArray).append(",\n");                    // constructors
        src.append("            new Annotation[0],\n");                                        // annotations (runtime annotations not available at compile time)
        src.append("            ").append(isInterfaceFlag).append(",\n");                      // isInterface
        src.append("            false,\n");                                                    // isArray
        src.append("            false,\n");                                                    // isPrimitive
        src.append("            ").append(isAnnotationFlag).append(",\n");                     // isAnnotation
        src.append("            ").append(isEnumFlag).append(",\n");                           // isEnum
        src.append("            ").append(isRecordFlag).append(",\n");                         // isRecord
        src.append("            false,\n");                                                    // isSealed
        src.append("            false,\n");                                                    // isHidden
        src.append("            ").append(typeElement.getNestingKind().isNested()).append(",\n"); // isMemberClass
        src.append("            false,\n");                                                    // isLocalClass
        src.append("            false,\n");                                                    // isAnonymousClass
        src.append("            false\n");                                                     // isSynthetic
        src.append("        );\n");
        src.append("    }\n\n");

        // Override getType() — direct class literal, no Class.forName()
        src.append("    @Override\n");
        src.append("    public Class<").append(simpleName).append("> getType() {\n");
        src.append("        return ").append(simpleName).append(".class;\n");
        src.append("    }\n\n");

        // Close class
        src.append("}\n");

        return src.toString();
    }

    // --- Field array ---

    private String buildFieldsArray(Set<String> imports) {
        if (!allDeclaredFields) {
            return "new AOTField[0]";
        }
        StringBuilder sb = new StringBuilder("new AOTField[] {\n");
        boolean first = true;
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                if (!first) sb.append(",\n");
                first = false;
                // AOTField(name, declaringClassName, typeName, modifiers, annotations, genericType)
                sb.append("            new AOTField(\"")
                  .append(field.getSimpleName()).append("\", \"")
                  .append(qualifiedName).append("\", \"")
                  .append(getTypeName(field.asType())).append("\", ")
                  .append(toReflectModifiers(field.getModifiers())).append(", ")
                  .append("new Annotation[0], null)");
            }
        }
        if (first) return "new AOTField[0]";
        sb.append("\n        }");
        return sb.toString();
    }

    // --- Method array ---

    private String buildMethodsArray(Set<String> imports) {
        if (!queryAllDeclaredMethods && !queryAllPublicMethods) {
            return "new AOTMethod[0]";
        }
        StringBuilder sb = new StringBuilder("new AOTMethod[] {\n");
        boolean first = true;
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                if (!queryAllDeclaredMethods && !method.getModifiers().contains(Modifier.PUBLIC)) {
                    continue;
                }
                if (!first) sb.append(",\n");
                first = false;
                // AOTMethod(name, declaringClassName, returnTypeName, parameterTypeNames,
                //           parameterNames, modifiers, annotations, bridge, defaultMethod, varArgs, exceptionTypeNames)
                sb.append("            new AOTMethod(\"")
                  .append(method.getSimpleName()).append("\", \"")
                  .append(qualifiedName).append("\", \"")
                  .append(getTypeName(method.getReturnType())).append("\", ")
                  .append(buildStringArray(method.getParameters())).append(", ")
                  .append(buildParamNamesArray(method.getParameters())).append(", ")
                  .append(toReflectModifiers(method.getModifiers())).append(", ")
                  .append("new Annotation[0], ")
                  .append("false, ")                                                        // bridge
                  .append(method.getModifiers().contains(Modifier.DEFAULT)).append(", ")    // defaultMethod
                  .append(method.isVarArgs()).append(", ")                                  // varArgs
                  .append(buildExceptionTypesArray(method)).append(")");                                   // isVarArgs
            }
        }
        if (first) return "new AOTMethod[0]";
        sb.append("\n        }");
        return sb.toString();
    }

    // --- Constructor array ---

    private String buildConstructorsArray(Set<String> imports) {
        if (!queryAllDeclaredConstructors && !queryAllPublicConstructors) {
            return "new AOTConstructor[0]";
        }
        StringBuilder sb = new StringBuilder("new AOTConstructor<?>[] {\n");
        boolean first = true;
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement ctor = (ExecutableElement) enclosed;
                if (!queryAllDeclaredConstructors && !ctor.getModifiers().contains(Modifier.PUBLIC)) {
                    continue;
                }
                if (!first) sb.append(",\n");
                first = false;
                // AOTConstructor(declaringClassName, parameterTypeNames, parameterNames,
                //                modifiers, annotations, varArgs, exceptionTypeNames)
                sb.append("            new AOTConstructor<>(\"")
                  .append(qualifiedName).append("\", ")
                  .append(buildStringArray(ctor.getParameters())).append(", ")
                  .append(buildParamNamesArray(ctor.getParameters())).append(", ")
                  .append(toReflectModifiers(ctor.getModifiers())).append(", ")
                  .append("new Annotation[0], ")
                  .append(ctor.isVarArgs()).append(", ")
                  .append(buildExceptionTypesArray(ctor)).append(")");
            }
        }
        if (first) return "new AOTConstructor[0]";
        sb.append("\n        }");
        return sb.toString();
    }

    // --- Helpers ---

    private String getSuperClassName() {
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            return "null";
        }
        return "\"" + getTypeName(superclass) + "\"";
    }

    private String buildInterfaceNamesArray() {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        if (interfaces.isEmpty()) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < interfaces.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(getTypeName(interfaces.get(i))).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildStringArray(List<? extends VariableElement> params) {
        if (params.isEmpty()) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(getTypeName(params.get(i).asType())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildParamNamesArray(List<? extends VariableElement> params) {
        if (params.isEmpty()) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(params.get(i).getSimpleName()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildExceptionTypesArray(ExecutableElement executable) {
        List<? extends TypeMirror> thrownTypes = executable.getThrownTypes();
        if (thrownTypes.isEmpty()) {
            return "new String[0]";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < thrownTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(getTypeName(thrownTypes.get(i))).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String getTypeName(TypeMirror typeMirror) {
        return switch (typeMirror.getKind()) {
            case DECLARED -> {
                DeclaredType declaredType = (DeclaredType) typeMirror;
                TypeElement element = (TypeElement) declaredType.asElement();
                yield element.getQualifiedName().toString();
            }
            case ARRAY -> {
                javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) typeMirror;
                yield getTypeName(arrayType.getComponentType()) + "[]";
            }
            case VOID -> "void";
            default -> typeMirror.toString();
        };
    }

    private static int toReflectModifiers(Set<Modifier> modifiers) {
        int flags = 0;
        for (Modifier mod : modifiers) {
            flags |= switch (mod) {
                case PUBLIC -> java.lang.reflect.Modifier.PUBLIC;
                case PROTECTED -> java.lang.reflect.Modifier.PROTECTED;
                case PRIVATE -> java.lang.reflect.Modifier.PRIVATE;
                case ABSTRACT -> java.lang.reflect.Modifier.ABSTRACT;
                case STATIC -> java.lang.reflect.Modifier.STATIC;
                case FINAL -> java.lang.reflect.Modifier.FINAL;
                case TRANSIENT -> java.lang.reflect.Modifier.TRANSIENT;
                case VOLATILE -> java.lang.reflect.Modifier.VOLATILE;
                case SYNCHRONIZED -> java.lang.reflect.Modifier.SYNCHRONIZED;
                case NATIVE -> java.lang.reflect.Modifier.NATIVE;
                case STRICTFP -> java.lang.reflect.Modifier.STRICT;
                default -> 0;
            };
        }
        return flags;
    }
}
