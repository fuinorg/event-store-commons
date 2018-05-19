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
package org.fuin.esc.api;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotEmpty;
import org.fuin.objects4j.vo.AbstractStringValueObject;

/**
 * Name that uniquely identifies a type of data.
 */
@Immutable
public final class TypeName extends AbstractStringValueObject {

    private static final long serialVersionUID = 811127657088134517L;

    @NotNull
    private String value;

    /**
     * Protected default constructor for deserialization.
     */
    protected TypeName() {
        super();
    }

    /**
     * Constructor with unique type name.
     * 
     * @param value
     *            Type name.
     */
    public TypeName(@NotEmpty final String value) {
        super();
        Contract.requireArgNotEmpty("value", value);
        this.value = value;
    }

    @Override
    public final String asBaseType() {
        return value;
    }

    @Override
    public final String toString() {
        return asBaseType();
    }

}
