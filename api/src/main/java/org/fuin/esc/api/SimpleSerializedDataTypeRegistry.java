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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all known types and the corresponding class.
 */
public final class SimpleSerializedDataTypeRegistry implements SerializedDataTypeRegistry {

    private final Map<SerializedDataType, Class<?>> map;

    /**
     * Default constructor.
     */
    private SimpleSerializedDataTypeRegistry() {
        super();
        map = new HashMap<>();
    }

    private void add(@NotNull final SerializedDataType type, final Class<?> clasz) {
        map.put(type, clasz);
    }

    private void add(@NotNull final SerializedDataType2ClassMapping mapping) {
        map.put(mapping.type(), mapping.clasz());
    }


    @Override
    @NotNull
    public Class<?> findClass(@NotNull final SerializedDataType type) {
        Contract.requireArgNotNull("type", type);
        final Class<?> clasz = map.get(type);
        if (clasz == null) {
            throw new IllegalArgumentException("No class found for: " + type);
        }
        return clasz;
    }

    @Override
    public List<TypeClass> findAll() {
        return map.entrySet().stream()
                .map(e -> new TypeClass(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Builds an instance of the outer class.
     */
    public static final class Builder implements SerializedDataTypeRegistry.Builder<SimpleSerializedDataTypeRegistry, Builder> {

        private SimpleSerializedDataTypeRegistry delegate;

        /**
         * Default constructor.
         */
        public Builder() {
            delegate = new SimpleSerializedDataTypeRegistry();
        }

        @Override
        public Builder add(SerializedDataType type, Class<?> clasz) {
            Contract.requireArgNotNull("type", type);
            Contract.requireArgNotNull("clasz", clasz);
            delegate.add(type, clasz);
            return this;
        }

        @Override
        public Builder add(SerializedDataType2ClassMapping mapping) {
            Contract.requireArgNotNull("mapping", mapping);
            delegate.add(mapping);
            return this;
        }

        @Override
        public SimpleSerializedDataTypeRegistry build() {
            final SimpleSerializedDataTypeRegistry tmp = delegate;
            delegate = new SimpleSerializedDataTypeRegistry();
            return tmp;
        }


    }

}
