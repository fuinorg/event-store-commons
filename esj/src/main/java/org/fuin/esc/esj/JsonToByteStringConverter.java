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
package org.fuin.esc.esj;

import javax.json.JsonObject;

import lt.emasina.esj.model.converter.ObjectToByteStringConverter;
import lt.emasina.esj.model.converter.StringToByteStringConverter;

import com.google.protobuf.ByteString;

/**
 * Converts a JSON object into protobuf's ByteString.
 */
public final class JsonToByteStringConverter implements
        ObjectToByteStringConverter<JsonObject> {

    private final StringToByteStringConverter delegate;

    /**
     * Default constructor.
     */
    public JsonToByteStringConverter() {
        super();
        delegate = new StringToByteStringConverter();
    }

    @Override
    public final int getContentType() {
        return JSON_DATA_TYPE;
    }

    @Override
    public final ByteString convert(final JsonObject obj) {
        return delegate.convert(obj);
    }

}
