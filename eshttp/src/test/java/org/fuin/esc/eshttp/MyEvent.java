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
package org.fuin.esc.eshttp;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.objects4j.common.Contract;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotEmpty;

/**
 * Something interesting happened. Equals and hash code are based on the UUID.
 */
@XmlRootElement(name = MyEvent.EL_ROOT_NAME)
public final class MyEvent implements Serializable {

    private static final long serialVersionUID = 100L;

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "MyEvent"; 
    
    /** Unique name of the event. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);
    
    /** Unique serialization name of the event. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(EL_ROOT_NAME);

    @JsonbProperty("id")
    @XmlElement(name = "id")
    private String id;

    @JsonbProperty("description")
    @XmlElement(name = "description")
    private String description;

    /**
     * Protected default constructor for JAXB.
     */
    protected MyEvent() {
        super();
    }

    /**
     * Constructor with random UUID.
     * 
     * @param description
     *            The description.
     */
    public MyEvent(@NotEmpty final String description) {
        super();
        Contract.requireArgNotEmpty("description", description);
        this.id = UUID.randomUUID().toString();
        this.description = description;
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param uuid
     *            The unique identifier of the event.
     * @param description
     *            The description.
     */
    public MyEvent(@NotNull UUID uuid, @NotEmpty final String description) {
        super();
        Contract.requireArgNotNull("uuid", uuid);
        Contract.requireArgNotEmpty("description", description);
        this.id = uuid.toString();
        this.description = description;
    }

    /**
     * Returns the unique identifier.
     * 
     * @return UUID string.
     */
    @NotNull
    public final String getId() {
        return id;
    }

    /**
     * Returns the description.
     * 
     * @return The description.
     */
    @NotEmpty
    public final String getDescription() {
        return description;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MyEvent other = (MyEvent) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public final String toString() {
        return "My event: " + description;
    }

}
