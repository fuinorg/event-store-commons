package org.fuin.esc.client;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.HasSerializedDataTypeConstantValidator;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.objects4j.common.TypeConstantValidator;
import org.fuin.utils4j.jandex.JandexIndexFileReader;
import org.fuin.utils4j.jandex.JandexUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry that is built up by scanning for classes that are annotated with {@link HasSerializedDataTypeConstant}.
 * Inner classes are ignored.
 */
public final class JandexSerializedDataTypeRegistry implements SerializedDataTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(JandexSerializedDataTypeRegistry.class);

    private final SerializedDataTypeRegistry delegate;

    private final List<File> classesDirs;

    private final Set<Class<?>> classes;

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
        final SimpleSerializedDataTypeRegistry.Builder builder = new SimpleSerializedDataTypeRegistry.Builder();
        this.classesDirs = Arrays.asList(classesDirs);
        classes = scanForClasses();
        for (final Class<?> domainEventClass : classes) {
            builder.add(serializedDataTypeConstant(domainEventClass), domainEventClass);
        }
        delegate = builder.build();
    }

    @Override
    @NotNull
    public Class<?> findClass(@NotNull SerializedDataType type) {
        return delegate.findClass(type);
    }

    @Override
    @NotNull
    public Set<TypeClass> findAll() {
        return delegate.findAll();
    }

    /**
     * Returns a list of known classes that can be serialized.
     *
     * @return SerializedDataType classes.
     */
    public Set<Class<?>> getClasses() {
        return Collections.unmodifiableSet(classes);
    }

    private Set<Class<?>> scanForClasses() {
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

    private static Set<Class<?>> findClasses(final IndexView index) {
        Set<Class<?>> classes = new HashSet<>();
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

    private static SerializedDataType serializedDataTypeConstant(Class<?> domainEventClass) {
        final HasSerializedDataTypeConstant annotation = domainEventClass.getAnnotation(HasSerializedDataTypeConstant.class);
        return TypeConstantValidator.extractValue(domainEventClass, SerializedDataType.class, annotation.value());
    }

}
