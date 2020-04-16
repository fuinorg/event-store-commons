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
package org.fuin.esc.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.SimpleTenantId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Tests the {@link TenantStreamId} class.
 */
// CHECKSTYLE:OFF Test
public class TenantStreamIdTest {

    private static final TenantId TENANT_ID = new SimpleTenantId("mycompany");
    
    private static final StreamId STREAM_ID = new SimpleStreamId("MyStream1");

    private TenantStreamId testee;

    @Before
    public void setup() {        
        testee = new TenantStreamId(TENANT_ID, STREAM_ID);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(TenantStreamId.class)
        .withNonnullFields("tenantId", "delegate")
        .withPrefabValues(TenantId.class, new SimpleTenantId("red"), new SimpleTenantId("black"))
        .withPrefabValues(StreamId.class, new SimpleStreamId("green"), new SimpleStreamId("yellow"))
        .verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getName()).isEqualTo(TENANT_ID.asString() + "-" + STREAM_ID.asString());
        assertThat(testee.asString()).isEqualTo(TENANT_ID.asString() + "-" + STREAM_ID.asString());
        assertThat(testee.isProjection()).isFalse();
        assertThat(testee.getParameters()).isEmpty();
        assertThat(testee.getDelegate()).isEqualTo(STREAM_ID);
        assertThat(testee.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSingleParamValue() {
        testee.getSingleParamValue();
    }
}
// CHECKSTYLE:ON
