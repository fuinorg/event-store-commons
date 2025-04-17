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
package org.fuin.esc.jpa.examples;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.utils4j.TestOmitted;

/**
 * A vendor entity was created.
 */
@TestOmitted("This is only a test class")
@XmlRootElement(name = "vendor-created-event")
public final class VendorCreatedEvent {

    /** Unique name of the event used to store it - Should never change. */
    public static final String TYPE = VendorCreatedEvent.class.getSimpleName();

    @XmlAttribute(name = "id")
    private String id;

    /**
     * Default constructor only for deserialization.
     */
    protected VendorCreatedEvent() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param vendorId
     *            Vendor ID.
     */
    public VendorCreatedEvent(@NotNull final String vendorId) {
        super();
        this.id = vendorId;
    }

    /**
     * Returns the vendor ID.
     *
     * @return Unique vendor identifier.
     */
    @NotNull
    public final String getId() {
        return id;
    }

    @Override
    public final String toString() {
        return "Created vendor '" + id + "'";
    }

}
