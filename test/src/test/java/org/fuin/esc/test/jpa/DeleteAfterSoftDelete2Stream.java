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
package org.fuin.esc.test.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fuin.esc.jpa.JpaEvent;
import org.fuin.esc.jpa.JpaStream;
import org.fuin.esc.jpa.JpaStreamEvent;

/**
 * Contains stream.
 */
@Table(name = "DELETE_AFTER_SOFT_DELETE_2_STREAMS")
@Entity
public class DeleteAfterSoftDelete2Stream extends JpaStream {

    @Id
    @NotNull
    @Column(name = "ID", nullable = false, updatable = false)
    private Integer id;

    /**
     * Protected default constructor for JPA.
     */
    public DeleteAfterSoftDelete2Stream() {
        super();
        this.id = 1;
    }

    /**
     * Creates a container that stores the given event entry.
     * 
     * @param eventEntry
     *            Event entry to convert into a JPA variant.
     * 
     * @return JPA entity.
     */
    public final JpaStreamEvent createEvent(@NotNull final JpaEvent eventEntry) {
        incVersion();
        return new DeleteAfterSoftDelete2Event(getVersion(), eventEntry);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }

}
