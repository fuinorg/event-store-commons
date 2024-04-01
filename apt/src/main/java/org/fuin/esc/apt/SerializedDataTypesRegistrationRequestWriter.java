package org.fuin.esc.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates the source code for
 */
public final class SerializedDataTypesRegistrationRequestWriter {

    private SerializedDataTypesRegistrationRequestWriter() {
    }

    public static void write(final ProcessingEnvironment processingEnv, final Set<SerializedDataTypesRegistrationRequestTarget> targets, final Set<SerializedDataTypeResult> results) {
        if (targets == null || results == null) {
            return;
        }
        for (final SerializedDataTypesRegistrationRequestTarget target : targets) {
            write(processingEnv, target, results);
        }
    }

    public static void write(final ProcessingEnvironment processingEnv, final SerializedDataTypesRegistrationRequestTarget target, final Set<SerializedDataTypeResult> results) {
        try {
            final JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(target.toString());
            try (final PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                out.write(createSource(target, results));
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to write:" + target, ex);
        }
    }

    private static String createSource(final SerializedDataTypesRegistrationRequestTarget target, final Set<SerializedDataTypeResult> results) {
        final StringBuilder sb = new StringBuilder();
        if (target.packageName() != null) {
            sb.append("package ").append(target.packageName()).append(";\n\n");
        }
        results.forEach(result -> {
            if (result.packageName() != null) {
                sb.append("import ").append(result.getFullClassName()).append(";\n");
            }
        });
        sb.append("""
                import com.google.auto.service.AutoService;
                import org.fuin.esc.api.SerializedDataType2ClassMapping;
                import org.fuin.esc.api.SerializedDataTypesRegistrationRequest;
                import java.util.Set;
                                
                /**
                 * Request to register {@link org.fuin.esc.api.SerializedDataType} to class mappings.
                 */
                @AutoService(SerializedDataTypesRegistrationRequest.class)
                public class ${className} implements SerializedDataTypesRegistrationRequest, ${interfaceName} {
                                
                    @Override
                    public Set<SerializedDataType2ClassMapping> getMappingsToRegister() {
                        return Set.of(${entries});
                    }
                                
                }
                """
                .replace("${className}", target.simpleClassName())
                .replace("${interfaceName}", target.simpleInterfaceName())
                .replace("${entries}", asString(results)));
        return sb.toString();
    }

    private static String asString(final Set<SerializedDataTypeResult> results) {
        return results.stream()
                .map(SerializedDataTypeResult::toString)
                .collect(Collectors.joining(", "));
    }

}
