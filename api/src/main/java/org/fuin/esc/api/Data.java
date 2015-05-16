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
package org.fuin.esc.api;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.NeverNull;
import org.fuin.objects4j.vo.ValueObject;

/**
 * Represents a block of data in a serialized form.
 */
@Immutable
@XmlRootElement(name = "data")
public class Data implements ValueObject, Serializable {

    private static final long serialVersionUID = 1000L;

    /** Unique type of the data. */
    @NotNull
    @XmlAttribute(name = "type")
    private String type;

    /** Internet Media Type that classifies the raw event data. */
    @NotNull
    @XmlAttribute(name = "mime-type")
    private VersionedMimeType mimeType;

    /** Raw event data in format defined by the mime type and encoding. */
    @NotNull
    @XmlValue
    @XmlJavaTypeAdapter(CDataXmlAdapter.class)
    private String content;

    /**
     * Protected constructor for deserialization.
     */
    protected Data() {
        super();
    }

    /**
     * Creates a data object.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * @param mimeType
     *            Internet Media Type with encoding and version that classifies
     *            the data.
     * @param content
     *            Content.
     */
    public Data(@NotNull final String type,
            @NotNull final VersionedMimeType mimeType,
            @NotNull final String content) {
        super();

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("content", content);

        this.type = type;
        this.mimeType = mimeType;
        this.content = content;
    }

    /**
     * Returns the unique identifier for the type of data.
     * 
     * @return Unique and never changing type name.
     */
    @NeverNull
    public final String getType() {
        return type;
    }

    /**
     * Returns the Internet Media Type that classifies the data.
     * 
     * @return Mime type.
     */
    @NeverNull
    public final VersionedMimeType getMimeType() {
        return mimeType;
    }

    /**
     * Returns the raw data block.
     * 
     * @return Raw data.
     */
    @NeverNull
    public final String getContent() {
        return content;
    }

    /**
     * Returns the information if the content is XML.
     * 
     * @return TRUE if the mime type is 'application/xml' else FALSE.
     */
    public final boolean isXml() {
        return mimeType.getBaseType().equals("application/xml");
    }

    /**
     * Returns the information if the content is JSON.
     * 
     * @return TRUE if the mime type is 'application/json' else FALSE.
     */
    public final boolean isJson() {
        return mimeType.getBaseType().equals("application/json");
    }

}
