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

import java.util.UUID;

import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.jpa.examples.AggregateStreamId;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class JpaUtilsTest {

    
    @Test
    public void testCamelCaseToUnderscore() {

        // TEST & VERIFY
        assertThat(JpaUtils.camel2Underscore(null)).isEqualTo(null);
        assertThat(JpaUtils.camel2Underscore("")).isEqualTo("");
        assertThat(JpaUtils.camel2Underscore("a")).isEqualTo("a");
        assertThat(JpaUtils.camel2Underscore("ab")).isEqualTo("ab");
        assertThat(JpaUtils.camel2Underscore("A")).isEqualTo("a");
        assertThat(JpaUtils.camel2Underscore("AB")).isEqualTo("a_b");
        assertThat(JpaUtils.camel2Underscore("Ab")).isEqualTo("ab");
        assertThat(JpaUtils.camel2Underscore("aB")).isEqualTo("a_b");
        assertThat(JpaUtils.camel2Underscore("aBcDeF")).isEqualTo("a_bc_de_f");

    }

    @Test
    public void testNativeEventsTableNameJpaStreamId() {

        // PREPARE
        final String entityName = "MyTable";
        final String nativeTableName = "any_name";
        final StreamId streamId = new SimpleJpaStreamId(entityName, nativeTableName);

        // TEST
        final String result = JpaUtils.nativeEventsTableName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(nativeTableName);

    }

    @Test
    public void testNativeEventsTableNameProjection() {

        // PREPARE
        final String entityName = "MyOwn";
        final StreamId streamId = new ProjectionStreamId(entityName);

        // TEST
        final String result = JpaUtils.nativeEventsTableName(streamId);

        // VERIFY
        assertThat(result).isEqualTo("my_own");

    }
    
    @Test
    public void testNativeEventsTableNameWithArgs() {

        // PREPARE
        final String entityName = "Customer";
        final StreamId streamId = new AggregateStreamId(entityName, "customerId", UUID.randomUUID().toString());


        // TEST
        final String result = JpaUtils.nativeEventsTableName(streamId);

        // VERIFY
        assertThat(result).isEqualTo("customer_events");

    }

    @Test
    public void testNativeEventsTableNameNoArgs() {

        // PREPARE
        final String entityName = "MyOwn";
        final StreamId streamId = new SimpleStreamId(entityName);

        // TEST
        final String result = JpaUtils.nativeEventsTableName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(NoParamsEvent.NO_PARAMS_EVENTS_TABLE);

    }
    
    @Test
    public void testStreamEntityNameJpaStreamId() {

        // PREPARE
        final String entityName = "MyEntity";
        final String nativeTableName = "any_name";
        final StreamId streamId = new SimpleJpaStreamId(entityName, nativeTableName);

        // TEST
        final String result = JpaUtils.streamEntityName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(entityName);

    }

    @Test
    public void testStreamEntityNameProjection() {

        // PREPARE
        final String entityName = "MyProjection";
        final StreamId streamId = new ProjectionStreamId(entityName);

        // TEST
        final String result = JpaUtils.streamEntityName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(entityName);

    }
    
    @Test
    public void testStreamEntityNameWithArgs() {

        // PREPARE
        final String entityName = "Customer";
        final StreamId streamId = new AggregateStreamId(entityName, "customerId", UUID.randomUUID().toString());

        // TEST
        final String result = JpaUtils.streamEntityName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(entityName + "Stream");

    }

    @Test
    public void testStreamEntityNameNoArgs() {

        // PREPARE
        final String entityName = "MyOwn";
        final StreamId streamId = new SimpleStreamId(entityName);

        // TEST
        final String result = JpaUtils.streamEntityName(streamId);

        // VERIFY
        assertThat(result).isEqualTo(NoParamsStream.class.getSimpleName());

    }
    
}
// CHECKSTYLE:ON
