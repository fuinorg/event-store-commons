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
package org.fuin.esc.api;

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

/**
 * Decorates a {@link DeserializerRegistry} so that every event is automatically lifted to its latest version
 * (via a {@link ConverterRegistry}) right after it is deserialized. Because all persistent backends resolve
 * their deserializer through {@link DeserializerRegistry#getDeserializer(SerializedDataType, EnhancedMimeType)},
 * passing an instance of this class as the deserializer registry upcasts events for every backend
 * (relational, gRPC, encrypting) without any backend-specific changes. The in-memory backend keeps live
 * objects and never deserializes, so it is unaffected.
 */
@ThreadSafe
public final class UpcastingDeserializerRegistry implements DeserializerRegistry {

    private final DeserializerRegistry delegate;

    private final ConverterRegistry converters;

    /**
     * Constructor with all mandatory data.
     *
     * @param delegate   Registry that resolves the version-specific deserializers.
     * @param converters Registry that lifts the deserialized object to the latest version.
     */
    public UpcastingDeserializerRegistry(final DeserializerRegistry delegate, final ConverterRegistry converters) {
        super();
        Contract.requireArgNotNull("delegate", delegate);
        Contract.requireArgNotNull("converters", converters);
        this.delegate = delegate;
        this.converters = converters;
    }

    @Override
    @NotNull
    public Deserializer getDeserializer(final SerializedDataType type, final EnhancedMimeType mimeType) {
        return new UpcastingDeserializer(delegate.getDeserializer(type, mimeType), converters);
    }

    @Override
    @NotNull
    public Deserializer getDeserializer(final SerializedDataType type) {
        return new UpcastingDeserializer(delegate.getDeserializer(type), converters);
    }

    @Override
    @Nullable
    public EnhancedMimeType getDefaultMimeType() {
        return delegate.getDefaultMimeType();
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type) {
        return delegate.deserializerExists(type);
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type, final EnhancedMimeType mimeType) {
        return delegate.deserializerExists(type, mimeType);
    }

    /**
     * Deserializer that unmarshals via a delegate and then lifts the result to the latest version.
     */
    @ThreadSafe
    private static final class UpcastingDeserializer implements Deserializer {

        private final Deserializer delegate;

        private final ConverterRegistry converters;

        private UpcastingDeserializer(final Deserializer delegate, final ConverterRegistry converters) {
            this.delegate = delegate;
            this.converters = converters;
        }

        @Override
        public <T> T unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
            final Object deserialized = delegate.unmarshal(data, type, mimeType);
            return converters.upcastToLatest(type, mimeType.getVersion(), deserialized);
        }

    }

}
