package io.quarkiverse.logback.runtime.events;

import org.xml.sax.Locator;

public class EndSub extends EventSub {
    public EndSub(String namespaceURI, String localName, String qName, Locator locator) {
        super(namespaceURI, localName, qName, locator);
    }

    public EndSub() {
    }
}
