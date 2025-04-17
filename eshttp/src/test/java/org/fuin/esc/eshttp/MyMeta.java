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

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SerializedDataType;

import jakarta.annotation.Nullable;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Example meta data. .
 */
@XmlRootElement(name = MyMeta.EL_ROOT_NAME)
public final class MyMeta implements Serializable {

    private static final long serialVersionUID = 100L;

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "MyMeta"; 
    
    /** Unique name of the event. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique serialization name of the event. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(EL_ROOT_NAME);
    
    @JsonbProperty("user")
    @XmlElement(name = "user")
    private String user;

    /**
     * Protected default constructor for JAXB.
     */
    protected MyMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param user
     *            User ID.
     */
    public MyMeta(@Nullable final String user) {
        super();
        this.user = user;
    }

    /**
     * Returns the user.
     * 
     * @return User ID.
     */
    public final String getUser() {
        return user;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MyMeta other = (MyMeta) obj;
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public final String toString() {
        return "My meta: " + user;
    }

}
