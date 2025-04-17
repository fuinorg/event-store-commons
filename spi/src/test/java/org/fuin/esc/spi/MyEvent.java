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
package org.fuin.esc.spi;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.TestOmitted;

import java.io.Serializable;
import java.util.UUID;

/**
 * Something interesting happened. Equals and hash code are based on the UUID.
 */
@TestOmitted("This is only a test class")
public final class MyEvent implements Serializable {

    private static final long serialVersionUID = 100L;

    /** Unique name of the event. */
    public static final TypeName TYPE = new TypeName("MyEvent");

    /** Unique name of the serialized event. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    private String id;

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
    public MyEvent(@NotNull final UUID uuid, @NotEmpty final String description) {
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

    @Override
    public final String toString() {
        return "My event: " + description;
    }

}
