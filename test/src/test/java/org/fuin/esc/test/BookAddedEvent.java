package org.fuin.esc.test;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "book-added-event")
public class BookAddedEvent {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String author;
    
    protected BookAddedEvent() {
        super();
    }

    public BookAddedEvent(String name, String author) {
        super();
        this.name = name;
        this.author = author;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }
    
}
