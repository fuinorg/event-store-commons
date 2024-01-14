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

import org.fuin.esc.api.EnhancedMimeType;

/**
 * Container for different result types.
 * 
 * @param <TYPE>
 *            Type of data and meta data.
 */
public final class AtomEntry<TYPE> {

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
     *            Unique stream identifier.
     * @param eventNumber
     *            Number of the event.
     * @param eventType
     *            Type of event.
     * @param eventId
     *            Unique event identifier.
     * @param dataContentType
     *            Data content type,
     * @param metaContentType
     *            Meta content type.
     * @param metaType
     *            Type of meta data.
     * @param data
     *            Event data.
     * @param meta
     *            Meta data.
     */
    public AtomEntry(final String eventStreamId, final int eventNumber, final String eventType,
            final String eventId, final EnhancedMimeType dataContentType,
            final EnhancedMimeType metaContentType, final String metaType, final TYPE data, final TYPE meta) {
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

    /**
     * Returns the stream ID.
     * 
     * @return Unique stream identifier.
     */
    public final String getEventStreamId() {
        return eventStreamId;
    }

    /**
     * Returns the event number.
     * 
     * @return Number of the event.
     */
    public final int getEventNumber() {
        return eventNumber;
    }

    /**
     * Returns the type of event.
     * 
     * @return Type of event.
     */
    public final String getEventType() {
        return eventType;
    }

    /**
     * Returns the unique event identifier.
     * 
     * @return Unique event identifier.
     */
    public final String getEventId() {
        return eventId;
    }

    /**
     * Returns the Data content type.
     * 
     * @return Data content type.
     */
    public final EnhancedMimeType getDataContentType() {
        return dataContentType;
    }

    /**
     * Returns the meta content type.
     * 
     * @return Meta content type.
     */
    public final EnhancedMimeType getMetaContentType() {
        return metaContentType;
    }

    /**
     * Returns the type of meta data.
     * 
     * @return Type of meta data.
     */
    public final String getMetaType() {
        return metaType;
    }

    /**
     * Returns the event data.
     * 
     * @return Event data.
     */
    public final TYPE getData() {
        return data;
    }

    /**
     * Returns the meta data.
     * 
     * @return Meta data.
     */
    public final TYPE getMeta() {
        return meta;
    }

    @Override
    public final String toString() {
        return "AtomEntry [eventStreamId=" + eventStreamId + ", eventNumber=" + eventNumber + ", eventType="
                + eventType + ", eventId=" + eventId + ", dataContentType=" + dataContentType
                + ", metaContentType=" + metaContentType + "]";
    }

}
