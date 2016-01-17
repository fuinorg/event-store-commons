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
package org.fuin.esc.jpa.examples;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.jpa.JpaEvent;
import org.fuin.esc.jpa.JpaStream;
import org.fuin.esc.jpa.JpaStreamEvent;
import org.fuin.objects4j.common.Contract;

/**
 * Vendor stream.
 */
@Table(name = "VENDOR_STREAMS")
@Entity
public class VendorStream extends JpaStream {

    @Id
    @NotNull
    @Column(name = "VENDOR_ID", nullable = false, updatable = false, length = 36)
    private String vendorId;

    /**
     * Protected default constructor for JPA.
     */
    protected VendorStream() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param vendorId
     *            Unique vendor identifier.
     */
    public VendorStream(@NotNull final String vendorId) {
        super();
        Contract.requireArgNotNull("vendorId", vendorId);
        this.vendorId = vendorId;
    }

    /**
     * Returns the unique vendor identifier as string.
     * 
     * @return Vendor identifier.
     */
    public final String getVendorId() {
        return vendorId;
    }

    @Override
    public final JpaStreamEvent createEvent(final StreamId streamId, final JpaEvent eventEntry) {
        incVersion();
        return new VendorEvent(getVendorId(), getVersion(), eventEntry);
    }

    @Override
    public final String toString() {
        return vendorId;
    }

}
