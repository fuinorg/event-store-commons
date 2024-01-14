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

import jakarta.json.JsonStructure;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.ToJsonCapable;
import org.fuin.objects4j.common.Contract;

/**
 * A structure that wraps another object of different types.
 */
@XmlRootElement(name = DataWrapper.EL_ROOT)
public final class DataWrapper implements ToJsonCapable {

    /** Unique name of the type. */
    protected static final String EL_ROOT = "Wrapper";

    @XmlAnyElement(lax = true)
    private Object obj;

    /**
     * Default constructor for JAXB.
     */
    protected DataWrapper() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param obj
     *            Object to wrap.
     */
    public DataWrapper(@NotNull final Object obj) {
        super();
        Contract.requireArgNotNull("obj", obj);
        this.obj = obj;
    }

    /**
     * Returns the wrapped object.
     * 
     * @return Inner object.
     */
    @NotNull
    public final Object getObj() {
        return obj;
    }

    @Override
    public final JsonStructure toJson() {
        if (obj instanceof ToJsonCapable) {
            return ((ToJsonCapable) obj).toJson();
        } else if (obj instanceof JsonStructure) {
            return (JsonStructure) obj;
        }
        throw new IllegalStateException("Wrapped object is not an instance of '" + ToJsonCapable.class.getSimpleName()
                + "' or 'JsonStructure': " + obj + " [" + obj.getClass().getName() + "]");
    }

}
