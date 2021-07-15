package io.quarkiverse.logback.runtime.events;

import org.xml.sax.Locator;

public class EventSub {

    public String namespaceURI;
    public String localName;
    public String qName;
    public Locator locator;

    public EventSub(String namespaceURI, String localName, String qName, Locator locator) {
        this.namespaceURI = namespaceURI;
        this.localName = localName;
        this.qName = qName;
        this.locator = locator;
    }

    public EventSub() {
    }
}
