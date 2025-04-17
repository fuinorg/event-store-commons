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
package org.fuin.esc.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public final class ExpectedVersionTest {

    @Test
    public final void testValid() {

        // TEST + VERIFY
        assertThat(ExpectedVersion.valid("DOES_NOT_EXIST")).isFalse();
        assertThat(ExpectedVersion.valid(ExpectedVersion.ANY.name())).isTrue();

    }

    @Test
    public final void testNo() {

        // TEST + VERIFY
        assertThat(ExpectedVersion.no(null)).isEqualTo(ExpectedVersion.ANY.getNo());
        assertThat(ExpectedVersion.no(ExpectedVersion.NO_OR_EMPTY_STREAM.name())).isEqualTo(
                ExpectedVersion.NO_OR_EMPTY_STREAM.getNo());
        assertThat(ExpectedVersion.no("123")).isEqualTo(123);

    }

}

