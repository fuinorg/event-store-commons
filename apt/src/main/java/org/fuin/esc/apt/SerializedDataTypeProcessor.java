package org.fuin.esc.apt;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates a "SerializedDataTypesRegistrationRequest" containing all types with a "HasSerializedDataTypeConstant" annotation.
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
        SerializedDataTypeProcessor.HAS_SERIALIZED_DATA_TYPE_CONSTANT,
        SerializedDataTypeProcessor.GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST
})
public class SerializedDataTypeProcessor extends AbstractProcessor {

    static final String HAS_SERIALIZED_DATA_TYPE_CONSTANT = "org.fuin.esc.api.HasSerializedDataTypeConstant";

    private static final String GENERATOR_ANNOTATION = "GenerateSerializedDataTypesRegistrationRequest";

    static final String GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST = "org.fuin.esc.api." + GENERATOR_ANNOTATION;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!annotations.isEmpty()) {
            Set<SerializedDataTypesRegistrationRequestTarget> targets = null;
            Set<SerializedDataTypeResult> results = null;
            for (final TypeElement annotation : annotations) {
                final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
                final String fqnAnnotationName = annotation.getQualifiedName().toString();
                if (fqnAnnotationName.equals(HAS_SERIALIZED_DATA_TYPE_CONSTANT)) {
                    ensureHasNotBeenSetBefore("results", results);
                    results = processHasSerializedDataTypeConstant(annotatedElements);
                } else if (fqnAnnotationName.equals(GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST)) {
                    ensureHasNotBeenSetBefore("targets", targets);
                    targets = processGenerateSerializedDataTypesRegistrationRequest(annotatedElements);
                }
            }
            SerializedDataTypesRegistrationRequestWriter.write(processingEnv, targets, results);
        }
        return true;
    }

    private void ensureHasNotBeenSetBefore(String name, Object value) {
        if (value != null) {
            throw new IllegalStateException(name + " has already been set before: " + value);
        }
    }

    private Set<SerializedDataTypeResult> processHasSerializedDataTypeConstant(Set<? extends Element> annotatedElements) {
        final Set<SerializedDataTypeResult> results = new HashSet<>();
        for (final Element element : annotatedElements) {
            logInfo("Process: " + element);
            final AnnotationMirror annotationMirror = findAnnotationMirror(element, HAS_SERIALIZED_DATA_TYPE_CONSTANT);
            if (annotationMirror == null) {
                logError("Failed to find annotation: " + HAS_SERIALIZED_DATA_TYPE_CONSTANT);
            } else {
                final AnnotationValue annotationValue = findAnnotationValue(annotationMirror, "value");
                if (annotationValue == null) {
                    logError("Failed to find annotation 'value()': " + HAS_SERIALIZED_DATA_TYPE_CONSTANT);
                } else {
                    final String fieldName = (String) annotationValue.getValue();
                    PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
                    final String packageName = pkg.getQualifiedName().toString();
                    final String className = element.getSimpleName().toString();
                    if (pkg.isUnnamed()) {
                        throw new IllegalStateException("The default package is not allowed for generated classes: " + className);
                    }
                    results.add(new SerializedDataTypeResult(packageName, className, fieldName));
                }
            }
        }
        return results;
    }

    private Set<SerializedDataTypesRegistrationRequestTarget> processGenerateSerializedDataTypesRegistrationRequest(final Set<? extends Element> annotatedElements) {
        final Set<SerializedDataTypesRegistrationRequestTarget> results = new HashSet<>();
        for (final Element element : annotatedElements) {
            logInfo("Process: " + element);
            if (element.getKind().isInterface()) {
                final AnnotationMirror annotationMirror = findAnnotationMirror(element, GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST);
                if (annotationMirror == null) {
                    logError("Failed to find annotation: " + GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST);
                } else {
                    final String simpleInterfaceName = element.getSimpleName().toString();
                    final AnnotationValue annotationValue = findAnnotationValue(annotationMirror, "name");
                    if (annotationValue == null) {
                        logError("Failed to find annotation 'name()': " + GENERATE_SERIALIZED_DATA_TYPES_REGISTRATION_REQUEST);
                    } else {
                        results.add(createAnnotation(element, annotationValue, simpleInterfaceName));
                    }
                }
            } else {
                logError("Expected type annotated with '@" + GENERATOR_ANNOTATION + "' to be an interface, but was not: " + element);
            }
        }
        return results;
    }

    private SerializedDataTypesRegistrationRequestTarget createAnnotation(Element element, AnnotationValue annotationValue, String simpleInterfaceName) {
        final String simpleClassName = simpleClassName(annotationValue, simpleInterfaceName);
        final String packageName = packageName(element);
        return new SerializedDataTypesRegistrationRequestTarget(packageName, simpleClassName, simpleInterfaceName);
    }

    private String packageName(Element element) {
        final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
        if (pkg.isUnnamed()) {
            throw new IllegalStateException("The default package is not allowed for generated classes: " + element);
        }
        return pkg.getQualifiedName().toString();
    }

    private static String simpleClassName(AnnotationValue annotationValue, String simpleInterfaceName) {
        final String nameStr = (String) annotationValue.getValue();
        if (nameStr.isEmpty()) {
            return simpleInterfaceName + "Impl";
        }
        return (String) annotationValue.getValue();
    }

    private AnnotationValue findAnnotationValue(AnnotationMirror annotationMirror, String methodName) {
        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : getElementValuesWithDefaults(annotationMirror).entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(methodName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(AnnotationMirror annotationMirror) {
        return processingEnv.getElementUtils().getElementValuesWithDefaults(annotationMirror);
    }

    private AnnotationMirror findAnnotationMirror(final Element element, final String annotationClassName) {
        for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationClassName)) {
                return mirror;
            }
        }
        return null;
    }

    private void logInfo(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    private void logError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }


}