/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.spi;

import javax.json.JsonObject;

/**
 * Handles access to JSON meta data.
 */
public final class JsonMetaDataAccessor implements MetaDataAccessor<JsonObject> {

    private JsonObject obj;

    @Override
    public final void init(final JsonObject obj) {
        this.obj = obj;
    }

    @Override
    public final String getString(final String key) {
        if (obj == null) {
            return null;
        }
        if (!obj.containsKey(key)) {
            return null;
        }
        return obj.getString(key);
    }

    @Override
    public final Boolean getBoolean(final String key) {
        if (obj == null) {
            return null;
        }
        if (!obj.containsKey(key)) {
            return null;
        }
        return obj.getBoolean(key);
    }

    @Override
    public final Integer getInteger(final String key) {
        if (obj == null) {
            return null;
        }
        if (!obj.containsKey(key)) {
            return null;
        }
        return obj.getInt(key);
    }

}
