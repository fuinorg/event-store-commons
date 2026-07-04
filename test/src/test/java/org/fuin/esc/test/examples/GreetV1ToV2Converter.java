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
package org.fuin.esc.test.examples;

import org.fuin.esc.api.Converter;

/**
 * Up-casts a stored {@link GreetV1} greet event to the latest {@link GreetV2} representation by rendering the
 * greeting from the raw name.
 */
public class GreetV1ToV2Converter implements Converter<GreetV1, GreetV2> {

    @Override
    public Class<GreetV1> getSourceType() {
        return GreetV1.class;
    }

    @Override
    public Class<GreetV2> getTargetType() {
        return GreetV2.class;
    }

    @Override
    public GreetV2 convert(final GreetV1 source) {
        return new GreetV2("Hello, " + source.getName());
    }

}
