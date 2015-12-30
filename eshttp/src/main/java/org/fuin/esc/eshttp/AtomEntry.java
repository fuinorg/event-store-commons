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
package org.fuin.esc.eshttp;

import org.fuin.esc.spi.EnhancedMimeType;

/**
 * Container for different result types.
 * 
 * @param <TYPE>
 *            Type of data and meta data.
 */
public class AtomEntry<TYPE> {

    private final String eventStreamId;
    private final int eventNumber;
    private final String eventType;
    private final String eventId;
    private final EnhancedMimeType dataContentType;
    private final EnhancedMimeType metaContentType;
    private final String metaType;
    private final TYPE data;
    private final TYPE meta;

    /**
     * Constructor with all data.
     * 
     * @param eventStreamId
     * @param eventNumber
     * @param eventType
     * @param eventId
     * @param dataContentType
     * @param metaContentType
     * @param metaType
     * @param data
     * @param meta
     */
    public AtomEntry(String eventStreamId, int eventNumber, String eventType, String eventId,
            EnhancedMimeType dataContentType, EnhancedMimeType metaContentType, String metaType, TYPE data, TYPE meta) {
        super();
        this.eventStreamId = eventStreamId;
        this.eventNumber = eventNumber;
        this.eventType = eventType;
        this.eventId = eventId;
        this.dataContentType = dataContentType;
        this.metaContentType = metaContentType;
        this.metaType = metaType;
        this.data = data;
        this.meta = meta;
    }

    public String getEventStreamId() {
        return eventStreamId;
    }

    public int getEventNumber() {
        return eventNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public EnhancedMimeType getDataContentType() {
        return dataContentType;
    }

    public EnhancedMimeType getMetaContentType() {
        return metaContentType;
    }

    public String getMetaType() {
        return metaType;
    }

    public TYPE getData() {
        return data;
    }

    public TYPE getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return "AtomEntry [eventStreamId=" + eventStreamId + ", eventNumber=" + eventNumber + ", eventType="
                + eventType + ", eventId=" + eventId + ", dataContentType=" + dataContentType
                + ", metaContentType=" + metaContentType + "]";
    }

}
