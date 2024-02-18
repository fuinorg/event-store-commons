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
package org.fuin.esc.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.esc.spi.EscSpiUtils;

/**
 * Multiple slice of data from a stream.
 */
@XmlRootElement(name = "slices")
public final class Slices implements Serializable {

    private static final long serialVersionUID = 1000L;

    @XmlElement(name = "slice")
    private List<Slice> slices;

    /**
     * Default constructor.
     */
    public Slices() {
        super();
        this.slices = new ArrayList<Slice>();
    }

    /**
     * Constructor with all data.
     *
     * @param slices
     *            The slices read. The list is internally copied to avoid
     */
    public Slices(final List<Slice> slices) {
        this();
        append(slices);
    }

    /**
     * Appends slices to the list.
     *
     * @param slices
     *            Slices to add.
     */
    public void append(final List<Slice> slices) {
        if (slices != null && slices.size() > 0) {
            this.slices.addAll(slices);
        }
    }

    /**
     * Appends slices to the list.
     *
     * @param slices
     *            Slices to add.
     */
    public void append(final Slice... slices) {
        if (slices != null && slices.length > 0) {
            this.slices.addAll(EscSpiUtils.asList(slices));
        }
    }

    /**
     * Returns the slices read.
     *
     * @return Unmodifiable list of slices.
     */
    @NotNull
    public List<Slice> getSlices() {
        return Collections.unmodifiableList(slices);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("slices", slices).toString();
    }

}
