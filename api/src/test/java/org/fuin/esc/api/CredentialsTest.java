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
package org.fuin.esc.api;

import static org.fest.assertions.Assertions.assertThat;

import org.fuin.objects4j.common.ContractViolationException;
import org.junit.Test;

// CHECKSTYLE:OFF Test code
public final class CredentialsTest {

    @Test
    public final void testConstructValid() {
        
        // PREPARE
        final String name = "james";
        final char[] pw = new char[] { 'a', 'b', 'c' };
        
        // TEST
        final Credentials testee = new Credentials(name, pw);
        
        // VERIFY
        assertThat(testee.getUsername()).isEqualTo(name);
        assertThat(testee.getPassword()).isEqualTo(pw);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructNullNull() {
        new Credentials(null, null);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructStringNull() {
        new Credentials("abc", null);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructNullString() {
        new Credentials(null, new char[] { 'a', 'b', 'c' });
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructEmptyChars() {
        new Credentials("", new char[] { 'a', 'b', 'c' });
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructStringEmpty() {
        new Credentials("james", new char[] {});
    }

}
// CHECKSTYLE:ON
