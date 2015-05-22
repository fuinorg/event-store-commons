/**
 * Copyright (C) 2015 Future Invent Informationsmanagement GmbH. All rights
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
package org.fuin.esc.spi;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.NotEmpty;
import org.fuin.objects4j.vo.AbstractStringValueObject;

/**
 * A combination of a unique type name and it's version.
 */
@Immutable
public final class TypeVersion extends AbstractStringValueObject {

    private static final long serialVersionUID = 1000L;

    private final String type;

    private final String version;

    /**
     * Constructor with all data.
     * 
     * @param type
     *            Unique type name.
     * @param version
     *            Version of the type.
     */
    public TypeVersion(@NotEmpty final String type,
            @NotEmpty final String version) {
        super();
        Contract.requireArgNotEmpty("type", type);
        Contract.requireArgNotEmpty("version", version);
        this.type = type;
        this.version = version;
    }

    /**
     * Returns the type.
     * 
     * @return Unique type name.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the version of the type.
     * 
     * @return The version
     */
    public final String getVersion() {
        return version;
    }

    @Override
    public final String asBaseType() {
        return type + ":" + version;
    }

    @Override
    public final String toString() {
        return asBaseType();
    }

}
