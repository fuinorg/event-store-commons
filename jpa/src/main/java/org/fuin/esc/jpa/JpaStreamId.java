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
package org.fuin.esc.jpa;

import org.fuin.esc.api.StreamId;

/**
 * Specialized version of a stream identifier that allows to configure table and JPA entity name.
 */
public interface JpaStreamId extends StreamId {

    /**
     * Returns the name of the entity class.
     * 
     * @return Name of the entity that has the {@link javax.persistence.Table} JPA annotation.
     */
    public String getEntityName();

    /**
     * Returns the name of the database table to use for the stream.
     * 
     * @return Name that is configured in the {@link javax.persistence.Table} JPA annotation.
     */
    public String getNativeTableName();

}
