/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.crypto;

import jakarta.validation.constraints.NotEmpty;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.crypto.EncryptedDataService;
import org.fuin.objects4j.crypto.EncryptionKeyIdUnknownException;
import org.fuin.utils4j.jandex.JandexIndexFileReader;
import org.fuin.utils4j.jandex.JandexUtils;
import org.jboss.jandex.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Resolves the encryption key based on the event type: an event is encrypted only if
 * its class implements a configured "requires encryption at rest" marker interface
 * (for example {@code org.fuin.ddd4j.core.RequiresEncryptionAtRest}). The implementing
 * classes are discovered once at construction time by scanning the Jandex index
 * (the {@code META-INF/jandex.idx} resources on the classpath plus optional class
 * directories), and each is mapped to its event type name via a public static
 * constant (by default the field {@code TYPE}, using its {@link Object#toString()}).
 * <p>
 * For an event whose type requires encryption the key identifier is derived
 * from the {@link StreamId} and the optional {@link TenantId} (by default {@code <tenant>/<stream>},
 * or just the stream when there is no tenant, which for an aggregate stream is one key per aggregate
 * instance). The key <b>must already exist</b> in the {@link EncryptedDataService}: its current
 * version is looked up via {@link EncryptedDataService#getKeyVersion(String)} and, if the key
 * is unknown, an {@link EscEncryptionException} is thrown. The resolver never creates keys.
 * For all other event types an empty {@link Optional} is returned so they are stored in plain text.
 * <p>
 * This class intentionally has no compile-time dependency on the marker interface or the
 * event classes; it refers to them only by name so it can live in the encryption module
 * without a dependency on the domain model.
 */
@ThreadSafe
public final class JandexKeyIdResolver implements KeyIdResolver {

    /** Default name of the public static constant that holds an event's type name. */
    public static final String DEFAULT_TYPE_FIELD = "TYPE";

    private static final Logger LOG = LoggerFactory.getLogger(JandexKeyIdResolver.class);

    /** Default derivation: {@code <tenant>/<stream>} when a tenant is present, otherwise just {@code <stream>}. */
    private static final KeyIdFunction DEFAULT_KEY_ID_FUNCTION = (streamId, tenantId) ->
            tenantId == null ? streamId.asString() : tenantId.asString() + "/" + streamId.asString();

    private final Set<String> encryptedTypeNames;

    private final EncryptedDataService service;

    private final KeyIdFunction keyIdFunction;

    /**
     * Convenience constructor using the default type-name constant ({@value #DEFAULT_TYPE_FIELD}) and deriving the key
     * identifier from {@link StreamId#asString()}.
     *
     * @param markerInterfaceName Fully qualified name of the "requires encryption at rest" marker interface to scan for.
     * @param service             Service used to determine the key version (and thus verify the key exists).
     * @param classesDirs         Optional directories with class files to index in addition to the classpath Jandex index.
     */
    public JandexKeyIdResolver(@NotEmpty final String markerInterfaceName,
                               final EncryptedDataService service,
                               final File... classesDirs) {
        this(markerInterfaceName, DEFAULT_TYPE_FIELD, service, DEFAULT_KEY_ID_FUNCTION, classesDirs);
    }

    /**
     * Constructor with all dependencies.
     *
     * @param markerInterfaceName Fully qualified name of the "requires encryption at rest" marker interface to scan for.
     * @param typeNameFieldName   Name of the public static constant on each event class that holds its type name.
     * @param service             Service used to determine the key version (and thus verify the key exists).
     * @param keyIdFunction       Derives the key identifier from the stream and optional tenant the event belongs to.
     * @param classesDirs         Optional directories with class files to index in addition to the classpath Jandex index.
     */
    public JandexKeyIdResolver(@NotEmpty final String markerInterfaceName, @NotEmpty final String typeNameFieldName,
                               final EncryptedDataService service, final KeyIdFunction keyIdFunction,
                               final File... classesDirs) {
        super();
        Contract.requireArgNotEmpty("markerInterfaceName", markerInterfaceName);
        Contract.requireArgNotEmpty("typeNameFieldName", typeNameFieldName);
        Contract.requireArgNotNull("service", service);
        Contract.requireArgNotNull("keyIdFunction", keyIdFunction);
        this.service = service;
        this.keyIdFunction = keyIdFunction;
        this.encryptedTypeNames = scan(markerInterfaceName, typeNameFieldName, classesDirs);
    }

    @Override
    public Optional<String> getKeyId(final StreamId streamId, @Nullable final TenantId tenantId, final TypeName dataType) {
        if (!encryptedTypeNames.contains(dataType.asBaseType())) {
            // Type does not require encryption at rest: store in plain text.
            return Optional.empty();
        }
        final String keyId = keyIdFunction.apply(streamId, tenantId);
        try {
            // Determine the current version of the key. This also verifies the key exists.
            service.getKeyVersion(keyId);
        } catch (final EncryptionKeyIdUnknownException ex) {
            throw new EscEncryptionException("No encryption key '" + keyId + "' for stream " + streamId.asString()
                    + " (event type '" + dataType.asBaseType() + "' requires encryption at rest)", ex);
        }
        return Optional.of(keyId);
    }

    private static Set<String> scan(final String markerInterfaceName, final String typeNameFieldName,
                                    final File... classesDirs) {
        final IndexView index = CompositeIndex.create(loadClasspathIndex(), indexClassesDirs(classesDirs));
        final Set<String> typeNames = new LinkedHashSet<>();
        for (final ClassInfo classInfo : index.getAllKnownImplementations(DotName.createSimple(markerInterfaceName))) {
            if (Modifier.isAbstract(classInfo.flags()) || Modifier.isInterface(classInfo.flags())) {
                continue;
            }
            final Class<?> clasz = JandexUtils.loadClass(classInfo.name());
            final String typeName = readTypeName(clasz, typeNameFieldName);
            typeNames.add(typeName);
            LOG.info("Event type requires encryption at rest: '{}' ({})", typeName, clasz.getName());
        }
        return typeNames;
    }

    private static IndexView loadClasspathIndex() {
        try {
            return new JandexIndexFileReader.Builder().addDefaultResource().build().loadR();
        } catch (final RuntimeException ex) {
            // No (readable) classpath Jandex index - fall back to the indexed class directories only.
            LOG.warn("Could not read the classpath Jandex index, relying on indexed class directories: {}", ex.getMessage());
            return new Indexer().complete();
        }
    }

    private static IndexView indexClassesDirs(final File... classesDirs) {
        final Indexer indexer = new Indexer();
        final List<File> knownClassFiles = new ArrayList<>();
        for (final File classesDir : classesDirs) {
            JandexUtils.indexDir(indexer, knownClassFiles, classesDir);
        }
        return indexer.complete();
    }

    private static String readTypeName(final Class<?> clasz, final String typeNameFieldName) {
        try {
            final Object value = clasz.getField(typeNameFieldName).get(null);
            if (value == null) {
                throw new IllegalStateException("The '" + typeNameFieldName + "' constant of " + clasz.getName() + " is null");
            }
            return value.toString();
        } catch (final NoSuchFieldException | IllegalAccessException ex) {
            throw new IllegalStateException("Cannot read the '" + typeNameFieldName + "' type name constant from "
                    + clasz.getName() + " (every class flagged for encryption at rest must declare it)", ex);
        }
    }

    /**
     * Derives the encryption key identifier from the stream and the optional tenant an event belongs to.
     */
    @FunctionalInterface
    public interface KeyIdFunction {

        /**
         * Returns the key identifier to use for the given stream and tenant.
         *
         * @param streamId Stream the event is appended to.
         * @param tenantId Optional tenant the event belongs to (may be {@code null}).
         * @return Key identifier.
         */
        String apply(StreamId streamId, @Nullable TenantId tenantId);

    }

}
