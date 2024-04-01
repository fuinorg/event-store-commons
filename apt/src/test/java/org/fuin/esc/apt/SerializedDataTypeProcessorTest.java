package org.fuin.esc.apt;

import io.toolisticon.cute.Cute;
import io.toolisticon.cute.CuteApi;
import io.toolisticon.cute.JavaFileObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SerializedDataTypeProcessorTest {

    private CuteApi.BlackBoxTestSourceFilesInterface compileTestBuilder;

    @BeforeEach
    public void init() {
        compileTestBuilder = Cute.blackBoxTest().given().processors(SerializedDataTypeProcessor.class);
    }

    @Test
    public void testYourProcessor() {
        compileTestBuilder
                .andSourceFiles("DefaultValueTestClass.java",
                        "DedicatedValueTestClass.java",
                        "DefaultNameGenerateTestClass.java",
                        "DedicatedNameGenerateTestClass.java")
                .whenCompiled()
                .thenExpectThat()
                    .compilationSucceeds()
                .andThat()
                    .generatedSourceFile("org.fuin.esc.apt.gen.AnotherName").equals(JavaFileObjectUtils.readFromString(anotherNameSource()))
                .andThat()
                    .generatedSourceFile("org.fuin.esc.apt.gen.DefaultNameGenerateTestClassImpl").equals(JavaFileObjectUtils.readFromString(defaultNameGenerateTestClassImplSource()))
                .executeTest();
    }

    private static String anotherNameSource() {
        return """
                package org.fuin.esc.apt.gen;
                                
                import org.fuin.esc.apt.demo.DedicatedValueTestClass;
                import org.fuin.esc.apt.demo2.DefaultValueTestClass;
                import com.google.auto.service.AutoService;
                import org.fuin.esc.api.SerializedDataType2ClassMapping;
                import org.fuin.esc.api.SerializedDataTypesRegistrationRequest;
                import java.util.Set;
                                
                /**
                 * Request to register {@link org.fuin.esc.api.SerializedDataType} to class mappings.
                 */
                @AutoService(SerializedDataTypesRegistrationRequest.class)
                public class AnotherName implements SerializedDataTypesRegistrationRequest, DedicatedNameGenerateTestClass {
                                
                    @Override
                    public Set<SerializedDataType2ClassMapping> getMappingsToRegister() {
                        return Set.of(new SerializedDataType2ClassMapping(DedicatedValueTestClass.THE_TYPE, DedicatedValueTestClass.class), new SerializedDataType2ClassMapping(DefaultValueTestClass.SER_TYPE, DefaultValueTestClass.class));
                    }
                                
                }
                """;
    }

    private static String defaultNameGenerateTestClassImplSource() {
        return """
                package org.fuin.esc.apt.gen;
                                
                import org.fuin.esc.apt.demo.DedicatedValueTestClass;
                import org.fuin.esc.apt.demo2.DefaultValueTestClass;
                import com.google.auto.service.AutoService;
                import org.fuin.esc.api.SerializedDataType2ClassMapping;
                import org.fuin.esc.api.SerializedDataTypesRegistrationRequest;
                import java.util.Set;
                                
                /**
                 * Request to register {@link org.fuin.esc.api.SerializedDataType} to class mappings.
                 */
                @AutoService(SerializedDataTypesRegistrationRequest.class)
                public class DefaultNameGenerateTestClassImpl implements SerializedDataTypesRegistrationRequest, DefaultNameGenerateTestClass {
                                
                    @Override
                    public Set<SerializedDataType2ClassMapping> getMappingsToRegister() {
                        return Set.of(new SerializedDataType2ClassMapping(DedicatedValueTestClass.THE_TYPE, DedicatedValueTestClass.class), new SerializedDataType2ClassMapping(DefaultValueTestClass.SER_TYPE, DefaultValueTestClass.class));
                    }
                                
                }
                """;
    }


}
