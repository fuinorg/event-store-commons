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
package org.fuin.esc.jaxb;

import com.tngtech.archunit.junit.ArchIgnore;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;

import java.io.Serializable;

/**
 * Example meta data. .
 */
@ArchIgnore
@XmlRootElement(name = "MyMeta")
public final class MyMeta implements Serializable {

    private static final long serialVersionUID = 100L;

    /**
     * Unique name of the meta type.
     */
    public static final TypeName TYPE = new TypeName("MyMeta");

    /**
     * Unique name of the serialized meta type.
     */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

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
     * @param user User ID.
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


    @Override
    public final String toString() {
        return "My meta: " + user;
    }

}
