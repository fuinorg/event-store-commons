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
package org.fuin.esc.test;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Example event.
 */
@XmlRootElement(name = "book-added-event")
public class BookAddedEvent {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String author;

    /**
     * Protected default constructor for deserialization.
     */
    protected BookAddedEvent() {
        super();
    }

    /**
     * Constructor with name and author.
     * 
     * @param name Name.
     * @param author Author.
     */
    public BookAddedEvent(final String name, final String author) {
        super();
        this.name = name;
        this.author = author;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * @return the author
     */
    public final String getAuthor() {
        return author;
    }

}
