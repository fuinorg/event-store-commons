package org.fuin.esc.api;

import jakarta.validation.constraints.NotNull;
import org.fuin.utils4j.JandexIndexFileReader;
import org.fuin.utils4j.JandexUtils;
import org.jboss.jandex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Registry that is built up by scanning for classes that are annotated with {@link HasSerializedDataTypeConstant}.
 * Inner classes are ignored.
 */
public class JandexSerializedDataTypeRegistry implements SerializedDataTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(JandexSerializedDataTypeRegistry.class);

    private final SimpleSerializedDataTypeRegistry delegate;

    private final List<File> classesDirs;

    private final List<Class<?>> classes;

    /**
     * Default constructor.
     */
    public JandexSerializedDataTypeRegistry() {
        this(new File("target/classes"));
    }

    /**
     * Constructor with classes directories. Most likely only used in tests.
     *
     * @param classesDirs Directories with class files.
     */
    public JandexSerializedDataTypeRegistry(final File... classesDirs) {
        delegate = new SimpleSerializedDataTypeRegistry();
        this.classesDirs = Arrays.asList(classesDirs);
        classes = scanForClasses();
        for (final Class<?> domainEventClass : classes) {
            delegate.add(serializedDataTypeConstant(domainEventClass), domainEventClass);
        }
    }

    @Override
    @NotNull
    public Class<?> findClass(@NotNull SerializedDataType type) {
        return delegate.findClass(type);
    }

    /**
     * Returns a list of known classes that can be serialized.
     *
     * @return SerializedDataType classes.
     */
    public List<Class<?>> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    private List<Class<?>> scanForClasses() {
        final List<IndexView> indexes = new ArrayList<>();
        indexes.add(new JandexIndexFileReader.Builder().addDefaultResource().build().loadR());
        indexes.add(indexClassesDirs());
        return findClasses(CompositeIndex.create(indexes));
    }

    private IndexView indexClassesDirs() {
        final Indexer indexer = new Indexer();
        final List<File> knownClassFiles = new ArrayList<>();
        for (final File classesDir : classesDirs) {
            JandexUtils.indexDir(indexer, knownClassFiles, classesDir);
        }
        return indexer.complete();
    }

    private static List<Class<?>> findClasses(final IndexView index) {
        List<Class<?>> classes = new ArrayList<>();
        final Collection<AnnotationInstance> annotationInstances = index.getAnnotations(DotName.createSimple(HasSerializedDataTypeConstant.class));
        for (final AnnotationInstance annotationInstance : annotationInstances) {
            final ClassInfo classInfo = annotationInstance.target().asClass();
            if (!Modifier.isAbstract(classInfo.flags())
                    && !Modifier.isInterface(classInfo.flags())
                    && !classInfo.name().toString().contains("$")) {
                final Class<?> clasz = JandexUtils.loadClass(classInfo.name());
                classes.add(clasz);
                LOG.info("Added SerializedDataType to {}: {}", JandexSerializedDataTypeRegistry.class.getSimpleName(), clasz.getName());
            }
        }
        return classes;
    }

    public SerializedDataType serializedDataTypeConstant(Class<?> domainEventClass) {
        final HasSerializedDataTypeConstant annotation = domainEventClass.getAnnotation(HasSerializedDataTypeConstant.class);
        return HasSerializedDataTypeConstantValidator.extractValue(domainEventClass, annotation.value());
    }

}
