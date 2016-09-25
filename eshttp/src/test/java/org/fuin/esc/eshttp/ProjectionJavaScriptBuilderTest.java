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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.TypeName;
import org.junit.Test;

/**
 * Tests the {@link ProjectionJavaScriptBuilder} class.
 */
// CHECKSTYLE:OFF Test
public class ProjectionJavaScriptBuilderTest {

    @Test
    public void testNoEventType() {

        final ProjectionJavaScriptBuilder testee = new ProjectionJavaScriptBuilder("AccountsView", "account");
        try {
            testee.build();
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage())
                    .isEqualTo("No types were added. Use 'type(String)' to add at least one event.");
        }

    }

    @Test
    public void testOneEventType() {

        final ProjectionJavaScriptBuilder testee = new ProjectionJavaScriptBuilder("AccountsView", "account");
        testee.type("AccountDebited");
        assertThat(testee.build()).isEqualTo("fromCategory('account').foreachStream().when({"
                + "'AccountDebited': function(state, ev) { linkTo('AccountsView', ev); }" + "})");

    }

    @Test
    public void testTwoEventTypes() {

        final ProjectionJavaScriptBuilder testee = new ProjectionJavaScriptBuilder("AccountsView", "account");
        testee.type("AccountDebited");
        testee.type("AccountCredited");
        assertThat(testee.build()).isEqualTo("fromCategory('account').foreachStream().when({"
                + "'AccountDebited': function(state, ev) { linkTo('AccountsView', ev); },"
                + "'AccountCredited': function(state, ev) { linkTo('AccountsView', ev); }" + "})");

    }

    @Test
    public void testEventType() {

        final ProjectionJavaScriptBuilder testee = new ProjectionJavaScriptBuilder(
                new SimpleStreamId("AccountsView"), new SimpleStreamId("account"));
        testee.type(new TypeName("AccountDebited"));
        assertThat(testee.build()).isEqualTo("fromCategory('account').foreachStream().when({"
                + "'AccountDebited': function(state, ev) { linkTo('AccountsView', ev); }" + "})");

    }

    @Test
    public void testEventTypes() {

        final ProjectionJavaScriptBuilder testee = new ProjectionJavaScriptBuilder(
                new SimpleStreamId("AccountsView"), new SimpleStreamId("account"));
        final List<TypeName> list = new ArrayList<>();
        list.add(new TypeName("AccountDebited"));
        list.add(new TypeName("AccountCredited"));
        testee.types(list);
        ;
        assertThat(testee.build()).isEqualTo("fromCategory('account').foreachStream().when({"
                + "'AccountDebited': function(state, ev) { linkTo('AccountsView', ev); },"
                + "'AccountCredited': function(state, ev) { linkTo('AccountsView', ev); }" + "})");

    }

}
// CHECKSTYLE:ON
