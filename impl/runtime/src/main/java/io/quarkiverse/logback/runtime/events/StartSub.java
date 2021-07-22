package io.quarkiverse.logback.runtime.events;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Locator;

public class StartSub extends EventSub {
    public ArrayList<String> partList;
    public AttributesImpl attributes;

    public StartSub(String namespaceURI, String localName, String qName, Locator locator, AttributesImpl attributes,
            List<String> partList) {
        super(namespaceURI, localName, qName, locator);
        this.attributes = attributes;
        this.partList = new ArrayList<>(partList);
    }

    public StartSub() {
    }
}
