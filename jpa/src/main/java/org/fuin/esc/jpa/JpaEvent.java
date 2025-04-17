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
package org.fuin.esc.jpa;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EventId;

import java.time.ZonedDateTime;


/**
 * Stores an event and it's meta data.
 */
@Table(name = JpaEvent.TABLE_NAME)
@Entity
@SequenceGenerator(name = "EventEntrySequenceGenerator", sequenceName = "EVENTS_SEQ", allocationSize = 1000)
public class JpaEvent {

    /** SQL table name. */
    public static final String TABLE_NAME = "events";

    /** SQL ID column name. */
    public static final String COLUMN_ID = "id";

    /** SQL EVENT ID column name. */
    public static final String COLUMN_EVENT_ID = "event_id";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EventEntrySequenceGenerator")
    @Column(name = COLUMN_ID, nullable = false)
    private Long id;

    @Column(name = COLUMN_EVENT_ID, length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String eventId;

    @Column(name = "created", nullable = false)
    private ZonedDateTime created;

    @Embedded
    @NotNull
    private JpaData data;

    @Embedded
    @AttributeOverride(name = "type", column = @Column(name = "META_TYPE"))
    @AttributeOverride(name = "mimeType", column = @Column(name = "META_MIME_TYPE"))
    @AttributeOverride(name = "raw", column = @Column(name = "META_RAW"))
    private JpaData meta;

    /**
     * Protected default constructor only required for JPA.
     */
    protected JpaEvent() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor without metadata.
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
    public JpaEvent(@NotNull final EventId eventId, @NotNull final JpaData data,
                    @Nullable final JpaData meta) {
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
    public Long getId() {
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
    public EventId getEventId() {
        return new EventId(eventId);
    }

    /**
     * Returns the time when the event was created.
     *
     * @return Date, time and zone of event's creation.
     */
    @NotNull
    public ZonedDateTime getCreated() {
        return created;
    }

    /**
     * Returns the data of the event.
     *
     * @return The event.
     */
    @NotNull
    public JpaData getData() {
        return data;
    }

    /**
     * Returns the metadata of the event (Optional).
     *
     * @return The event's metadata or NULL.
     */
    public JpaData getMeta() {
        return meta;
    }

    /**
     * Initializes object values before it's created in the database.
     */
    @PrePersist
    void onPrePersist() {
        created = ZonedDateTime.now();
    }

}
