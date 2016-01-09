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

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.HashMap;

import javax.persistence.TypedQuery;

import org.fuin.esc.api.EventId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.units4j.AbstractPersistenceTest;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class JpaEventTest extends AbstractPersistenceTest {

    @Test
    public void testPersist() {

        // PREPARE
        final EventId eventId = new EventId();
        final TypeName type = new TypeName("HelloWorld");
        final int version = 1;
        final String xml = "<hello name=\"world\" />";
        final JpaEvent testee = create(eventId, type, version, xml);

        // beginTransaction();
        // Nothing to prepare here...
        // commitTransaction();

        // TEST
        beginTransaction();
        getEm().persist(testee);
        commitTransaction();

        // VERIFY
        beginTransaction();
        final TypedQuery<JpaEvent> query = getEm().createQuery(
                "select ee from JpaEvent ee where ee.eventId=:eventId", JpaEvent.class);
        query.setParameter("eventId", eventId.toString());
        final JpaEvent found = query.getSingleResult();
        assertThat(found).isNotNull();
        assertThat(found.getEventId()).isEqualTo(eventId);
        assertThat(found.getCreated()).isNotNull();
        assertThat(found.getData()).isNotNull();
        assertThat(found.getData().getType()).isEqualTo(type);
        assertThat(found.getData().getRaw()).isNotNull();
        assertThat(found.getMeta()).isNull();
        final String data = new String(found.getData().getRaw(), found.getData().getMimeType().getEncoding());
        assertThat(data).isEqualTo(xml);
        commitTransaction();

    }

    private JpaEvent create(final EventId eventId, final TypeName type, final int version, final String xml) {
        final Charset encoding = Charset.forName("utf-8");
        final EnhancedMimeType mimeType = EnhancedMimeType.create("application", "xml", encoding, ""
                + version, new HashMap<String, String>());
        final JpaEvent eventEntry = new JpaEvent(eventId, new JpaData(type, mimeType, xml.getBytes(encoding)));
        return eventEntry;
    }

}
// CHECKSTYLE:ON
