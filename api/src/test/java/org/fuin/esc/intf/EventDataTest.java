/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
package org.fuin.esc.intf;

import static org.fest.assertions.Assertions.assertThat;
import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link EventData} class.
 */
// CHECKSTYLE:OFF Test
public class EventDataTest {

    private static final String ID = "5741bcf1-9292-446b-84c1-957ed53b8d88";

    private static final String TYPE = "MyEvent";

    private static final VersionedMimeType MIME_TYPE = VersionedMimeType
            .create("application/xml; encoding=utf-8; version=1");

    private static final String CONTENT = "<myEvent/>";

    private static final Data EVENT_DATA = new Data(TYPE, MIME_TYPE, CONTENT);

    private EventData testee;

    @Before
    public void setup() {
        final MetaDataBuilder metaDataBuilder = null;
        testee = new EventData(ID, EVENT_DATA, metaDataBuilder);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(EventData.class).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getId()).isEqualTo(ID);
        assertThat(testee.getData().getType()).isEqualTo(TYPE);
        assertThat(testee.getData().getMimeType()).isEqualTo(MIME_TYPE);
        assertThat(testee.getData().getContent()).isEqualTo(CONTENT);
    }

}
// CHECKSTYLE:ON
