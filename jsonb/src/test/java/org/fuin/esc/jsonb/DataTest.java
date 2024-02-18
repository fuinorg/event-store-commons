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
package org.fuin.esc.jsonb;

import org.fuin.esc.api.EnhancedMimeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Data} class.
 */
public class DataTest {

    @Test
    public void testCreate() {

        final Data testee = new Data("MyEvent", EnhancedMimeType
                .create("text/plain; version=1; encoding=utf-8"), "Hello, world!");

        assertThat(testee.getType()).isEqualTo("MyEvent");
        assertThat(testee.getMimeType()).isEqualTo(EnhancedMimeType
                .create("text/plain; version=1; encoding=utf-8"));
        assertThat(testee.getContent()).isEqualTo("Hello, world!");

    }

}

