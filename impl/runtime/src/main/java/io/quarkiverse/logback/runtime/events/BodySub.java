package io.quarkiverse.logback.runtime.events;

import org.xml.sax.Locator;

public class BodySub extends EventSub {
    public String text;

    public BodySub(String namespaceURI, String localName, String qName, Locator locator, String text) {
        super(namespaceURI, localName, qName, locator);
        this.text = text;
    }

    public BodySub() {
    }
}
