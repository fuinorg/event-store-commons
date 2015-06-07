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
package org.fuin.esc.jpa.examples;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.objects4j.common.NeverNull;

/**
 * A vendor entity was created.
 */
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
    @NeverNull
    public final String getId() {
        return id;
    }

    @Override
    public final String toString() {
        return "Created vendor '" + id + "'";
    }

}
