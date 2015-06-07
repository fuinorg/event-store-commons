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
package org.fuin.esc.jpa;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.EventId;
import org.fuin.objects4j.common.DateTimeAdapter;
import org.fuin.objects4j.common.Nullable;
import org.joda.time.DateTime;

/**
 * Stores an event and it's meta data.
 */
@Table(name = "EVENTS")
@Entity
@SequenceGenerator(name = "EventEntrySequenceGenerator", sequenceName = "EVENTS_SEQ", allocationSize = 1000)
public class JpaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EventEntrySequenceGenerator")
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "EVENT_ID", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String eventId;

    /** Date, time and zone the event was created. */
    @Convert(converter = DateTimeAdapter.class)
    @Column(name = "TIMESTAMP", nullable = false)
    private DateTime timestamp;

    @Embedded
    @NotNull
    private JpaData data;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "META_TYPE")),
            @AttributeOverride(name = "mimeType", column = @Column(name = "META_MIME_TYPE")),
            @AttributeOverride(name = "raw", column = @Column(name = "META_RAW")) })
    private JpaData meta;

    /**
     * Protected default constructor only required for JPA.
     */
    protected JpaEvent() {
        super();
    }

    /**
     * Constructor without meta data.
     * 
     * @param eventId
     *            Unique identifier of the event. Generated on the client and
     *            used to achieve idempotence when trying to append the same
     *            event multiple times.
     * @param data
     *            Data of the event.
     */
    public JpaEvent(@NotNull final EventId eventId, @NotNull final JpaData data) {
        this(eventId, data, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param eventId
     *            Unique identifier of the event. Generated on the client and
     *            used to achieve idempotence when trying to append the same
     *            event multiple times.
     * @param data
     *            Data of the event.
     * @param meta
     *            Meta data (Optional).
     */
    public JpaEvent(@NotNull final EventId eventId,
            @NotNull final JpaData data, @Nullable final JpaData meta) {
        super();
        this.eventId = eventId.asBaseType().toString();
        this.data = data;
        this.meta = meta;
    }

    /**
     * Returns the unique identifier of the entry.
     * 
     * @return Unique entry ID.
     */
    public final Long getId() {
        return id;
    }

    /**
     * Returns the unique identifier of the event. Generated on the client and
     * used to achieve idempotence when trying to append the same event multiple
     * times.
     * 
     * @return Unique event ID.
     */
    @NotNull
    public final EventId getEventId() {
        return new EventId(eventId);
    }

    /**
     * Returns the time when the event was created.
     * 
     * @return Date, time and zone of event's creation.
     */
    @NotNull
    public final DateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the data of the event.
     * 
     * @return The event.
     */
    @NotNull
    public final JpaData getData() {
        return data;
    }

    /**
     * Returns the meta data of the event (Optional).
     * 
     * @return The event's meta data or NULL.
     */
    public final JpaData getMeta() {
        return meta;
    }

    /**
     * Initializes object values before it's created in the database.
     */
    @PrePersist
    final void onPrePersist() {
        timestamp = new DateTime();
    }

}
