/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.spi;

import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Handles JSON meta data creation/update.
 */
public final class JsonMetaDataBuilder implements MetaDataBuilder<JsonObject> {

    private JsonObjectBuilder builder;

    @Override
    public final void init(final JsonObject obj) {
        if (obj == null) {
            this.builder = Json.createObjectBuilder();
        } else {
            this.builder = copy(obj);
        }
    }

    @Override
    public final void add(final String key, final String value) {
        builder.add(key, value);
    }

    @Override
    public final void add(final String key, final boolean value) {
        builder.add(key, value);
    }

    @Override
    public final void add(final String key, final int value) {
        builder.add(key, value);
    }

    @Override
    public final JsonObject build() {
        return builder.build();
    }

    /**
     * Creates a new builder by copying the given object.
     * 
     * @param obj
     *            Object to copy.
     * 
     * @return Builder pre-populated with copied content.
     */
    public static JsonObjectBuilder copy(final JsonObject obj) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final Iterator<String> keyIt = obj.keySet().iterator();
        while (keyIt.hasNext()) {
            final String key = keyIt.next();
            final JsonValue value = obj.get(key);
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                builder.add(key, copy((JsonObject) value));
            } else if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                builder.add(key, copy((JsonArray) value));
            } else {
                builder.add(key, value);
            }
        }
        return builder;
    }

    /**
     * Creates a new builder by copying the given array.
     * 
     * @param arr
     *            Array to copy.
     * 
     * @return Builder pre-populated with copied content.
     */
    public static JsonArrayBuilder copy(final JsonArray arr) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        final Iterator<JsonValue> it = arr.iterator();
        while (it.hasNext()) {
            final JsonValue value = it.next();
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                builder.add(copy((JsonObject) value));
            } else if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                builder.add(copy((JsonArray) value));
            } else {
                builder.add(value);
            }
        }
        return builder;
    }

}
