/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
package org.fuin.esc.intf;

import java.nio.charset.Charset;

import org.fuin.objects4j.common.Nullable;

/**
 * Creates a meta data byte array on demand and allows setting some standard
 * properties.
 */
public interface MetaDataBuilder {

    /**
     * Sets the encoding of the event data structure. CAUTION: This is NOT the
     * encoding of the meta data, but the encoding for the event data.
     * 
     * @param encoding
     *            Encoding of the event data.
     */
    public void setDataEncoding(@Nullable Charset encoding);

    /**
     * Sets the version of the event data structure. CAUTION: This is NOT the
     * version of the meta data, but the version for the event data.
     * 
     * @param version
     *            Version of the event data.
     */
    public void setDataVersion(@Nullable String version);

    /**
     * Builds the data structure.
     * 
     * @return Serialized meta data.
     */
    public Data build();

}
