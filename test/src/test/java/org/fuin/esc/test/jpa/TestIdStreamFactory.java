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

import org.fuin.esc.api.StreamId;
import org.fuin.esc.jpa.JpaIdStreamFactory;
import org.fuin.esc.jpa.JpaStream;

/**
 * Creates a simple stream based on an identifer.
 */
public final class TestIdStreamFactory implements JpaIdStreamFactory {

    @Override
    public boolean containsType(final StreamId streamId) {
        // For test always true
        return true;
    }

    @Override
    public JpaStream createStream(final StreamId streamId) {
        if (streamId.getName().equals("AppendSingleAgain")) {
            return new AppendSingleAgainStream();
        }
        if (streamId.getName().equals("AppendMultipleAgain")) {
            return new AppendSingleAgainStream();
        }
        throw new IllegalArgumentException("Unknown stream: " + streamId.getName());
    }

}
