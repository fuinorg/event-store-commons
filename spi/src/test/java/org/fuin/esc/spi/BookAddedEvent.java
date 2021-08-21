// CHECKSTYLE:OFF
package org.fuin.esc.spi;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.SerializedDataType;

/**
 * Example event.
 */
@XmlRootElement(name = "book-added-event")
public class BookAddedEvent {

    /** Never changing unique event type name. */
    public static final TypeName TYPE = new TypeName("BookAddedEvent");
    
    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement
    private String name;

    @XmlElement
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
     * @param name
     *            Name.
     * @param author
     *            Author.
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

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((author == null) ? 0 : author.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BookAddedEvent other = (BookAddedEvent) obj;
        if (author == null) {
            if (other.author != null) {
                return false;
            }
        } else if (!author.equals(other.author)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public final String toString() {
        return "BookAddedEvent [name=" + name + ", author=" + author + "]";
    }

}
// CHECKSTYLE:ON
